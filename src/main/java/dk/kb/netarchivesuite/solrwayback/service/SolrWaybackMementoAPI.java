package dk.kb.netarchivesuite.solrwayback.service;


import dk.kb.netarchivesuite.solrwayback.memento.DatetimeNegotiation;
import dk.kb.netarchivesuite.solrwayback.memento.TimeMap;
import dk.kb.netarchivesuite.solrwayback.service.exception.SolrWaybackServiceException;
import dk.kb.netarchivesuite.solrwayback.util.PathResolver;
import org.apache.http.client.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

import static dk.kb.netarchivesuite.solrwayback.memento.TimeMap.getTimeMap;

@Path("/memento")
public class SolrWaybackMementoAPI {
    private static final Logger log = LoggerFactory.getLogger(SolrWaybackMementoAPI.class);

    @GET
    @Path("timemap/test/{url}")
    public String timemapExists(@PathParam("url") String url){
        return "You have reached the endpoint for timemaps and requested: '" + url + "'";
    }

    @GET
    @Path("timemap/{url}")
    public Response timeMap(@PathParam("url") String url, @HeaderParam("Accept") String responseFormat) {
        log.info("Calling timemap with url: '{}'.", url);

        StreamingOutput timemap = getTimeMap(url, responseFormat);

        return Response.ok(timemap).build();
    }

    /*
    @GET
    @Path("{url}")
    public Response getTimeGate(@PathParam("url") String url, @HeaderParam("Host") String host, @HeaderParam("Accept-Datetime") String acceptDatetime) throws Exception {
        //TODO: Introduce property that decides which return version to use as PyWb does. https://pywb.readthedocs.io/en/latest/manual/memento.html#redirecting-timegate-memento-pattern-2-3
        String returnFormat = "200";

        Response timeGate = DatetimeNegotiation.getMemento(url, host, acceptDatetime, returnFormat);

        return timeGate;
    }

     */

    @GET
    @Path("/{url:.+}")
    public Response getResolvedTimeGate(@Context UriInfo uriInfo, @Context HttpServletRequest httpRequest,
                                        @PathParam("url") String url,
                                        @HeaderParam("Accept-Datetime") String acceptDatetime) throws Exception {

        if (acceptDatetime == null){
            acceptDatetime = DateUtils.formatDate(new Date(System.currentTimeMillis()));
        }

        URI uri =  PathResolver.mementoAPIResolver("/memento/", uriInfo, httpRequest, url);
        Response timeGate = DatetimeNegotiation.getMemento(String.valueOf(uri), acceptDatetime, "200");

        return timeGate;
    }


    /*
    @HEAD
    @Path("/{url:.+}")
    public Response headTimeGate(@Context UriInfo uriInfo, @Context HttpServletRequest httpRequest,
                                 @PathParam("url") String url, @HeaderParam("Accept-Datetime") String acceptDatetime) throws Exception {

        if (acceptDatetime == null){
            acceptDatetime = DateUtils.formatDate(new Date(System.currentTimeMillis()));
        }

        URI uri =  PathResolver.mementoAPIResolver("/memento/", uriInfo, httpRequest, url);
        Response timeGate = DatetimeNegotiation.getMemento(String.valueOf(uri), acceptDatetime, "200");

        return timeGate;
    }

     */



}
