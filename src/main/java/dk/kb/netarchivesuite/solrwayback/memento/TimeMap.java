package dk.kb.netarchivesuite.solrwayback.memento;

import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoaderWeb;
import dk.kb.netarchivesuite.solrwayback.service.SolrWaybackMementoAPI;
import dk.kb.netarchivesuite.solrwayback.solr.SRequest;
import dk.kb.netarchivesuite.solrwayback.util.DateUtils;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.text.ParseException;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TimeMap {

    private static final Logger log = LoggerFactory.getLogger(TimeMap.class);


    public static Response getTimeMap(String originalResource, String responseFormat) throws ParseException {
        switch (responseFormat){
            case "application/link-format":
                return  getTimeMapAsLinkFormat(originalResource);
            case "application/json":
                return getTimeMapAsJson(originalResource);
        }
        return null;
    }

    private static Response getTimeMapAsJson(String originalResource) {
        return null;
    }

    private static Response getTimeMapAsLinkFormat(String originalResource) throws ParseException {
        MementoMetadata metadata = new MementoMetadata();


        String mementos = SRequest.builder().query("url_norm:\""+ originalResource + "\"")
                .fields("url", "url_norm", "wayback_date")
                .sort("id asc")
                .stream()
                .map(doc -> saveFirstAndLastDate(doc, metadata))
                .map(TimeMap::createMementoInLinkFormat)
                .collect(Collectors.joining())
                .replaceFirst("rel=\"memento\"", "rel=\"first memento\"");

        //.collect(Collectors.joining());

        metadata.setFirstMementoFromFirstWaybackDate();
        metadata.setLastMementoFromLastWaybackDate();
        System.out.println("Current first memento: " + metadata.getFirstMemento());

        String timemapHead = "<" + originalResource + ">;rel=\"original\",\n" +
                "<"+ PropertiesLoaderWeb.WAYBACK_SERVER + "memento/timemap/" + originalResource + ">\n" +
                "; rel=\"self\";type=\"application/link-format\"\n" +
                "; from\"" + metadata.getFirstMemento() + "\"\n" +
                "; until\"" + metadata.getLastMemento() + "\",\n" +
                "<"+ PropertiesLoaderWeb.WAYBACK_SERVER +"memento/timegate/" + originalResource + ">\n" +
                "; rel=\"timegate\",\n";


        String result = timemapHead+mementos;
        log.info("When is this?");


        return Response.ok()
                .header("Content-Length", result.length())
                .header("Content-Type", "application/link-format")
                .entity(result).build();
    }

    private static SolrDocument saveFirstAndLastDate(SolrDocument doc, MementoMetadata metadata) {
        if ((long) doc.getFieldValue("wayback_date") < metadata.getFirstWaybackDate()){
            metadata.setFirstWaybackDate( (Long) doc.getFieldValue("wayback_date"));
            log.info("Found new first date: " + doc.getFieldValue("wayback_date"));
        }
        if ((long) doc.getFieldValue("wayback_date") > metadata.getLastWaybackDate()){
            metadata.setLastWaybackDate( (Long) doc.getFieldValue("wayback_date"));
            log.info("Found new latest date: " + doc.getFieldValue("wayback_date"));
        }
        return doc;
    }

    private static String createMementoInLinkFormat(SolrDocument doc) {
        String memento = "";
        try {
            memento = "<" + PropertiesLoaderWeb.WAYBACK_SERVER + "services/web/" +
                        doc.getFieldValue("wayback_date") + "/" + doc.getFieldValue("url") + ">\n" +
                        "; rel=\"memento\"; datetime=\"" + DateUtils.convertWaybackdate2Mementodate((Long) doc.getFieldValue("wayback_date")) + "\"\n";
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        // TODO: add relations, first memento, last memento
        // TODO: Add license
        return memento;
    }

}
