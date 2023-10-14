package dk.kb.netarchivesuite.solrwayback.solr;

import dk.kb.netarchivesuite.solrwayback.util.CollectionUtils;
import dk.kb.netarchivesuite.solrwayback.util.SolrUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.CursorMarkParams;
import org.apache.solr.common.params.FacetParams;
import org.apache.solr.common.params.GroupParams;
import org.apache.solr.common.params.HighlightParams;
import org.apache.solr.common.params.StatsParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.apache.commons.lang3.StringUtils.join;

// TODO: Add support for redirects; needs to work with expandResources
// TODO: Add support for revisits; needs (W)ARC lookup and needs to work with expandResources
// TODO: Add optional graph traversal of JavaScript & CSS-includes with expandResources

/**
 * Cursormark based chunking search client allowing for arbitrary sized result sets.
 *
 * Use this by creating a {@link SRequest} and calling {@link #stream} or {@link #iterate}.
 *
 *
 * Sample calls below.
 *
 * Extract {@code id} for all documents matching a simple query
 * <pre>
 * SRequest request = SRequest.builder().
 *     query("kittens").
 *     fields("id");
 * List<String> ids = SolrGenericStreaming.stream(request).
 *     map(d -> d.getFieldValue("id").toString()).
 *     collect(Collectors.toList());
 * </pre>
 *
 * Request all source-path and offsets for images matching the query {@code kittens},
 * with de-duplication on {@code url}and with the images closest to the time {@code 2019-04-15T12:31:51Z}:
 * <pre>
 * SRequest request = SRequest.builder().
 *     query("kitten").
 *     filterQueries("content_type_norm:image").
 *     fields("source_file_path", "source_file_offset").
 *     timeProximityDeduplication("2019-04-15T12:31:51Z", "url"));
 * List<SolrDocument> sources = SolrGenericStreaming.stream(request).collect(Collectors.toList());
 * </pre>
 *
 * Request all url_norms, source-paths and offsets for all pages about {@code kittens}, including embedded images,
 * JavaScript and CSS.
 * With de-duplication on page {@code url} and with the pages closest to the time {@code 2019-04-15T12:31:51Z}.
 * Furthermore ensure that all resources are unique. Note: The requirement for uniqueness imposes memory overhead and
 * therefore a limit in result size.
 * <pre>
 * SRequest request = SRequest.builder().
 *     query("kitten").
 *     filterQueries("content_type_norm:html").
 *     fields("url_norm", "source_file_path", "source_file_offset").
 *     timeProximityDeduplication("2019-04-15T12:31:51Z", "url").
 *     expandResources(true).
 *     ensureUnique(true));
 * List<SolrDocument> allUnique = SolrGenericStreaming.stream(request).collect(Collectors.toList());
 * </pre>
 * 
 * For most use cases, the state of the {@code SolrGenericStreaming} instance is irrelevant. If the Stream of
 * {@code Solrdocument}s is the only result needed, the {@link SRequest#stream()} shorthand can be used:
 * <pre>
 *     Stream<Solrdocuments> docs = SRequest.builder().query("foo").fields("url_norm).stream();
 * </pre>
 */
public class SolrGenericStreaming implements Iterable<SolrDocument> {
  private static final Logger log = LoggerFactory.getLogger(SolrGenericStreaming.class);

  /**
   * Default page size (rows) for the cursormark paging.
   */
  public static final int DEFAULT_PAGESIZE = 1000;
  /**
   * Used for batching multiple queries. Newer Solrs (at least 9+) share a default upper limit of 1024 boolean clauses
   * recursively in the user issued query tree.
   */
  public static final int DEFAULT_QUERY_BATCHSIZE = 200;

  /**
   * Magic value for signalling that there are no more pages.
   */
  public static final String STOP_PAGING = "___STOP_PAGING___";

  static final AtomicLong solrRequests = new AtomicLong(0);
  static final AtomicLong totalDelivered = new AtomicLong(0);

  private final SRequest request;
  private final SolrQuery originalSolrQuery; // The original SolrQuery from the SRequest. Never modify this!
  private SolrQuery solrQuery;               // Deep copy of originalSolrQuery. Potentially modified with e.g. q and cursorMark
  private final Iterator<String> queries;    // Only defined if the request is multi-query
  private boolean queryDepleted;             // Whether or not all documents has been resolved for the current q
  private String lastDeduplicateValue = null; // Used for simulated cursorMark
  private String userQuery = null;            // Used for simulated cursorMark

  private final List<String> adjustedFields; // Fields after adjusting for unique etc.

  private int delivered = 0;
  private SolrDocumentList undelivered = null; // Leftover form previous call to keep deliveries below pageSize

  private Object lastStreamDeduplicateValue = null; // Used with timeProximity

  private boolean hasFinished = false;

  /**
   * The default SolrClient is simple and non-caching as streaming exports typically makes unique requests.
   * Shared with {@link NetarchiveSolrClient}.
   */
  static SolrClient defaultSolrClient = NetarchiveSolrClient.noCacheSolrServer;

  /**
   * Generic stream where all parts except {@link SRequest#query(String)} and {@link SRequest#fields(String...)}
   * are optional.
   * <p>
   * Note: This always uses a collection oriented procedure. It is recommended to use
   * {@link SolrStreamShard#streamStrategy} instead as that allows for the faster shard division strategy.
   * @param request stream setup.
   * @return a stream of {@code SolrDocument}s, as specified in the {@code request}.
   * @see SolrStreamShard#streamStrategy(SRequest)
   */
  public static Stream<SolrDocument> stream(SRequest request) throws IllegalArgumentException {
    SolrGenericStreaming base = new SolrGenericStreaming(request);
    return SolrStreamDecorators.addPostProcessors(base.stream(), base.request, String.join(",", base.adjustedFields));
  }

  /**
   * Generic delivery of Solr documents where all parts except {@link SRequest#query(String)} and
   * {@link SRequest#fields(String...)} are optional.
   * <p>
   * Note: This always uses a collection oriented procedure. It is recommended to use
   * {@link SolrStreamShard#iterateStrategy} instead as that allows for the faster shard division strategy.
   * @param request stream setup.
   * @return an iterator of {@code SolrDocument}s, as specified in the {@code request}.
   * @see SolrStreamShard#iterateStrategy(SRequest)
   */
  public static Iterator<SolrDocument> iterate(SRequest request) throws IllegalArgumentException {
    SolrGenericStreaming base = new SolrGenericStreaming(request);
    return SolrStreamDecorators.addPostProcessors(base.iterator(), base.request, String.join(",", base.adjustedFields));
  }

  /**
   * Export the documents matching query and filterQueries with no limit on result size.
   * {@link #defaultSolrClient} will be used for the requests.
   * <p>
   * Note: This always uses a collection oriented procedure. It is recommended to use
   * {@link SolrStreamShard#streamStrategy} instead as that allows for the faster shard division strategy.
   * Default page size 1000, expandResources=false and ensureUnique=false.
   * @param fields        the fields to export.
   * @param query         standard Solr query.
   * @param filterQueries optional Solr filter queries. For performance, 0 or 1 filter query is recommended.
   *                      If multiple filters are to be used, consider collapsing them into one:
   *                      {@code ["foo", "bar"]} → {@code ["(foo) AND (bar)"]}.
   * @return a stream of {@code SolrDocment}s with the requested fields, satisfying the given query ande filter queries.
   * @see SolrStreamShard#streamStrategy(SRequest)
   */
  public static Stream<SolrDocument> stream(List<String> fields, String query, String... filterQueries) {
    return stream(SRequest.create(query, fields).filterQueries(filterQueries));
  }

  /**
   * Export the documents matching query and filterQueries with no limit on result size.
   * {@link #defaultSolrClient} will be used for the requests.
   *
   * Default page size 1000, expandResources=false and ensureUnique=false.
   * <p>
   * Note: This always uses a collection oriented procedure. It is recommended to use
   * {@link SolrStreamShard#iterateStrategy} instead as that allows for the faster shard division strategy.
   * @param fields        the fields to export.
   * @param query         standard Solr query.
   * @param filterQueries optional Solr filter queries. For performance, 0 or 1 filter query is recommended.
   *                      If multiple filters are to be used, consider collapsing them into one:
   *                      {@code ["foo", "bar"]} → {@code ["(foo) AND (bar)"]}.
   * @return an iterator of {@code SolrDocment}s with the requested fields, satisfying the given query ande filter queries.
   * @see SolrStreamShard#iterateSharded(SRequest, List)
   */
  public static Iterator<SolrDocument> iterate(List<String> fields, String query, String... filterQueries) {
    return iterate(SRequest.create(query, fields).filterQueries(filterQueries).shardDivide("never"));
  }

  /**
   * Generic stream where all parts except {@link SRequest#query(String)} and {@link SRequest#fields(String...)}
   * are optional.
   * <p>
   * Note: This is the "raw" stream where post-processors such as ensure uniqueness and expand resources are not added.
   * @param request stream setup.
   */
  protected SolrGenericStreaming(SRequest request) {
    this.request = request;
    originalSolrQuery = request.getMergedSolrQuery();
    solrQuery = SolrUtils.deepCopy(originalSolrQuery);
    queryDepleted = request.isMultiQuery(); // Single query starts assigned, multi are initialized later
    userQuery = request.isMultiQuery() ? null : solrQuery.getQuery();

    adjustSolrQuery(solrQuery, request.expandResources, request.ensureUnique, request.deduplicateField);
    optimizeSolrQuery(solrQuery, request);
    this.adjustedFields = Arrays.asList(solrQuery.getFields().split(","));

    queries = request.queries == null ? null:
            CollectionUtils.splitToLists(request.queries, request.queryBatchSize).
                    map(batch -> "(" + String.join(") OR (", batch) + ")").
                    iterator(); // 1 big OR query
  }

  /**
   * Adjust the given solrQuery so that it will work for streaming processing.
   * This is called automatically when using {@link SRequest}s.
   *
   * If {@code fl} is not already set in solrQuery it will be set to {@code source_file_path,source_file_offset}.
   * If {@code cursorMark} is not already set in solrQuery it will be set to {@code *}.
   * If {@code rows} is not already set in solrQuery it will be set to {@link #DEFAULT_PAGESIZE}.
   * If expandResources is true and {@code fl} in solrQuery does not contain {@code content_type_norm},
   * {code source_file_path} and {@code source_file_offset} they will be added.
   * If ensureUnique is true and {@code fl} in solrQuery does not contain the field {@code id} it will be added.
   * If deduplicateField is specified and {@code fl} in solrQuery does not already contain the field it will be added.
   * {@code facets}, {@code stats} and {@code hl} will always be set to false, no matter their initial value.
   * If uniqueFields are specified, they are added to fields.
   *
   * @param solrQuery a standard solrQuery.
   * @param expandResources  if true, embedded resources for HTML pages are extracted and added to the delivered
   *                         lists of Solr Documents.
   *                         Note: Indirect references (through JavaScript & CSS) are not followed.
   * @param ensureUnique     if true, unique documents are guaranteed. This is only sane if expandResources is true.
   *                         Note that a HashSet is created to keep track of encountered documents and will impose
   *                         a memory overhead linear to the number of results.
   * @param deduplicateField if not null, the value for the given field for a document will be compared to the value
     *                         for the previous document. If they are equal, the current document will be skipped.
   * @throws IllegalArgumentException if solrQuery has {@code group=true}.
   */
  // TODO: Deprecate this in favor of SRequest methods
  public static void adjustSolrQuery(
          SolrQuery solrQuery, boolean expandResources, boolean ensureUnique, String deduplicateField) {
    if (solrQuery.getBool(GroupParams.GROUP, false)) {
      throw new IllegalArgumentException("group==true is not compatible with cursorMark paging");
    }

    // Properties defined parameters
    SolrUtils.setSolrParams(solrQuery);

    // TODO: Replace fl handling with SRequest.getExpandedFieldList
    // Set default values if not already set
    solrQuery.set(CommonParams.FL,
                  solrQuery.get(CommonParams.FL, "source_file_path,source_file_offset"));
    solrQuery.set(CursorMarkParams.CURSOR_MARK_PARAM,
                  solrQuery.get(CursorMarkParams.CURSOR_MARK_PARAM, CursorMarkParams.CURSOR_MARK_START));
    solrQuery.set(CommonParams.ROWS,
                  solrQuery.get(CommonParams.ROWS, Integer.toString(DEFAULT_PAGESIZE)));

    // Adjust fl based on enabled features
    Set<String> fl = new LinkedHashSet<>(Arrays.asList(solrQuery.get(CommonParams.FL).split(", *")));
    if (expandResources) {
      fl.add("content_type_norm");  // Needed to determine if a resource is a webpage
      fl.add("source_file_path");   // Needed to fetch the webpage for link extraction
      fl.add("source_file_offset"); // Needed to fetch the webpage for link extraction
    }
    if (expandResources && ensureUnique) {
      fl.add("id"); // id is shorter than sourcefile@offset in webarchive-discovery compatible indexes
    }
    if (deduplicateField != null) {
      fl.add(deduplicateField);
    }
    solrQuery.set(CommonParams.FL, String.join(",", fl));

    // Disable irrelevant processing
    solrQuery.set(FacetParams.FACET, false);
    solrQuery.set(StatsParams.STATS, false);
    solrQuery.set(HighlightParams.HIGHLIGHT, false);
  }

  /**
   * Optimizes the query for faster deduplication, is possible.
   * Important: This must be called AFTER {@link #adjustSolrQuery(SolrQuery, boolean, boolean, String)}.
   * @param solrQuery a Solrquery that has been through {@link #adjustSolrQuery(SolrQuery, boolean, boolean, String)}.
   * @param request the request responsible for the solrQuery.
   */
  private void optimizeSolrQuery(SolrQuery solrQuery, SRequest request) {
    if (request.deduplicateField == null) { // No need for grouping
      return;
    }

    // Check for already existing cursorMark
    String cursorMark = solrQuery.get(CursorMarkParams.CURSOR_MARK_PARAM, CursorMarkParams.CURSOR_MARK_START);
    if (!CursorMarkParams.CURSOR_MARK_START.equals(cursorMark)) {
      log.debug("Unable to enable group-based deduplication on '{}' as cursorMark is defined to '{}'",
                request.deduplicateField, cursorMark);
      // TODO: Should this throw an Exception instead?
      return;
    }

    //log.debug("Enabling group-based deduplication on '{}'", request.deduplicateField);
    // group=true&group.field=url_norm&group.limit=1&group.format=simple&group.main=true
    solrQuery.set(GroupParams.GROUP, true);
    solrQuery.set(GroupParams.GROUP_FIELD, request.deduplicateField);
    solrQuery.set(GroupParams.GROUP_LIMIT, 1);

    // Make grouping return the first (and only) document from each group flattened as a standard search result
    solrQuery.set(GroupParams.GROUP_FORMAT, "simple");
    solrQuery.set(GroupParams.GROUP_MAIN, true);

    // Remove cursorMark if it is present
    solrQuery.remove(CursorMarkParams.CURSOR_MARK_PARAM);
  }

  /**
   * Constructs a "raw" Stream for {@link SolrDocument}s. In order for the end result to conform to
   * the properties in the {@link SRequest} used to construct this {@code SolrGenericStreaming}, it is
   * necessary to use {@link SolrStreamDecorators#addPostProcessors(Stream, SRequest, String)}.
   * @return a stream of SolrDocuments.
   */
  public Stream<SolrDocument> stream() {
    return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator(), 0), false);
  }

  /**
   * Constructs a "raw" iterator for {@link SolrDocument}s. In order for the end result to conform to
   * the properties in the {@link SRequest} used to construct this {@code SolrGenericStreaming}, it is
   * necessary to use {@link SolrStreamDecorators#addPostProcessors(Iterator, SRequest, String)}.
   * @return an iterator of SolrDocuments.
   */
  @Override
  public Iterator<SolrDocument> iterator() {
    return new Iterator<SolrDocument>() {
      SolrDocumentList list = null;
      int index = 0;

      @Override
      public boolean hasNext() {
        // Request new list if it is depleted and there are more document lists available
        if ((list == null || index == list.size()) && !hasFinished()) {
          try {
            list = nextDocuments();
            index = 0;
          } catch (Exception e) {
            throw new RuntimeException("Exception requesting next batch", e);
          }
        }
        // Remove list if it is depleted
        if (list != null && index == list.size()) {
          list = null;
        }
        return list != null;
      }

      @Override
      public SolrDocument next() {
        if (!hasNext()) {
          throw new NoSuchElementException("No more elements");
        }
        return list.get(index++);
      }
    };
  }

  /**
   * @param defaultSolrClient the SolrClient that will be used if no SolrClient is provided for the calls.
   */
  public static void setDefaultSolrClient(SolrClient defaultSolrClient) {
    SolrGenericStreaming.defaultSolrClient = defaultSolrClient;
  }

  /**
   * @return at least 1 and at most {@link SRequest#pageSize} documents or null if there are no more documents.
   *         Call {@link #hasFinished()} to see if more document lists are available.
   * @throws SolrServerException if Solr could not handle a request for new documents.
   * @throws IOException if general communication with Solr failed.
   */
  protected SolrDocumentList nextDocuments() throws SolrServerException, IOException {
    while (!hasFinished && delivered < request.maxResults) {

      // Return batch if undelivered contains any documents
      if (undelivered != null && undelivered.size() > 0) {
        return nextPageUndelivered();
      }

      // Move to next query is needed
      if (queryDepleted) {
        if (request.isMultiQuery() && queries.hasNext()) {
          userQuery = queries.next();
          if (userQuery.isEmpty()) {
            continue;
          }
          solrQuery.setQuery(userQuery);
          queryDepleted = false;
          if (request.usePaging) {
            if (request.deduplicateField == null) { // Plain paging initialization
              solrQuery.set(
                      CursorMarkParams.CURSOR_MARK_PARAM,
                      originalSolrQuery.get(CursorMarkParams.CURSOR_MARK_PARAM, CursorMarkParams.CURSOR_MARK_START));
            } else { // Simulated cursorMark reset
              lastStreamDeduplicateValue = null;
            }
          }
        } else {
          hasFinished = true;
          return null;
        }
      }

      // Handle paging (cursorMark) setup
      String cursorMark = solrQuery.get(CursorMarkParams.CURSOR_MARK_PARAM, CursorMarkParams.CURSOR_MARK_START);
      if (request.usePaging) {
        if (request.deduplicateField == null) { // Plain cursorMark
          solrQuery.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);
        } else if (lastStreamDeduplicateValue == null) { // Simulated cursorMark initial call
          solrQuery.setQuery(userQuery);
        } else {  // Simulated cursorMark subsequent call
          solrQuery.setQuery(String.format(
                  Locale.ROOT, "(%s) AND %s:{%s TO *]", // Range query with non-inclusive start and open end
                  userQuery, request.deduplicateField, SolrUtils.createPhrase(lastDeduplicateValue)));
        }
      }

      // Perform request and update depleted & paging variables
      solrRequests.incrementAndGet();
      //log.debug("Issuing '{}'", SolrUtils.fieldValueToString(solrQuery));

      QueryResponse rsp;
      long st = System.currentTimeMillis();
      try {
        rsp = request.collection == null ?
                request.solrClient.query(solrQuery, METHOD.POST) :
                request.solrClient.query(request.getCollectionGuaranteed(), solrQuery, METHOD.POST);
      } catch (HttpSolrClient.RemoteSolrException e) {
        log.warn("RemoteSolrException for POST request to collection '" + request.getCollectionGuaranteed() + "': " +
                 SolrUtils.fieldValueToString(solrQuery), e);
        throw e;
      }         ;
      undelivered = rsp.getResults();
      totalDelivered.addAndGet(undelivered.size());
      log.debug("Got " + undelivered.size() + " hits with total delivered counter " + totalDelivered.get() + " in " + (System.currentTimeMillis()-st) + " ms");
      if (undelivered.size() < solrQuery.getRows() || rsp.getResults().getNumFound() <= solrQuery.getRows()) {
        queryDepleted = true;
      }
      if (request.usePaging && request.deduplicateField != null && !undelivered.isEmpty()) {
        lastDeduplicateValue = SolrUtils.fieldValueToString(
                undelivered.get(undelivered.size()-1).getFieldValue(request.deduplicateField));
      }

      // Reduce to maxResults
      while (request.maxResults-delivered < undelivered.size() && !undelivered.isEmpty()) {
        undelivered.remove(undelivered.size()-1);
      }

      // Prepare for next page
      if (!request.usePaging) { // No paging
        queryDepleted = true;
      } else if (request.deduplicateField == null) { // Plain cursorMark
        String newCursormark = rsp.getNextCursorMark();
        if (newCursormark.equals(cursorMark)) { // No more documents for this query
          queryDepleted = true;
        } else {
          solrQuery.set(CursorMarkParams.CURSOR_MARK_PARAM, newCursormark);
        }
      } // Simulated cursorMark is handled right after the raw response from the query

      // Loop as deduplication & unique might mean that the current batch is empty
    }
    return null; // Finished and no more documents
  }

  /**
   * If there are more than {@link SRequest#pageSize} documents in {@link #undelivered}, exactly pageSize documents
   * are returned and the rest are kept in {@link #undelivered}. Else the full amount of documents in
   * {@link #undelivered} is returned.
   */
  private SolrDocumentList nextPageUndelivered() {
    if (undelivered == null || undelivered.size() < request.pageSize) {
      SolrDocumentList oldUndelivered = undelivered;
      undelivered = null;
      delivered += oldUndelivered.size();
      return oldUndelivered;
    }

    SolrDocumentList batch = new SolrDocumentList();
    batch.addAll(undelivered.subList(0, request.pageSize));

    SolrDocumentList newUndelivered = new SolrDocumentList();
    newUndelivered.addAll(undelivered.subList(request.pageSize, undelivered.size()));
    undelivered = newUndelivered;
    delivered += batch.size();

    return batch;
  }

  public boolean hasFinished() {
    return hasFinished;
  }

}
