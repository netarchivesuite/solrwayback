package dk.kb.netarchivesuite.solrwayback.service;


import dk.kb.netarchivesuite.solrwayback.memento.DatetimeNegotiation;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.HttpHead;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

@Path("/memento/")
public class SolrWaybackMementoAPI {
    private static final Logger log = LoggerFactory.getLogger(SolrWaybackMementoAPI.class);

    @GET
    @Path("timemap/{url}")
    public Response timeMap(@PathParam("url") String url){
        log.info("Calling timemap with url: '{}'.", url);

        return Response.ok(url).build();
    }

    @GET
    @Path("{url}")
    public Response timeGate(@PathParam("url") String url, @HeaderParam("Host") String host, @HeaderParam("Accept-Datetime") String acceptDatetime) throws Exception {

        return DatetimeNegotiation.redirectToDistinctMemento(url, host, acceptDatetime);
    }
}
