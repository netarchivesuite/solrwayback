package dk.kb.netarchivesuite.solrwayback.service;


import dk.kb.netarchivesuite.solrwayback.memento.DatetimeNegotiation;
import dk.kb.netarchivesuite.solrwayback.memento.TimeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import static dk.kb.netarchivesuite.solrwayback.memento.TimeMap.getTimeMap;

@Path("/memento/")
public class SolrWaybackMementoAPI {
    private static final Logger log = LoggerFactory.getLogger(SolrWaybackMementoAPI.class);

    @GET
    @Path("timemap/{url}")
    public Response timeMap(@PathParam("url") String url, @HeaderParam("Accept") String responseFormat){
        log.info("Calling timemap with url: '{}'.", url);

        getTimeMap(url, responseFormat);

        return Response.ok(url).build();
    }

    @GET
    @Path("{url}")
    public Response timeGate(@PathParam("url") String url, @HeaderParam("Host") String host, @HeaderParam("Accept-Datetime") String acceptDatetime) throws Exception {

        return DatetimeNegotiation.redirectToDistinctMemento(url, host, acceptDatetime);
    }
}
