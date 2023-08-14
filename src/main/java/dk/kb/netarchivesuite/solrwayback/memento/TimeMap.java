package dk.kb.netarchivesuite.solrwayback.memento;

import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.solr.SRequest;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.StreamingOutput;
import java.net.URI;
import java.text.ParseException;
import java.util.stream.Stream;

public class TimeMap {
    private static final Logger log = LoggerFactory.getLogger(TimeMap.class);

    /**
     * Get timemap for specified URI-R. Timemap contains all captured mementos for the given resource.
     * @param originalResource  URI-R to fetch timemap for.
     * @param responseFormat    Mimetype which specifies how the response is to be delivered.
     *                          Defaults to application/link-type.
     * @return                  The timemap in the specified format.
     */
    public static StreamingOutput getTimeMap(URI originalResource, String responseFormat, Integer pageNumber) {

        if (responseFormat.equals("application/json")){
            return TimeMapAsJSON.getTimeMapAsJson(originalResource, pageNumber);
        } else {
            return output -> TimeMapAsLink.getTimeMapAsLinkFormat(originalResource, output, pageNumber);
        }
    }


    /**
     * Stream solrdocuments containing metadata used to create memento entries for a timemap.
     * Sorting on ID to easier find first and last mementos.
     * @param originalResource  to create timemap for
     * @return                  A stream of solr documents. Containing all harvests of given url.
     */
     static Stream<SolrDocument> getMementoStream(URI originalResource) {
        return SRequest.builder().query("url_norm:\"" + originalResource + "\"")
                .fields("url", "url_norm", "wayback_date")
                .sort("id asc")
                .stream();
    }


    /**
     * Method used as an intermediate operation on a stream of SolrDocuments.
     * This method sorts the mementos and saves the earliest and latest version
     * of dates to the given MementoMetadata object.
     * Remember that streams are lazy and only fired when a terminal operation is issued.
     * @param originalResource  to query solr with.
     * @param metadata          object that earliest and latest dates gets contained in.
     * @return                  the created stream of SolrDocuments for further use.
     */
    static Stream<SolrDocument> getDocStreamAndUpdateDatesForFirstAndLastMemento(URI originalResource, MementoMetadata metadata) {
        return SRequest.builder().query("url_norm:\"" + originalResource + "\"")
                .fields("wayback_date")
                .sort("id asc")
                .stream()
                .map(doc1 -> saveFirstAndLastDate(doc1, metadata));
    }

    /**
     * Extract wayback date from solr document and convert to RFC 7089 compliant date.
     * Only adds the first and last date to the metadata object.
     * @param doc       solr document to extract date from.
     * @param metadata  object containint metadata for each memento.
     * @return          the original solr document, for further streaming.
     */
    private static SolrDocument saveFirstAndLastDate(SolrDocument doc, MementoMetadata metadata) {
        if ((long) doc.getFieldValue("wayback_date") < metadata.getFirstWaybackDate()){
            metadata.setFirstWaybackDate( (Long) doc.getFieldValue("wayback_date"));
        }
        if ((long) doc.getFieldValue("wayback_date") > metadata.getLastWaybackDate()){
            metadata.setLastWaybackDate( (Long) doc.getFieldValue("wayback_date"));
        }
        try{
            metadata.setFirstMementoFromFirstWaybackDate();
            metadata.setLastMementoFromLastWaybackDate();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return doc;
    }


    // PAGING METHOD AND HELPER CLASS

    /**
     * Method to divide a stream of SolrDocuments into pages of streams.
     * @param streamOfDocs          original stream, which pages are created from.
     * @param pageNumber            of the page to retrieve.
     * @param numberOfDocsInStream  amount of total documents in the main stream.
     * @return                      a page of solr documents.
     */
    static Page<SolrDocument> getPage(Stream<SolrDocument> streamOfDocs, int pageNumber, long numberOfDocsInStream) {
        int skipCount = (pageNumber - 1) * PropertiesLoader.MEMENTO_TIMEMAP_PAGESIZE;

        //TODO: something smells
        if (pageNumber == 0){
            skipCount = 0;
        }

        Stream<SolrDocument> solrDocs = streamOfDocs
                                        .skip(skipCount)
                                        .limit(PropertiesLoader.MEMENTO_TIMEMAP_PAGESIZE);

        Page<SolrDocument> page = new Page<>(pageNumber, numberOfDocsInStream, solrDocs);

        return page;
    }


    /**
     * Class that implements a paging mechanism for streams.
     * @param <T> the type of objects in the stream, that is to be paged.
     */
    static class Page<T> {
        private Integer pageNumber;
        private Integer resultsPerPage;
        private Long totalResults;
        Stream<T> items;

        /**
         * Standard constructor for a page.
         * @param pageNumber    to retrieve.
         * @param totalResults  the size of the original stream.
         * @param items         to include on the page.
         */
        public Page(Integer pageNumber, Long totalResults, Stream<T> items) {
            this.pageNumber = pageNumber;
            this.resultsPerPage = PropertiesLoader.MEMENTO_TIMEMAP_PAGESIZE;
            this.totalResults = totalResults;
            this.items = items;
        }

        public Integer getPageNumber() {
            return pageNumber;
        }
        public void setPageNumber(Integer pageNumber) {
            this.pageNumber = pageNumber;
        }
        public Integer getResultsPerPage() {
            return resultsPerPage;
        }
        public void setResultsPerPage(Integer resultsPerPage) {
            this.resultsPerPage = resultsPerPage;
        }
        public Stream<T> getItems() {
            return items;
        }
        public void setItems(Stream<T> items) {
            this.items = items;
        }
        public Long getTotalResults() {
            return totalResults;
        }
        public void setTotalResults(Long totalResults) {
            this.totalResults = totalResults;
        }
    }
}
