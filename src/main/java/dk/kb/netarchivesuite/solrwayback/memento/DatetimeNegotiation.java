package dk.kb.netarchivesuite.solrwayback.memento;

import dk.kb.netarchivesuite.solrwayback.facade.Facade;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoaderWeb;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;
import dk.kb.netarchivesuite.solrwayback.service.dto.MementoDoc;
import dk.kb.netarchivesuite.solrwayback.solr.NetarchiveSolrClient;
import dk.kb.netarchivesuite.solrwayback.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.text.ParseException;
import java.time.format.DateTimeFormatter;
import java.util.Optional;



/**
 * This class implements the Datetime Negotiation of the Memento Framework
 * as specified in <a href="https://datatracker.ietf.org/doc/html/rfc7089#section-4.1">RFC 7089</a>.
 */
public class DatetimeNegotiation {
    private static final Logger log = LoggerFactory.getLogger(DatetimeNegotiation.class);

    public static Response getMemento(String url, String acceptDatetime) throws Exception {
        //TODO: use property instead of string

        if (PropertiesLoader.MEMENTO_REDIRECT) {
            return redirectingTimegate(url, acceptDatetime);

        } else {
            return nonRedirectingTimeGate(url, acceptDatetime);
        }
    }

    /**
     * Non-Redirecting TimeGate (Memento Pattern 2.2)
     * This behavior is consistent with Memento Pattern 2.2 and is the default behavior for timegates in SolrWayback.
     *
     * @param url            to find timegate for.
     * @param acceptDatetime the datetime that the enduser wants to obtain a memento for. SolrWayback delivers
     *                       the closest possible memento.
     * @return an HTTP 200 response with memento headers and the memento as the entity.
     */
    public static Response nonRedirectingTimeGate(String url,
                                                  String acceptDatetime) throws Exception {

        MementoMetadata metadata = new MementoMetadata();
        String solrDate = DateUtils.convertMementodate2Solrdate(acceptDatetime);
        log.info("Converted RFC1123 date to solrdate: '{}'", solrDate);

        // Create response through streaming of a single SolrDocument.
        Optional<Response> responseOpt = NetarchiveSolrClient.getInstance()
                .findNearestHarvestTimeForSingleUrlFewFields(url, solrDate)
                .map(doc -> addHeadersToMetadataObjectNonRedirecting(doc, metadata))
                .map(doc -> streamMementoFromNonRedirectingTimeGate(doc, metadata))
                .reduce((first, second) -> first);

        return responseOpt.orElseGet(() -> Response.status(404).build());
    }

    /**
     * Redirecting TimeGate (Memento Pattern 2.1)
     * This behavior is consistent with Memento Pattern 2.1 and can be configured through a property.
     *
     * @param url            to find timegate for.
     * @param acceptDatetime the datetime that the enduser wants to obtain a memento for. SolrWayback delivers
     *                       the closest possible memento.
     * @return an HTTP 302 response with memento headers and the memento as the entity.
     */
    private static Response redirectingTimegate(String url, String acceptDatetime) throws ParseException {
        MementoMetadata metadata = new MementoMetadata();
        String solrDate = DateUtils.convertMementodate2Solrdate(acceptDatetime);
        log.info("Converted RFC1123 date to solrdate: '{}'", solrDate);

        // Create response through streaming of a single SolrDocument.
        Optional<Response> responseOpt = NetarchiveSolrClient.getInstance()
                .findNearestHarvestTimeForSingleUrlFewFields(url, solrDate)
                .map(doc -> addHeadersToMetadataObjectForRedirectingTimegate(doc, metadata))
                .map(doc -> streamMementoFromRedirectingTimeGate(doc, metadata))
                .reduce((first, second) -> first);

        return responseOpt.orElseGet(() -> Response.status(404).build());
    }

    /**
     * Create HTTP headers for timegate found from Solr Index
     * @param doc      contains data used to construct the header for the memento.
     * @param metadata object which stores variables, that are used to construct the headers. Headers are also stored
     *                 in this object when constructed.
     * @return         the input doc for further use in a streaming chain.
     */
    private static MementoDoc addHeadersToMetadataObjectNonRedirecting(MementoDoc doc, MementoMetadata metadata) {
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
        String linkString = "<" + doc.getUrl() + ">; rel=\"original\"," +
                "<" + PropertiesLoaderWeb.WAYBACK_SERVER + "services/memento/timemap/" + doc.getUrl() + ">" +
                "; rel=\"timemap\"; type=\"application/link-format\"," +
                "<" + PropertiesLoaderWeb.WAYBACK_SERVER + "services/memento/timegate/" + doc.getUrl() + ">" +
                "; rel=\"timegate\"";
        headers.add("Link", linkString);
        headers.add("Content-Length", doc.getContent_length());
        headers.add("Content-Type", doc.getContent_type());
        headers.add("Connection", "close");

        metadata.setHttpHeaders(headers);

        return doc;
    }

    /**
     * Create HTTP headers for timegate found from Solr Index
     * @param doc      contains data used to construct the header for the memento.
     * @param metadata object which stores variables, that are used to construct the headers. Headers are also stored
     *                 in this object when constructed.
     * @return         the input doc for further use in a streaming chain.
     */
    private static MementoDoc addHeadersToMetadataObjectForRedirectingTimegate(MementoDoc doc, MementoMetadata metadata) {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add("Date", java.time.OffsetDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME));
        headers.add("Vary", "accept-datetime");
        headers.add("Location", PropertiesLoaderWeb.WAYBACK_SERVER + "services/web/" +
                doc.getWayback_date() + "/" + doc.getUrl());
        String linkString = "<" + doc.getUrl() + ">; rel=\"original\"," +
                "<" + PropertiesLoaderWeb.WAYBACK_SERVER + "services/memento/timemap/" + doc.getUrl() + ">" +
                "; rel=\"timemap\"; type=\"application/link-format\""; //TODO: Missing from and until in timemap
        headers.add("Link", linkString);
        headers.add("Content-Length", 0);
        headers.add("Content-Type", doc.getContent_type());
        headers.add("Connection", "close");

        metadata.setHttpHeaders(headers);

        return doc;
    }


    /**
     * Streams the memento found for the timegate.
     * @param doc           containing data from solr for accessing the memento in the WARC files.
     * @param metadata      object which contains metadata on the memento. Including headers.
     * @return              a response containing correct memento headers and the memento as the response entity.
     */
    private static Response streamMementoFromNonRedirectingTimeGate(MementoDoc doc, MementoMetadata metadata) {
        try {
            ArcEntry mementoEntity =  Facade.getArcEntry(doc.getSource_file_path(), doc.getSource_file_offset());
            return Response.ok(mementoEntity.getBinaryRaw()).replaceAll(metadata.getHttpHeaders()).build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Streams the memento found for the timegate.
     * @param doc           containing data from solr for accessing the memento in the WARC files.
     * @param metadata      object which contains metadata on the memento. Including headers.
     * @return              a response containing correct memento headers and the memento as the response entity.
     */
    private static Response streamMementoFromRedirectingTimeGate(MementoDoc doc, MementoMetadata metadata) {
        try {
            ArcEntry mementoEntity =  Facade.getArcEntry(doc.getSource_file_path(), doc.getSource_file_offset());
            return Response.status(302).entity(mementoEntity.getBinaryRaw()).replaceAll(metadata.getHttpHeaders()).build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
