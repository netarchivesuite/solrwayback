package dk.kb.netarchivesuite.solrwayback.solr;

import dk.kb.netarchivesuite.solrwayback.parsers.ArcParserFileResolver;
import dk.kb.netarchivesuite.solrwayback.parsers.HtmlParserUrlRewriter;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;
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
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.apache.commons.lang3.StringUtils.join;

// TODO: Add support for redirects; needs to work with expandResources
// TODO: Add support for revisits; needs (W)ARC lookup and needs to work with expandResources
// TODO: Add optional graph traversal of JavaScript & CSS-includes with expandResources
// TODO: Avoid extra paging request by looking at numFound and counting received documents

/**
 * Cursormark based chunking search client allowing for arbitrary sized result sets.
 *
 * Use this by creating a {@link SRequest} and calling {@link #create(SRequest)}.
 *
 * Results can be retrieved in chunks with {@link #nextDocuments()}, one at a time with {@link #iterator()} or
 * streaming with {@link #stream()}.
 *
 *
 * Sample calls below.
 *
 * Extract {@code id} for all documents matching a simple query
 * <pre>
 * List<String> ids = SolrGenericStreaming.create(
 *                 SolrGenericStreaming.SRequest.builder().
 *                         query("kittens").
 *                         fields("id")).
 *         stream().
 *         map(d -> d.getFieldValue("id").toString()).
 *         collect(Collectors.toList());
 * </pre>
 *
 * Get all source-path and offsets for images matching the query {@code kittens}, with de-duplication on {@code url}
 * and with the images closest to the time {@code 2019-04-15T12:31:51Z}:
 * <pre>
 * SolrGenericStreaming.create(SolrGenericStreaming.SRequest.builder().
 *     query("kitten").
 *     filterQueries("content_type_norm:image").
 *     fields("source_file_path", "source_file_offset").
 *     timeProximityDeduplication("2019-04-15T12:31:51Z", "url"))...
 * </pre>
 *
 * Get all url_norms, source-paths and offsets for all pages about {@code kittens}, including embedded images,
 * JavaScript and CSS.
 * With de-duplication on page {@code url} and with the pages closest to the time {@code 2019-04-15T12:31:51Z}.
 * Furthermore ensure that all resources are unique. Note: The requirement for uniqueness imposes memory overhead and
 * a limit in result size.
 * <pre>
 *     SolrGenericStreaming.create(SolrGenericStreaming.SRequest.builder().
 *         query("kitten").
 *         filterQueries("content_type_norm:html").
 *         fields("url_norm", "source_file_path", "source_file_offset").
 *         timeProximityDeduplication("2019-04-15T12:31:51Z", "url").
 *         expandResources(true).
 *         ensureUnique(true))...
 * </pre>
 */
public class SolrGenericStreaming implements Iterable<SolrDocument> {
  private static final Logger log = LoggerFactory.getLogger(SolrGenericStreaming.class);

  /**
   * Default page size (rows) for the cursormark paging.
   */
  public static final int DEFAULT_PAGESIZE = 1000;

  /**
   * Default sort used when exporting. Ends with tie breaking on id.
   */
  public static final String DEFAULT_SORT = "score desc, id asc";

  /**
   * Default maximum number of elements when the request requires unique results.
   * If this limit is exceeded during processing, an exception is thrown.
   * The uniquifier uses a HashSet (which is a Map underneath the hood) for tracking
   * unique values. Each entry takes up about ~150 bytes plus the value itself, so
   * something like 250 bytes/entry as a rule of thumb. The default MAX_UNIQUE is thus
   * about 1.25GB of maximum heap.
   */
  public static final int DEFAULT_MAX_UNIQUE = 5_000_000;
  /**
   * Magic value for signalling that there are no more pages.
   */
  public static final String STOP_PAGING = "___STOP_PAGING___";

  /**
   * Solr ISO timestamp parsing. Supports optional milliseconds.
   * Sample inputs: {@code 2022-09-26T12:05:00Z}, {@code 2022-09-26T12:05:00.123Z}.
   */
  private static final Pattern ISO_TIME = Pattern.compile(
          "[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]T[012][0-9]:[0-5][0-9]:[0-5][0-9][.]?[0-9]?[0-9]?[0-9]?Z");

  private final SRequest request;
  private final SolrQuery solrQuery; // Constructed from request

  private final List<String> initialFields;  // Caller provided fields
  private final List<String> adjustedFields; // Fields after adjusting for unique etc.

  private final Set<String> uniqueTracker;
  private int delivered = 0;
  private long duplicatesRemoved = 0;
  private SolrDocumentList undelivered = null; // Leftover form previous call to keep deliveries below pageSize

  private Object lastStreamDeduplicateValue = null; // Used with timeProximity

  private boolean hasFinished = false;

  /**
   * The default SolrClient is simple and non-caching as streaming exports typically makes unique requests.
   */
  private static SolrClient defaultSolrClient = new HttpSolrClient.Builder(PropertiesLoader.SOLR_SERVER).build();


  /**
   * Generic stream where all parts except {@link SRequest#query(String)} and {@link SRequest#fields(String...)}
   * are optional.
   *
   * @param request stream setup.
   * @return an instance of SolrGenericStreaming, ready for use.
   */
  public static SolrGenericStreaming create(SRequest request) throws IllegalArgumentException {
    return new SolrGenericStreaming(request);
  }

  /**
   * Export the documents matching query and filterQueries with no limit on result size.
   * {@link #defaultSolrClient} will be used for the requests.
   *
   * Default page size 1000, expandResources=false and ensureUnique=false.
   * @param fields        the fields to export.
   * @param query         standard Solr query.
   * @param filterQueries optional Solr filter queries. For performance, 0 or 1 filter query is recommended.
   *                      If multiple filters are to be used, consider collapsing them into one:
   *                      {@code ["foo", "bar"]} → {@code ["(foo) AND (bar)"]}.
   * @return an instance of SolrGenericStreaming, ready for use.
   */
  public static SolrGenericStreaming create(List<String> fields, String query, String... filterQueries) {
    return create(SRequest.create(query, fields).filterQueries(filterQueries));
  }

  /**
   * Issues multiple queries as batch requests. The individual queries will be {@code OR}ed together:
   *
   * If {@code queries = Arrays.asList("url:http://example.com/foo", "url:http://example.com/bar").stream()},
   * the batch request will be {@code "url:http://example.com/foo OR url:http://example.com/bar"}.
   *
   * Each batch request is independent from previous requests so de-duplication and uniqueness is not guaranteed to
   * work well.
   *
   * Sample call: Get all SolrDocuments matching the queries {@code foo} and {@code bar}:
   * <pre>
   * Stream<SolrDocument> solrDocs =
   *   multiQuery(SRequest.builder().fields("id"),
   *              Stream.of("foo", "var"),
   *              10).
   *       flatMap(SolrGenericStreaming::stream);
   * </pre>
   * @param baseRequest the SRequest used as a base for each batch request: {@link SRequest#solrQuery} will be
   *                    overridden with the batch queries constructed from queries.
   * @param queries     the individual queries. These will be concatenated in batches with {@code " OR "} as separator.
   * @param batchSize   the number of queries to use for each batch. Setting this above 1000 is not likely to work
   *                    due to Solr's default of {@code maxBooleanClauses = 1024}.
   * @return a stream of instances of {@link SolrGenericStreaming}.
   */
  public static Stream<SolrGenericStreaming> multiQuery(SRequest baseRequest, Stream<String> queries, int batchSize) {
    return CollectionUtils.splitToLists(queries, batchSize).
            map(batch -> "(" + String.join(") OR (", batch) + ")"). // 1 big OR query
            map(query -> baseRequest.deepCopy().query(query)).
            map(SolrGenericStreaming::create);
  }

  /**
   * Generic stream where all parts except {@link SRequest#query(String)} and {@link SRequest#fields(String...)}
   * are optional.
   *
   * @param request stream setup.
   */
  public SolrGenericStreaming(SRequest request) {
    this.request = request;
    solrQuery = request.getMergedSolrQuery();
    this.initialFields = Arrays.asList(solrQuery.getFields().split(","));

    adjustSolrQuery(solrQuery, request.expandResources, request.ensureUnique, request.deduplicateField);
    this.adjustedFields = Arrays.asList(solrQuery.getFields().split(","));

    this.uniqueTracker = request.ensureUnique ? new HashSet<>() : null;
  }

  /**
   * Adjust the given solrQuery so that it will work for streaming processing.
   *
   * If {@code fl} is not already set in solrQuery it will be set to {@code source_file_path,source_file_offset}.
   * If {@code cursorMark} is not already set in solrQuery it will be set to {@code *}.
   * If {@code rows} is not already set in solrQuery it will be set to {@link #DEFAULT_PAGESIZE}.
   * If {@code sort} is not already set in solrQuery it will be set to {@link #DEFAULT_SORT}.
   * If {@code sort} does not end with {@code id asc} or {@code id desc}, {@code id asc} will be appended.
   * If expandResources is true and {@code fl} in solrQuery does not contain {@code content_type_norm},
   * {code source_file_path} and {@code source_file_offset} they will be added.
   * If ensureUnique is true and {@code fl} in solrQuery does not contain the field {@code id} it will be added.
   * If deduplicateField is specified and {@code fl} in solrQuery does not already contain the field it will be added.
   * If deduplicateField is specified and {@code sort} does not already have it as primary sort field it will be added.
   * {@code facets}, {@code stats} and {@code hl} will always be set to false, no matter their initial value.
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
  public static void adjustSolrQuery(SolrQuery solrQuery,
                                     boolean expandResources, boolean ensureUnique, String deduplicateField) {
    if (solrQuery.getBool(GroupParams.GROUP, false)) {
      throw new IllegalArgumentException("group==true is not compatible with cursorMark paging");
    }

    // Properties defined parameters
    SolrUtils.setSolrParams(solrQuery);

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

    // Adjust sort to ensure presence of tie breaker and deduplication field (if enabled)
    String sort = solrQuery.get(CommonParams.SORT, DEFAULT_SORT);
    if (!(sort.endsWith("id asc") || sort.endsWith("id desc"))) {
      sort = sort + ", id asc"; // A tie breaker is needed when using cursormark
    }
    if (deduplicateField != null && !sort.startsWith(deduplicateField)) {
      solrQuery.set(CommonParams.SORT, deduplicateField + " asc, " + sort);
    }
    solrQuery.set(CommonParams.SORT, sort);

    // Disable irrelevant processing
    solrQuery.set(FacetParams.FACET, false);
    solrQuery.set(StatsParams.STATS, false);
    solrQuery.set(HighlightParams.HIGHLIGHT, false);
  }

  /**
   * Stream the Solr response one document at a time.
   * @return a stream of SolrDocuments.
   */
  public Stream<SolrDocument> stream() {
    return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator(), 0), false);
  }

  /**
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
   * Stream the Solr response one document list at a time.
   * @return a stream of lists of SolrDocuments.
   */
  public Stream<SolrDocumentList> streamList() {
    return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iteratorList(), 0), false);
  }

  /**
   * @return an iterator of SolrDocumentLists.
   */
  public Iterator<SolrDocumentList> iteratorList() {
    return new Iterator<SolrDocumentList>() {
      @Override
      public boolean hasNext() {
        return !hasFinished();
      }

      @Override
      public SolrDocumentList next() {
        if (!hasNext()) {
          throw new NoSuchElementException("No more elements");
        }
        try {
          return nextDocuments();
        } catch (Exception e) {
          throw new RuntimeException("Exception requesting next document list", e);
        }
      }
    };

  }

  /**
   * @return at least 1 and at most {@link SRequest#pageSize} documents or null if there are no more documents.
   *         Call {@link #hasFinished()} to see if more document lists are available.
   * @throws SolrServerException if Solr could not handle a request for new documents.
   * @throws IOException if general communication with Solr failed.
   */
  public SolrDocumentList nextDocuments() throws SolrServerException, IOException {
    String cursorMark = solrQuery.get(CursorMarkParams.CURSOR_MARK_PARAM, CursorMarkParams.CURSOR_MARK_START);
    while (!hasFinished && delivered < request.maxResults) {

      // Return batch if undelivered contains any documents
      if (undelivered != null && undelivered.size() > 0) {
        return nextPageUndelivered();
      }

      // Only page to next page if there are more pages
      if (STOP_PAGING.equals(cursorMark)) {
        hasFinished = true;
        return null;
      }

      // No more documents buffered, attempt to require new documents
      solrQuery.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);
      QueryResponse rsp = request.solrClient.query(solrQuery, METHOD.POST);
      undelivered = rsp.getResults();

      if (request.deduplicateField != null) {
        streamDeduplicate(undelivered);
      }
      if (request.expandResources) {
        expandResources(undelivered);
      }
      if (uniqueTracker != null) {
        removeDuplicates(undelivered);
      }
      // Reduce to maxResults
      while (request.maxResults-delivered < undelivered.size() && !undelivered.isEmpty()) {
        undelivered.remove(undelivered.size()-1);
      }

      // Only deliver the fields that were requested
      undelivered.forEach(this::reduceAndSortFields);

      // Has the last page been reached?
      String newCursormark = rsp.getNextCursorMark();
      if (newCursormark.equals(cursorMark)) { // No more documents
        cursorMark = STOP_PAGING;
      } else {
        cursorMark = newCursormark;
      }
      solrQuery.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);

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

  private void expandResources(SolrDocumentList documents) {
    int initialSize = documents.size();
    for (int i = 0 ; i < initialSize ; i++) {
      if ("html".equals(documents.get(i).getFieldValue("content_type_norm"))) {
        getHTMLResources(documents.get(i)).forEach(documents::add);
      }
    }
  }

  /**
   * Performs a lookup of a HTML resource, extracting links to embedded resources and issues searches for
   * those. Plain {@code <a href="..." ...>} links are not part of this. The graph traversal is only 1 level deep.
   * @param html a SolrDocument representing a HTML page.
   * @return the resources (images, CSS, JavaScript...) used by the page.
   */
  private Stream<SolrDocument> getHTMLResources(SolrDocument html) {
    try {
      String sourceFile = html.getFieldValue("source_file_path").toString();
      long offset = Long.parseLong(html.getFieldValue("source_file_offset").toString());
      ArcEntry arc= ArcParserFileResolver.getArcEntry(sourceFile, offset);
      HashSet<String> resources = HtmlParserUrlRewriter.getResourceLinksForHtmlFromArc(arc);

      return NetarchiveSolrClient.getInstance().findNearestDocuments(
              resources.stream(), arc.getCrawlDate(), join(adjustedFields, ","));
    } catch (Exception e) {
      log.warn("Unable to get resources for SolrDocument '" + html + "'", e);
      return Stream.of();
    }
  }

  public long getDuplicatesRemoveCount() {
    return duplicatesRemoved;
  }

  public boolean hasFinished() {
    return hasFinished;
  }

  private void removeDuplicates(SolrDocumentList documents) {
    List<SolrDocument> unique = new ArrayList<>(documents.size());

    // Important: We ensure that first version (in sort order) of duplicate entries win
    documents.forEach(doc -> {
      if (uniqueTracker.add(getID(doc))) {
        unique.add(doc);
      } else {
        duplicatesRemoved++;
      }
    });
    if (uniqueTracker.size() > request.maxUnique) {
      throw new ArrayIndexOutOfBoundsException(
              "The number of elements in the unique tracker exceeded the limit " + request.maxUnique +
              ". Processing has been stopped to avoid Out Of Memory errors");
    }
    documents.clear();
    documents.addAll(unique);
  }

  /**
   * Streaming deduplication where the incoming documents are expected to be in order.
   */
  private void streamDeduplicate(SolrDocumentList documents) {
    List<SolrDocument> unique = new ArrayList<>(documents.size());
    for (SolrDocument doc: documents) {
      if (lastStreamDeduplicateValue == null ||
          !lastStreamDeduplicateValue.equals(doc.getFieldValue(request.deduplicateField))) {
          lastStreamDeduplicateValue = doc.getFieldValue(request.deduplicateField);
          unique.add(doc);
      } else {
        duplicatesRemoved++;
      }
    }
    documents.clear();
    documents.addAll(unique);
  }

  /**
   * Remove all fields not explicitly requested by the caller and ensure the order matches the given field order.
   * @param doc a Solrdocuments that potentially holds more fields that defined by the caller.
   */
  private void reduceAndSortFields(SolrDocument doc) {
    Map<String, Object> entries = new LinkedHashMap<>(doc.size());
    for (String fieldName: initialFields) {
      if (doc.containsKey(fieldName)) {
        entries.put(fieldName, doc.get(fieldName));
      }
    }
    doc.clear();
    doc.putAll(entries);
  }

  private String getID(SolrDocument solrDocument) {
    return solrDocument.getFieldValue("id").toString();
    //return solrDocument.getFieldValue("source_file_path") + "@" +
    //       solrDocument.getFieldValue("source_file_offset");
  }

  /**
   * Takes a SolrDocument which has at most 1 multi-valued field and flattens that to multiple documents where the
   * multi-valued field has been converted to single-valued.
   *
   * Typically used for exporting to CSV where multi-value is not desirable.
   *
   * The following example delivers a list of documents with a single source and a single destination URL for each link
   * on each unique page on the kb.dk domain:
   * <pre>
   *   List<SolrDocument> docs = SolrGenericStreaming.timeProximity(
   *       Arrays.asList("url", "links"), false, false, 0, "2019-04-15T12:31:51Z", "hash", "domain:kb.dk").
   *       stream().
   *       flatMap(SolrGenericStreaming::flatten).
   *       collect(Collectors.toList());
   * </pre>
   *
   * @param doc a document with at most 1 multi-valued field.
   * @return the input document flattened to at least 1 documents holding only single-valued field.
   */
  public static Stream<SolrDocument> flatten(SolrDocument doc) {
    String multiField = null;
    for  (String fieldName: doc.getFieldNames()) {
      if (doc.getFieldValues(fieldName).size() > 1) {
        if (multiField != null) {
          throw new IllegalArgumentException(
                  "There are at least 2 multi-value fields '" + multiField + "' and '" + fieldName +
                  "' where at most 1 is allowed");
        }
        multiField = fieldName;
      }
    }

    // If there are no multi-valued field, just return the input document
    if (multiField == null) {
      return Stream.of(doc);
    }

    final String mField = multiField; // lambda requires final
    // Return a stream of documents where the multi-valued field has been flattened
    return doc.getFieldValues(mField).stream().
            map(value -> {
              SolrDocument newDoc = new SolrDocument(new LinkedHashMap<>(doc));
              newDoc.setField(mField, value);
              return newDoc;
            });

  }

  /**
   * Encapsulation of the request to SolrGenericStreaming. Care has been taken to ensure sane defaults; the only
   * required attributes are {@link #query(String)} and {@link #fields(String...)}.
   * 
   * Use as a builder: {@code SRequest.builder().query("*:*").fields("url", "url_norm")}
   * 
   * Note: If {@link SRequest#solrQuery(SolrQuery)} is specified it will be used as a base for the full request.
   */
  public static class SRequest {
    public SolrClient solrClient = defaultSolrClient;
    public SolrQuery solrQuery = new SolrQuery();
    public boolean expandResources = false;
    public boolean ensureUnique = false;
    public Integer maxUnique = DEFAULT_MAX_UNIQUE;
    private String idealTime; // If defined, a sort will be created as String.format(Locale.ROOT, "%s asc, abs(sub(ms(%s), crawl_date)) asc", deduplicateField, idealTime);
    public String deduplicateField = null;
    List<String> fields;
    public long maxResults = Long.MAX_VALUE;
    public String sort = DEFAULT_SORT;
    public String query = null;
    public List<String> filterQueries;
    public int pageSize = DEFAULT_PAGESIZE;

    /**
     * @return a fresh instance of SRequest intended for further adjustment.
     */
    public static SRequest builder() {
      return new SRequest();
    }

    /**
     * Creates a request with query and fields.
     * As the returned request is initialized with query and fields, it can be used without further adjustments.
     * @return an instance of SRequest, initialized with the provided query and fields.
     */
    public static SRequest create(String query, String... fields) {
      return new SRequest().query(query).fields(fields);
    }

    /**
     * Creates a request with query and fields.
     * As the returned request is initialized with query and fields, it can be used without further adjustments.
     * @return an instance of SRequest, initialized with the provided query and fields.
     */
    public static SRequest create(String query, List<String> fields) {
      return new SRequest().query(query).fields(fields);
    }

    /**
     * @param solrClient       used for issuing Solr requests. If not specified, {@link #defaultSolrClient} will be used.
     * @return the SRequest adjusted with the provided value.
     */
    public SRequest solrClient(SolrClient solrClient) {
      if (solrClient == null) {
        log.debug("solrClient(null) called. Leaving solrClient unchanged");
        return this;
      }
      this.solrClient = solrClient;
      return this;
    }

    /**
     * The parameters in the solrQuery has the lowest priority: All calls to modifier methods will override matching
     * values in the solrQuery.
     * @param solrQuery        the base for the request. If not provided it will be constructed from scratch.
     * @return the SRequest adjusted with the provided value.
     */
    public SRequest solrQuery(SolrQuery solrQuery) {
      this.solrQuery = solrQuery;
      return this;
    }

    /**
     * @param expandResources  if true, embedded resources for HTML pages are extracted and added to the delivered
     *                         lists of Solr Documents. Default is false.
     *                         Note: Indirect references (through JavaScript & CSS) are not followed.
     * @return the SRequest adjusted with the provided value.
     */
    public SRequest expandResources(boolean expandResources) {
      this.expandResources = expandResources;
      return this;
    }

    /**
     * @param ensureUnique     if true, unique documents are guaranteed. This is only sane if expandResources is true.
     *                         Default is false.
     *                         Note that a HashSet is created to keep track of encountered documents and will impose
     *                         a memory overhead linear to the number of results.
     * @return the SRequest adjusted with the provided value.
     * @see #maxUnique(Integer)
     */
    public SRequest ensureUnique(boolean ensureUnique) {
      this.ensureUnique = ensureUnique;
      return this;
    }

    /**
     * @param maxUnique        the maximum number of uniques to track when ensureUnique is true.
     *                         If the number of uniques exceeds this limit, an exception will be thrown.
     *                         Default is {@link #DEFAULT_MAX_UNIQUE}.
     * @return the SRequest adjusted with the provided value.
     * @see #ensureUnique(boolean)
     */
    public SRequest maxUnique(Integer maxUnique) {
      this.maxUnique = maxUnique;
      return this;
    }

    /**
     * @param deduplicateField The field to use for de-duplication. This is typically {@code url}.
     *                         Default is null (no deduplication).
     *                         Note: deduplicateField does not affect expandResources. Set ensureUnique to true if
     *                         if expandResources is true and uniqueness must also be guaranteed for resources.
     * @return the SRequest adjusted with the provided value.
     */
    public SRequest deduplicateField(String deduplicateField) {
      this.deduplicateField = deduplicateField;
      return this;
    }

    /**
     * Deduplication combined with time proximity sorting. This is a shorthand for
     * {@code request.deduplicatefield(deduplicateField).sort("abs(sub(ms(idealTime), crawl_date)) asc");}
     *
     * Use case: Extract unique URLs for matches that are closest to a given point in time:
     *           {@code timeProximityDeduplication("2014-01-03T11:56:58Z", "url_norm"}.
     *
     * Note: This overrides any existing {@link #sort(String)}.
     * @param idealTime        The time that the resources should be closest to, stated as a Solr timestamp
     *                         {@code YYYY-MM-DDTHH:mm:SSZ}.
     *                         Also supports {@code oldest} and {@code newest} as values.
     * @param deduplicateField The field to use for de-duplication. This is typically {@code url}.
     * @return the SRequest adjusted with the provided values.
     */
    public SRequest timeProximityDeduplication(String idealTime, String deduplicateField) {
      String origo = idealTime;
      if ("newest".equals(idealTime)) {
        origo = "9999-12-31T23:59:59Z";
      } else if ("oldest".equals(idealTime)) {
        origo = "0001-01-01T00:00:01Z";
      } else if (!ISO_TIME.matcher(idealTime).matches()) {
        throw new IllegalArgumentException(
                "The idealTime '" + idealTime + "' does not match 'oldest', 'newest', 'YYYY-MM-DDTHH:mm:SSZ' or " +
                "'YYYY-MM-DDTHH:mm:SS.sssZ");
      }
      this.idealTime = origo;

      if (deduplicateField == null) {
        throw new NullPointerException("deduplicateField == null which is not allowed for timeProximityDeduplication");
      }
      this.deduplicateField = deduplicateField;
      return this;
    }

    /**
     * @param fields           fields to export (fl). deduplicateField will be added to this is not already present.
     *                         This parameter has no default and must be defined.
     * @return the SRequest adjusted with the provided value.
     * @see #fields(String...)
     */
    public SRequest fields(List<String> fields) {
      this.fields = fields;
      return this;
    }

    /**
     * @param fields           fields to export (fl). deduplicateField will be added to this is not already present.
     *                         This parameter has no default and must be defined.
     * @return the SRequest adjusted with the provided value.
     * @see #fields(List)
     */
    public SRequest fields(String... fields) {
      this.fields = Arrays.asList(fields);
      return this;
    }

    /**
     * @param maxResults       the maximum number of results to return. This includes expanded resources.
     *                         Default is {@link Long#MAX_VALUE} (effectively no limit).
     * @return the SRequest adjusted with the provided value.
     */
    public SRequest maxResults(long maxResults) {
      this.maxResults = maxResults;
      return this;
    }

    /**
     * Note: {@link #timeProximityDeduplication(String, String)} takes precedence over sort.
     * @param sort             standard Solr sort. Depending on deduplicateField and tie breaker it might be adjusted
     *                         by {@link #adjustSolrQuery(SolrQuery, boolean, boolean, String)}.
     *                         Default is {@link #DEFAULT_SORT}.
     * @return the SRequest adjusted with the provided value.
     */
    public SRequest sort(String sort) {
      this.sort = sort;
      return this;
    }

    /**
     * @param query            standard Solr query.
     *                         This parameter has no default and must be defined either directly or
     *                         through {@link #solrQuery(SolrQuery)}.
     * @return the SRequest adjusted with the provided value.
     */
    public SRequest query(String query) {
      this.query = query;
      return this;
    }

    /**
     * @param filterQueries    optional Solr filter queries. For performance, 0 or 1 filter query is recommended.
     *                         If multiple filters are to be used, consider collapsing them into one:
     *                         {@code ["foo", "bar"]} → {@code ["(foo) AND (bar)"]}.
     * @return the SRequest adjusted with the provided value.
     * @see #filterQueries(String...)
     */
    public SRequest filterQueries(List<String> filterQueries) {
      this.filterQueries = filterQueries;
      return this;
    }
    /**
     * @param filterQueries    optional Solr filter queries. For performance, 0 or 1 filter query is recommended.
     *                         If multiple filters are to be used, consider collapsing them into one:
     *                         {@code ["foo", "bar"]} → {@code ["(foo) AND (bar)"]}.
     * @return the SRequest adjusted with the provided value.
     * @see #filterQueries(List)
     */
    public SRequest filterQueries(String... filterQueries) {
      this.filterQueries = Arrays.asList(filterQueries);
      return this;
    }

    /**
     * @param pageSize         paging size. Typically 500-100,000 depending on fields.
     *                         Default is {@link #DEFAULT_PAGESIZE}.
     * @return the SRequest adjusted with the provided value.
     */
    public SRequest pageSize(int pageSize) {
      this.pageSize = pageSize;
      return this;
    }

    public SolrQuery finalizeSolrQuery() {
      throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Copies {@link #solrQuery} and adjusts it with defined attributes from the SRequest, extending with needed
     * SolrRequest-attributes.
     * @return a SolrQuery ready for processing.
     */
    public SolrQuery getMergedSolrQuery() {
      SolrQuery solrQuery = SolrUtils.deepCopy(this.solrQuery);
      if (query != null) {
        solrQuery.setQuery(query);
      }
      if (filterQueries != null) {
        solrQuery.setFilterQueries(filterQueries.toArray(new String[0]));
      }

      if (idealTime != null) {
        sort = String.format(Locale.ROOT, "%s asc, abs(sub(ms(%s), crawl_date)) asc", deduplicateField, idealTime);
      }
      solrQuery.set(CommonParams.SORT, sort);
      if (fields != null) {
        solrQuery.set(CommonParams.FL, String.join(",", fields));
      }
      solrQuery.set(CommonParams.ROWS, (int)Math.min(maxResults, pageSize));
      return solrQuery;
    }

    /**
     * @return a copy of this SRequest, as independent as possible: {@link #solrQuery} and Lists are deep-copied.
     */
    public SRequest deepCopy() {
      SRequest copy = new SRequest().
              solrClient(solrClient).
              solrQuery(solrQuery == null ? null : SolrUtils.deepCopy(solrQuery)).
              expandResources(expandResources).
              ensureUnique(ensureUnique).
              maxUnique(maxUnique).
              deduplicateField(deduplicateField).
              fields(copy(fields)).
              maxResults(maxResults).
              sort(sort).
              query(query).
              filterQueries(copy(filterQueries)).
              pageSize(pageSize);
      copy.idealTime = idealTime;
      return copy;
    }

    private List<String> copy(List<String> fields) {
      if (fields == null) {
        return null;
      }
      return new ArrayList<>(fields);
    }
  }

}
