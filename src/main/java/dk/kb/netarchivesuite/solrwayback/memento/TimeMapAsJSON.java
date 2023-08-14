package dk.kb.netarchivesuite.solrwayback.memento;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.xml.fastinfoset.sax.Properties;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoaderWeb;
import dk.kb.netarchivesuite.solrwayback.util.DateUtils;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.text.ParseException;
import java.util.stream.Stream;

import static dk.kb.netarchivesuite.solrwayback.memento.TimeMap.getDocStreamAndUpdateDatesForFirstAndLastMemento;
import static dk.kb.netarchivesuite.solrwayback.memento.TimeMap.getMementoStream;
import static dk.kb.netarchivesuite.solrwayback.memento.TimeMap.getPage;

public class TimeMapAsJSON {
    private static final Logger log = LoggerFactory.getLogger(TimeMapAsJSON.class);


    /**
     * Creates a timemap (URI-T) for a URI-R as JSON.
     * @param originalResource  URI-R to create URI-T from.
     * @return                  A json representation of the timemap ready for streaming.
     */
    static StreamingOutput getTimeMapAsJson(URI originalResource, Integer pageNumber) {
        MementoMetadata metadata = new MementoMetadata();

        long count = getDocStreamAndUpdateDatesForFirstAndLastMemento(originalResource, metadata)
                .count();
        log.info("Original resource has been harvested '{}' times.",count);
        if (count < PropertiesLoader.MEMENTO_TIMEMAP_PAGINGLIMIT){
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
     * Create a JSON representation of a timemap for an original resource and return it as a streaming output.
     * @param originalResource  to create timemap of.
     * @param metadata          object containing different metatdata extracted from solr, used to create the timemap.
     * @param mementoStream     Stream of solr documents containing all mementos of the given original resource.
     * @return                  A JSON timemap for the input original resource.
     */
    private static StreamingOutput getJSONStreamingOutput(URI originalResource, MementoMetadata metadata, Stream<SolrDocument> mementoStream) {

        return os -> {
            JsonGenerator jg = getStartOfJsonTimeMap(originalResource, metadata, os, 0);
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
     * Return the requested timemap as a paged result.
     * @param originalResource  which the timemap is fetched for.
     * @param metadata          containing information on the mementos of the original resource.
     * @param mementoStream     containing all mementos, ready for being paged.
     * @param count             total amount of mementos.
     * @return                  A paged JSON timemap ready for streaming.
     */
    private static StreamingOutput getJSONPagedStreamingOutput(URI originalResource, MementoMetadata metadata,
                                                               Stream<SolrDocument> mementoStream,
                                                               long count, Integer pageNumber) {

        if (pageNumber == null || (long) PropertiesLoader.MEMENTO_TIMEMAP_PAGESIZE * pageNumber > count){
            pageNumber = 1;
            log.info("Set page number to: " + pageNumber);
        }

        int finalPageNumber = pageNumber;
        TimeMap.Page<SolrDocument> pageOfResults = getPage(mementoStream, pageNumber, count);

        return os -> createPagedJsonTimemap(originalResource, metadata, count, finalPageNumber, pageOfResults, os);
    }

    /**
     * Create the beginning of a JSON timemap. Containing original URI, timegate URI, timemap URI and information on first and last memento.
     * @param originalResource  to create timemap for.
     * @param metadata          object which contains data used for constructing mementos.
     * @param os                outputstream to write JSON to.
     * @return                  the JSON generator with the beginning of the timemap written to it.
     */
    private static JsonGenerator getStartOfJsonTimeMap(URI originalResource, MementoMetadata metadata, OutputStream os, Integer pageNumber) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonGenerator jg = objectMapper.getFactory().createGenerator(os, JsonEncoding.UTF8);

        jg.writeStartObject(); // timemap start
        jg.writeFieldName("original_uri");
        jg.writeString(originalResource.toString());
        jg.writeFieldName("timegate_uri");
        jg.writeString(PropertiesLoaderWeb.WAYBACK_SERVER + "services/memento/" + originalResource);

        jg.writeFieldName("timemap_uri");
        if (pageNumber == null || pageNumber.equals(0)) {
            jg.writeStartObject(); //timemap_uri start
            jg.writeFieldName("link_format");
            jg.writeString(PropertiesLoaderWeb.WAYBACK_SERVER + "services/memento/timemap/link/" + originalResource);
            jg.writeFieldName("json_format");
            jg.writeString(PropertiesLoaderWeb.WAYBACK_SERVER + "services/memento/timemap/json/" + originalResource);
            jg.writeEndObject(); //timemap_uri end
        } else {
            jg.writeStartObject(); //timemap_uri start
            jg.writeFieldName("link_format");
            jg.writeString(PropertiesLoaderWeb.WAYBACK_SERVER + "services/memento/timemap/"+
                                pageNumber + "/link/" + originalResource);
            jg.writeFieldName("json_format");
            jg.writeString(PropertiesLoaderWeb.WAYBACK_SERVER + "services/memento/timemap/"+
                                pageNumber + "/json/" + originalResource);
            jg.writeEndObject(); //timemap_uri end
        }

        jg.writeFieldName("mementos");
        jg.writeStartObject(); //mementos start
        jg.writeFieldName("first");
        jg.writeStartObject(); //first start
        jg.writeFieldName("datetime");
        jg.writeString(metadata.getFirstMemento());
        jg.writeFieldName("uri");
        jg.writeString(PropertiesLoaderWeb.WAYBACK_SERVER + "services/web/" + metadata.getFirstWaybackDate() + "/" + originalResource);
        jg.writeEndObject(); //first end

        jg.writeFieldName("last");
        jg.writeStartObject(); //last start
        jg.writeFieldName("datetime");
        jg.writeString(metadata.getLastMemento());
        jg.writeFieldName("uri");
        jg.writeString(PropertiesLoaderWeb.WAYBACK_SERVER + "services/web/" + metadata.getLastWaybackDate() + "/" + originalResource);
        jg.writeEndObject(); //last end
        return jg;
    }

    /**
     * Create a paged JSON timemap for the original resource.
     * @param originalResource          to create timemap for.
     * @param metadata                  about the mementos for the original resource. Used to create the timemap.
     * @param totalMementosForResource  amount of mementos for the given resource. Used for creating pages.
     * @param pageNumber                of the page to retrieve.
     * @param pageOfResults             a page containing SolrDocuments that are to be written as a paged timemap.
     */
    private static void createPagedJsonTimemap(URI originalResource, MementoMetadata metadata, long totalMementosForResource,
                                               int pageNumber, TimeMap.Page<SolrDocument> pageOfResults, OutputStream os)
                                               throws IOException {

        JsonGenerator jg = getStartOfJsonTimeMap(originalResource, metadata, os, pageNumber);

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
        if ((long) PropertiesLoader.MEMENTO_TIMEMAP_PAGESIZE * pageNumber < totalMementosForResource){
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
}
