package dk.kb.netarchivesuite.solrwayback.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

import javax.activation.DataHandler;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.brotli.dec.BrotliInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.netarchivesuite.solrwayback.encoders.Sha1Hash;
import dk.kb.netarchivesuite.solrwayback.facade.Facade;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoaderWeb;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntryDescriptor;
import dk.kb.netarchivesuite.solrwayback.service.dto.HarvestDates;
import dk.kb.netarchivesuite.solrwayback.service.dto.ImageUrl;
import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDoc;
import dk.kb.netarchivesuite.solrwayback.service.dto.PagePreview;
import dk.kb.netarchivesuite.solrwayback.service.dto.TimestampsForPage;
import dk.kb.netarchivesuite.solrwayback.service.dto.UrlWrapper;
import dk.kb.netarchivesuite.solrwayback.service.dto.graph.D3Graph;
import dk.kb.netarchivesuite.solrwayback.service.dto.smurf.SmurfYearBuckets;
import dk.kb.netarchivesuite.solrwayback.service.exception.InternalServiceException;
import dk.kb.netarchivesuite.solrwayback.service.exception.InvalidArgumentServiceException;

import dk.kb.netarchivesuite.solrwayback.service.exception.SolrWaybackServiceException;
import dk.kb.netarchivesuite.solrwayback.solr.NetarchiveSolrClient;
import dk.kb.netarchivesuite.solrwayback.util.DateUtils;


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
    @Path("smurf/text")
    @Produces({ MediaType.APPLICATION_JSON})
    public  SmurfYearBuckets smurfNetarchiveText( @QueryParam("q") String q , @QueryParam("fq") String filterQuery,  @QueryParam("startyear") Integer startyear) throws SolrWaybackServiceException {
        try {                                                                                                
          if (startyear == null){
            startyear=1990;
          }
          return Facade.generateNetarchiveTextSmurfData(q, filterQuery,startyear);                  
        } catch (Exception e) {         
            throw handleServiceExceptions(e);
        }
    }
    
    @GET
    @Path("smurf/tags")
    @Produces({ MediaType.APPLICATION_JSON})
    public  SmurfYearBuckets smurfNetarchiveTags( @QueryParam("tag") String tag , @QueryParam("fq") String filterQuery,  @QueryParam("startyear") Integer startyear) throws SolrWaybackServiceException {
        try {                                                                                      
          
          if (startyear == null){
            startyear=1990;
          }
          return Facade.generateNetarchiveSmurfData(tag, filterQuery,startyear);                  
        } catch (Exception e) {         
            throw handleServiceExceptions(e);
        }
    }

    
    @GET
    @Path("graph/domain_result")
    @Produces({ MediaType.TEXT_PLAIN})
    public String domainResultGraph(@QueryParam("q") String q, @QueryParam("fq") List<String> fq ) throws SolrWaybackServiceException {

     try {
       return Facade.generateDomainResultGraph(q,fq);
      } catch (Exception e) {           
        throw handleServiceExceptions(e);
      }    
     
    }
    
   
    //TODO will this be used???
    /*
     *    
     * Example call:
     * image/pagepreviewurl?waybackdata=19990914144635/http://209.130.118.14/novelle/novelle.asp?id=478&grp=3
     * Since the URL part is not url encoded we can not use a jersey queryparam for the string
     * The part after 'waybackdata=' is same syntax as the (archive.org) wayback machine. (not url encoded).
     * Also supports URL encoding of the parameters as fallback if above syntax does not validate   
     */
    @GET
    @Path("/image/pagepreviewurl")
    @Produces("image/png")        

    public Response getHtmlPagePreviewForCrawltime (@Context UriInfo uriInfo) throws SolrWaybackServiceException {      
      //Get the full request url and find the waybackdata object

      //Duplicate code below, refactor!
      try {           
        String fullUrl = uriInfo.getRequestUri().toString();
        int dataStart=fullUrl.indexOf("/pagepreviewurl?waybackdata=");
        if (dataStart <0){
          throw new InvalidArgumentServiceException("no waybackdata parameter in call. Syntax is: /image/pagepreviewurl?waybackdata={time}/{url}");
        }

        String waybackDataObject = fullUrl.substring(dataStart+28);
        log.info("Waybackdata object:"+waybackDataObject);

        int indexFirstSlash = waybackDataObject.indexOf("/");  
        if (indexFirstSlash == -1){ //Fallback, try URL decode
          waybackDataObject = java.net.URLDecoder.decode(waybackDataObject, "UTF-8");
          log.info("urldecoded wayback dataobject:"+waybackDataObject);
          indexFirstSlash = waybackDataObject.indexOf("/");          
        }
        String waybackDate = waybackDataObject.substring(0,indexFirstSlash);
        String url = waybackDataObject.substring(indexFirstSlash+1);
        String solrDate = DateUtils.convertWaybackDate2SolrDate(waybackDate);
        
        IndexDoc doc = NetarchiveSolrClient.getInstance().findClosestHarvestTimeForUrl(url, solrDate);
        if (doc == null){
          log.info("Url has never been harvested:"+url);
          throw new IllegalArgumentException("Url has never been harvested:"+url);
        }

        String source_file_path = doc.getSource_file_path();
        long offset = doc.getOffset();

        BufferedImage image = Facade.getHtmlPagePreview(source_file_path, offset);
        return convertToPng(image);
           
      } catch (Exception e) {
        log.error("error thumbnail html image:" +uriInfo.getRequestUri().toString());  
        throw handleServiceExceptions(e);
      }
    }



        
    
    @GET
    @Path("/pagepreviews")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    public ArrayList<PagePreview> search(@QueryParam("url") String url) throws SolrWaybackServiceException {
      try {                    
        return Facade.getPagePreviewsForUrl(url);
      } catch (Exception e) {           
        throw handleServiceExceptions(e);
      }
    }


    @GET
    @Path("/help/about")
    @Produces(MediaType.TEXT_PLAIN)
    public String getAboutText() throws SolrWaybackServiceException {
      try {                    
        return Facade.getAboutText();
      } catch (Exception e) {           
        throw handleServiceExceptions(e);
      }
    }
      

    @GET
    @Path("/help/search")
    @Produces( MediaType.TEXT_PLAIN)
    public String getHelpText() throws SolrWaybackServiceException {
      try {                    
        return Facade.getSearchHelpText();
      } catch (Exception e) {           
        throw handleServiceExceptions(e);
      }
    }
    
    
    @GET
    @Path("/harvestDates")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    public HarvestDates harvestDates(@QueryParam("url") String url) throws SolrWaybackServiceException {
      try {                    
        return Facade.getHarvestTimesForUrl(url);
      } catch (Exception e) {           
        throw handleServiceExceptions(e);
      }
    }
    
    @GET
    @Path("/images/search")
    @Produces(MediaType.APPLICATION_JSON +"; charset=UTF-8")
    public  ArrayList<ImageUrl> imagesSearch(@QueryParam("query") String query) throws SolrWaybackServiceException {
      try {                                          
        ArrayList<ArcEntryDescriptor> img = Facade.findImages(query);
        return Facade.arcEntrys2Images(img);                                                            
      } catch (Exception e) {           
        throw handleServiceExceptions(e);
      }
    }
    
 // TODO https://wiki.apache.org/solr/SpatialSearch#How_to_boost_closest_results
    @GET
    @Path("/images/search/location")
    @Produces(MediaType.APPLICATION_JSON +"; charset=UTF-8")
    public  ArrayList<ImageUrl> imagesLocationSearch(@QueryParam("query") String query, @QueryParam("fq") String fq, @QueryParam("results") String results,@QueryParam("latitude") double latitude, @QueryParam("longitude") double longitude, @QueryParam("d") double d,@QueryParam("sort") String sort) throws SolrWaybackServiceException {
  //sort is optional
      if(d <=0 || d>5001){
        throw new InvalidArgumentServiceException("d parameter must be between 1 and 5000 (radius in km)");
      }

      try {                                          
        ArrayList<ImageUrl> images = Facade.imagesLocationSearch(query,fq, results, latitude, longitude, d,sort);
        return images;                                                            
      } catch (Exception e) {           
        throw handleServiceExceptions(e);
      }
    }
    
    @GET
    @Path("/wordcloud/domain")
    @Produces("image/png")
    public Response  wordCloudForDomain(@QueryParam("domain") String domain) throws SolrWaybackServiceException {
      try {                        
          BufferedImage image = Facade.wordCloudForDomain(domain);           
          return convertToPng(image);
          
      } catch (Exception e) {           
        throw handleServiceExceptions(e);
      }
    }

    
    
    @GET
    @Path("/util/normalizeurl")
    @Produces(MediaType.APPLICATION_JSON)
    public UrlWrapper waybackgraph(@QueryParam("url") String url) throws SolrWaybackServiceException {
      try{
       
        //also rewrite to puny code
        String url_norm =  Facade.punyCodeAndNormaliseUrl(url);       
        log.info("Normalize url"+url +" ->" +  url_norm);        
        UrlWrapper wrapper = new UrlWrapper();
        wrapper.setUrl(url_norm);      
        return wrapper;
      } catch (Exception e) {
        throw handleServiceExceptions(e);
      }
    }
        
    
    @POST
    @Path("upload/gethash")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    public String uploadPdf(List<Attachment> attachments,@Context HttpServletRequest request) throws  SolrWaybackServiceException { 

        log.info("upload called");          
        if (attachments.size() != 1) {
          log.info("upload most have 1 attachments, #attachments="+attachments.size());
         throw new InvalidArgumentServiceException("Only 1 attachment allowed. #attachments="+attachments.size());   
        }      
     try {                             
            Attachment attr= attachments.get(0);
            DataHandler handler = attr.getDataHandler();     
            InputStream uploadedInputStream = handler.getInputStream();
            String sha1 = Sha1Hash.createSha1(uploadedInputStream);               
            return sha1;

      } catch (Exception e) {         
        throw handleServiceExceptions(e);
      }      
    }
    
    //No facets! Only results
    @GET
    @Path("solr/search/results") 
    @Produces(MediaType.APPLICATION_JSON +"; charset=UTF-8")
    public String  solrSearchResults(@QueryParam("query") String query, @QueryParam("fq") List<String> fq ,  @QueryParam("grouping") boolean grouping,  @QueryParam("revisits") boolean revisits , @QueryParam("start") int start) throws SolrWaybackServiceException {
      try {
        String res = Facade.solrSearchNoFacets(query,fq, grouping, revisits, start);          
        return res;
      } catch (Exception e) {
        throw handleServiceExceptions(e);
      }
    }
    
    
    //No results, only facets
    @GET
    @Path("solr/search/facets") 
    @Produces(MediaType.APPLICATION_JSON +"; charset=UTF-8")
    public String  solrSearchFacets(@QueryParam("query") String query, @QueryParam("fq") List<String> fg , @QueryParam("revisits") boolean revisits) throws SolrWaybackServiceException {
      try {
       String res = Facade.solrSearchFacetsOnly(query,fg, revisits);          
       return res;
      } catch (Exception e) {        
        throw handleServiceExceptions(e);
      }
    }
    

    //No results, only facets
    @GET
    @Path("solr/search/facets/loadmore") 
    @Produces(MediaType.APPLICATION_JSON +"; charset=UTF-8")
    public String  solrSearchFacetsLoadMore(@QueryParam("query") String query, @QueryParam("fq") List<String> fg ,  @QueryParam("facetfield") String facetField, @QueryParam("revisits") boolean revisits) throws SolrWaybackServiceException {
      try {
       String res = Facade.solrSearchFacetsOnlyLoadMore(query,fg, facetField,revisits);          
       return res;
      } catch (Exception e) {        
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

    @GET
    @Path("/tools/linkgraph")
    @Produces(MediaType.APPLICATION_JSON)
    public D3Graph waybackgraph(@QueryParam("domain") String domain, @QueryParam("ingoing") Boolean ingoing, @QueryParam("facetLimit") Integer facetLimit, @QueryParam("dateStart") String dateStart, @QueryParam("dateEnd") String dateEnd) throws SolrWaybackServiceException {
      try{        
        int fLimit =10;//Default
        boolean in=false;//Default
        if (facetLimit != null){
          fLimit=facetLimit.intValue();
        }
        if(ingoing != null){
          in=ingoing.booleanValue();
        }

        // Default dates if not in input        
       if (dateStart == null) {
          int startYear=PropertiesLoaderWeb.ARCHIVE_START_YEAR;        
          dateStart = ""+new GregorianCalendar(startYear, 00, 1).getTime();
       }
       if (dateEnd== null) {
           dateEnd=""+System.currentTimeMillis();
       }
        
        return Facade.waybackgraph(domain, fLimit,in,dateStart,dateEnd);        

      } catch (Exception e) {
        throw handleServiceExceptions(e);
      }

    }

    

    //TODO want to remove this method from web frontend
    @GET
    @Path("/downloadRaw")
    public Response downloadRaw(@QueryParam("source_file_path") String source_file_path, @QueryParam("offset") long offset) throws SolrWaybackServiceException {
      try {

        log.debug("Download from FilePath:" + source_file_path + " offset:" + offset);
        ArcEntry arcEntry= Facade.getArcEntry(source_file_path, offset);
        
        //Only solr lookup if redirect.
        if (arcEntry.getStatus_code() >= 300 &&  arcEntry.getStatus_code() <= 399 ){
          IndexDoc indexDoc = NetarchiveSolrClient.getInstance().getArcEntry(source_file_path, offset);         
          Response responseRedirect = SolrWaybackResource.getRedirect(indexDoc,arcEntry);
          log.debug("Redirecting. status code from arc:"+arcEntry.getStatus_code() + " vs index " +indexDoc.getStatusCode()); 
          return responseRedirect;
        }
        
        //temp dirty hack to see if it fixes brotli
        InputStream in;
        if ("br".equalsIgnoreCase(arcEntry.getContentEncoding())){
        in = new BrotliInputStream(new ByteArrayInputStream(arcEntry.getBinary()));
        arcEntry.setContentEncoding(null); //Clear encoding.
        arcEntry.setHasBeenDecompressed(true);
        }
        else{      
         in = new ByteArrayInputStream(arcEntry.getBinary());
         
        }
        ResponseBuilder response = null;
        try{
          String contentType = arcEntry.getContentType();
          if (arcEntry.getContentCharset() != null){
            contentType = contentType +"; charset="+arcEntry.getContentCharset();
          }        
          response= Response.ok((Object) in).type(contentType);          
        }
        catch (Exception e){         
          IndexDoc indexDoc = NetarchiveSolrClient.getInstance().getArcEntry(source_file_path, offset); 
           log.warn("Error setting HTTP header Content-Type:'"+arcEntry.getContentType() +"' using index Content-Type:'"+indexDoc.getContentType()+"'");         
           response = Response.ok((Object) in).type(indexDoc.getContentType()); 
        }
              
        if (arcEntry.getFileName() != null){
          response.header("Content-Disposition", "filename=\"" + arcEntry.getFileName() +"\"");      
        }
        
        if (arcEntry.getContentEncoding() != null){
          response.header("Content-Encoding", arcEntry.getContentEncoding());      
        }
        
        log.debug("Download from source_file_path:" + source_file_path + " offset:" + offset + " is mimetype:" + arcEntry.getContentType() + " and has filename:" + arcEntry.getFileName());
        return response.build();

      } catch (Exception e) {
        log.error("Error download from source_file_path:"+ source_file_path + " offset:" + offset,e);
        throw handleServiceExceptions(e);
      }
    }

    
    @GET
    @Path("images/htmlpage")
    @Produces(MediaType.APPLICATION_JSON +"; charset=UTF-8")
    public ArrayList<ImageUrl> imagesForPage(@QueryParam("source_file_path") String source_file_path, @QueryParam("offset") long offset ) throws SolrWaybackServiceException {

      if (source_file_path == null || offset < 0){
        log.error("source_file_path and offset queryparams missing");
        throw new InvalidArgumentServiceException("source_file_path and offset queryparams missing");
      }

      try {    
        ArrayList<ImageUrl> images = Facade.getImagesForHtmlPageNew(source_file_path, offset);
        return images;     
      }
      catch (Exception e) {           
        throw handleServiceExceptions(e);
      }
    }

    
    @GET
    @Path("/timestampsforpage")
    @Produces(MediaType.APPLICATION_JSON +"; charset=UTF-8")
    public TimestampsForPage timestamps(@QueryParam("source_file_path") String source_file_path, @QueryParam("offset") long offset) throws Exception {
      log.debug("timestamps:" + source_file_path + " offset:" + offset);
      TimestampsForPage ts = Facade.timestampsForPage(source_file_path, offset);                                                                
      return ts;
    }
    
    private Response convertToPng(BufferedImage image)  throws Exception { 
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ImageIO.write(image, "png", baos);
      byte[] imageData = baos.toByteArray();
      baos.flush();
      baos.close();
      ResponseBuilder response = Response.ok(new ByteArrayInputStream(imageData));
      return response.build();
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
