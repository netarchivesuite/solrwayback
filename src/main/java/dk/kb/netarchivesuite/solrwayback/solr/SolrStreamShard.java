/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.kb.netarchivesuite.solrwayback.solr;

import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.util.CollectionUtils;
import dk.kb.netarchivesuite.solrwayback.util.SolrUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.FacetParams;
import org.apache.solr.common.params.GroupParams;
import org.apache.solr.common.params.HighlightParams;
import org.apache.solr.common.params.StatsParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Handles the logistics of streaming search results from Solr Shards.
 * <p>
 * {@link SolrGenericStreaming} uses either plain Solr cursorMarks or paging through a group field
 * for batching calls to Solr. Both of these methods 
 */
public class SolrStreamShard {
    private static final Logger log = LoggerFactory.getLogger(SolrStreamShard.class);

    /**
     * Shared executor for all shard dividing streaming calls.
     * <p>
     * The thread pool size is unbounded to avoid deadlocks caused by queues blocking threads.
     */
    private static final Executor executor = Executors.newCachedThreadPool(new ThreadFactory() {
        int threadCount = 0;
        @SuppressWarnings("NullableProblems")
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "SolrStream_" + threadCount++);
            t.setDaemon(true);
            return t;
        }
    });

    /**
     * Shared gatekeeper for all shard divided requests.
     */
    private static final Semaphore gatekeeper = new Semaphore(PropertiesLoader.SOLR_STREAM_SHARD_DIVIDE_CONCURRENT_MAX);

    /**
     * Depending on the backing Solr Cloud topology, the collection and the {@link SRequest#shardDivide} and
     * {@link SRequest#autoShardDivideLimit}, either standard collection based document search & delivery or
     * shard dividing search & delivery is used to provide an Stream of {@link SolrDocument}s.
     * <p>
     * Important: This method returns a {@link dk.kb.netarchivesuite.solrwayback.util.CollectionUtils.CloseableStream}
     * and the caller <strong>must</strong> ensure that it is either depleted or closed after use, to avoid resource
     * leaking. It is highly recommended to use {@code try-with-resources} directly on the returned stream:
     * <pre>
     * try (CollectionUtils.CloseableStream<SolrDocument> docs =
     *      SolrStreamShard.streamStrategy(myRequest) {
     *     long hugeIDs = docs.map(doc -> doc.get("id)).filter(id -> id.length() > 200).count();
     * }
     * </pre>
     * @param request stream setup.
     * @return an Stream of {@code SolrDocument}s, as specified in the {@code request}.
     */
    public static CollectionUtils.CloseableStream<SolrDocument> streamStrategy(
            SRequest request) throws IllegalArgumentException {
       return new CollectionUtils.CloseableStream<>(iterateStrategy(request));
    }


    /**
     * Depending on the backing Solr Cloud topology, the collection and the {@link SRequest#shardDivide} and
     * {@link SRequest#autoShardDivideLimit}, either standard collection based document search & delivery or
     * shard dividing search & delivery is used to provide an iterator of {@link SolrDocument}s.
     * <p>
     * Important: This method returns a {@link dk.kb.netarchivesuite.solrwayback.util.CollectionUtils.CloseableIterator}
     * and the caller <strong>must</strong> ensure that it is either depleted or closed after use, to avoid resource
     * leaking.
     * @param request stream setup.
     * @return an iterator of {@code SolrDocument}s, as specified in the {@code request}.
     */
    public static CollectionUtils.CloseableIterator<SolrDocument> iterateStrategy(
            SRequest request) throws IllegalArgumentException {
        // Never shardDivide
        if (SRequest.CHOICE.never.equals(request.shardDivide)) {
            log.debug("Using collection oriented Solr document stream as shardDivide == never");
            return CollectionUtils.CloseableIterator.single(SolrGenericStreaming.iterate(request));
        }

        List<SolrUtils.Shard> shards = null;
        List<String> shardIDs = request.shards;
        if (shardIDs == null || shardIDs.isEmpty()) {
            shards = SolrUtils.getShards();
        } else { // Need to convert shardIDs to collection-qualified Shards
            String collection = SolrUtils.getBaseCollection();
            shards = shardIDs.stream()
                    .map(shardID -> new SolrUtils.Shard(collection, shardID))
                    .collect(Collectors.toList());
        }

        // Always shardDivide (if possible)
        if (SRequest.CHOICE.always.equals(request.shardDivide)) {
            if (shards == null) {
                log.warn("shardDivide == always, but shards could not be resolved. " +
                         "Falling back to collection oriented Solr document streaming");
                return CollectionUtils.CloseableIterator.single(SolrGenericStreaming.iterate(request));
            }
            if (shards.size() == 1) {
                log.warn("shardDivide == always, but only 1 shard is specified/available: '{}'. " +
                         "Forcing shard dividing Solr document streaming although this does not make sense",
                         shards.get(0));
            } else {
                log.debug("shardDivide == always. Using shard dividing Solr document streaming for {} shards: {}",
                          shards.size(), shards);
            }
            return iterateSharded(request, shards);
        }

        // Maybe shardDivide
        if (SRequest.CHOICE.auto.equals(request.shardDivide)) {
            if (shards == null) {
                log.debug("shardDivide == auto, but shards could not be resolved. " +
                          "Falling back to collection oriented Solr document streaming");
                return CollectionUtils.CloseableIterator.single(SolrGenericStreaming.iterate(request));
            }
            if (shards.size() == 1) {
                log.debug("shardDivide == auto, but only 1 shard is specified/available: '{}'. " +
                          "Falling back to collection oriented Solr document streaming", shards.get(0));
                return CollectionUtils.CloseableIterator.single(SolrGenericStreaming.iterate(request));
            }
            long hits = getApproximateHits(request);
            if (hits < request.autoShardDivideLimit) {
                log.debug("shardDivide == auto, but approximate hitcount {} is < limit {}. " +
                          "Falling back to collection oriented Solr document streaming",
                          hits, request.autoShardDivideLimit);
                return CollectionUtils.CloseableIterator.single(SolrGenericStreaming.iterate(request));
            }
            log.debug("shardDivide == auto, and hitcount {} is >= limit {}. " +
                      "Using shard dividing Solr document streaming for {} shards: {}",
                      hits, request.autoShardDivideLimit, shards.size(), shards);
            return iterateSharded(request, shards);
        }

        throw new UnsupportedOperationException(
                "shardDivide == '" + request.shardDivide + "', which is unsupported");
    }

    /**
     * Used by {@link dk.kb.netarchivesuite.solrwayback.solr.SRequest.CHOICE#auto} mode for {@link SRequest#shardDivide}
     * to determine if the number of hits is large enough to enable shard division.
     * @param request the base request.
     * @return the raw number of hits for the request, ignoring grouping, deduplication, resource expancion etc.
     */
    public static long getApproximateHits(SRequest request) {
        if (request.queries != null) {
            throw new UnsupportedOperationException("Multi-queries are not yet supported for chard division");
        }
        SolrQuery solrQuery = request.getMergedSolrQuery();
        solrQuery.set(CommonParams.ROWS, 0);
        solrQuery.set(GroupParams.GROUP, false);
        solrQuery.set(FacetParams.FACET, false);
        solrQuery.set(HighlightParams.HIGHLIGHT, false);
        solrQuery.set(StatsParams.STATS, false);
        try {
            return request.solrClient.query(solrQuery).getResults().getNumFound();
        } catch (Exception e) {
            throw new RuntimeException("Unable to resolve hit count for " + request, e);
        }
    }

    /**
     * Sets up an individual {@link SolrGenericStreaming} for each shard in the collection, or each shard in the
     * {@code request} is shards are explicitly stated there. The resulting documents are merged using
     * {@link dk.kb.netarchivesuite.solrwayback.util.CollectionUtils#mergeIterators(Collection, Comparator)} and
     * relevant post-processors are added using {@link SolrStreamDecorators#addPostProcessors(Iterator, SRequest, String)}.
     * The end result from the returned iterator should be exactly the same as a direct call to
     * {@link SolrGenericStreaming#iterate(SRequest)} but with better performance for large result sized.
     * <p>
     * Note: This method ignores {@link SRequest#shardDivide} and {@link SRequest#shards}.
     * <p>
     * Important: This method returns a {@link dk.kb.netarchivesuite.solrwayback.util.CollectionUtils.CloseableIterator}
     * and the caller <strong>must</strong> ensure that it is either depleted or closed after use, to avoid reosurce
     * leaking.
     * @param request stream setup.
     * @param shards the shards to divide the requests to.
     * @return an iterator of {@code SolrDocument}s, as specified in the {@code request}.
     */
    protected static CollectionUtils.CloseableIterator<SolrDocument> iterateSharded(
            SRequest request, List<SolrUtils.Shard> shards) {
        if (shards == null || shards.isEmpty()) {
            throw new IllegalArgumentException("No shards specified");
        }
        final SRequest base = request.deepCopy();
        base.shardDivide(SRequest.CHOICE.never);
        // Ensure sort fields are delivered by the shard divided streams
        Set<String> fl = base.getExpandedFieldList();
        fl.addAll(getSortFieldNames(base));
        // TODO: Reduce to original fields before delivering merged result
        base.forceFields(new ArrayList<>(fl));

        // TODO: Resolve adjustedFields (by moving it into SRequest?)
        String adjustedFields = String.join(",", fl);
        final AtomicBoolean continueProcessing = new AtomicBoolean(true);

        // Randomize to spread the load as much as possible (without doing a deeper analysis of the topology)
        Collections.shuffle(shards);
        // TODO: Consider a different pageSize for shardDivide requests
        List<Iterator<SolrDocument>> documentIterators = shards.stream()
                .map(shard -> base.deepCopy().collection(shard.collectionID).shards(shard.shardID))
                .map(SolrGenericStreaming::new)
                // Basic "raw results"
                .map(SolrGenericStreaming::iterator)
                // Limit hammering on the Solr Cloud
                .map(iterator -> CollectionUtils.SharedConstraintIterator.of(iterator, gatekeeper))
                // Speed up processing by threading most of the deduplication
                .map(iterator -> makeDeduplicatingIfStated(iterator, base))
                // Speed up processing by reading ahead
                .map(iterator -> CollectionUtils.BufferingIterator.of(iterator, executor, base.pageSize, continueProcessing))
                .collect(Collectors.toList());
        // Merge all shard divisions to one iterator
        Iterator<SolrDocument> docs = CollectionUtils.mergeIterators(documentIterators, getDocumentComparator(base));
        // Needed for proper maxResult limiting. If not here, the subsequent CloseableIterator might close too early
        docs = makeDeduplicatingIfStated(docs, base);
        // Limit the amount of results
        // Not connected to the other CloseableIterators as expandResources might result in more than maxResults docs
        docs = CollectionUtils.CloseableIterator.of(docs, new AtomicBoolean(true), base.maxResults);
        // Remove duplicates, add resources... Note that the raw request is used as this has the non-expanded fields
        docs = SolrStreamDecorators.addPostProcessors(docs, request, adjustedFields);
        // Ensure that close() propagates to the BufferingIterator to avoid Thread & buffer leaks
        return CollectionUtils.CloseableIterator.of(docs, continueProcessing);
    }

    private static Iterator<SolrDocument> makeDeduplicatingIfStated(Iterator<SolrDocument> iterator, SRequest request) {
        return request.deduplicateField == null ? iterator :
                CollectionUtils.ReducingIterator.of(
                        iterator, new SolrStreamDecorators.OrderedDeduplicator(request.deduplicateField));
    }

    /**
     * Extracts all field names needed by {@link SRequest#sort}.
     * @param request {@link SRequest#sort} will be used from this.
     * @return all field names needed by {@link SRequest#sort}.
     */
    private static Set<String> getSortFieldNames(SRequest request) {
        log.debug("Constructing shard divide sort from '{}'", request.getFullSort());
        Set<String> fields = Arrays.stream(request.getFullSort().split(", *"))
                .map(SORT_FIELD_PATTERN::matcher)
                .filter(Matcher::matches)
                .map(matcher -> matcher.group(1))
                .collect(Collectors.toCollection(HashSet::new));
        Arrays.stream(request.getFullSort().split(", *"))
                .map(SORT_FIELD_TIME_PROXIMITY_PATTERN::matcher)
                .filter(Matcher::matches)
                .map(matcher -> matcher.group(2))
                .forEach(fields::add);
        return fields;
    }

    /**
     * Limited recreation of Solr sort. Basic score & field-based sorting is supported as well as the time-proximity
     * function sort used by {@link SRequest#timeProximityDeduplication}.
     * @param request a request with a comma separates sort chain in {@link SRequest#sort}.
     * @return a chained comparator for the sort elements.
     */
    public static Comparator<SolrDocument> getDocumentComparator(SRequest request) {
        // https://solr.apache.org/guide/solr/latest/query-guide/common-query-parameters.html#sort-parameter
        Comparator<SolrDocument> comparator = null;
        Matcher clauseMatcher = SORT_CLAUSES_PATTERN.matcher(request.getFullSort());
        while (clauseMatcher.find()) {
            String clause = clauseMatcher.group(1);
            if (comparator == null) {
                comparator = getSingleComparator(clause);
            } else {
                comparator = comparator.thenComparing(getSingleComparator(clause));
            }
        }
        return comparator;
    }
    public static final Pattern SORT_CLAUSES_PATTERN = Pattern.compile(" *(.*? (?:asc|desc)),? *");

    /**
     * Limited recreation of Solr sort. Basic score & field-based sorting is supported as well as the time-proximity
     * function sort used by {@link SRequest#timeProximityDeduplication}.
     * @param sortElement a single sort element.
     * @return a comparator for the sort element.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Comparator<SolrDocument> getSingleComparator(String sortElement) {
        // See org.apache.solr.search.SortSpecParsing#parseSortSpecImpl for proper parsing (requires schema)

        // iso_date asc
        Matcher fieldMatcher = SORT_FIELD_PATTERN.matcher(sortElement);
        if (fieldMatcher.matches()) {
            final String field = fieldMatcher.group(1);
            int dir = "asc".equals(fieldMatcher.group(2)) ? 1 : -1;
            return (doc1, doc2) -> {
                Object o1 = doc1.getFieldValue(field);
                Object o2 = doc2.getFieldValue(field);
                if (!(o1 instanceof Comparable) || !(o2 instanceof Comparable)) { // This also checks for null
                    return 0;
                }
                return dir*((Comparable)o1).compareTo(o2);
            };
        }

        // abs(sub(ms(2014-01-03T11:56:58Z), crawl_date)) asc
        Matcher proximityMatcher = SORT_FIELD_TIME_PROXIMITY_PATTERN.matcher(sortElement);
        if (proximityMatcher.matches()) {
            String origoS = proximityMatcher.group(1);
            String field = proximityMatcher.group(2);
            int dir = "asc".equals(proximityMatcher.group(3)) ? 1 : -1;
            final long origoEpoch = Instant.from(DateTimeFormatter.ISO_INSTANT.parse(origoS)).getEpochSecond();
            return (doc1, doc2) -> {
                Object o1 = doc1.getFieldValue(field);
                Object o2 = doc2.getFieldValue(field);
                if (!(o1 instanceof Date) || !(o2 instanceof Date)) { // This also checks for null
                    return 0;
                }
                Long timeDist1 = Math.abs(origoEpoch-((Date)o1).getTime());
                Long timeDist2 = Math.abs(origoEpoch-((Date)o2).getTime());
                return dir*timeDist1.compareTo(timeDist2);
            };
        }

        throw new UnsupportedOperationException("Unable to recognize sort clause '" + sortElement + "'");
    }

    // score and plain fields work the same from a sorting perspective
    public static final Pattern SORT_FIELD_PATTERN = Pattern.compile("^([a-zA-Z_][a-zA-Z0-9_]*) (asc|desc)$");
    public static final Pattern SORT_FIELD_TIME_PROXIMITY_PATTERN = Pattern.compile(
            "^abs *\\( *sub *\\( *ms *\\(([^)]+)\\) *, *([a-zA-Z_][a-zA-Z0-9_]*) *\\) *\\)  *(asc|desc)$");

}
