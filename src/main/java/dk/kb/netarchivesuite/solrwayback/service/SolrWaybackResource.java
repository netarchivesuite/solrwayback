package dk.kb.netarchivesuite.solrwayback.service;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.brotli.dec.BrotliInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

import dk.kb.netarchivesuite.solrwayback.encoders.Sha1Hash;
import dk.kb.netarchivesuite.solrwayback.facade.Facade;
import dk.kb.netarchivesuite.solrwayback.image.ImageUtils;
import dk.kb.netarchivesuite.solrwayback.parsers.Normalisation;
import dk.kb.netarchivesuite.solrwayback.parsers.WarcParser;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
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
import dk.kb.netarchivesuite.solrwayback.service.dto.statistics.DomainYearStatistics;
import dk.kb.netarchivesuite.solrwayback.service.exception.InternalServiceException;
import dk.kb.netarchivesuite.solrwayback.service.exception.InvalidArgumentServiceException;
import dk.kb.netarchivesuite.solrwayback.service.exception.NotFoundServiceException;
import dk.kb.netarchivesuite.solrwayback.service.exception.ServiceException;
import dk.kb.netarchivesuite.solrwayback.solr.NetarchiveSolrClient;
import dk.kb.netarchivesuite.solrwayback.util.DateUtils;
import dk.kb.netarchivesuite.solrwayback.util.UrlUtils;

//No path except the context root+servletpath for the application. Example http://localhost:8080/officemood/services 

@Path("/")
public class SolrWaybackResource {

  private static final Logger log = LoggerFactory.getLogger(SolrWaybackResource.class);
  
  

  /*
   * Only for debugging/error finding. Not called from SolrWayback frontend.
   * Can be improved to not also load binary which are not shown. 
   */
  @GET
  @Path("warc/header")
  @Produces({ MediaType.TEXT_PLAIN})
  public String getWarcHeader( @QueryParam("source_file_path") String source_file_path, @QueryParam("offset") long offset) throws ServiceException {
      try {                                                                                      
        ArcEntry arcEntry= Facade.getArcEntry(source_file_path, offset);
        return arcEntry.getHeader();                              
      } catch (Exception e) {         
          throw handleServiceExceptions(e);
      }
  }
  
  @GET
  @Path("warc/parsed")
  @Produces(MediaType.APPLICATION_JSON +"; charset=UTF-8")
  public ArcEntry getWarcParsed( @QueryParam("source_file_path") String source_file_path, @QueryParam("offset") long offset) throws ServiceException {
      try {                                                                                      
        ArcEntry arcEntry= Facade.getArcEntry(source_file_path, offset);
         return arcEntry;                              
      } catch (Exception e) {         
          throw handleServiceExceptions(e);
      }
  }
  
  
  @GET
  @Path("/images/search")
  @Produces(MediaType.APPLICATION_JSON +"; charset=UTF-8")
  public  ArrayList<ImageUrl> imagesSearch(@QueryParam("query") String query) throws ServiceException {
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
  public  ArrayList<ImageUrl> imagesLocationSearch(@QueryParam("query") String query, @QueryParam("fq") String fq, @QueryParam("results") String results,@QueryParam("latitude") double latitude, @QueryParam("longitude") double longitude, @QueryParam("d") double d,@QueryParam("sort") String sort) throws ServiceException {
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
  @Path("smurf/tags")
  @Produces({ MediaType.APPLICATION_JSON})
  public  SmurfYearBuckets smurfNetarchiveTags( @QueryParam("tag") String tag , @QueryParam("fq") String filterQuery,  @QueryParam("startyear") Integer startyear) throws ServiceException {
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
  @Path("/util/normalizeurl")
  @Produces(MediaType.APPLICATION_JSON)
  public UrlWrapper waybackgraph(@QueryParam("url") String url) throws ServiceException {
    try{
      String urlDecoded = java.net.URLDecoder.decode(url, "UTF-8"); //frontend sending this encoded
      
      log.info("url:"+urlDecoded);
      //also rewrite to puny code
      String url_norm =  Facade.punyCodeAndNormaliseUrl(urlDecoded);       
      UrlWrapper wrapper = new UrlWrapper();
      wrapper.setUrl(url_norm);      
      return wrapper;
    } catch (Exception e) {
      throw handleServiceExceptions(e);
    }
  }
  
  
  @GET
  @Path("statistics/domain")
  @Produces({ MediaType.APPLICATION_JSON})
  public  ArrayList<DomainYearStatistics> statisticsDomain (@QueryParam("domain") String domain) throws ServiceException {
      try {                                                                                                   
        return Facade.statisticsDomain(domain);
      } catch (Exception e) {         
          throw handleServiceExceptions(e);
      }
  }
 
  
  
  @GET
  @Path("smurf/text")
  @Produces({ MediaType.APPLICATION_JSON})
  public  SmurfYearBuckets smurfNetarchiveText( @QueryParam("q") String q , @QueryParam("fq") String filterQuery,  @QueryParam("startyear") Integer startyear) throws ServiceException {
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
  @Path("solr/search")
  @Produces(MediaType.APPLICATION_JSON +"; charset=UTF-8")
  public String  solrSearch(@QueryParam("query") String query, @QueryParam("fq") String filterQuery ,  @QueryParam("grouping") boolean grouping,  @QueryParam("revisits") boolean revisits , @QueryParam("start") int start) throws ServiceException {
    try {
      String res = Facade.solrSearch(query,filterQuery, grouping, revisits, start);          
      return res;
    } catch (Exception e) {
      log.error("error for search:"+query, e);
      throw handleServiceExceptions(e);
    }
  }
  
  
  
  @GET
  @Path("solr/idlookup")
  @Produces(MediaType.APPLICATION_JSON +"; charset=UTF-8")
  public String  solrSearch(@QueryParam("id") String id) throws ServiceException {
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
  public HashMap<String,String>  getPropertiesWeb() throws ServiceException {
    try {                    
      log.info("PropertiesWeb returned");
      return Facade.getPropertiesWeb();          
    } catch (Exception e) {
      throw handleServiceExceptions(e);
    }
  }


  @GET
  @Path("images/htmlpage")
  @Produces(MediaType.APPLICATION_JSON +"; charset=UTF-8")
  public ArrayList<ImageUrl> imagesForPage(@QueryParam("source_file_path") String source_file_path, @QueryParam("offset") long offset ) throws ServiceException {

 
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
  @Path("/harvestDates")
  @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
  public HarvestDates harvestDates(@QueryParam("url") String url) throws ServiceException {
    try {                    
      return Facade.getHarvestTimesForUrl(url);
    } catch (Exception e) {           
      throw handleServiceExceptions(e);
    }
  }

  @GET
  @Path("/pagepreviews")
  @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
  public ArrayList<PagePreview> search(@QueryParam("url") String url) throws ServiceException {
    try {                    
      return Facade.getPagePreviewsForUrl(url);
    } catch (Exception e) {           
      throw handleServiceExceptions(e);
    }
  }
  

  @GET
  @Path("/wordcloud/domain")
  @Produces("image/png")
  public Response  wordCloudForDomain(@QueryParam("domain") String domain) throws ServiceException {
    try {                        
        BufferedImage image = Facade.wordCloudForDomain(domain);           
        return Response.ok(image).build();     
    } catch (Exception e) {           
      throw handleServiceExceptions(e);
    }
  }
  
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
  public Response getHtmlPagePreviewForCrawltime (@Context UriInfo uriInfo) throws ServiceException {      
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
      return Response.ok(image).build();   
    } catch (Exception e) {
      log.error("error thumbnail html image:" +uriInfo.getRequestUri().toString());  
      throw handleServiceExceptions(e);
    }
  }



  @GET
  @Path("/image/pagepreview")
  @Produces("image/png")
  public Response getHtmlPagePreview(@QueryParam("source_file_path") String source_file_path, @QueryParam("offset") long offset)
      throws ServiceException {
    try {
      log.debug("Getting thumbnail html image from source_file_path:" + source_file_path + " offset:" + offset);
      BufferedImage image = Facade.getHtmlPagePreview(source_file_path, offset);          
      return Response.ok(image).build();                       
    } catch (Exception e) {
      log.error("error thumbnail html image:"+source_file_path +" offset:"+offset);  
      throw handleServiceExceptions(e);
    }
  }

  @GET
  @Path("/image")
  @Produces("image/png")
  public Response getImage(@QueryParam("source_file_path") String source_file_path, @QueryParam("offset") long offset, @QueryParam("height") int height, @QueryParam("width") int width)
      throws ServiceException {
    try {
      log.debug("Getting image from source_file_path:" + source_file_path + " offset:" + offset + " targetWidth:" + width + " targetHeight:" + height);

      ArcEntry arcEntry= Facade.getArcEntry(source_file_path, offset);

      BufferedImage image = ImageUtils.getImageFromBinary(arcEntry.getBinary());

      if (image== null){
        // java does not support ico format. Just serve it RAW... 
        // Also SVG scaling bugs too much in java
        if (arcEntry.getUrl().toLowerCase().indexOf("/favicon.ico") >0 || arcEntry.getContentType().indexOf("image/svg+xml") >=0){ 
           log.info("image is ico-image or SVG serving it raw");
           return downloadRaw(source_file_path, offset);          
        }
        log.warn("image is null and not .ico file, source_file_path:"+source_file_path +" offset:"+offset);
        throw new IllegalArgumentException("image is null and not .ico image, source_file_path:"+source_file_path +" offset:"+offset +" contentType:"+arcEntry.getContentType());                
      }

      int sourceWidth = image.getWidth();
      int sourceHeight = image.getHeight();

      if (sourceHeight <= height && sourceWidth <= width) { // No resize, image is smaller
        ResponseBuilder response = Response.ok((Object) image);
        return response.build();
      } else {
        Image resizeImage = ImageUtils.resizeImage(image, sourceWidth, sourceHeight, width, height);
        ResponseBuilder response = Response.ok((Object) resizeImage);
        return response.build();
      }
    } catch (Exception e) {
      log.error("error getImage:"+source_file_path +" offset:"+offset +" height:"+height +" width:"+width); //Java can not read all images. 
      throw handleServiceExceptions(e);
    }
    catch (Throwable e) { //Can happen due to the batik dependency. Sometimes SVG scaling failes. Also weird image formats can fail
      log.error("Throwable error in getImage:"+source_file_path +" offset:"+offset +" height:"+height +" width:"+width + " error:"+e.getMessage()); //Java can not read all images. 
      throw handleServiceExceptions(new NotFoundServiceException());
    }
  }

  @GET
  @Path("/downloadRaw")
  public Response downloadRaw(@QueryParam("source_file_path") String source_file_path, @QueryParam("offset") long offset) throws ServiceException {
    try {

      log.debug("Download from FilePath:" + source_file_path + " offset:" + offset);
      ArcEntry arcEntry= Facade.getArcEntry(source_file_path, offset);
      
      //Only solr lookup if redirect.
      if (arcEntry.getStatus_code() >= 300 &&  arcEntry.getStatus_code() <= 399 ){
        IndexDoc indexDoc = NetarchiveSolrClient.getInstance().getArcEntry(source_file_path, offset);         
        Response responseRedirect = getRedirect(indexDoc,arcEntry);
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
  @Path("/export/warc")    
  @Produces(MediaType.APPLICATION_OCTET_STREAM)    
  public Response exportWarc(@QueryParam("query") String q, @QueryParam("fq") String fq) throws ServiceException {
   
    //This is also required even if the option is removed on the web-page.
    if (!PropertiesLoaderWeb.ALLOW_EXPORT_WARC){ 
      throw new InvalidArgumentServiceException("Export to warc not allowed!");
    }    
    return exportWarcImpl(q, fq, false, false);
  }

  @GET
  @Path("/export/linkgraph")    
  @Produces(MediaType.APPLICATION_OCTET_STREAM)    
  public Response exportLinkGraph(@QueryParam("query") String q) throws ServiceException {
   
    //This is also required even if the option is removed on the web-page.
    if (!PropertiesLoaderWeb.ALLOW_EXPORT_CSV){ 
      throw new InvalidArgumentServiceException("Export to csv not allowed!");
    }        
    try {
      log.debug("Export linkgraph. query:"+q);
      DateFormat formatOut= new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
      String dateStr = formatOut.format(new Date());
      InputStream is = Facade.exportLinkGraphStreaming(q);
      return Response.ok(is).header("Content-Disposition", "attachment; filename=\"solrwayback_linkgraph_"+dateStr+".csv\"").build();
    } catch (Exception e) {
      log.error("Error in export linkgraph",e);
      throw handleServiceExceptions(e);
    }    
  }
  
  @GET
  @Path("/export/warcExpanded")    
  @Produces(MediaType.APPLICATION_OCTET_STREAM)    
  public Response exportWarcExpanded(@QueryParam("query") String q, @QueryParam("fq") String fq) throws ServiceException {
    //This is also required even if the option is removed on the web-page.
    if (!PropertiesLoaderWeb.ALLOW_EXPORT_WARC){ 
      throw new InvalidArgumentServiceException("Export to warc not allowed!");
    }        
    return exportWarcImpl(q, fq, true, true);
  }
  
  
  private Response exportWarcImpl(@QueryParam("query") String q,
                                     @QueryParam("fq") String fq,
                                     @QueryParam("expand") boolean expandResources,
                                     @QueryParam("deduplicate") boolean avoidDuplicates) throws ServiceException {
    try {
      log.debug("Export warc. query:"+q +" filterquery:"+fq);
      DateFormat formatOut= new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
      String dateStr = formatOut.format(new Date());
      InputStream is = Facade.exportWarcStreaming(expandResources, avoidDuplicates, q, fq);
      return Response.ok(is).header("Content-Disposition", "attachment; filename=\"solrwayback_"+dateStr+".warc\"").build();

    } catch (Exception e) {
      log.error("Error in export warc",e);
      throw handleServiceExceptions(e);
    }
  }


  
  @GET
  @Path("/export/brief")    
  @Produces(MediaType.TEXT_PLAIN)
  public Response exportBrief(@QueryParam("query") String q, @QueryParam("fq") String fq) throws ServiceException {
    if (!PropertiesLoaderWeb.ALLOW_EXPORT_CSV){ 
      throw new InvalidArgumentServiceException("Export to csv not allowed!");
    }
    
    try {              
      log.debug("Export brief. query:"+q +" filterquery:"+fq);
      DateFormat formatOut= new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");                                                                              
      String dateStr = formatOut.format(new Date());                        
      InputStream is = Facade.exportBriefStreaming(q, fq);
      return Response.ok(is).header("Content-Disposition", "attachment; filename=\"solrwayback_"+dateStr+".csv\"").build();

    } catch (Exception e) {
      log.error("Error in export brief",e);
      throw handleServiceExceptions(e);
    }
  }

  
  @GET
  @Path("/linkgraph/csv")    
  @Produces(MediaType.TEXT_PLAIN)
  public Response linkgraphCsv(@QueryParam("query") String q, @QueryParam("fq") String fq) throws ServiceException {
    
    try {              
      log.debug("Export brief. query:"+q +" filterquery:"+fq);
      DateFormat formatOut= new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");                                                                              
      String dateStr = formatOut.format(new Date());                        
      InputStream is = Facade.exportBriefStreaming(q, fq);
      return Response.ok(is).header("Content-Disposition", "attachment; filename=\"solrwayback_"+dateStr+".csv\"").build();

    } catch (Exception e) {
      log.error("Error in generating linkgraph csv",e);
      throw handleServiceExceptions(e);
    }
  }

  
  

  @GET
  @Path("/export/full")    
  public Response exportFull(@QueryParam("query") String q, @QueryParam("fq") String fq) throws ServiceException {
    if (!PropertiesLoaderWeb.ALLOW_EXPORT_CSV){ 
      throw new InvalidArgumentServiceException("Export to csv not allowed!");
    }
    try {               
      log.debug("Export full. query:"+q +" filterquery:"+fq);
      DateFormat formatOut= new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");                                                                                                                                                         
      String dateStr = formatOut.format(new Date());                        
      InputStream is = Facade.exportFullStreaming(q, fq);
      return Response.ok(is).header("Content-Disposition", "attachment; filename=\"solrwayback_"+dateStr+".csv\"").build();

    } catch (Exception e) {
      log.error("Error in export full",e);      
      throw handleServiceExceptions(e);
    }

  }


  /*
//   *  This will be called from solrwayback page views, when resources can not be resolved (not harvested)  
   */    
  @GET
  @Path("/notfound")    
  public Response notfound() throws ServiceException {                      
    log.info("not found called");
    throw new NotFoundServiceException("");                  
  }



  @GET
  @Path("/getContentType")
  public String getContentType(@QueryParam("source_file_path") String source_file_path, @QueryParam("offset") long offset) throws ServiceException {
    try {               
      return Facade.getEncoding(source_file_path, ""+offset);       
    } catch (Exception e) {
      throw handleServiceExceptions(e);
    }

  }


  /*
   * Almost the same as the '/web/' method.
   * But this method is only called from proxy direct. Do not include toolbar.
   * Also knowing it is from proxy can be used to improve playback even more. (TODO)  
   * 
   */
  @GET
  @Path("/webProxy/{var:.*?}")
  public Response waybackProxyAPIResolver(@Context UriInfo uriInfo, @PathParam("var") String path) throws ServiceException {
    try {        
      //For some reason the var regexp does not work with comma (;) and other characters. So I have to grab the full url from uriInfo
      log.info("/webProxy/ called with data:"+path);
      String fullUrl = uriInfo.getRequestUri().toString();
      log.info("full url:"+fullUrl);
     
      int dataStart=fullUrl.indexOf("/webProxy/");
      
      String waybackDataObject = fullUrl.substring(dataStart+10);
      log.info("Waybackdata object:"+waybackDataObject);

      int indexFirstSlash = waybackDataObject.indexOf("/");  
             
      String waybackDate = waybackDataObject.substring(0,indexFirstSlash);
      String url = waybackDataObject.substring(indexFirstSlash+1);

      String solrDate = DateUtils.convertWaybackDate2SolrDate(waybackDate);

      //log.info("solrDate="+solrDate +" , url="+url);
      IndexDoc doc = NetarchiveSolrClient.getInstance().findClosestHarvestTimeForUrl(url, solrDate);
      if (doc == null){
        log.info("Url has never been harvested:"+url);
        throw new NotFoundServiceException("Url has never been harvested:"+url);
      }
      //log.info("Found url with harvesttime:"+doc.getUrl() +" and arc:"+doc.getArc_full());        
      log.info("return viewImpl for type:"+doc.getMimeType() +" and url:"+doc.getUrl());
      return viewImpl(doc.getSource_file_path() , doc.getOffset(),false); //NO TOOLBAR!        
      
                     
    } catch (Exception e) {
      throw handleServiceExceptions(e);
    }

  }

  /*
   * '/web/' is the same as wayback machine uses. 
   * 
   * Jersey syntax to match all after /web/.
   */
  @GET
  @Path("/web/{var:.*?}")
  public Response waybackAPIResolver(@Context UriInfo uriInfo, @Context HttpServletRequest httpRequest, @PathParam("var") String path) throws ServiceException {
    try {        
      //For some reason the var regexp does not work with comma (;) and other characters. So I have to grab the full url from uriInfo
      log.info("/web/ called with data:"+path);
      String fullUrl = uriInfo.getRequestUri().toString();
      log.info("full url:"+fullUrl);
     
      int dataStart=fullUrl.indexOf("/web/");
      
      String waybackDataObject = fullUrl.substring(dataStart+5);
      log.info("Waybackdata object:"+waybackDataObject);

      int indexFirstSlash = waybackDataObject.indexOf("/");  
             
      String waybackDate = waybackDataObject.substring(0,indexFirstSlash);
      String url = waybackDataObject.substring(indexFirstSlash+1);

      //Stupid fix, some webservices makes parameter http:// into http:/  ( removes a slash)
 
      //TODO into method
      if (url.startsWith("http:/") && !url.startsWith("http://")) {
        url = url.replaceFirst("http:/", "http://");
        
      }
      
    //TODO into method
      if (url.startsWith("https:/") && !url.startsWith("https://")) {
        url = url.replaceFirst("https:/", "https://");
      }
  
           
      //Validate this is a URL with domain (can be releative leak).
      //etc. http://images/horse.png.
      //use referer to match the correct url
                       
      String solrDate = DateUtils.convertWaybackDate2SolrDate(waybackDate);

      boolean urlOK = UrlUtils.isUrlWithDomain(url);
      if (!urlOK){        
        String refererUrl = httpRequest.getHeader("referer");       
        log.info("url not with domain:"+url +" referer:"+refererUrl);         
        IndexDoc doc = Facade.matchRelativeUrlForDomain(refererUrl,url,solrDate);           
        return downloadRaw(doc.getSource_file_path(),doc.getOffset());      
      }      
            
      //log.info("solrDate="+solrDate +" , url="+url);
      IndexDoc doc = NetarchiveSolrClient.getInstance().findClosestHarvestTimeForUrl(url, solrDate);
      if (doc == null){
        log.info("Url has never been harvested:"+url);
        throw new NotFoundServiceException("Url has never been harvested:"+url);
      }        
      log.info("return viewImpl for type:"+doc.getMimeType() +" and url:"+doc.getUrl());
      return viewImpl(doc.getSource_file_path() , doc.getOffset(),true);                                   
    } catch (Exception e) {
      throw handleServiceExceptions(e);
    }

  }

  
  /*
   *  will be called with
   *   pwid/web/urn:pwid:netarkivet.dk:2018-12-10T06:27:01Z:part:https://www.petdreams.dk/katteracer-siameser
   * 
   * Jersey syntax to match all after pwid/web/.
   */
  @GET
  @Path("/pwid/web/{var:.*?}")
  public Response waybackPwidAPIResolver(@Context UriInfo uriInfo, @Context HttpServletRequest httpRequest, @PathParam("var") String path) throws ServiceException {
    try {        
      //For some reason the var regexp does not work with comma (;) and other characters. So I have to grab the full url from uriInfo
      log.info("/pwid/web/ called with data:"+path);
      String fullUrl = uriInfo.getRequestUri().toString();
      log.info("full url:"+fullUrl);
      int pwidStart=fullUrl.indexOf("/pwid/web/"); //urn:pwid:netarkivet.dk:2018-12-10T06:27:01Z:part:https://www.petdreams.dk/katteracer-siameser
      String pwid = fullUrl.substring(pwidStart+10);
      System.out.println("Pwid object:"+pwid);
      if (!(pwid.startsWith("urn:pwid:"))){
        //syntax not correct
         log.warn("pwid syntax not correct:"+pwid);
        throw new InvalidArgumentServiceException("Pwid does not start with 'urn:pwid: , pwid= "+pwid);
      }
         String collectionStart = pwid.substring(9);         
        int collectionEnd = collectionStart.indexOf(":");  
        String thisCollectionName = PropertiesLoader.PID_COLLECTION_NAME;      
        String urlCollectionName = collectionStart.substring(0,collectionEnd);
             
         //int indexFirstSlash = waybackDataObject.indexOf("/");  
       if (!(urlCollectionName.equals(thisCollectionName))){
         log.warn("Wrong collection. This collection has PWID:"+thisCollectionName +" requested collection name was:"+urlCollectionName);
         throw new InvalidArgumentServiceException("Wrong collection. This collection has PWID:"+thisCollectionName +" requested collection name was:"+urlCollectionName);       
       }
       String utcStart =  collectionStart.substring(thisCollectionName.length()); // This now equals:  :part:https://www.petdreams.dk/katteracer-siameser
       //validate first char is :
       if (!(utcStart.startsWith(":"))){
         log.warn("pwid syntax not correct:"+pwidStart);
         throw new InvalidArgumentServiceException("pwid syntax not correct:"+pwidStart);
       }       
       utcStart = utcStart.substring(1); //  :2018-12-10T06:27:01Z:part:https://www.petdreams.dk/katteracer-siameser
    
       int utcEnd = utcStart.indexOf(":part:");
       String onlyUTC= utcStart.substring(0,utcEnd);
                     
       String lastPartStart = utcStart.substring(utcEnd);    
       if (!(lastPartStart.startsWith(":part:"))){
         log.warn("pwid syntax not correct,  'part' not found:"+pwidStart);
         throw new InvalidArgumentServiceException("'part' not found"+pwidStart);
       }
       String pwidUrl= lastPartStart.substring(6);// only the url
   
       //now we have url and UTZ, see if we have exact match in collection.
       IndexDoc doc = Facade.findExactMatchPWID(pwidUrl, onlyUTC);
       if (doc == null){
         throw new NotFoundServiceException("URL:"+pwidUrl +" and time:"+onlyUTC + " is not found in collection:"+thisCollectionName);
       }

      return viewImpl(doc.getSource_file_path() , doc.getOffset(),true);                                   
    } catch (Exception e) {
      throw handleServiceExceptions(e);
    }

  }

  
  
  /*
   * This happens for leaks to solrwayback/services/  
   * The proxy will handle it
   */
  @GET
  @Path("/{var:.*?}")
  public Response waybackAPIResolverRoot(@Context UriInfo uriInfo, @Context HttpServletRequest httpRequest, @PathParam("var") String path) throws ServiceException {
    try {
      
      String leakUrlStr = uriInfo.getRequestUri().toString();
      String refererUrl = httpRequest.getHeader("referer");
                           
      
      //Referer: http://teg-desktop.sb.statsbiblioteket.dk:8080/solrwayback/services/view?source_file_path=/media/teg/1200GB_SSD/solrwayback_package_3.2_webrecorder/indexing/warcs/thomas_egense_dk.warc&offset=9485066
      Map<String, String> queryMap = getQueryMap(refererUrl);
      String source_file_path = queryMap.get("source_file_path");      
      String offsetStr = queryMap.get("offset");
     
      if (source_file_path == null || offsetStr ==  null){
        log.warn("Need to fix leak, no source_file/offset for refererUrl:"+refererUrl + "url:"+leakUrlStr);        
        return Response.status(Response.Status.NOT_FOUND).build();
      }
      int leakUrlIndex=leakUrlStr.indexOf("/services/");
      String leakUrlPart=leakUrlStr.substring(leakUrlIndex+10);
      long offset=Long.parseLong(offsetStr);      
      log.info("leakurlStr:"+leakUrlStr);
      log.info("leakurlParth:"+leakUrlPart);      
      log.info("forwaring to view From leakedResource:"+source_file_path +" offset:"+offset +" leakPart:"+leakUrlPart);
      return viewFromLeakedResource(source_file_path, offset, leakUrlPart);
      

    } catch (Exception e) {
      log.error("Error resolving leak:"+uriInfo.toString(), e);
      return Response.ok().build();
    }
  }

  
/*
 * Showtoolbarnot working here.
 * 
 */
  @GET
  @Path("/viewForward")
  public Response viewForward(@QueryParam("source_file_path") String source_file_path, @QueryParam("offset") long offset, @QueryParam("showToolbar") Boolean showToolbar) throws ServiceException {
    try {
      IndexDoc arcEntry = NetarchiveSolrClient.getInstance().getArcEntry(source_file_path, offset);
      log.info("loading crawlDate:"+arcEntry.getCrawlDate());
      String url =  arcEntry.getUrl();
      String crawlDate = arcEntry.getCrawlDate();           
      String waybackDate = DateUtils.convertUtcDate2WaybackDate(crawlDate);      
                                             
     //Format is: /web/20080331193533/http://ekstrabladet.dk/112/article990050.ece 
      String newUrl=PropertiesLoader.WAYBACK_BASEURL+"services/web/"+waybackDate+"/"+url;
      newUrl = newUrl.replace("|", "%7C");//For some unknown reason Java does not accept |, must encode.
      //Below is for Open wayback at KB
    // String newUrl="http://kb-test-way-001.kb.dk:8082/jsp/QueryUI/Redirect.jsp?url="+url+"&time="+waybackDate;
      //http://kb-test-way-001.kb.dk:8082/jsp/QueryUI/Redirect.jsp?url=http%3A%2F%2Fwww.stiften.dk%2F&time=20120328044226
      log.info("forward url:"+newUrl);
      
      
      URI uri = UriBuilder.fromUri(newUrl).build();
      log.info("forwarding to:"+uri.toString());
      return Response.seeOther( uri ).build(); //Jersey way to forward response.
           
    } catch (Exception e) {
      throw handleServiceExceptions(e);
    }
  }


  
  @GET
  @Path("/viewFromLeakedResource")
  public Response viewFromLeakedResource(@QueryParam("source_file_path") String source_file_path, @QueryParam("offset") long offset, @QueryParam("urlPart") String urlPart) throws ServiceException {
    //this method is only called from the tomcat solrwaybackrootproxy if that proxy mode is used.
    try {

      log.info("viewFromLeakedResource called: source_file_path:"+source_file_path +" offset:"+offset);      
      log.info("urlPath:"+urlPart);

      //This is from the URL where the leak came from      
      IndexDoc arcEntry = NetarchiveSolrClient.getInstance().getArcEntry(source_file_path, offset);
      String orgUrl=arcEntry.getUrl();
      
      URL base = new URL(orgUrl);
      String resolvedUrl = new URL(base ,urlPart).toString();
      log.info("Resource should be located at:"+resolvedUrl);          
      return viewhref(resolvedUrl, arcEntry.getCrawlDate(), false);

    } catch (Exception e) {
      throw handleServiceExceptions(e);
    }
  }
  
  
  @GET
  @Path("/view")
  public Response view(@QueryParam("source_file_path") String source_file_path, @QueryParam("offset") long offset, @QueryParam("showToolbar") Boolean showToolbar) throws ServiceException {
    try {

      return viewImpl(source_file_path, offset,showToolbar);

    } catch (Exception e) {
      throw handleServiceExceptions(e);
    }
  }
  
  
  @GET
  @Path("/generatepwid")
  public String generatePid(@QueryParam("source_file_path") String source_file_path, @QueryParam("offset") long offset) throws Exception {
    log.debug("generatepwid:" + source_file_path + " offset:" + offset);
    String xml =Facade.generatePid(source_file_path, offset);

    return xml;   
  }


  @GET
  @Path("/timestampsforpage")
  public TimestampsForPage timestamps(@QueryParam("source_file_path") String source_file_path, @QueryParam("offset") long offset) throws Exception {
    log.debug("timestamps:" + source_file_path + " offset:" + offset);
    TimestampsForPage ts = Facade.timestampsForPage(source_file_path, offset);                                                                
    return ts;
  }

  @GET
  @Path("frontend/timestampsforpage")
  @Produces(MediaType.APPLICATION_JSON)
  public String timestampsFrontEnd(@QueryParam("source_file_path") String source_file_path, @QueryParam("offset") long offset) throws Exception {    
    return Facade.proxyBackendResources(source_file_path, ""+offset, "timestampsforpage");      

  }


  private Response viewImpl(String source_file_path, long offset,Boolean showToolbar) throws Exception{    	    	
    log.debug("View from FilePath:" + source_file_path + " offset:" + offset);
    IndexDoc doc = NetarchiveSolrClient.getInstance().getArcEntry(source_file_path, offset); // better way to detect html pages than from arc file
   
    Response redirect = getRedirect(doc, null);
    if (redirect != null){
      return redirect;
    }
    
    ArcEntry arcEntry= Facade.viewHtml(source_file_path, offset, doc, showToolbar);
    
    
    InputStream in = new ByteArrayInputStream(arcEntry.getBinary());
    
    String contentType = arcEntry.getContentType();
   if (contentType ==  null){    
    log.warn("no contenttype, using content_type from tika:"+doc.getContentType());
    contentType=doc.getContentType(); 
   }   
   if (arcEntry.getContentCharset() != null){
     contentType = contentType +"; charset="+arcEntry.getContentCharset();
   }
   else{
     contentType=doc.getContent_type_full();     
   }     
    //ResponseBuilder response = Response.ok((Object) in).type(contentType+"; charset="+arcEntry.getContentEncoding());                 
    //log.info("seting contentype:"+ contentType+"; charset="+arcEntry.getContentEncoding());
//          
   ResponseBuilder response = Response.ok((Object) in).type(contentType );                    

    if (arcEntry.isHasBeenDecompressed()){     
      response.header("Content-Encoding", doc.getContentEncoding()); 
    }else {      
      response.header("Content-Encoding", arcEntry.getContentEncoding());
    }
          
    return response.build();
  }


  @GET
  @Path("/viewhref")
  public Response viewhref(@QueryParam("url") String url, @QueryParam("crawlDate") String crawlDate,  @QueryParam("showToolbar") Boolean showToolbar  ) throws ServiceException {
    try {

      // We have to remove anchor # from URL. Not part of the harvested url
      //Notice it is not set again on URL, so anchor  autoscroll down will not work. 
      int anchorIndex =url.lastIndexOf("#");
      if (anchorIndex > 0){
        log.info("Anchor will be removed from url:"+url);
        url = url.substring(0,anchorIndex);            
      }

      IndexDoc indexDoc = NetarchiveSolrClient.getInstance().findClosestHarvestTimeForUrl(url, crawlDate);
      if (indexDoc == null){
        throw new NotFoundServiceException("Url has never been harvested:"+url);
      }

      log.info("Closest harvest to: " +crawlDate +" is "+indexDoc.getCrawlDate());
      return view(indexDoc.getSource_file_path(),indexDoc.getOffset(),showToolbar);

    } catch (Exception e) {
      throw handleServiceExceptions(e);
    }

  }


  @POST
  @Path("/upload/gethash")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.TEXT_PLAIN)
  public String uploadPdf(@FormDataParam("file") InputStream uploadedInputStream ,
      @FormDataParam("file") FormDataContentDisposition fileDetail
      ) throws  ServiceException { 

    try {                  
      log.info("upload called for file:"+fileDetail.getFileName());          
      String sha1 = Sha1Hash.createSha1(uploadedInputStream);
      log.info("uploaded file has sha1:"+sha1);
      return sha1;

    } catch (Exception e) {         
      throw handleServiceExceptions(e);
    }      
  }


  @GET
  @Path("/waybacklinkgraph")
  @Produces(MediaType.APPLICATION_JSON)
  public D3Graph waybackgraph(@QueryParam("domain") String domain, @QueryParam("ingoing") Boolean ingoing, @QueryParam("facetLimit") Integer facetLimit, @QueryParam("dateStart") String dateStart, @QueryParam("dateEnd") String dateEnd) throws ServiceException {
    try{        
      log.info("ingoing:"+ingoing +" facetLimit:"+facetLimit +" dateStart:"+dateStart +" dateEnd:"+dateEnd);
      int fLimit =10;//Default
      boolean in=false;//Default
      if (facetLimit != null){
        fLimit=facetLimit.intValue();
      }
      if(ingoing != null){
        in=ingoing.booleanValue();
      }

      //TODO use ingoing, facetlimit. with defaults
      return Facade.waybackgraph(domain, fLimit,in,dateStart,dateEnd);        

    } catch (Exception e) {
      throw handleServiceExceptions(e);
    }

  }


  
  /*
   * Some leaks refers to the contextroot of the webapplication(/solrwayback). Etc. /solrwayback/images/horse.png
   * This page does not exist and the tomcat error page will forward to this method.
   * Both the original url and the resource (warc+offset) are known, so the correct relative url can be constructed.  
   */
  
  @GET
  @Path("/resolveLeak")
  public Response proxy(@Context UriInfo uriInfo, @Context HttpServletRequest httpRequest) throws Exception {
    try {
      
      // refererUrl="http://teg-desktop.sb.statsbiblioteket.dk:8080/solrwayback/services/view?source_file_path=/media/teg/1200GB_SSD/netarkiv/warcs/solrwayback_2018-08-27-13-29-21.warc&offset=1226957110";
      // leakUrl= "http://localhost:8080/images/leaked.png?test=123";      
            
      String leakUrl = httpRequest.getParameter("url");
      String refererUrl = httpRequest.getHeader("referer");
      Map<String, String> queryMap = getQueryMap(refererUrl);
      String source_file_path = queryMap.get("source_file_path");      
      String offsetStr = queryMap.get("offset");
           
      if (source_file_path == null || offsetStr ==  null){
        log.warn("Need to fix leak, no source_file/offset for refererUrl:"+refererUrl +" request url:"+leakUrl);        
        return Response.status(Response.Status.NOT_FOUND).build();
      }      
      IndexDoc doc = Facade.resolveRelativUrlForResource(source_file_path, Long.parseLong(offsetStr), leakUrl);
      log.info("Resolved leak to doc url:"+doc.getUrl());  
      log.info("Resolved leak to doc offset:"+doc.getOffset());      
      return downloadRaw(doc.getSource_file_path(), doc.getOffset());
    }
    catch(Exception e){
      throw handleServiceExceptions(e);
    }
    
    }
      
  public static Map<String, String> getQueryMap(String url)
  {        
      Map<String, String> map = new HashMap<String, String>();
      if (url == null){
        return map;
      } 
    
      int index = url.indexOf("?");
       if(index == -1){
         log.warn("no paramters for url:"+url);         
         return new HashMap<String, String>();
       }
       
      url =url.substring(index+1);
      String[] params = url.split("&");
    
      for (String param : params)
      {       
         String name = param.split("=")[0];
         String value = param.split("=")[1];
         map.put(name, value);
      }
      return map;
  }
  
  
  /*
   * This will set the correct status and redirect 
   */
  private static Response getRedirect (IndexDoc doc, ArcEntry arc) throws Exception{
    int status = doc.getStatusCode();
    
    if (status>= 300 && status <=399){ //Redirects.
      ResponseBuilder response = Response.status(status);
      
      if(arc == null){
        arc = Facade.getArcEntry(doc.getSource_file_path(), doc.getOffset());
      }      
      response.status(status); // jersey require a legal status code.

      String redirectUrl = Normalisation.resolveRelative(arc.getUrl(), arc.getRedirectUrl(), false);
      log.info("Redirect url resolved to:"+redirectUrl);      
      if (redirectUrl != null){
        //build the new redirect url
        String crawlDate = doc.getCrawlDate();        
        String waybackDate = DateUtils.convertUtcDate2WaybackDate(crawlDate);
        String newUrl=PropertiesLoader.WAYBACK_BASEURL+"services/web/"+waybackDate+"/"+redirectUrl;
        response.header("location", newUrl);
        return response.build();        
      }      
    }
    return null;
    
  }
  
  private ServiceException handleServiceExceptions(Exception e) {
    if (e instanceof ServiceException) {
      log.info("Handling serviceException:" + e.getMessage());
      return (ServiceException) e; // Do nothing, exception already correct
    } else if (e instanceof IllegalArgumentException) {
      log.error("ServiceException(HTTP 400) in Service:", e.getMessage());
      return new InvalidArgumentServiceException(e.getMessage());
    } else {// unforseen exceptions.... should not happen.
      log.error("ServiceException(HTTP 500) in Service:", e);
      return new InternalServiceException(e.getMessage());
    }
  }
}
