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

        Stream<String> mementos = SRequest.builder().query("url_norm:\"" + originalResource + "\"")
                .fields("url", "url_norm", "wayback_date")
                .stream()
                .map(doc -> saveFirstAndLastDate(doc, metadata) )
                .map(TimeMap::createMementoInLinkFormat);
        //.collect(Collectors.joining());

        String timemap = "<" + originalResource + ">;rel=\"original\",\n" +
                "<"+ PropertiesLoaderWeb.WAYBACK_SERVER_PROPERTY + "memento/timemap/" + originalResource + ">\n" +
                "; rel=\"self\";type=\"application/link-format\"\n" +
                "; from\"" + metadata.getFirstMemento() + "\"\n" +
                "; until\"" + metadata.getLastMemento() + "\",\n" +
                "<"+ PropertiesLoaderWeb.WAYBACK_SERVER_PROPERTY +"memento/timegate/" + originalResource + ">\n" +
                "; rel=\"timegate\",\n";


        Stream<String> populatedTimeMap = Stream.concat(Stream.of(timemap), mementos);

        String result = populatedTimeMap.collect(Collectors.joining());


        return Response.ok()
                .header("Content-Length", timemap.length())
                .header("Content-Type", "application/link-format")
                .entity(result).build();
    }

    private static SolrDocument saveFirstAndLastDate(SolrDocument doc, MementoMetadata metadata) {
        if ((long) doc.getFieldValue("wayback_date") < metadata.getFirstWaybackDate()){
            metadata.setFirstWaybackDate( (Long) doc.getFieldValue("wayback_date"));
        }
        if ((long) doc.getFieldValue("wayback_date") > metadata.getLastWaybackDate()){
            metadata.setLastWaybackDate( (Long) doc.getFieldValue("wayback_date"));
        }
        return doc;
    }

    private static String createMementoInLinkFormat(SolrDocument doc) {
        String memento = "<" + PropertiesLoaderWeb.WAYBACK_SERVER_PROPERTY + "services/web/" +
                    doc.getFieldValue("waybackdate") + doc.getFieldValue("url") + ">";
        // TODO: add relations, first memento, last memento and memento
        // TODO: Add datetime
        // TODO: Add license
        return memento;
    }

}
