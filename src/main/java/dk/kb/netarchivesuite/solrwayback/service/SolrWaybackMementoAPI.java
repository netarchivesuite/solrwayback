package dk.kb.netarchivesuite.solrwayback.service;

import dk.kb.netarchivesuite.solrwayback.memento.DatetimeNegotiation;
import dk.kb.netarchivesuite.solrwayback.memento.TimeMap;
import dk.kb.netarchivesuite.solrwayback.service.exception.NotFoundServiceException;
import dk.kb.netarchivesuite.solrwayback.util.PathResolver;
import org.apache.http.client.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

import static dk.kb.netarchivesuite.solrwayback.util.DateUtils.validateTimestamp;

/**
 * <h2>Memento framework</h2>
 * <p>Endpoint for accessing archived websites in SolrWayback through the <a href="https://datatracker.ietf.org/doc/html/rfc7089">memento framework</a>.
 * The framework consists of two primary methods:
 * <ul>
 *     <li>Datetime Negotiation</li>
 *     <li>Timemaps</li>
 * </ul></p>
 *
 * <p>The following abbreviations are often used in the framework:</p>
 * <ul>
 *     <li>Original Resource (URI-R): A Web resource that exists or used to exist on the live Web for which we want to find a prior version.
 *     By prior version is meant a Web resource that encapsulates what the Original Resource was like at some time in the past. </li>
 *     <li>Memento (URI-M): A Web resource that is a prior version of the Original Resource, i.e. that encapsulates what
 *     the Original Resource was like at some time in the past.</li>
 *     <li>Timegate (URI-G): A Web resource that "decides" on the basis of a given datetime, which Memento best matches
 *     what the Original Resource was like around that given datetime.</li>
 *     <li>Timemap (URI-T): A TimeMap for an Original Resource is a resource from which a list of URIs of Mementos of the Original Resource is available.</li>
 * </ul>
 *
 * <h3>Datetime Negotiation</h3>
 * <p>The datetime negotiation API can be reached at /services/memento/&lt;url&gt; for any &lt;url&gt; in the collection.</p>
 * <p>Datetime negotiation can happen through <a href="https://datatracker.ietf.org/doc/html/rfc7089#section-4">four different patterns</a> in the framework.
 * Webarchives and archiving tools can implement the framework through Datetime Negotiation <a href="https://datatracker.ietf.org/doc/html/rfc7089#section-4.2">pattern 2</a>.
 * This pattern can be implemented in three different ways.</p>
 * <br>
 * <p>SolrWayback defaults to <a href="https://datatracker.ietf.org/doc/html/rfc7089#section-4.2.2">pattern 2.2</a>,
 * where a URI-M for the URI-R is delivered through a URI-G using negotiation style 200.
 * SolrWayback can be configured to use pattern 2.1, where a URI-M is delivered through a URI-G, but using negotiation style 302.
 * This can be done by setting the property memento_redirect_to_exact: true in the solrwayback properties </p>
 * <br>
 * <h3>Timemap</h3>
 * <p>The timemap API is available at /services/memento/timemap/&lt;url&gt; for any &lt;url&gt; in the collection.</p>
 * <p>The timemap can be provided in multiple output formats, where the default is to deliver the
 * timemap as application/link-format. This can be changed to JSON, by supplying the HTTP header Accept: application/json</p>
 *
 */
@Path("/memento")
public class SolrWaybackMementoAPI {
    private static final Logger log = LoggerFactory.getLogger(SolrWaybackMementoAPI.class);

    // Different methods exists for choosing returntype.
    // PyWB does it by a path argument: https://pywb.readthedocs.io/en/latest/manual/memento.html#timegate-api
    // This makes totally sense and is also described here from 2016: http://mementoweb.org/guide/timemap-json/#basic
    // However the RFC 7089 from 2013 only mentions the support for multiple mimetypes through the HTTP accept header: https://datatracker.ietf.org/doc/html/rfc7089#section-5
    // specifying it as a path argument is the "modern" way.
    @GET
    @Path("timemap/{type}/{date}/{url:.+}")
    public Response timeMap(@Context UriInfo uriInfo, @Context HttpServletRequest httpRequest,
                            @PathParam("url") String url, @HeaderParam("Accept") String acceptHeader,
                            @PathParam("type") String type, @PathParam("date") String date)
                            throws URISyntaxException {

        String mimeTypeForResponse = acceptHeader;

        mimeTypeForResponse = getMimeTypeForResponse(type, mimeTypeForResponse);

        URI uri =  PathResolver.mementoAPIResolver("/timemap/" + type + "/", uriInfo, url);
        TimeMap timeMap = new TimeMap();
        StreamingOutput timemap = timeMap.getTimeMap(uri, type, null);
        String fileType = fileEndingFromAcceptHeader(mimeTypeForResponse);

        return Response.ok().type(mimeTypeForResponse)
                .entity(timemap)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline") // Ensure inline displa
                .build();
    }

    /**
     * The paged timemap is not implemented for the json timemap, therefore trying to access a paged version throws an exception.
     */
    @GET
    @Path("timemap/{page:\\d+}/json/{url:.+}")
    public Response timeMapPagedJson(@Context UriInfo uriInfo, @Context HttpServletRequest httpRequest, @PathParam("url") String url, @PathParam("page") String page)
            throws NotFoundServiceException {

        String message = "Endpoint timemap/json does not support pagination. Either access the full timemap at /services/memento/timemap/json/" + url + " or request a paged " +
                "timemap in the link or spec-json format at /services/memento/timemap/" + page + "/spec/" + url;
        throw new NotFoundServiceException(message);
    }

    @GET
    @Path("timemap/{page:\\d+}/{type}/{url:.+}")
    public Response timeMapPaged(@Context UriInfo uriInfo, @Context HttpServletRequest httpRequest,
                            @PathParam("url") String url, @HeaderParam("Accept") String acceptHeader,
                            @PathParam("page") String page, @PathParam("type") String type)
            throws URISyntaxException {

        String mimeTypeForResponse = acceptHeader;

        mimeTypeForResponse = getMimeTypeForResponse(type, mimeTypeForResponse);

        URI uri =  PathResolver.mementoAPIResolver("/timemap/" + page + "/" + type + "/", uriInfo, url);
        TimeMap timeMap = new TimeMap();
        StreamingOutput output = timeMap.getTimeMap(uri, type, Integer.valueOf(page));
        String fileType = fileEndingFromAcceptHeader(mimeTypeForResponse);

        // TODO: Fresh eyes on http headers for timemap
        return Response.ok().type(mimeTypeForResponse)
                .entity(output)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline") // Ensure inline displa
                //.header(HttpHeaders.CONTENT_DISPOSITION, "attachment ; filename = \"timemap"+ fileType + "\"")
                .build();
    }


    @GET
    @Path("/{url:.+}")
    public Response getResolvedTimeGate(@Context UriInfo uriInfo, @Context HttpServletRequest httpRequest,
                                        @PathParam("url") String url,
                                        @HeaderParam("Accept-Datetime") String acceptDatetime) throws Exception {

        if (acceptDatetime == null || acceptDatetime.isEmpty()){
            acceptDatetime = DateUtils.formatDate(new Date(System.currentTimeMillis()));
            log.info("Requested memento without accept-datetime header. The newest memento is returned.");
        }

        URI uri =  PathResolver.mementoAPIResolver("/memento/", uriInfo, url);
        Response timeGate = DatetimeNegotiation.getMemento(String.valueOf(uri), acceptDatetime);

        return timeGate;
    }

    /**
     *
     * @param url
     * @param timestamp
     * @return
     * @throws Exception
     */
    @GET
    @Path("/{timestamp:\\d+}/{url:.+}")
    public Response getResolvedTimeGateFromPathTimestamp(@Context UriInfo uriInfo, @Context HttpServletRequest httpRequest, @PathParam("url") String url, @PathParam("timestamp")
                                         String timestamp) throws Exception {
        if (timestamp == null || timestamp.isEmpty()){
            timestamp = org.apache.http.client.utils.DateUtils.formatDate(new Date(System.currentTimeMillis()));
            log.info("Requested memento without timestamp info. The newest memento is returned.");
        }

        // Here we need the original timestamp
        URI uri =  PathResolver.mementoAPIResolver("/memento/", uriInfo, url, timestamp);
        // Using correctedTimestamp here
        String correctDatetime = validateTimestamp(timestamp);
        return DatetimeNegotiation.getMemento(String.valueOf(uri), correctDatetime);
    }

    /**
     * Define a string containing a filetype extension for the timemap from the given accept-header
     * @param responseFormat header containing mimetype for request.
     * @return filetype for response.
     */
    private String fileEndingFromAcceptHeader(String responseFormat) {
        log.info("accept header is: " + responseFormat);
        if (responseFormat.equals("application/json")){
            return ".json";
        } else {
            return ".wlnk";
        }
    }


    /**
     * Define mimetype from url type parameter
     * @param type from path
     * @param mimeTypeForResponse original mimetype
     * @return correct mimetype from path type
     */
    private static String getMimeTypeForResponse(String type, String mimeTypeForResponse) {
        switch (type){
            case "json":
                mimeTypeForResponse = "application/json";
                break;
            case "spec":
                mimeTypeForResponse = "application/json";
                break;
            case "link":
                mimeTypeForResponse = "application/link-format";
                break;
            default:
                if (mimeTypeForResponse.isEmpty()) {
                    mimeTypeForResponse = "application/link-format";
                }
                log.info("Accept header not specified in path. Returning: '{}'", mimeTypeForResponse);
        }
        return mimeTypeForResponse;
    }

}
