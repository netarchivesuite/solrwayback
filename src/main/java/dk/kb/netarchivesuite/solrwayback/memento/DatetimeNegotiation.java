package dk.kb.netarchivesuite.solrwayback.memento;

import dk.kb.netarchivesuite.solrwayback.facade.Facade;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoaderWeb;
import dk.kb.netarchivesuite.solrwayback.service.SolrWaybackResource;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;
import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDoc;
import dk.kb.netarchivesuite.solrwayback.service.dto.MementoDoc;
import dk.kb.netarchivesuite.solrwayback.service.exception.InvalidArgumentServiceException;
import dk.kb.netarchivesuite.solrwayback.solr.NetarchiveSolrClient;
import dk.kb.netarchivesuite.solrwayback.util.DateUtils;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import java.text.ParseException;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * This class implements the Datetime Negotiation of the Memento Framework
 * as specified in <a href="https://datatracker.ietf.org/doc/html/rfc7089#section-4.1">RFC 7089</a>.
 * 
 * All methods for non-redirect (2.2 mode) has been commented out since 2.2 is not supported.
 */
public class DatetimeNegotiation {
    private static final Logger log = LoggerFactory.getLogger(DatetimeNegotiation.class);

    public static Response getMemento(String url, String acceptDatetime) throws Exception {
        if (PropertiesLoader.MEMENTO_REDIRECT) {
            return redirectingTimegate(url, acceptDatetime);
        } else {
        
            throw new InvalidArgumentServiceException("Memento playback only supports redirect mode."); //
            //return nonRedirectingTimeGate(url, acceptDatetime);
        }
    }

    /**
     * 
     * 
     * 
     * Non-Redirecting TimeGate (Memento Pattern 2.2)
     * This behavior is consistent with Memento Pattern 2.2 and is the default behavior for timegates in SolrWayback.
     *
     * @param url            to find timegate for.
     * @param acceptDatetime the datetime that the enduser wants to obtain a memento for. SolrWayback delivers
     *                       the closest possible memento.
     * @return an HTTP 200 response with memento headers and the memento as the entity.
     */
  
    
    //Playback will be very flawed with this approach since url now has /mememto.
    //Only solution is to return a small HTML page with a frame, that has the correct playback url.
    //Playback logic is implemented in root-service. Html URL parser, serviceworker, live leak referrer fix. We do not want to have double logic for playback     
    //Noone cares.. Everyone uses browser they will not notice a redirect, so this 2.2 option is not required.
    //Also I do not like it! Playback is  polluted with Memento header fields.
    /*
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
*/
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
                .map(doc -> saveFirstAndLastDate(doc, metadata))
                .map(doc -> addHeadersToMetadataObjectForRedirectingTimegate(doc, metadata))
                .map(doc -> streamMementoFromRedirectingTimeGate(doc, metadata))
                .reduce((first, second) -> first);

        return responseOpt.orElseGet(() -> Response.status(404).build());
    }

    private static MementoDoc saveFirstAndLastDate(MementoDoc doc, MementoMetadata metadata) {
        if (doc.getWayback_date() < metadata.getFirstWaybackDate()){
            metadata.setFirstWaybackDate(doc.getWayback_date());
        }
        if (doc.getWayback_date() > metadata.getLastWaybackDate()){
            metadata.setLastWaybackDate(doc.getWayback_date());
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
     * Create HTTP headers for timegate found from Solr Index
     * @param doc      contains data used to construct the header for the memento.
     * @param metadata object which stores variables, that are used to construct the headers. Headers are also stored
     *                 in this object when constructed.
     * @return         the input doc for further use in a streaming chain.
     */
    
     /*
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
                "<" + PropertiesLoaderWeb.WAYBACK_SERVER + "services/memento/" + doc.getUrl() + ">" +
                "; rel=\"timegate\"";
        headers.add("Link", linkString);
        headers.add("Content-Length", doc.getContent_length());
        headers.add("Content-Type", doc.getContent_type());
        headers.add("Connection", "close");

        metadata.setHttpHeaders(headers);

        return doc;
    }
*/
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
                "; rel=\"timemap\"; type=\"application/link-format\"\n" +
                "; from=\"" + metadata.getFirstMemento() + "\"\n" +
                "; until=\"" + metadata.getLastMemento() + "\"";
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
    /*
    private static Response streamMementoFromNonRedirectingTimeGate(MementoDoc doc, MementoMetadata metadata) {
        if (PropertiesLoader.PLAYBACK_DISABLED){
         return Response.noContent().replaceAll(metadata.getHttpHeaders()).build();
        } else {
            try {
                SolrWaybackResource resource = new SolrWaybackResource();
                Response resp = resource.viewImpl(doc.getSource_file_path(), doc.getSource_file_offset(), true, true);                               
                ResponseBuilder entity = Response.fromResponse(resp);
                addMementoHeadersToReponse(entity, metadata);                                                       
               return entity.build();                
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
    */
    /**
     * Return the header with the 302 redirection location. Has no payload.
     * 
     * @param doc           containing data from solr for accessing the memento in the WARC files.
     * @param metadata      object which contains metadata on the memento. Including the additional headers.
     * @return              a response containing correct memento headers and the memento as the response entity.
     */
    private static Response streamMementoFromRedirectingTimeGate(MementoDoc doc, MementoMetadata metadata) {
        if (PropertiesLoader.PLAYBACK_DISABLED){
            return Response.noContent().replaceAll(metadata.getHttpHeaders()).build();
        } else {
            try {               
                ResponseBuilder entity = Response.status(302);
                addMementoHeadersToReponse(entity,metadata);                
                return entity.build();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Add the additional headers for mememto reponse. The headers are different for redirecting and non-redirecting requests. 
     * 
     * @param entity The response to enrinch with additioal headers
     * @param metadata Additional headers
     */
    private static void addMementoHeadersToReponse(ResponseBuilder entity, MementoMetadata metadata) {
        for (String header: metadata.getHttpHeaders().keySet()) {
            String value=metadata.getHttpHeaders().get(header).get(0).toString();
            log.debug("adding memento header:"+header +" value:"+value);
            entity.header(header, value);
        }
    }
    
}
