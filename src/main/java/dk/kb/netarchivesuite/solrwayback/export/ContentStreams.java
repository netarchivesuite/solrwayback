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
package dk.kb.netarchivesuite.solrwayback.export;

import com.google.common.base.Functions;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntryDescriptor;
import dk.kb.netarchivesuite.solrwayback.solr.SRequest;
import dk.kb.netarchivesuite.solrwayback.solr.UniqueFilter;
import dk.kb.netarchivesuite.solrwayback.util.CollectionUtils;
import dk.kb.netarchivesuite.solrwayback.util.DateUtils;
import dk.kb.netarchivesuite.solrwayback.util.JsonUtils;
import dk.kb.netarchivesuite.solrwayback.util.Processing;
import dk.kb.netarchivesuite.solrwayback.util.SolrUtils;
import dk.kb.netarchivesuite.solrwayback.util.StreamBridge;
import dk.kb.netarchivesuite.solrwayback.util.ThroughputTracker;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

/**
 * Stream oriented content delivery, suitable for export as well as special searches such as image search.
 */
public class ContentStreams {
    private static final Logger log = LoggerFactory.getLogger(ContentStreams.class);

    public enum FORMAT {
        /** Comma Separated Values. */
        csv,
        /** Standard JSON, with the documents in a JSON array. */
        json,
        /** <a href="https://jsonlines.org/">JSON-Lines</a> with a single JSON block per line. */
        jsonl}

    /**
     * Searches images and webpages matching the given searchText. For webpages, linked images are resolved and
     * returned. In the case of multiple hits for the same image URL, the one harvested closest to the webpage
     * in time will be delivered.
     * <p>
     * If attemptUnique is true, this search attempts to deliver unique images using the Solr field {@code hash},
     * but does not guarantee it fully as {@link dk.kb.netarchivesuite.solrwayback.solr.UniqueFilter} is used with
     * hashing to {@code int}. Requesting uniqueness limits the export size to 20 million.
     * <p>
     * If the caller needs {@link ArcEntryDescriptor}, use the helpers method in SolrUtils as
     * {@code findImages(50, "kittens").map(SolrUtils::solrDocument2ArcEntryDescriptor)}.
     * @param attemptUnique if true, uniqueness of results is attempted. This is based on Solr {@code hash}.
     * @param goFast if true, images linked from webpages are not prioritised by time proximity to {@code crawl_date}
     *               for the webpage. This is markedly faster, but might give false positives.
     *               goFast also enables deduplication on {@code url_norm}.
     * @param maxImagesPerPage maximum number of images to resolve for any single page.
     * @param query       a query for images.
     * @param filterQueries 0 or more Solr queries.
     * @return a stream of SolrDocuments representing images, containing the fields from
     *         {@link SolrUtils#arcEntryDescriptorFieldList}.
     */
    public static Stream<SolrDocument> findImages(
            boolean attemptUnique, boolean goFast, int maxImagesPerPage, String query, String... filterQueries) {
        log.debug("findImages(attemptUnique={}, goFast={}, maxImagesPerPage={}, query='{}', filterQueries='{}') called",
                  attemptUnique, goFast, maxImagesPerPage, query, Arrays.asList(filterQueries));
        // Pruners are shared between direct and webpage, but not for final pruning as that would give 0 results
        final Predicate<SolrDocument> sharedHashPruner = attemptUnique ?
                new UniqueFilter(true, 20_000_000, "hash") :
                doc -> true;
        final Predicate<SolrDocument> finalHashPruner = attemptUnique ?
                new UniqueFilter(true, 20_000_000, "hash") :
                doc -> true;
        final Predicate<SolrDocument> sharedUrlPruner;
        final Predicate<SolrDocument> finalUrlPruner;
        final Predicate<String> sharedLinkPruner;
        if (goFast) {
            finalUrlPruner = new UniqueFilter(true, 20_000_000, "url_norm");
            UniqueFilter uf = new UniqueFilter(true, 20_000_000, "url_norm");
            sharedUrlPruner = uf;
            sharedLinkPruner = uf::test;
        } else {
            finalUrlPruner = doc -> true;
            sharedUrlPruner = doc -> true;
            sharedLinkPruner = link -> true;
        }

        SRequest directImagesReq = SRequest.builder().
                query(query).
                shardDivide("never"). // We are conservative here
                filterQueries(SolrUtils.extend("content_type_norm:image", filterQueries)).
                fields(SolrUtils.arcEntryDescriptorFieldList);
        if (goFast) { // TODO: Consider if this should be an explicit option instead
            directImagesReq.useCachingClient(true);
        }
        Stream<SolrDocument> directImages = directImagesReq.stream().
                filter(sharedHashPruner).
                filter(sharedUrlPruner);

        SRequest htmlRequest = SRequest.builder().
                query(query).
                shardDivide("never"). // The conservative choice
                filterQueries(SolrUtils.extend("content_type_norm:html", filterQueries)).
                fields("crawl_date, links_images").
                pageSize(100); // The links-field can be heavy and we want low latency
        Stream<SolrDocument> htmlPages = htmlRequest.stream().
                filter(solrDoc -> solrDoc.containsKey("links_images"));

        Stream<SolrDocument> htmlImages;
        if (goFast) {
            htmlImages = resolveImagesFromPageRequest(htmlPages, maxImagesPerPage, sharedLinkPruner).
                    useCachingClient(true).
                    shardDivide("never"). // Conservative choice
                    stream();
        } else {
            Stream<Callable<Stream<SolrDocument>>> htmlCallbacks = htmlPages.
                    map(htmlPage -> createHTMLImageCallback(htmlPage, maxImagesPerPage));
            htmlImages = Processing.batch(htmlCallbacks).
                    flatMap(Functions.identity()).
                    filter(sharedHashPruner).
                    filter(sharedUrlPruner);
        }

        Stream<SolrDocument> merged =
                CollectionUtils.interleave(Arrays.asList(directImages, htmlImages), Arrays.asList(4, 1)).
                filter(finalHashPruner).
                filter(finalUrlPruner);

        // Mix the two streams, 4 direct images for each 1 image derived from a page
        return merged.filter(new ThroughputTracker("findImages:", "images", log, 100));
    }

    /**
     * Creates a callable delivering up to maxImages {@link SolrDocument}s for images listed in
     * {@code links_images} for the given htmlPage. The documents will contain the fields from
     * {@link SolrUtils#arcEntryDescriptorFieldList}.
     * The images are searched using {@link SRequest#timeProximityDeduplication(String, String)}
     * meaning that only one instance of a given image URL is returned, with preference to the one nearest in time to
     * the htmlPage.
     * <p>
     * Note: Small images (less than 2000 pixels) are ignored, as are revisits.
     * @param htmlPage  a representation of a HTML page with links to images.
     *                  Required fields are {@code crawl_date} and {@code links_images}.
     * @param maxImages the maximum number of images to return.
     * @return a callable that will result in at most maxImages images linked from the given htmlPage.
     */
    public static Callable<Stream<SolrDocument>> createHTMLImageCallback(SolrDocument htmlPage, int maxImages) {
        String isotime = DateUtils.getSolrDate((Date) htmlPage.get("crawl_date"));
        Stream<String> urlQueries = ((List<String>)htmlPage.get("links_images")).stream().
                distinct().
                map(SolrUtils::createQueryStringForUrl);

        SRequest request = SRequest.builder().
                queries(urlQueries).
                queryBatchSize(500). // URL-searches are single-clause queries, so we can use large batches
                filterQueries("content_type_norm:image",   // only images
                              SolrUtils.NO_REVISIT_FILTER, // No binary for revisits.
                              "image_size:[2000 TO *]").   // No small images. (fillers etc.)
                fields(SolrUtils.arcEntryDescriptorFieldList). // Contains hash used for uniqueness
                timeProximityDeduplication(isotime, "url_norm").
                shardDivide("never"). // Conservatice choise. Maybe too conservative?
                maxResults(maxImages); // No sense in returning more than maxImages from a sub-request

        // The strange construction where the stream is collected and then re-streamed is to ensure that the
        // resolving of all images happens at evaluation time of the lambda, i.e. by the executor service.
        // If the stream is returned directly, the evaluation will happen in the calling thread.
        return () ->  request.stream().collect(Collectors.toList()).stream();
    }

    /**
     * Extracts {@code links_images} from the given {@code SolrDocument}s with HTML pages and resolves them against
     * the index. This DOES NOT perform time-proximity prioritisation of the images relative to the HMTL page!
     * <p>
     * This method is fully streaming and supports processing of arbitrary size. Not that it does not guarantee that
     * the resolved images are unique!
     *
     * @param htmlPages        a stream of {@code SolrDocument}s representing HTML pages.
     * @param maxImagesPerPage the maximum number of image links to use per page.
     * @param linkPruner       filters which links are considered. To disable pruning, use {@code link -> true}.
     * @return request for {@code SolrDocument}s representing images from the pages.
     */
    public static SRequest resolveImagesFromPageRequest(
            Stream<SolrDocument> htmlPages, int maxImagesPerPage, Predicate<String> linkPruner) {
        Stream<String> urlQueries = htmlPages.
                // Get maxImagesPerPage links from each page
                flatMap(page -> ((List<String>)page.get("links_images")).stream().filter(linkPruner).limit(maxImagesPerPage)).
                map(SolrUtils::createQueryStringForUrl);
        return SRequest.builder().
                queries(urlQueries).
                // We limit query batch size for lower latency.

                // TODO: Internal for kb.dk: Not setting queryBatchSize to 100 and exporting images for 'gedeost' throws
                // org.apache.solr.client.solrj.impl.HttpSolrClient$RemoteSolrException: Error from server at http://localhost:52300/solr/ns: Expected mime type application/octet-stream but got text/html. <h1>Bad Message 414</h1><pre>reason: URI Too Long</pre>
                // But the request uses POST!?
                queryBatchSize(10). // Number of image links to resolve at once
                filterQueries("content_type_norm:image",   // Only images
                              SolrUtils.NO_REVISIT_FILTER, // No binary for revisits.
                              "image_size:[2000 TO *]").   // No small images. (fillers etc.)
                fields(SolrUtils.arcEntryDescriptorFieldList). // Contains hash used for uniqueness
                deduplicateField("url_norm");
    }

    /**
     * Use a Thread to write the {@code fields} from the given {@code docs} in the given {@code format} to the
     * returned InputStream.
     *
     * Supported formats are defined in {@link FORMAT}.
     * @param docs   Solr documents with the stated {@code fields}.
     * @param fields the fields to write. Some {@code format}s ignore these and write all fields in the {@code docs}.
     * @param format the format to write the data. See {@link FORMAT}.
     * @param gzip   if true, the output is GZIPped.
     * @throws IOException if the content could not be written.
     */
    public static InputStream deliver(Stream<SolrDocument> docs, String fields, String format, Boolean gzip)
            throws IOException {
        FORMAT realFormat = FORMAT.valueOf(format.toLowerCase(Locale.ROOT));

        return StreamBridge.outputToInputSafe(out -> {
            StreamBridge.SafeOutputStream finalOut = out;
            if (gzip) {
                try {
                    finalOut = new StreamBridge.SafeOutputStream(new GZIPOutputStream(out));
                } catch (IOException e) {
                    throw new RuntimeException("Unable to construct GZIPOutputStream");
                }
            }

            switch (realFormat) {
                case csv: {
                    writeCSV(docs, fields, finalOut);
                    break;
                }
                case json: {
                    writeJSON(docs, finalOut);
                    break;
                }
                case jsonl: {
                    writeJSONLines(docs, finalOut);
                    break;
                }
                default:
                    throw new UnsupportedOperationException("The format '" + format + "' is not supported");
            }
            finalOut.flush();
            finalOut.close();
        });
    }

    /**
     * Write the given SolrDocuments as <a href="https://jsonlines.org/">JSON-Lines</a>: One JSON block for each
     * document, with one block per line.
     * @param docs a Stream of Solr documents.
     * @param out where to write the output.
     */
    private static void writeJSONLines(Stream<SolrDocument> docs, StreamBridge.SafeOutputStream out) {
        docs.map(JsonUtils::toJSON).
                forEach(out::writeln);
    }

    /**
     * Write the given SolrDocuments as a single JSON array containing one JSON block for each document.
     * @param docs a Stream of Solr documents.
     * @param out where to write the output.
     */
    private static void writeJSON(Stream<SolrDocument> docs, StreamBridge.SafeOutputStream out) {
        AtomicBoolean hasWritten = new AtomicBoolean(false);
        out.writeln("[");
        docs.map(JsonUtils::toJSON).
                peek(json -> {
                    if (!hasWritten.get()) {
                        hasWritten.set(true);
                    } else {
                        out.writeln(",");
                    }
                }).
                forEach(out::write);
        out.writeln("\n]");
    }

    /**
     * Write the given SolrDocuments as Comma Separated Values.
     * First line acts as header and lists the field names.
     * @param docs a Stream of Solr documents.
     * @param out where to write the output.
     */
    private static void writeCSV(Stream<SolrDocument> docs, String fields, StreamBridge.SafeOutputStream out) {
        GenerateCSV csvMapper = new GenerateCSV(fields.split(", *"));
        docs.map(csvMapper::toCVSLine).
                forEach(out::write);
    }
}
