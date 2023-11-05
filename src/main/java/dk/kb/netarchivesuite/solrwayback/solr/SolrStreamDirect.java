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
import org.apache.solr.common.params.CursorMarkParams;
import org.apache.solr.common.params.GroupParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
public class SolrStreamDirect implements Iterable<SolrDocument> {
    private static final Logger log = LoggerFactory.getLogger(SolrStreamDirect.class);

    /**
     * How to page through the search results.
     */
    public enum PAGING {
        /**
         * Initial search result only, no paging.
         */
        none,
        /**
         * Standard Solr cursorMark paging.
         */
        cursorMark,
        /**
         * Simulated cursorMark paging using grouping and range requests on the group field.
         * Note that this is only used when there is exactly one {@link SRequest#deduplicateFields}.
         * {@link #lastDeduplicateValue} holds the latest received value for the group field.
         */
        group }

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
    private boolean hasMoreQueries = true;     // Whether or not there are non-depleted queries
    private PAGING paging = null;              // Set to null as default as thus MUST be specified during setup
    private String lastDeduplicateValue = null; // Used for PAGING.group (single deduplicateField only)

    private final List<String> adjustedFields; // Fields after adjusting for unique etc.

    private SolrDocumentList undelivered = null; // Leftover form previous call to keep deliveries below pageSize


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
     * {@link SolrStreamFactory#stream} instead as that allows for the faster shard division strategy.
     * @param request stream setup.
     * @return a stream of {@code SolrDocument}s, as specified in the {@code request}.
     * @see SolrStreamFactory#stream(SRequest)
     */
    public static Stream<SolrDocument> stream(SRequest request) throws IllegalArgumentException {
        SolrStreamDirect base = new SolrStreamDirect(request);
        return SolrStreamFactory.addPostProcessors(base.stream(), base.request, String.join(",", base.adjustedFields));
    }

    /**
     * Generic delivery of Solr documents where all parts except {@link SRequest#query(String)} and
     * {@link SRequest#fields(String...)} are optional.
     * <p>
     * Note: This always uses a collection oriented procedure. It is recommended to use
     * {@link SolrStreamFactory#iterate} instead as that allows for the faster shard division strategy.
     * @param request stream setup.
     * @return an iterator of {@code SolrDocument}s, as specified in the {@code request}.
     * @see SolrStreamFactory#iterate(SRequest)
     */
    public static Iterator<SolrDocument> iterate(SRequest request) throws IllegalArgumentException {
        SolrStreamDirect base = new SolrStreamDirect(request);
        return SolrStreamFactory.addPostProcessors(base.iterator(), base.request, String.join(",", base.adjustedFields));
    }

    /**
     * Generic stream where all parts except {@link SRequest#query(String)} and {@link SRequest#fields(String...)}
     * are optional.
     * <p>
     * Note: This is the "raw" stream where post-processors such as ensure uniqueness and expand resources are not added.
     * @param request stream setup.
     * @see SolrStreamFactory#addPostProcessors
     */
    protected SolrStreamDirect(SRequest request) {
        this.request = request;
        originalSolrQuery = request.getMergedSolrQuery();
        solrQuery = SolrUtils.deepCopy(originalSolrQuery);
        queryDepleted = request.isMultiQuery(); // Single query starts assigned, multi are initialized later

        setupPaging(solrQuery, request);

        this.adjustedFields = Arrays.asList(solrQuery.getFields().split(","));

        queries = request.queries == null ? null:
                CollectionUtils.splitToLists(request.queries, request.queryBatchSize).
                        map(batch -> "(" + String.join(") OR (", batch) + ")").
                        iterator(); // 1 big OR query
        if (solrQuery.getQuery() == null && queries != null && queries.hasNext()) {
            solrQuery.setQuery(queries.next());
        }
    }

    /**
     * Set up paging parameters for the {@code solrQuery} and for the {@code SolrStreamDirect}.
     * @param solrQuery a SolrQuery.
     * @param request the request responsible for the solrQuery.
     */
    private void setupPaging(SolrQuery solrQuery, SRequest request) {
        if (request.usePaging && solrQuery.getBool(GroupParams.GROUP, false)) {
            throw new IllegalArgumentException("Preset group==true is not compatible with paging");
        }

        if (request.deduplicateFields != null && request.isMultiQuery() && !request.ensureUnique) {
            log.warn("Using deduplicationFields with multi query without ensureUnique might result in duplicates: " +
                     request);
        }

        if (request.deduplicateFields == null || request.deduplicateFields.size() > 1) { // Standard cursorMark paging
            if (request.usePaging) {
                solrQuery.set(CursorMarkParams.CURSOR_MARK_PARAM,
                              solrQuery.get(CursorMarkParams.CURSOR_MARK_PARAM, CursorMarkParams.CURSOR_MARK_START));
                paging = PAGING.cursorMark;
            } else {
                paging = PAGING.none;
            }
            return;
        }

        // Group based cursorMark strategy below as there are exactly 1 deduplicateField
        // Solr grouping only supports 1 field. The SolrStreamDecorators.addPostProcessors handles an arbitrary number
        // of deduplication fields at the cost of speed, especially for result sets with many duplicates

        // Check for already existing cursorMark
        if (request.usePaging && solrQuery.getMap().containsKey(CursorMarkParams.CURSOR_MARK_PARAM)) {
            throw new IllegalArgumentException(
                    "Unable to enable group-based deduplication on '" + request.deduplicateFields + "' as query has " +
                    "existing cursorMark '" + solrQuery.get(CursorMarkParams.CURSOR_MARK_PARAM) + "'");
        } else {
            solrQuery.remove(CursorMarkParams.CURSOR_MARK_PARAM);
        }

        // group=true&group.field=url_norm&group.limit=1&group.format=simple&group.main=true
        solrQuery.set(GroupParams.GROUP, true);
        solrQuery.set(GroupParams.GROUP_FIELD, request.deduplicateFields.get(0));
        solrQuery.set(GroupParams.GROUP_LIMIT, 1);

        // Make grouping return the first (and only) document from each group flattened as a standard search result
        solrQuery.set(GroupParams.GROUP_FORMAT, "simple");
        solrQuery.set(GroupParams.GROUP_MAIN, true);
        paging = request.usePaging ? PAGING.group : PAGING.none;
    }

    /**
     * Constructs a "raw" Stream for {@link SolrDocument}s. In order for the end result to conform to
     * the properties in the {@link SRequest} used to construct this {@code SolrGenericStreaming}, it is
     * necessary to use {@link SolrStreamFactory#addPostProcessors(Stream, SRequest, String)}.
     * @return a stream of SolrDocuments.
     */
    public Stream<SolrDocument> stream() {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator(), 0), false);
    }

    /**
     * Constructs a "raw" iterator for {@link SolrDocument}s. In order for the end result to conform to
     * the properties in the {@link SRequest} used to construct this {@code SolrGenericStreaming}, it is
     * necessary to use {@link SolrStreamFactory#addPostProcessors(Iterator, SRequest, String)}.
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
        SolrStreamDirect.defaultSolrClient = defaultSolrClient;
    }

    /**
     * @return at least 1 and at most {@link SRequest#pageSize} documents or null if there are no more documents.
     *         Call {@link #hasFinished()} to see if more document lists are available.
     * @throws SolrServerException if Solr could not handle a request for new documents.
     * @throws IOException if general communication with Solr failed.
     */
    protected SolrDocumentList nextDocuments() throws SolrServerException, IOException {
        while (hasMoreQueries || (undelivered != null && !undelivered.isEmpty())) {

            // Return batch if undelivered contains any documents
            if (undelivered != null && !undelivered.isEmpty()) {
                SolrDocumentList deliver = undelivered;
                undelivered = null;
                return deliver;
            }

            // Perform the Solr request
            QueryResponse rsp = performQuery();
            undelivered = rsp.getResults();
            if (undelivered.size() < solrQuery.getRows() || rsp.getResults().getNumFound() <= solrQuery.getRows()) {
                queryDepleted = true;
            }

            updatePaging(rsp); // Must be called before check for queryDepleted as paging might trigger that

            if (queryDepleted) {
                nextQuery();
            }
        }
        return null; // Finished and no more documents
    }

    /**
     * Issue a Solr request using {@link #solrQuery}, updates stats and return the Solr response to the caller.
     * @return the response from the Solr request.
     * @throws SolrServerException if Solr fails.
     * @throws IOException if sending the Solr request or retrieving the Solr response fails.
     */
    private QueryResponse performQuery() throws SolrServerException, IOException {
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
        }
        ;
        totalDelivered.addAndGet(rsp.getResults().size());
        return rsp;
    }

    /**
     * Updates {@code cursorMark} or equivalent parameters, readying next call to Solr.
     * @param response response from previous call to Solr.
     */
    private void updatePaging(QueryResponse response) {
        // Setup cursorMark og group based simulated cursorMark for next request
        switch (paging) {
            case none:
                queryDepleted = true;
                break;
            case cursorMark: // 0 or > 1 deduplicateValues
                String oldCursorMark = solrQuery.get(CursorMarkParams.CURSOR_MARK_PARAM, CursorMarkParams.CURSOR_MARK_START);
                String newCursorMark = response.getNextCursorMark();
                if (newCursorMark.equals(oldCursorMark)) { // No more documents for this query
                    queryDepleted = true;
                } else {
                    solrQuery.set(CursorMarkParams.CURSOR_MARK_PARAM, newCursorMark);
                }
                break;
            case group: // Exactly 1 deduplicateValue
                if (!undelivered.isEmpty()) {
                    lastDeduplicateValue = SolrUtils.fieldValueToString(
                            undelivered.get(undelivered.size()-1).getFieldValue(request.deduplicateFields.get(0)));
                    if (lastDeduplicateValue == null || lastDeduplicateValue.isEmpty()) {
                        log.warn("updatePaging(): Missing lastDeduplicateValue for request " + request);
                    }
                    String nextPageQuery = String.format(
                            // TODO: Test for deduplication on numeric or date field
                            Locale.ROOT, "%s:{%s TO *]", // Range query with non-inclusive start and open end
                            request.deduplicateFields.get(0), lastDeduplicateValue);
                    if (originalSolrQuery.getQuery() == null) {
                        solrQuery.setQuery(nextPageQuery);
                    } else {
                        solrQuery.setQuery(String.format(
                                Locale.ROOT, "(%s) AND %s", // Range query with non-inclusive start and open end
                                originalSolrQuery.getQuery(), nextPageQuery));
                    }
                }
                break;
            default:
                throw new UnsupportedOperationException("Paging strategy '" + paging + "' is not supported");
        }
    }

    /**
     * If {@link SRequest#isMultiQuery()} is true and tre are more queries from {@link SRequest#queries} then
     * the next query is assigned to {@link #solrQuery} and the relevant paging structures are reset.
     * <p>
     * If no more queries are available, {@link #hasMoreQueries} is set to false.
     */
    private void nextQuery() {
        if (!request.isMultiQuery() || !queries.hasNext()) {
            hasMoreQueries = false;
            return;
        }

        solrQuery.setQuery(queries.next());
        queryDepleted = false;
        switch (paging) {
            case none:
                break;
            case cursorMark:
                solrQuery.set(
                        CursorMarkParams.CURSOR_MARK_PARAM,
                        originalSolrQuery.get(CursorMarkParams.CURSOR_MARK_PARAM, CursorMarkParams.CURSOR_MARK_START));
                break;
            case group:
                lastDeduplicateValue = null;
                break;
            default:
                throw new UnsupportedOperationException("Paging strategy '" + paging + "' is not supported");
        }
    }


    /**
     * {@code hasFinished()} does not guarantee that more documents can be delivered, only that there might be more.
     * @return true if there are no more documents, false if there might be more documents.
     */
    public boolean hasFinished() {
        return !hasMoreQueries && (undelivered == null || undelivered.isEmpty());
    }

}
