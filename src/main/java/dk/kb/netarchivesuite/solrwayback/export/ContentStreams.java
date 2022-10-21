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
import dk.kb.netarchivesuite.solrwayback.solr.SolrGenericStreaming;
import dk.kb.netarchivesuite.solrwayback.util.CollectionUtils;
import dk.kb.netarchivesuite.solrwayback.util.Processing;
import dk.kb.netarchivesuite.solrwayback.util.SolrUtils;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

/**
 * Stream oriented content delivery, suitable for export as well as special searches such as image search.
 */
public class ContentStreams {
    private static final Logger log = LoggerFactory.getLogger(ContentStreams.class);

    /**
     * Searches images and webpages matching the given searchText. For webpages, linked images are resolved and
     * returned. In the case of multiple hits for the same image URL, the one harvested closest to the webpage
     * in time will be delivered.
     *
     * If the caller needs {@link ArcEntryDescriptor}, use the helpers method in SolrUtils as
     * {@code findImages(50, "kittens").map(SolrUtils::solrDocument2ArcEntryDescriptor)}.
     * @param maxImagesPerPage maximum number of images to resolve for any single page.
     * @param query       a query for images.
     * @param filterQueries 0 or more Solr queries.
     * @return a stream of SolrDocuments representing images, containing the fields from
     *         {@link SolrUtils#arcEntryDescriptorFieldList}.
     */
    public static Stream<SolrDocument> findImages(int maxImagesPerPage, String query, String... filterQueries) {
        Stream<SolrDocument> directImages =
                SolrGenericStreaming.create(
                                Arrays.asList(SolrUtils.arcEntryDescriptorFieldList.split(", *")),
                                query, SolrUtils.extend("content_type_norm:image", filterQueries)).stream();

        Stream<SolrDocument> htmlPages = SolrGenericStreaming.create(
                        Arrays.asList("crawl_date", "links_images"),
                        query, SolrUtils.extend("content_type_norm:html", filterQueries)).stream().
                filter(solrDoc -> solrDoc.containsKey("links_images") &&
                                  !solrDoc.getFieldValues("links_images").isEmpty());

        Stream<SolrDocument> htmlImages =
                // TODO: Make the maxImages per page configurable
                Processing.batch(htmlPages.map(htmlPage -> createHTMLImageCallback(htmlPage, maxImagesPerPage))).
                        flatMap(Functions.identity());

        // Mix the two streams, 4 direct images for each 1 image derived from a page
        return CollectionUtils.interleave(Arrays.asList(directImages, htmlImages), Arrays.asList(4, 1));
    }

    /**
     * Creates a callable delivering up to maxImages {@link SolrDocument}s for images listed in
     * {@code links_images} for the given htmlPage. The documents will contain the fields from
     * {@link SolrUtils#arcEntryDescriptorFieldList}.
     * The images are searched using {@link SolrGenericStreaming.SRequest#timeProximityDeduplication(String, String)}
     * meaning that only one instance of a given image URL is returned, with preference to the one nearest in time to
     * the htmlPage.
     *
     * Note: Small images (less than 2000 pixels) are ignored, as are revisits.
     * @param htmlPage  a representation of a HTML page with links to images.
     *                  Required fields are {@code crawl_date} and {@code links_images}.
     * @param maxImages the maximum number of images to return.
     * @return a callable that will result in at most maxImages images linked from the given htmlPage.
     */
    public static Callable<Stream<SolrDocument>> createHTMLImageCallback(SolrDocument htmlPage, int maxImages) {
        String timestamp = htmlPage.get("crawl_date").toString();
        Stream<String> urlQueries = ((List<String>)htmlPage.get("links_images")).stream().
                distinct().
                map(SolrUtils::createQueryStringForUrl);

        SolrGenericStreaming.SRequest baseRequest =
                SolrGenericStreaming.SRequest.builder().
                        filterQueries("content_type_norm:image",   // only images
                                      SolrUtils.NO_REVISIT_FILTER, // No binary for revisits.
                                      "image_size:[2000 TO *]").   // No small images. (fillers etc.)
                        fields(SolrUtils.arcEntryDescriptorFieldList).
                        timeProximityDeduplication(timestamp, "url_norm").
                        maxResults(maxImages); // No sense in returning more than maxImages from a sub-request

        return () -> SolrGenericStreaming.multiQuery(baseRequest, urlQueries, 500).
                flatMap(SolrGenericStreaming::stream).
                limit(maxImages);
    }

}
