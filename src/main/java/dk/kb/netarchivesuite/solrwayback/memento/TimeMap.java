package dk.kb.netarchivesuite.solrwayback.memento;

import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoaderWeb;
import dk.kb.netarchivesuite.solrwayback.solr.SRequest;
import dk.kb.netarchivesuite.solrwayback.util.DateUtils;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.StreamingOutput;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.text.ParseException;
import java.util.concurrent.atomic.AtomicLong;

public class TimeMap {

    private static final Logger log = LoggerFactory.getLogger(TimeMap.class);


    public static StreamingOutput getTimeMap(URI originalResource, String responseFormat) {

        if (responseFormat.equals("application/json")){
            return  output -> {
                getTimeMapAsJson(originalResource, output);
            };
        } else {
            return output -> {
                getTimeMapAsLinkFormat(originalResource, output);
            };
        }
    }

    private static void getTimeMapAsJson(URI originalResource, OutputStream output) {

    }

    private static void getTimeMapAsLinkFormat(URI originalResource, OutputStream output) throws IOException {
        MementoMetadata metadata = new MementoMetadata();

        // Sadly we need to do two Solr calls as it doesn't seem possible to calculate the dates for the header,
        // in the second stream and add it to the output as the first thing.
        long count = SRequest.builder().query("url_norm:\""+ originalResource + "\"")
                .fields("wayback_date")
                .sort("id asc")
                .stream()
                .map(doc -> saveFirstAndLastDate(doc, metadata))
                .map(doc -> updateTimeMapHead(doc, metadata, originalResource.toString()))
                .count();

        log.info("Creating timemap of '{}' entries, with dates in range from '{}' to '{}'.",
                count, metadata.getFirstMemento(), metadata.getLastMemento());

        output.write(metadata.getTimeMapHead().getBytes());

        AtomicLong iterator = new AtomicLong(1);
        SRequest.builder().query("url_norm:\""+ originalResource + "\"")
                .fields("url", "url_norm", "wayback_date")
                .sort("id asc")
                .stream()
                .map(doc -> createMementoInLinkFormat(doc, iterator, count))
                .forEach(s -> writeStringSafe(s, output));
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
    private static SolrDocument updateTimeMapHead(SolrDocument doc, MementoMetadata metadata, String originalResource) {
        metadata.setTimeMapHead(originalResource);
        return doc;
    }


    /**
     * Create an application/link-format compliant memento representation of an archived resource from solr.
     *
     * @param doc             The solr document, that contains information on the individual harvested resource.
     * @param iterator
     * @param countOfMementos
     * @return The memento as a string, ready to be concatenated to a memento timemap.
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

}
