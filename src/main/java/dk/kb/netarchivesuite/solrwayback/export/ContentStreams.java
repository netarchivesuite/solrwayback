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
import dk.kb.netarchivesuite.solrwayback.facade.Facade;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntryDescriptor;
import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDoc;
import dk.kb.netarchivesuite.solrwayback.solr.NetarchiveSolrClient;
import dk.kb.netarchivesuite.solrwayback.solr.SolrGenericStreaming;
import dk.kb.netarchivesuite.solrwayback.util.CollectionUtils;
import dk.kb.netarchivesuite.solrwayback.util.Processing;
import dk.kb.netarchivesuite.solrwayback.util.SolrUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
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
     * @param searchText       a query for images.
     * @param maxImagesPerPage maximum number of images to resolve for any single page.
     * @return a stream of image descriptors.
     */
    public static Stream<ArcEntryDescriptor> findImages(String searchText, int maxImagesPerPage) {
        Stream<ArcEntryDescriptor> directImages =
                SolrGenericStreaming.create(
                                Arrays.asList(SolrUtils.arcEntryDescriptorFieldList.split(", *")),
                                searchText, "content_type_norm:image").stream().
                        map(SolrUtils::solrDocument2ArcEntryDescriptor);

        Stream<SolrDocument> htmlPages = SolrGenericStreaming.create(
                        Arrays.asList("crawl_date", "links_images"),
                        searchText, "content_type_norm:html").stream().
                filter(solrDoc -> solrDoc.containsKey("links_images") &&
                                  !solrDoc.getFieldValues("links_images").isEmpty());

        Stream<ArcEntryDescriptor> htmlImages =
                // TODO: Make the maxImages per page configurable
                Processing.batch(htmlPages.map(htmlPage -> createHTMLImageCallback(htmlPage, maxImagesPerPage))).
                        flatMap(Functions.identity());

        // Mix the two streams, 4 direct images for each 1 image derived from a page
        return CollectionUtils.interleave(Arrays.asList(directImages, htmlImages), Arrays.asList(4, 1));
    }

    /**
     * Resolves {@code crawl_date} and {@code links_images} for the webpage with the given source_file_path and
     * source_file_offset then resolves all the images in {@code links_images}. If there are multiple images with
     * the same {@code url_norm}, the one closest to {@code crawl_date} is returned.
     *
     * If the resource designated by the given path and offset is not a webpage, an empty stream is returned.
     * @param source_file_path   path for the resource, as indexed in Solr.
     * @param source_file_offset offset for the resource, as indexed in Solr.
     * @return the images linked from the page.
     * @throws Exception if the webpage could not be found or image resolving failed.
     */
    public Stream<ArcEntryDescriptor> getImagesFromWebpage(String source_file_path, long source_file_offset)
            throws Exception {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.set("facet", "false");
        solrQuery.add("fl", "crawl_date,links_images");
        solrQuery.setQuery("source_file_path:\"" + source_file_path + "\" AND source_file_offset:" + source_file_offset);
        solrQuery.setRows(1);
        QueryResponse rsp = NetarchiveSolrClient.query(solrQuery, false);
        SolrDocumentList docs = rsp.getResults();

        if (docs.isEmpty()) {
            throw new IOException("Could not find arc entry with source file path='" + source_file_path +
                                  "' and offset=" + source_file_offset);
        }
        return createHTMLImageCallback(docs.get(0), Integer.MAX_VALUE).call();
    }

    /**
     * Creates a callable delivering up to maxImages {@link ArcEntryDescriptor}s for images listed in
     * {@code links_images} for the given htmlPage. The images are searched using
     * {@link SolrGenericStreaming.SRequest#timeProximityDeduplication(String, String)} meaning that only one
     * instance of a given image URL is returned, with preference to the one nearest in time to the htmlPage.
     *
     * Note: Small images (less than 2000 pixels) are ignored, as are revisits.
     * @param htmlPage  a representation of a HTML page with links to images.
     *                  Required fields are {@code crawl_date} and {@code links_images}.
     * @param maxImages the maximum number of images to return.
     * @return a callable that will result in at most maxImages images linked from the given htmlPage.
     */
    public static Callable<Stream<ArcEntryDescriptor>> createHTMLImageCallback(SolrDocument htmlPage, int maxImages) {
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
                limit(maxImages).
                map(SolrUtils::solrDocument2ArcEntryDescriptor);
    }

}
