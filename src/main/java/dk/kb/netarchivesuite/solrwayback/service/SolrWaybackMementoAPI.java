package dk.kb.netarchivesuite.solrwayback.service;


import dk.kb.netarchivesuite.solrwayback.memento.DatetimeNegotiation;
import dk.kb.netarchivesuite.solrwayback.util.PathResolver;
import org.apache.http.client.utils.DateUtils;
import org.apache.ws.commons.schema.constants.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

import static dk.kb.netarchivesuite.solrwayback.memento.TimeMap.getTimeMap;

@Path("/memento")
public class SolrWaybackMementoAPI {
    private static final Logger log = LoggerFactory.getLogger(SolrWaybackMementoAPI.class);
    private static final String linkFormat = "application/link-format";

    @GET
    @Path("timemap/test/{url}")
    public String timemapExists(@PathParam("url") String url){
        return "You have reached the endpoint for timemaps and requested: '" + url + "'";
    }

    @GET
    @Path("timemap/{url:.+}")
    public Response timeMap(@Context UriInfo uriInfo, @Context HttpServletRequest httpRequest,
                            @PathParam("url") String url, @HeaderParam("Accept") String responseFormat)
                            throws URISyntaxException {

        // When Accept header is not specified Chrome and Firefox applies a comma separated list of multiple
        // headers, this should be replaced by the link-format mimetype.
        if (responseFormat == null || responseFormat.isEmpty() || responseFormat.contains(",")){

            responseFormat = "application/link-format";
            log.info("Accept header not included. Returning application/link-format.");
        }

        URI uri =  PathResolver.mementoAPIResolver("/timemap/", uriInfo, httpRequest, url);
        StreamingOutput timemap = getTimeMap(uri, responseFormat);

        return Response.ok().type(responseFormat)
                .entity(timemap)
                // TODO: Change filetype based on header. this is not correct for JSON
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment ; filename = \"timemap.wlnk\"")
                .build();
    }



    @GET
    @Path("/{url:.+}")
    public Response getResolvedTimeGate(@Context UriInfo uriInfo, @Context HttpServletRequest httpRequest,
                                        @PathParam("url") String url,
                                        @HeaderParam("Accept-Datetime") String acceptDatetime) throws Exception {

        if (acceptDatetime == null){
            acceptDatetime = DateUtils.formatDate(new Date(System.currentTimeMillis()));
        }

        //TODO: Introduce property that decides which return version to use as PyWb does. https://pywb.readthedocs.io/en/latest/manual/memento.html#redirecting-timegate-memento-pattern-2-3
        String returnFormat = "200";

        URI uri =  PathResolver.mementoAPIResolver("/memento/", uriInfo, httpRequest, url);
        Response timeGate = DatetimeNegotiation.getMemento(String.valueOf(uri), acceptDatetime, returnFormat);

        return timeGate;
    }





}
