package dk.kb.netarchivesuite.solrwayback.memento;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.stream.Stream;

import static dk.kb.netarchivesuite.solrwayback.memento.TimeMap.getDocStreamAndUpdateDatesForFirstAndLastMemento;
import static dk.kb.netarchivesuite.solrwayback.memento.TimeMap.getMementoStreamCdxFields;

/**
 * Construct a JSON Timemap with fields and values as delivered by PyWB and archive.org
 * This format is almost identical to a CDX API response.
 */
public class TimeMapAsCdxJSON {
    private static final Logger log = LoggerFactory.getLogger(TimeMapAsCdxJSON.class);

    /**
     * Creates a timemap (URI-T) for a URI-R as JSON.
     * @param originalResource  URI-R to create URI-T from.
     * @return                  A json representation of the timemap ready for streaming.
     */
    static StreamingOutput getTimeMapAsCdxJson(URI originalResource) {
        MementoMetadata metadata = new MementoMetadata();

        long count = getDocStreamAndUpdateDatesForFirstAndLastMemento(originalResource, metadata)
                .count();
        log.debug("Original resource has been harvested '{}' times.",count);

        log.info("Creating timemap of '{}' entries, with dates in range from '{}' to '{}'.",
                count, metadata.getFirstMemento(), metadata.getLastMemento());

        Stream<SolrDocument> mementoStream = getMementoStreamCdxFields(originalResource);

        return getJSONStreamingOutputCdxFields(mementoStream);
    }

    /**
     * Create a JSON representation of a timemap for an original resource and return it as a streaming output.
     * @param mementoStream     Stream of solr documents containing all mementos of the given original resource.
     * @return                  A JSON timemap for the input original resource.
     */
    private static StreamingOutput getJSONStreamingOutputCdxFields(Stream<SolrDocument> mementoStream) {

        return os -> {
            JsonGenerator jg = JsonFactory.builder().build().createGenerator(os);

            jg.writeStartArray(); // Overall array
            writeHeaderArray(jg);

            long processedMementoCount = mementoStream
                    .map(doc -> addMementoToTimeMapObject(doc, jg))
                    .count();
            log.info("Wrote '{}' mementos to JSON timemap.", processedMementoCount);
            jg.writeEndArray(); //End overall array
            jg.flush();
            jg.close();
        };
    }

    /**
     * Method used as an intermediate operation on a stream of SolrDocuments.
     * Adds the individual memento from the stream to the JsonGenerator, which is producing the timemap in CDX compliant JSON.
     * @param solrDoc           containing metadata on a single memento.
     * @param jsonGenerator     generating the full timemap for the original resource. This generator should already contain
     *                          the first part of the timemap and is now ready to generate the JSON representation of the
     *                          individual mementos.
     * @return                  The inputted SolrDocument for further use in the stream.
     */
    private static SolrDocument addMementoToTimeMapObject(SolrDocument solrDoc, JsonGenerator jsonGenerator) {
        List<String> hostSurtList = (List<String>) solrDoc.get("host_surt");
        
        String hostsurt = hostSurtList.get(hostSurtList.size()-1);
        String waybackdate = extractNonNullStringFromSolr(solrDoc, "wayback_date");
        String url = extractNonNullStringFromSolr(solrDoc, "url");
        String contentType = extractNonNullStringFromSolr(solrDoc, "content_type");
        String statusCode = extractNonNullStringFromSolr(solrDoc, "status_code");
        String hash = extractNonNullStringFromSolr(solrDoc, "hash");
        String contentLength = extractNonNullStringFromSolr(solrDoc, "content_length");

        try {
            jsonGenerator.writeStartArray(); // Start entry
            jsonGenerator.writeString(hostsurt);
            jsonGenerator.writeString(waybackdate);
            jsonGenerator.writeString(url);
            jsonGenerator.writeString(contentType);
            jsonGenerator.writeString(statusCode);
            jsonGenerator.writeString(hash);
            jsonGenerator.writeString(contentLength);
            jsonGenerator.writeEndArray(); // End entry
            jsonGenerator.writeRaw("\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return solrDoc;
    }

    /**
     * Return a value from the input SolrDocument if it is present otherwise return an empty string.
     * @param solrDoc to retrieve value from.
     * @param value to retrieve in doc.
     * @return the value if present.
     */
    private static String extractNonNullStringFromSolr(SolrDocument solrDoc, String value) {
        try {
            return solrDoc.get(value).toString();
        } catch (NullPointerException e){
            log.debug("A NullPointerException happened when extracting values from SolrDocument. The specific value will be empty in the timemap");
            return "";
        }
    }


    /**
     * Write a simple JSON array containing the following values: {@code ["urlkey","timestamp","original","mimetype","statuscode","digest","length"]}
     * @param jsonGenerator used to write the JSON.
     */
    private static void writeHeaderArray(JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeStartArray(); // Header array
        jsonGenerator.writeString("urlkey");
        jsonGenerator.writeString("timestamp");
        jsonGenerator.writeString("original");
        jsonGenerator.writeString("mimetype");
        jsonGenerator.writeString("statuscode");
        jsonGenerator.writeString("digest");
        jsonGenerator.writeString("length");
        jsonGenerator.writeEndArray(); // End header array
        jsonGenerator.writeRaw("\n");
    }
}
