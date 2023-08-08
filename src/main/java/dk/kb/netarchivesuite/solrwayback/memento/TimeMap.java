package dk.kb.netarchivesuite.solrwayback.memento;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoaderWeb;
import dk.kb.netarchivesuite.solrwayback.solr.SRequest;
import dk.kb.netarchivesuite.solrwayback.util.DateUtils;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.text.ParseException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

public class TimeMap {
    private static final int RESULTS_PER_PAGE = 2;


    private static final Logger log = LoggerFactory.getLogger(TimeMap.class);
    //TODO: Implement Paged TimeMaps (Can maybe be done through the count of the first query)
    /**
     * Get timemap for specified URI-R. Timemap contains all captured mementos for the given resource.
     * @param originalResource  URI-R to fetch timemap for.
     * @param responseFormat    Mimetype which specifies how the response is to be delivered.
     *                          Defaults to application/link-type.
     * @return                  The timemap in the specified format.
     */
    public static StreamingOutput getTimeMap(URI originalResource, String responseFormat, Integer pageNumber) {

        if (responseFormat.equals("application/json")){
            return getTimeMapAsJson(originalResource, pageNumber);
        } else {
            return output -> {
                getTimeMapAsLinkFormat(originalResource, output);
            };
        }
    }

    /**
     * Creates a timemap (URI-T) for a URI-R as JSON.
     * @param originalResource  URI-R to create URI-T from.
     * @return                  A json representation of the timemap ready for streaming.
     */
    private static StreamingOutput getTimeMapAsJson(URI originalResource, int pageNumber) {
        MementoMetadata metadata = new MementoMetadata();

        long count = getDocStreamAndUpdateDatesForFirstAndLastMemento(originalResource, metadata)
                .count();
        if (count < 2){ //TODO: Set this through a property.
            log.info("Creating timemap of '{}' entries, with dates in range from '{}' to '{}'.",
                    count, metadata.getFirstMemento(), metadata.getLastMemento());

            Stream<SolrDocument> mementoStream = getMementoStream(originalResource);

            return getJSONStreamingOutput(originalResource, metadata, mementoStream);
        } else {
            log.info("Creating paged timemaps of '{}' entries, with dates in range from '{}' to '{}'.",
                    count, metadata.getFirstMemento(), metadata.getLastMemento());

            Stream<SolrDocument> mementoStream = getMementoStream(originalResource);

            return getJSONPagedStreamingOutput(originalResource, metadata, mementoStream, count, pageNumber);
        }
    }

    /**
     * Return the requested timemap as a paged result.
     * @param originalResource  which the timemap is fetched for.
     * @param metadata          containing information on the mementos of the original resource.
     * @param mementoStream     containing all mementos, ready for being paged.
     * @param count             total amount of mementos.
     * @return                  A paged JSON timemap ready for streaming.
     */
    private static StreamingOutput getJSONPagedStreamingOutput(URI originalResource, MementoMetadata metadata,
                                                               Stream<SolrDocument> mementoStream,
                                                               long count, int pageNumber) {

        Page<SolrDocument> pageOfResults = getPage(mementoStream, pageNumber, count);
        if ((long) RESULTS_PER_PAGE * pageNumber > count){
            throw new IllegalArgumentException("The requested page does not exist. The last page for this memento is: '"
                                                + count/RESULTS_PER_PAGE + "'.");
        }

        return os -> {
            JsonGenerator jg = getStartOfJsonTimeMap(originalResource, metadata, os);

            jg.writeFieldName("list");
            jg.writeStartArray(); //list start
            long processedMementoCount = pageOfResults.getItems()
                    .map(doc -> addMementoToTimeMapObject(doc, jg, originalResource))
                    .count();
            log.info("processedMementoCount is: " + processedMementoCount);
            log.info("Wrote '{}' mementos to JSON timemap.", processedMementoCount);
            jg.writeEndArray(); //list end
            jg.writeEndObject(); //mementos end

            jg.writeFieldName("pages");
            jg.writeStartObject();//pages start
            if (pageNumber - 1 != 0) {
                jg.writeFieldName("prev");
                jg.writeStartObject(); //pages.prev start
                jg.writeFieldName("uri");
                jg.writeString(PropertiesLoaderWeb.WAYBACK_SERVER + "services/memento/timemap/" +
                        (pageNumber - 1) + "/json/" + originalResource);
                jg.writeEndObject();//pages.prev end
            }
            if ((long) RESULTS_PER_PAGE * pageNumber < count){
                jg.writeFieldName("next");
                jg.writeStartObject(); //pages.next start
                jg.writeFieldName("uri");
                jg.writeString(PropertiesLoaderWeb.WAYBACK_SERVER + "services/memento/timemap/" +
                        (pageNumber + 1) + "/json/" + originalResource);
                jg.writeEndObject(); //pages.next end
            }
            jg.writeEndObject(); //pages end
            jg.writeEndObject(); //timemap end
            jg.flush();
            jg.close();
        };
    }

    /**
     * Create the beginning of a JSON timemap. Containing original URI, timegate URI, timemap URI and information on first and last memento.
     * @param originalResource  to create timemap for.
     * @param metadata          object which contains data used for constructing mementos.
     * @param os                outputstream to write JSON to.
     * @return                  the JSON generator with the beginning of the timemap written to it.
     */
    private static JsonGenerator getStartOfJsonTimeMap(URI originalResource, MementoMetadata metadata, OutputStream os) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonGenerator jg = objectMapper.getFactory().createGenerator(os, JsonEncoding.UTF8);

        jg.writeStartObject(); // timemap start
        jg.writeFieldName("original_uri");
        jg.writeString(originalResource.toString());
        jg.writeFieldName("timegate_uri");
        jg.writeString(PropertiesLoaderWeb.WAYBACK_SERVER + "services/memento/" + originalResource);

        jg.writeFieldName("timemap_uri");
        jg.writeStartObject(); //timemap_uri start
        jg.writeFieldName("link_format");
        jg.writeString(PropertiesLoaderWeb.WAYBACK_SERVER + "services/memento/timemap/" + originalResource);
        jg.writeFieldName("json_format");
        jg.writeString(PropertiesLoaderWeb.WAYBACK_SERVER + "services/memento/timemap/" + originalResource);
        jg.writeEndObject(); //timemap_uri end

        jg.writeFieldName("mementos");
        jg.writeStartObject(); //mementos start
        jg.writeFieldName("first");
        jg.writeStartObject(); //first start
        jg.writeFieldName("datetime");
        jg.writeString(metadata.getFirstMemento());
        jg.writeFieldName("uri");
        jg.writeString(PropertiesLoaderWeb.WAYBACK_SERVER + metadata.getFirstWaybackDate() + "/" + originalResource);
        jg.writeEndObject(); //first end

        jg.writeFieldName("last");
        jg.writeStartObject(); //last start
        jg.writeFieldName("datetime");
        jg.writeString(metadata.getLastMemento());
        jg.writeFieldName("uri");
        jg.writeString(PropertiesLoaderWeb.WAYBACK_SERVER + metadata.getLastWaybackDate() + "/" + originalResource);
        jg.writeEndObject(); //last end
        return jg;
    }

    /**
     * Writes a timemap (URI-T) for a URI-R to an outputstream in the link-type format.
     * @param originalResource  URI-R to create URI-T from.
     * @param output            Stream which the output is delivered to.
     */
    private static void getTimeMapAsLinkFormat(URI originalResource, OutputStream output) throws IOException {
        MementoMetadata metadata = new MementoMetadata();

        // Sadly we need to do two Solr calls as it doesn't seem possible to calculate the dates for the header,
        // in the second stream and add it to the output as the first thing. Please correct me if im wrong.
        long count = getDocStreamAndUpdateDatesForFirstAndLastMemento(originalResource, metadata)
                .map(doc1 -> updateTimeMapHeadForLinkFormat(doc1, metadata, originalResource.toString()))
                .count();

        log.info("Creating timemap of '{}' entries, with dates in range from '{}' to '{}'.",
                count, metadata.getFirstMemento(), metadata.getLastMemento());

        output.write(metadata.getTimeMapHead().getBytes());

        AtomicLong iterator = new AtomicLong(1);
        getMementoStream(originalResource)
                .map(doc -> createMementoInLinkFormat(doc, iterator, count))
                .forEach(s -> writeStringSafe(s, output));
    }


    /**
     * Stream solrdocuments containing metadata used to create memento entries for a timemap.
     * Sorting on ID to easier find first and last mementos.
     * @param originalResource  to create timemap for
     * @return                  A stream of solr documents. Containing all harvests of given url.
     */
    private static Stream<SolrDocument> getMementoStream(URI originalResource) {
        return SRequest.builder().query("url_norm:\"" + originalResource + "\"")
                .fields("url", "url_norm", "wayback_date")
                .sort("id asc")
                .stream();
    }

    /**
     * Create a JSON representation of a timemap for an original resource and return it as a streaming output.
     * @param originalResource  to create timemap of.
     * @param metadata          object containing different metatdata extracted from solr, used to create the timemap.
     * @param mementoStream     Stream of solr documents containing all mementos of the given original resource.
     * @return                  A JSON timemap for the input original resource.
     */
    private static StreamingOutput getJSONStreamingOutput(URI originalResource, MementoMetadata metadata, Stream<SolrDocument> mementoStream) {

        return os -> {
            JsonGenerator jg = getStartOfJsonTimeMap(originalResource, metadata, os);
            jg.writeFieldName("list");
            jg.writeStartArray(); //list start
            long processedMementoCount = mementoStream
                    .map(doc -> addMementoToTimeMapObject(doc, jg, originalResource))
                    .count();
            log.info("Wrote '{}' mementos to JSON timemap.", processedMementoCount);
            jg.writeEndArray(); //list end
            jg.writeEndObject(); //mementos end
            jg.writeEndObject(); //timemap end
            jg.flush();
            jg.close();
        };
    }


    /**
     * Method used as an intermediate operation on a stream of SolrDocuments.
     * Adds the individual memento from the stream to the JsonGenerator, which is producing the timemap in JSON.
     * @param solrDoc               containing metadata on a single memento.
     * @param jsonGenerator     generating the full timemap for the original resource. This generator already contains
     *                          the first part of the timemap, is now ready to generate the JSON representation of the
     *                          individual mementos.
     * @param originalResource  contains the URI of the original resource.
     * @return                  The inputted SolrDocument for further use in the stream.
     */
    private static SolrDocument addMementoToTimeMapObject(SolrDocument solrDoc, JsonGenerator jsonGenerator, URI originalResource) {
        try {
            jsonGenerator.writeStartObject(); // listEntry start
            jsonGenerator.writeFieldName("datetime");
            jsonGenerator.writeString(DateUtils.convertWaybackdate2Mementodate((long) solrDoc.get("wayback_date")));
            jsonGenerator.writeFieldName("uri");
            jsonGenerator.writeString(PropertiesLoaderWeb.WAYBACK_SERVER + "services/web/" + solrDoc.get("wayback_date") + "/" + originalResource);
            jsonGenerator.writeEndObject(); // listEntry end
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }

        return solrDoc;
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
    private static Stream<SolrDocument> getDocStreamAndUpdateDatesForFirstAndLastMemento(URI originalResource, MementoMetadata metadata) {
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

    /**
     * Update the memento timemap "header", which contains information on the original resource and the range of dates
     * included in the timemap
     * @param doc               Solr document is not used by the method, only delivered and returned to make streaming
     *                          workflow continue.
     * @param metadata          object that gets updated with the timemap header.
     * @param originalResource  url of the original resource (URI-R). The URI-R is used in the header.
     * @return                  the original solr document for further streaming.
     */
    private static SolrDocument updateTimeMapHeadForLinkFormat(SolrDocument doc, MementoMetadata metadata, String originalResource) {
        metadata.setTimeMapHeadForLinkFormat(originalResource);
        return doc;
    }


    /**
     * Create an application/link-format compliant memento representation of an archived resource from solr.
     *
     * @param doc               The solr document, that contains information on the individual harvested resource.
     * @param iterator          Used define the relations 'first memento' and 'last memento'
     * @param countOfMementos   Used define the relations 'first memento' and 'last memento'
     * @return                  The memento as a string, ready to be concatenated to a memento timemap.
     */
    private static String createMementoInLinkFormat(SolrDocument doc, AtomicLong iterator, Long countOfMementos) {
        String memento = "";
        try {
            if (iterator.longValue() == 1L){
                memento = "<" + PropertiesLoaderWeb.WAYBACK_SERVER + "services/web/" +
                        doc.getFieldValue("wayback_date") + "/" + doc.getFieldValue("url") + ">\n" +
                        "; rel=\"first memento\"; datetime=\"" + DateUtils.convertWaybackdate2Mementodate((Long) doc.getFieldValue("wayback_date")) + "\"\n";
                iterator.getAndIncrement();
            } else if (iterator.longValue() == countOfMementos) {
                memento = "<" + PropertiesLoaderWeb.WAYBACK_SERVER + "services/web/" +
                        doc.getFieldValue("wayback_date") + "/" + doc.getFieldValue("url") + ">\n" +
                        "; rel=\"last memento\"; datetime=\"" + DateUtils.convertWaybackdate2Mementodate((Long) doc.getFieldValue("wayback_date")) + "\"\n";
                iterator.getAndIncrement();
            } else {
                memento = "<" + PropertiesLoaderWeb.WAYBACK_SERVER + "services/web/" +
                        doc.getFieldValue("wayback_date") + "/" + doc.getFieldValue("url") + ">\n" +
                        "; rel=\"memento\"; datetime=\"" + DateUtils.convertWaybackdate2Mementodate((Long) doc.getFieldValue("wayback_date")) + "\"\n";
                iterator.getAndIncrement();
            }
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        // TODO: Add license
        return memento;
    }

    /**
     * Writes a string/memento to an outputstream.
     * @param string        memento to add to outputstream.
     * @param outputStream  outputstream written to. This delivers the output to the request.
     */
    private static void writeStringSafe(String string, OutputStream outputStream) {
        try {
            outputStream.write(string.getBytes());
        } catch (RuntimeException | IOException e){
            throw new RuntimeException();
        }
    }

    // PAGING METHOD AND HELPER CLASS

    /**
     * Method to divide a stream of SolrDocuments into pages of streams.
     * @param streamOfDocs          original stream, which pages are created from.
     * @param pageNumber            of the page to retrieve.
     * @param numberOfDocsInStream  amount of total documents in the main stream.
     * @return                      a page of solr documents.
     */
    private static Page<SolrDocument> getPage(Stream<SolrDocument> streamOfDocs, int pageNumber, long numberOfDocsInStream) {
        int skipCount = (pageNumber - 1) * RESULTS_PER_PAGE;

        Stream<SolrDocument> solrDocs = streamOfDocs
                                        .skip(skipCount)
                                        .limit(RESULTS_PER_PAGE);

        Page<SolrDocument> page = new Page<>(pageNumber, numberOfDocsInStream, solrDocs);

        return page;
    }


    /**
     * Class that implements a paging mechanism for streams.
     * @param <T> the type of objects in the stream, that is to be paged.
     */
    private static class Page<T> {
        private Integer pageNumber;
        private Integer resultsPerPage;
        private Long totalResults;
        private Stream<T> items;

        /**
         * Standard constructor for a page.
         * @param pageNumber    to retrieve.
         * @param totalResults  the size of the original stream.
         * @param items         to include on the page.
         */
        public Page(Integer pageNumber, Long totalResults, Stream<T> items) {
            this.pageNumber = pageNumber;
            this.resultsPerPage = RESULTS_PER_PAGE;
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
