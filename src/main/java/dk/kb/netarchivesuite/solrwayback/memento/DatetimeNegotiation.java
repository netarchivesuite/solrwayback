package dk.kb.netarchivesuite.solrwayback.memento;

import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoaderWeb;
import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDocShort;
import dk.kb.netarchivesuite.solrwayback.service.dto.MementoDoc;
import dk.kb.netarchivesuite.solrwayback.solr.NetarchiveSolrClient;
import dk.kb.netarchivesuite.solrwayback.solr.SRequest;
import dk.kb.netarchivesuite.solrwayback.util.DateUtils;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


// TODO: Not sure that Host header is used correctly. It should point to the archive that hosts the material, not the original host of the website.

/**
 * This class implements the Datetime Negotiation of the Memento Framework
 * as specified in <a href="https://datatracker.ietf.org/doc/html/rfc7089#section-4.1">RFC 7089</a>.
 *
 */
public class DatetimeNegotiation {
    private static final Logger log = LoggerFactory.getLogger(DatetimeNegotiation.class);

    public static Response getMemento(String url, String host, String acceptDatetime, String returnFormat) throws Exception {
        //TODO: use property instead of string
        //TODO: prettify try-catches
        if (returnFormat.equals("302")){
            /*
            return output -> {
                try {
                    redirectToDistinctMemento(url, host, acceptDatetime, output);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };
             */
            return Response.ok().build();

        } else {
            return DatetimeNegotiation.remoteTimeGateForOriginalResource(url, host, acceptDatetime);
        }
    }

    /**
     * Pattern 1.1 of the memento framework, where the URI-R represents its own URI-G and delivers the URI-M
     * through a 302 Found HTTP status code. This is only usefull if the URI-R resides at the same server as the URI-M.
     * This is not the case for webarchives.
     * @param host a URI-R to fetch URI-M for.
     */
    public static void redirectToDistinctMemento(String url, String host, String acceptDatetime, OutputStream output) throws Exception {
        Long waybackdate = DateUtils.convertMementoAcceptDateTime2Waybackdate(acceptDatetime);

        log.info("Extracted host: '{}' and accept-datetime '{}' headers from http request",
                    host, acceptDatetime);

        // TODO: This stream is not really used. Maybe this endpoint could just try to resolve the direct url instead of doing a lookup
        List<SolrDocument> docs = SRequest.builder().query("url:" + url)
                                    .fields("url", "url_norm", "wayback_date")
                                    .stream()
                                    .filter(doc -> matchDates(doc, waybackdate))
                                    .collect(Collectors.toList());

        Response.status(302)
                .header("Location", PropertiesLoader.WAYBACK_BASEURL + "services/web/" + waybackdate + "/" + url)
                .header("Vary", acceptDatetime)
                .header("Link", "<" + url + ">; rel=\"original timegate\"")
                .build();
    }

    /**
     * Implements Pattern 2.2 of the memento framework.
     *
     * @param url
     * @param host
     * @param acceptDatetime
     * @throws ParseException
     */
    public static Response remoteTimeGateForOriginalResource(String url, String host,
                                                                     String acceptDatetime) throws ParseException {

        MementoMetadata metadata = new MementoMetadata();
        OutputStream outputStream = new ByteArrayOutputStream();
        //TODO: Create an actual memento2solrdate converter and vice-versa
        Long waybackdate = DateUtils.convertMementoAcceptDateTime2Waybackdate(acceptDatetime);
        String solrDate = DateUtils.convertWaybackDate2SolrDate(String.valueOf(waybackdate));
        log.info("Extracted host: '{}' and accept-datetime '{}' headers from http request",
                host, acceptDatetime);

        // Create response through streaming of a single SolrDocument.
        Response response = NetarchiveSolrClient.getInstance()
                .findNearestHarvestTimeForSingleUrlFewFields(url, solrDate)
                .map(doc -> addHeadersToMetadataObject(doc, metadata))
                .map(doc -> streamTimeGate(doc, metadata, outputStream))
                .iterator().next();

        return response;
    }

    private static MementoDoc addHeadersToMetadataObject(MementoDoc doc, MementoMetadata metadata) {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add("Date", java.time.OffsetDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME));
        headers.add("Vary", "accept-datetime");
        headers.add("Content-Location", PropertiesLoaderWeb.WAYBACK_SERVER + "services/web/" +
               doc.getWayback_date() + "/" + doc.getUrl());
        try {
            headers.add("Memento-Datetime", DateUtils.convertWaybackdate2Mementodate(doc.getWayback_date()));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        String linkString = "<" + doc.getUrl() + "; rel=\"original\",\n" +
                            "<" + PropertiesLoaderWeb.WAYBACK_SERVER + "services/memento/timemap/" + doc.getUrl() + ">"+
                            "; rel=\"timemap\"; type=\"application/link-format\"," +
                            "<" + PropertiesLoaderWeb.WAYBACK_SERVER + "services/memento/timegate/" + doc.getUrl() +">"+
                            "; rel=\"timegate\"";
        headers.add("Link", linkString);
        headers.add("Content-Length", doc.getContent_length());
        headers.add("Content-Type", doc.getContent_type());
        headers.add("Connection", "close");

        metadata.setHttpHeaders(headers);

        return doc;
    }

    private static Response streamTimeGate(MementoDoc doc, MementoMetadata metadata, OutputStream outputStream) {
        try {
            outputStream.write(doc.toString().getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return Response.ok(outputStream).replaceAll(metadata.getHttpHeaders()).build();
    }

    private static boolean matchDates(SolrDocument doc, Long waybackdate) {
        log.info("Comparing wayback date in solr: '{}', with waybackdate from header: '{}'", doc.getFieldValue("wayback_date"), waybackdate );
        return doc.getFieldValue("wayback_date").equals(waybackdate);
    }

}
