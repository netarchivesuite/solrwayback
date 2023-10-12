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

import dk.kb.netarchivesuite.solrwayback.parsers.ArcParserFileResolver;
import dk.kb.netarchivesuite.solrwayback.parsers.HtmlParserUrlRewriter;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;
import dk.kb.netarchivesuite.solrwayback.util.CollectionUtils;
import dk.kb.netarchivesuite.solrwayback.util.SolrUtils;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Methods for trimming, expanding and adjusting a Stream of Solr documents.
 */
public class SolrStreamDecorators {
    private static final Logger log = LoggerFactory.getLogger(SolrStreamDecorators.class);

    /**
     * Takes a SolrDocument which has 0 or more multi-valued fields and flattens those fields to single value by creating
     * multiple documents with all permutations of the values in the multi-valued fields converted to single-valued.
     *
     * Typically used for exporting to CSV where multi-value is not desirable.
     *
     * The following example delivers a list of documents with a single source and a single destination URL for each link
     * on each unique page on the kb.dk domain, where uniqueness is defined by hash:
     * <pre>
     * SRequest request = SRequest.builder().
     *     query("domain:kb").
     *     filterQueries("content_type_norm:html").
     *     fields("url", "links").
     *     timeProximityDeduplication("2019-04-15T12:31:51Z", "hash");
     *
     * List<SolrDocument> docs = SolrGenericStreaming.create(request).stream().
     *     flatMap(SolrGenericStreaming::flatten).
     *     collect(Collectors.toList());
     * </pre>
     *
     * Note: With multiple multi-value fields, the number of produced documents grows multiplicatively.
     *       As everything is streaming, this does not require excessive memory for flatten itself,
     *       but the amount of produced SolrDocuments can present a problem for the caller.
     * @param doc a document with at most 1 multi-valued field.
     * @return the input document flattened to at least 1 documents holding only single-valued field.
     */
    // TODO: Make this a post-processor
    @SuppressWarnings({"OptionalIsPresent", "unchecked"})
    public static Stream<SolrDocument> flatten(SolrDocument doc) {
        Optional<Map.Entry<String, Object>> firstMulti = doc.entrySet().stream().
                filter(entry -> entry.getValue() instanceof Collection).findFirst();
        if (!firstMulti.isPresent()) {
            return Stream.of(doc);
        }
        return ((Collection<Object>)firstMulti.get().getValue()).stream().
                map(value -> {
                    LinkedHashMap<String, Object> extraMap = new LinkedHashMap<>(doc);
                    extraMap.put(firstMulti.get().getKey(), value);
                    return new SolrDocument(extraMap);
                }).
                flatMap(SolrStreamDecorators::flatten);
    }

    /**
     * Extends the stream with post processing defined in {@code request}, such as duplicate removal and resource
     * expansion.
     * @param docs    a source stream with Solr Documents
     * @param request same request as used for the stream source.
     * @param adjustedFields comma separated field list expanded from the initial fields in the request,
     *                       with extra fields needed by requested processing steps.
     * @return the {@code docs} stream extended with steps as defined in the {@code request}.
     * @see #addPostProcessors(Iterator, SRequest, String)
     */
    public static Stream<SolrDocument> addPostProcessors(Stream<SolrDocument> docs, SRequest request, String adjustedFields) {
        if (request.deduplicateField != null) {
            docs = docs.filter(new OrderedDeduplicator(request.deduplicateField));
        }

        if (request.expandResources) {
            docs = docs.flatMap(new HTMLResourceExpander(
                    adjustedFields, request.getExpandResourcesFilterQueries(), true));
        }

        if (request.ensureUnique) {
            docs = docs.filter(new UniqueFilter(request.useHashingForUnique, request.maxUnique, request.uniqueFields));
        }

        // Reduce documents to contain requested fields only
        docs = docs.peek(SolrUtils.reduceAndSortFields(request.fields));

        return docs;
    }

    /**
     * Extends the iterator with post processing defined in {@code request}, such as duplicate removal and resource
     * expansion.
     * @param docs    a source iterator with Solr Documents
     * @param request same request as used for the iterator source.
     * @param adjustedFields comma separated field list expanded from the initial fields in the request,
     *                       with extra fields needed by requested processing steps.
     * @return the {@code docs} iterator extended with steps as defined in the {@code request}.
     * @see #addPostProcessors(Stream, SRequest, String)
     */
    // TODO: Consider moving this to a support class
    public static Iterator<SolrDocument> addPostProcessors(Iterator<SolrDocument> docs, SRequest request, String adjustedFields) {
        if (request.deduplicateField != null) {
            docs = CollectionUtils.ReducingIterator.of(
                    docs, new OrderedDeduplicator(request.deduplicateField));
        }

        if (request.expandResources) {
            docs = CollectionUtils.ExpandingIterator.ofStream(docs, new HTMLResourceExpander(
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

        return docs;
    }

    /**
     * Deduplicator that expects the incoming {@link SolrDocument}s to be in order.
     * Deduplication is done on 1 or more fields. All fields must match the fields in the previous document for
     * the current document to be classified as a duplicate.
     */
    static class OrderedDeduplicator implements Predicate<SolrDocument> {
        private final String[] deduplicateFields;
        private final Object[] lastStreamDeduplicateValues;

        public OrderedDeduplicator(String... deduplicateFields) {
            if (deduplicateFields.length == 0) {
                throw new IllegalArgumentException("No deduplicateFields given");
            }
            this.deduplicateFields = deduplicateFields;
            this.lastStreamDeduplicateValues = new Object[deduplicateFields.length];
        }

        @Override
        public boolean test(SolrDocument doc) {
            boolean isNew = true;
            out:
            if (lastStreamDeduplicateValues[0] != null) {
                for (int i = 0 ; i < deduplicateFields.length ; i++) {
                    if (!Objects.equals(lastStreamDeduplicateValues[i], doc.getFieldValue(deduplicateFields[i]))) {
                        break out;
                    }
                }
                isNew = false;
            }
            for (int i = 0 ; i < deduplicateFields.length ; i++) {
                lastStreamDeduplicateValues[i] = doc.getFieldValue(deduplicateFields[i]);
            }
            return isNew;
        }
    }

    /**
     * Perform a lookup of a HTML resource, extracting links to embedded resources and issue searches for
     * those nearest in time to the originating page.
     * Plain {@code <a href="..." ...>} links are not part of this.
     * The graph traversal is only 1 level deep.
     */
    public static class HTMLResourceExpander implements Function<SolrDocument, Stream<SolrDocument>> {
        private final String fields; // Comma separated
        private final String[] expandFilterQueries;
        private final boolean returnInput;

        /**
         * @param fields comma separated list of fields to request for resources.
         * @param expandFilterQueries 0 or more filters that are added when searching for resources.
         * @param returnInput if true, the input document in {@link #apply(SolrDocument)} will always be returned.
         */
        public HTMLResourceExpander(String fields, String[] expandFilterQueries, boolean returnInput) {
            this.fields = fields;
            this.expandFilterQueries = expandFilterQueries;
            this.returnInput = returnInput;
        }

        @Override
        public Stream<SolrDocument> apply(SolrDocument doc) {
            try {
                if (!"html".equals(doc.getFieldValue("content_type_norm"))) {
                    return returnInput ? Stream.of(doc) : Stream.of();
                }
                String sourceFile = doc.getFieldValue("source_file_path").toString();
                long offset = Long.parseLong(doc.getFieldValue("source_file_offset").toString());
                ArcEntry arc= ArcParserFileResolver.getArcEntry(sourceFile, offset);
                HashSet<String> resources = HtmlParserUrlRewriter.getResourceLinksForHtmlFromArc(arc);
                Stream<SolrDocument> resourceStream =
                        NetarchiveSolrClient.getInstance().findNearestDocuments(
                                fields, arc.getCrawlDate(), resources.stream(), expandFilterQueries);
                return returnInput ? Stream.concat(Stream.of(doc), resourceStream) : resourceStream;
            } catch (Exception e) {
                log.warn("Exception getting resources for SolrDocument '" + doc + "'", e);
                return returnInput ? Stream.of(doc) : Stream.of();
            }
        }
    }
}
