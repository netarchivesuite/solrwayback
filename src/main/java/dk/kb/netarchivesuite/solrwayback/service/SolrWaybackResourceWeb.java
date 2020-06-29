package dk.kb.netarchivesuite.solrwayback.service;

import java.util.HashMap;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.netarchivesuite.solrwayback.facade.Facade;
import dk.kb.netarchivesuite.solrwayback.service.exception.InternalServiceException;
import dk.kb.netarchivesuite.solrwayback.service.exception.InvalidArgumentServiceException;
import dk.kb.netarchivesuite.solrwayback.service.exception.NotFoundServiceException;
import dk.kb.netarchivesuite.solrwayback.service.exception.SolrWaybackServiceException;

@Path("/frontend/")
public class SolrWaybackResourceWeb {

    

    private static final Logger log = LoggerFactory.getLogger(SolrWaybackResourceWeb.class);

    @GET
    @Path("test")
    @Produces({ MediaType.TEXT_PLAIN})
    public String test() throws SolrWaybackServiceException {
        return "TEST";
    }
    
    
    
    @GET
    @Path("solr/search/results") //need to make solr/search/facets
    @Produces(MediaType.APPLICATION_JSON +"; charset=UTF-8")
    public String  solrSearchResults(@QueryParam("query") String query, @QueryParam("fq") String filterQuery ,  @QueryParam("grouping") boolean grouping,  @QueryParam("revisits") boolean revisits , @QueryParam("start") int start) throws SolrWaybackServiceException {
      try {
        String res = Facade.solrSearch(query,filterQuery, grouping, revisits, start);          
        return res;
      } catch (Exception e) {
        log.error("error for search:"+query, e);
        throw handleServiceExceptions(e);
      }
    }
    
    
    @GET
    @Path("solr/search/facets") 
    @Produces(MediaType.APPLICATION_JSON +"; charset=UTF-8")
    public String  solrSearchFacets(@QueryParam("query") String query, @QueryParam("fq") String filterQuery , @QueryParam("revisits") boolean revisits) throws SolrWaybackServiceException {
      try {
//        String res = Facade.solrSearch(query,filterQuery, grouping, revisits, start);          
        return "TODO";
      } catch (Exception e) {
        log.error("error when facets:"+query, e);
        throw handleServiceExceptions(e);
      }
    }
    
    
    
    
    @GET
    @Path("solr/idlookup")
    @Produces(MediaType.APPLICATION_JSON +"; charset=UTF-8")
    public String  solrSearch(@QueryParam("id") String id) throws SolrWaybackServiceException {
      try {                    
       
         String res = Facade.solrIdLookup(id);          
        return res;
      } catch (Exception e) {
        log.error("error id lookup:"+id, e);
        throw handleServiceExceptions(e);
      }
    }
    

    @GET
    @Path("properties/solrwaybackweb")
    @Produces(MediaType.APPLICATION_JSON +"; charset=UTF-8")
    public HashMap<String,String>  getPropertiesWeb() throws SolrWaybackServiceException {
      try {                    
        log.info("PropertiesWeb returned");
        return Facade.getPropertiesWeb();          
      } catch (Exception e) {
        throw handleServiceExceptions(e);
      }
    }

    
    private SolrWaybackServiceException handleServiceExceptions(Exception e) {
        if (e instanceof SolrWaybackServiceException) {
          log.info("Handling serviceException:" + e.getMessage());
          return (SolrWaybackServiceException) e; // Do nothing, exception already correct
        } else if (e instanceof IllegalArgumentException) {
          log.error("ServiceException(HTTP 400) in Service:", e.getMessage());
          return new InvalidArgumentServiceException(e.getMessage());
        } else {// unforseen exceptions.... should not happen.
          log.error("ServiceException(HTTP 500) in Service:", e);
          return new InternalServiceException(e.getMessage());
        }
      }
    
    
}
