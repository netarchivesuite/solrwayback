package dk.kb.netarchivesuite.solrwayback.service;


import dk.kb.netarchivesuite.solrwayback.memento.DatetimeNegotiation;
import dk.kb.netarchivesuite.solrwayback.memento.TimeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import java.io.OutputStream;
import java.text.ParseException;

import static dk.kb.netarchivesuite.solrwayback.memento.TimeMap.getTimeMap;

@Path("/memento/")
public class SolrWaybackMementoAPI {
    private static final Logger log = LoggerFactory.getLogger(SolrWaybackMementoAPI.class);

    @GET
    @Path("timemap/{url}")
    public Response timeMap(@PathParam("url") String url, @HeaderParam("Accept") String responseFormat) throws ParseException {
        log.info("Calling timemap with url: '{}'.", url);


        StreamingOutput timemap = getTimeMap(url, responseFormat);

        return Response.ok(timemap).build();
    }

    @GET
    @Path("{url}")
    public Response timeGate(@PathParam("url") String url, @HeaderParam("Host") String host, @HeaderParam("Accept-Datetime") String acceptDatetime) throws Exception {
        //TODO: Introduce property that decides which return version to use as PyWb does. https://pywb.readthedocs.io/en/latest/manual/memento.html#redirecting-timegate-memento-pattern-2-3
        String returnFormat = "200";

        Response timeGate = DatetimeNegotiation.getMemento(url, host, acceptDatetime, returnFormat);

        return timeGate;
    }

    @HEAD
    @Path("{url}")
    public Response headTimeGate(@PathParam("url") String url, @HeaderParam("Accept-Datetime") String acceptDatetime){
        return Response.ok().build();
    }

}
