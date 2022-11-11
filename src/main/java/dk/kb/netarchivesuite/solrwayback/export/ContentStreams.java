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
import dk.kb.netarchivesuite.solrwayback.solr.SolrGenericStreaming;
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
     * @param hashUnique if true, uniqueness of results is attempted.
     * @param maxImagesPerPage maximum number of images to resolve for any single page.
     * @param query       a query for images.
     * @param filterQueries 0 or more Solr queries.
     * @return a stream of SolrDocuments representing images, containing the fields from
     *         {@link SolrUtils#arcEntryDescriptorFieldList}.
     */
    public static Stream<SolrDocument> findImages(
            boolean hashUnique, int maxImagesPerPage, String query, String... filterQueries) {
        SRequest imageRequest = SRequest.builder().
                query(query).
                filterQueries(SolrUtils.extend("content_type_norm:image", filterQueries)).
                fields(SolrUtils.arcEntryDescriptorFieldList).
                deduplicateField("url_norm").
                maxResults(maxImagesPerPage);

        Stream<SolrDocument> directImages = imageRequest.stream().filter(new ThroughputTracker("direct:", "images", log, 100));

        Stream<SolrDocument> htmlPages = SolrGenericStreaming.create(
                        Arrays.asList("crawl_date", "links_images"),
                        query, SolrUtils.extend("content_type_norm:html", filterQueries)).stream().
                filter(solrDoc -> solrDoc.containsKey("links_images") &&
                                  !solrDoc.getFieldValues("links_images").isEmpty());

        UniqueFilter urlUnique = new UniqueFilter(true, 20_000_000, "url_norm");

        Stream<Callable<Stream<SolrDocument>>> htmlCallbacks = htmlPages.
                map(htmlPage -> createHTMLImageCallback(htmlPage, urlUnique, maxImagesPerPage));

        Stream<SolrDocument> htmlImages = Processing.batch(htmlCallbacks).flatMap(Functions.identity());

        Stream<SolrDocument> merged =
                CollectionUtils.interleave(Arrays.asList(directImages, htmlImages), Arrays.asList(4, 1));
        merged = merged.filter(new ThroughputTracker("beforeUnique:", "images", log, 100));
        if (hashUnique) {
            merged = merged.filter(new UniqueFilter(true, 20_000_000, "hash"));
        }
        // Mix the two streams, 4 direct images for each 1 image derived from a page
        return merged.filter(new ThroughputTracker("findImages:", "images", log, 10));
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
     *
     * @param htmlPage  a representation of a HTML page with links to images.
     *                  Required fields are {@code crawl_date} and {@code links_images}.
     * @param urlUnique
     * @param maxImages the maximum number of images to return.
     * @return a callable that will result in at most maxImages images linked from the given htmlPage.
     */
    public static Callable<Stream<SolrDocument>> createHTMLImageCallback(SolrDocument htmlPage, UniqueFilter uniqueFilter, int maxImages) {
        String isotime = DateUtils.getSolrDate((Date) htmlPage.get("crawl_date"));
        Stream<String> urlQueries = ((List<String>)htmlPage.get("links_images")).stream().
                filter(uniqueFilter::test).
                map(SolrUtils::createQueryStringForUrl);

        SRequest request = SRequest.builder().
                queries(urlQueries).
                queryBatchSize(500). // URL-searches are single-clause queries, so we can use large batches
                filterQueries("content_type_norm:image",   // only images
                              SolrUtils.NO_REVISIT_FILTER, // No binary for revisits.
                              "image_size:[2000 TO *]").   // No small images. (fillers etc.)
                fields(SolrUtils.arcEntryDescriptorFieldList). // Contains hash used for uniqueness
                timeProximityDeduplication(isotime, "url_norm").
                maxResults(maxImages); // No sense in returning more than maxImages from a sub-request
        return () -> {
//            long startNS = System.nanoTime();
            List<SolrDocument> result = request.stream().
//                    filter(new ThroughputTracker("htmlPageNearImages", "image", log, 10)).
                    collect(Collectors.toList());
            return result.stream();

        };
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
