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

import dk.kb.netarchivesuite.solrwayback.util.CollectionUtils;
import dk.kb.netarchivesuite.solrwayback.util.SolrUtils;
import dk.kb.netarchivesuite.solrwayback.util.ThroughputTracker;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Handles {@link SolrDocument} stream creation with decorators defined in {@link SRequest}s.
 * <p>
 * Depending on {@link SRequest} properties, configuration and backing Solr (Cloud), streams can be generated from 
 * "standard" Solr requests, merged from separate shard requests or using Solr streaming export.
 * TODO: Implement Solr streaming export
 */
public class SolrStreamFactory {
    private static final Logger log = LoggerFactory.getLogger(SolrStreamFactory.class);
    private static final Logger exportLog = LoggerFactory.getLogger("kb.dk.export");

    /**
     * Depending on the backing Solr (Cloud) topology, the collection and the {@link SRequest#shardDivide},
     * {@link SRequest#shardDivideAutoMinShards} and {@link SRequest#shardDivideAutoMinHits}, either standard
     * collection based document search & delivery or shard dividing search & delivery is used to provide a
     * Stream of {@link SolrDocument}s.
     * <p>
     * Important: This method returns a {@link CollectionUtils.CloseableStream} and the caller <strong>must</strong>
     * ensure that it is either depleted or closed after use, to avoid resource leaking. It is highly recommended to
     * use {@code try-with-resources} directly on the returned stream:
     * <pre>
     * try (CollectionUtils.CloseableStream<SolrDocument> docs = SolrStreamShard.streamStrategy(myRequest) {
     *     long hugeIDs = docs.map(doc -> doc.get("id)).filter(id -> id.length() > 200).count();
     * }
     * </pre>
     * @param request stream setup.
     * @return an Stream of {@code SolrDocument}s, as specified in the {@code request}.
     */
    public static CollectionUtils.CloseableStream<SolrDocument> stream(SRequest request)
            throws IllegalArgumentException {
        return new CollectionUtils.CloseableStream<>(iterate(request));
    }

    /**
     * Depending on the backing Solr (Cloud) topology, the collection and the {@link SRequest#shardDivide},
     * {@link SRequest#shardDivideAutoMinShards} and {@link SRequest#shardDivideAutoMinHits}, either standard
     * collection based document search & delivery or shard dividing search & delivery is used to provide an
     * iterator of {@link SolrDocument}s.
     * <p>
     * Important: This method returns a {@link CollectionUtils.CloseableIterator}
     * and the caller <strong>must</strong> ensure that it is either depleted or closed after use, to avoid resource
     * leaking.
     * @param request stream setup.
     * @return an iterator of {@code SolrDocument}s, as specified in the {@code request}.
     */
    public static CollectionUtils.CloseableIterator<SolrDocument> iterate(SRequest request)
            throws IllegalArgumentException {
        List<SolrUtils.Shard> shards;

        switch (request.shardDivide) {
            case never: // Never shardDivide
                log.debug("Using collection oriented Solr document stream as shardDivide == never");
                return CollectionUtils.CloseableIterator.single(SolrStreamDirect.iterate(request));

            case always: // Always shardDivide (if possible)
                shards = resolveShards(request);
                if (shards == null) {
                    log.warn("shardDivide == always, but shards could not be resolved. " +
                             "Falling back to collection oriented Solr document streaming");
                    return CollectionUtils.CloseableIterator.single(SolrStreamDirect.iterate(request));
                }
                if (shards.size() == 1) {
                    log.warn("shardDivide == always, but only 1 shard is specified/available: '{}'. " +
                             "Forcing shard dividing Solr document streaming although this does not make sense",
                             shards.get(0));
                } else {
                    log.debug("shardDivide == always. Using shard dividing Solr document streaming for {} shards",
                              shards.size());
                }
                return SolrStreamShard.iterateSharded(request, shards);

            case auto: // Maybe shardDivide
                shards = resolveShards(request);
                if (!request.isSingleCollection()) {
                    log.debug("shardDivide == auto and more than 1 collection is defined for shards. " +
                              "Using shard dividing Solr document streaming for {} shards",
                              shards.size());
                    return SolrStreamShard.iterateSharded(request, shards);
                }
                if (shards == null) {
                    log.debug("shardDivide == auto, but shards could not be resolved. " +
                              "Falling back to collection oriented Solr document streaming");
                    return CollectionUtils.CloseableIterator.single(SolrStreamDirect.iterate(request));
                }
                if (shards.size() == 1) {
                    log.debug("shardDivide == auto, but only 1 shard is specified/available: '{}'. " +
                              "Using collection oriented Solr document streaming", shards.get(0));
                    return CollectionUtils.CloseableIterator.single(SolrStreamDirect.iterate(request));
                }
                if (shards.size() < request.shardDivideAutoMinShards) {
                    log.debug("shardDivide == auto, but only {} shards are specified/available with " +
                              "shardDivideAutoMinShards = {}. Using collection oriented Solr document streaming",
                              shards.size(), request.shardDivideAutoMinShards);
                    return CollectionUtils.CloseableIterator.single(SolrStreamDirect.iterate(request));
                }
                long hits = SolrStreamShard.getApproximateHits(request);
                if (hits < request.shardDivideAutoMinHits) {
                    log.debug("shardDivide == auto, but approximate hitcount {} is < limit {}. " +
                              "Falling back to collection oriented Solr document streaming",
                              hits, request.shardDivideAutoMinHits);
                    return CollectionUtils.CloseableIterator.single(SolrStreamDirect.iterate(request));
                }
                if (hits <= request.maxResults) {
                    log.debug("shardDivide == auto, but approximate hitcount {} is <= maxResults {}. " +
                              "Falling back to collection oriented Solr document streaming",
                              hits, request.maxResults);
                    return CollectionUtils.CloseableIterator.single(SolrStreamDirect.iterate(request));
                }
                log.debug("shardDivide == auto, and hitcount {} is >= limit {}. " +
                          "Using shard dividing Solr document streaming for {} shards ",
                          hits, request.shardDivideAutoMinHits, shards.size());
                return SolrStreamShard.iterateSharded(request, shards);

            default:
                throw new UnsupportedOperationException(
                        "shardDivide == '" + request.shardDivide + "', which is unsupported");
        }
    }

    /**
     * Resolve shards primarily from the {@code request}, secondarily from the backing Solr (Cloud).
     * If not possible, {@code null} will be returned.
     * @param request standard streaming request.
     * @return the shards to use or null if unresolvable.
     */
    private static List<SolrUtils.Shard> resolveShards(SRequest request) {
        List<String> shardIDs = request.shards;
        if (shardIDs == null || shardIDs.isEmpty()) {
            return SolrUtils.getShards();
        }
        // Need to convert shardIDs to collection-qualified Shards
        String collection = SolrUtils.getBaseCollection();
        return shardIDs.stream()
                .map(shardID -> new SolrUtils.Shard(collection, shardID))
                .collect(Collectors.toList());
    }

    /**
     * Extend the stream with post processing defined in {@code request}, such as duplicate removal and resource
     * expansion.
     * @param docs    a source stream with Solr Documents
     * @param request same request as used for the stream source.
     * @param adjustedFields comma separated field list expanded from the initial fields in the request,
     *                       with extra fields needed by requested processing steps.
     * @return the {@code docs} stream extended with steps as defined in the {@code request}.
     * @see #addPostProcessors(Iterator, SRequest, String)
     */
    public static Stream<SolrDocument> addPostProcessors(
            Stream<SolrDocument> docs, SRequest request, String adjustedFields) {
        if (request.deduplicateFields != null) {
            docs = docs.filter(new SolrStreamDecorators.OrderedDeduplicator(request.deduplicateFields));
        }

        if (request.expandResources) {
            docs = docs.flatMap(new SolrStreamDecorators.HTMLResourceExpander(
                    adjustedFields, request.getExpandResourcesFilterQueries(), true));
        }

        if (request.ensureUnique) {
            docs = docs.filter(new UniqueFilter(request.useHashingForUnique, request.maxUnique, request.uniqueFields));
        }

        // Reduce documents to contain requested fields only
        docs = docs.peek(SolrUtils.reduceAndSortFields(request.fields));

        if (request.maxResults < Long.MAX_VALUE) {
            docs = docs.limit(request.maxResults);
        }

        // Log progress
        ThroughputTracker tracker = new ThroughputTracker()
                .prefix("Export(" + SRequest.limit(request.query, 20) + "):")
                .designation("docs")
                .logger(exportLog);
        docs = docs.filter(tracker).onClose(tracker::close);

        return docs;
    }

    /**
     * Extend the iterator with post processing defined in {@code request}, such as duplicate removal and resource
     * expansion.
     * @param docs    a source iterator with Solr Documents
     * @param request same request as used for the iterator source.
     * @param adjustedFields comma separated field list expanded from the initial fields in the request,
     *                       with extra fields needed by requested processing steps.
     * @return the {@code docs} iterator extended with steps as defined in the {@code request}.
     * @see #addPostProcessors(Stream, SRequest, String)
     */
    // TODO: Consider moving this to a support class
    @SuppressWarnings("resource")
    public static Iterator<SolrDocument> addPostProcessors(Iterator<SolrDocument> docs, SRequest request, String adjustedFields) {
        if (request.deduplicateFields != null) {
            docs = CollectionUtils.ReducingIterator.of(
                    docs, new SolrStreamDecorators.OrderedDeduplicator(request.deduplicateFields));
        }

        if (request.expandResources) {
            docs = CollectionUtils.ExpandingIterator.ofStream(docs, new SolrStreamDecorators.HTMLResourceExpander(
                    adjustedFields, request.getExpandResourcesFilterQueries(), true));
        }

        if (request.ensureUnique) {
            docs = CollectionUtils.ReducingIterator.of(
                    docs, new UniqueFilter(request.useHashingForUnique, request.maxUnique, request.uniqueFields));
        }

        // Reduce documents to contain requested fields only
        docs = CollectionUtils.AdjustingIterator.of(docs, element -> {
            SolrUtils.reduceAndSortFields(request.fields).accept(element);
            return element;
        });

        // Log progress
        ThroughputTracker tracker = new ThroughputTracker()
                .prefix("Export(" + SRequest.limit(request.query, 20) + "):")
                .designation("docs")
                .logger(exportLog);
        docs = CollectionUtils.ReducingIterator.of(docs, tracker::test);

        docs = new CollectionUtils.CloseableIterator<SolrDocument>(docs, new AtomicBoolean(true), request.maxResults) {
            @Override
            public void close() {
                tracker.close();
                super.close();
            }
        };

        return docs;
    }
}
