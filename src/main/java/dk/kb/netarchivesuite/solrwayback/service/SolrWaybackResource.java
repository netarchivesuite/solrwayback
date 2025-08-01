package dk.kb.netarchivesuite.solrwayback.service;


import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import dk.kb.netarchivesuite.solrwayback.util.PathResolver;
import org.apache.solr.client.solrj.SolrServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.netarchivesuite.solrwayback.facade.Facade;
import dk.kb.netarchivesuite.solrwayback.image.ImageUtils;
import dk.kb.netarchivesuite.solrwayback.normalise.Normalisation;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoaderWeb;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;
import dk.kb.netarchivesuite.solrwayback.service.dto.ImageUrl;
import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDoc;
import dk.kb.netarchivesuite.solrwayback.service.dto.TimestampsForPage;
import dk.kb.netarchivesuite.solrwayback.service.dto.statistics.DomainStatistics;
import dk.kb.netarchivesuite.solrwayback.service.exception.InternalServiceException;
import dk.kb.netarchivesuite.solrwayback.service.exception.InvalidArgumentServiceException;
import dk.kb.netarchivesuite.solrwayback.service.exception.NotFoundServiceException;
import dk.kb.netarchivesuite.solrwayback.service.exception.SolrWaybackServiceException;
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
  @Path("warc/header/parsed")
  @Produces(MediaType.APPLICATION_JSON +"; charset=UTF-8")
  public ArcEntry getArcEntry(@QueryParam("source_file_path") String source_file_path, @QueryParam("offset") long offset) throws SolrWaybackServiceException {
      try {                                                                                      
        ArcEntry arcEntry= Facade.getArcEntry(source_file_path, offset);
        return arcEntry;                              
      } catch (Exception e) {         
          throw handleServiceExceptions(e);
      }
  }
  
  
  @GET
  @Path("warc/header")
  @Produces({ MediaType.TEXT_PLAIN})
  public String getWarcHeader( @QueryParam("source_file_path") String source_file_path, @QueryParam("offset") long offset) throws SolrWaybackServiceException {
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
  public ArcEntry getWarcParsed( @QueryParam("source_file_path") String source_file_path, @QueryParam("offset") long offset) throws SolrWaybackServiceException {
      try {                                                                                      
        ArcEntry arcEntry= Facade.getArcEntry(source_file_path, offset);
         return arcEntry;                              
      } catch (Exception e) {         
          throw handleServiceExceptions(e);
      }
  }
  
 
  
  
  
  @GET
  @Path("statistics/domain")
  @Produces({ MediaType.APPLICATION_JSON})
  public  List<DomainStatistics> statisticsDomain (@QueryParam("domain") String domain, @QueryParam("startdate") String startdate,
          @QueryParam("enddate") String enddate, @QueryParam("scale") String scale) throws SolrWaybackServiceException {
      int limit = 90;
      LocalDate start = LocalDate.parse(startdate, DateTimeFormatter.ISO_DATE);
      LocalDate end = LocalDate.parse(enddate, DateTimeFormatter.ISO_DATE);
      
      // If the period is too big for the scale, block the statistics
      int buckets = DateUtils.calculateBucket(start, end, scale);
      if (buckets > limit) {
          String msg = "The defined period (" + buckets + ") is too large to match with the scale (limit: " + limit + " " + scale.toLowerCase() + "s)";
          log.error(msg);
          throw new InvalidArgumentServiceException(msg);
      }
      try {
        return Facade.statisticsDomain(domain, start , end, scale);
      } catch (Exception e) {
          throw handleServiceExceptions(e);
      }
  }
 
  
  
//Already removed to frontend. Keep until sure it is not used.
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
  @Path("/image")
  @Produces("image/png")
  public Response getImage(@QueryParam("source_file_path") String source_file_path, @QueryParam("offset") long offset, @QueryParam("height") int height, @QueryParam("width") int width)
      throws SolrWaybackServiceException {
    
      //If playback is disable, only tumbnals is allowed.
      if (PropertiesLoader.PLAYBACK_DISABLED && (height > 200 || width >200)) {
          throw new InvalidArgumentServiceException("Playback has been disabled in the configuration");          
      }
      
      
      try {

      //log.debug("Getting image from source_file_path:" + source_file_path + " offset:" + offset + " targetWidth:" + width + " targetHeight:" + height);

      ArcEntry arcEntry= Facade.getArcEntry(source_file_path, offset);

      // TODO: This is prone to OOM for large images. There should be a sanity check of width & height first
      BufferedImage image = ImageUtils.getImageFromBinary(arcEntry.getBinaryDecoded());

      if (image== null){
        // java does not support ico format. Just serve it RAW... 
        // Also SVG scaling bugs too much in java
        if (arcEntry.getUrl().toLowerCase().indexOf("/favicon.ico") >0 || arcEntry.getContentType().contains("image/svg+xml")){
           log.info("image is ico-image or SVG serving it raw");
           return downloadRaw(source_file_path, offset);          
        }
        log.warn("image is null and not .ico file, source_file_path:"+source_file_path +" offset:"+offset);
        throw new IllegalArgumentException("image is null and not .ico image, source_file_path:"+source_file_path +" offset:"+offset +" contentType:"+arcEntry.getContentType());                
      }

      int sourceWidth = image.getWidth();
      int sourceHeight = image.getHeight();

      if (sourceHeight <= height && sourceWidth <= width) { // No resize, image is smaller
       return convertToPng(image); 
        
      } else {
        BufferedImage resizeImage = ImageUtils.resizeImage(image, sourceWidth, sourceHeight, width, height);
        return convertToPng(resizeImage);
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

  /*
   * Moved to frontend
   */
  
  @GET
  @Path("/downloadRaw")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)    
  public Response downloadRaw(@QueryParam("source_file_path") String source_file_path, @QueryParam("offset") long offset) throws SolrWaybackServiceException {
    try {

        
        // If playback is disabled, the download raw must be blocked. Except for images which will be resized.
        // Too much work in frontend to rewrite the logic to call the /image url instead and then downloadRaw method could be blocked always
        if(PropertiesLoader.PLAYBACK_DISABLED) {
            //Temporary hack. Check if image and return tumbnail
            IndexDoc indexDoc = NetarchiveSolrClient.getInstance().getArcEntry(source_file_path, offset);      
            if ("image".equals(indexDoc.getContentTypeNorm())){
                return getImage(source_file_path, offset, 200, 200);                
            }
            
            throw new InvalidArgumentServiceException("Playback has been disabled in the configuration");
        }
        
  //  log.debug("Download from FilePath:" + source_file_path + " offset:" + offset);
      ArcEntry arcEntry= Facade.getArcEntry(source_file_path, offset);
      
      //Only solr lookup if redirect.
      if (arcEntry.getStatus_code() >= 300 &&  arcEntry.getStatus_code() <= 399 ){
        IndexDoc indexDoc = NetarchiveSolrClient.getInstance().getArcEntry(source_file_path, offset);         
        Response responseRedirect = getRedirect(indexDoc,arcEntry);
        log.debug("Redirecting. status code from arc:"+arcEntry.getStatus_code() + " vs index " +indexDoc.getStatusCode()); 
        return responseRedirect;
      }
      
      InputStream in = arcEntry.getBinaryNoChunking(); //Stream entry. Dechucking require as tomcat/apache also chunks.
      
      ResponseBuilder response = null;
      try{        
        String contentType = arcEntry.getContentType();
        if (arcEntry.getContentCharset() != null){ //Do I also have to check contentType not null?
          contentType = contentType +"; charset="+arcEntry.getContentCharset();
        }
        else {                          
            IndexDoc doc = NetarchiveSolrClient.getInstance().getArcEntry(source_file_path, offset); // better way to detect html pages than from arc file

            //is this the case for all images and binaries etc?
            //log.debug("No content charset in warc-header, using full contentType from tika:"+doc.getContentType() + " for  "+source_file_path +" offset:"+offset +" content-type:"+doc.getContentType());            
            contentType=doc.getContentType(); 
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
      
//      log.debug("Download from source_file_path:" + source_file_path + " offset:" + offset + " is mimetype:" + arcEntry.getContentType() + " and has filename:" + arcEntry.getFileName());      
      return response.build();

    } catch (Exception e) {
      log.error("Error download from source_file_path:"+ source_file_path + " offset:" + offset,e);
      throw handleServiceExceptions(e);
    }
  }

  

  @GET
  @Path("/export/linkgraph")    
  @Produces(MediaType.APPLICATION_OCTET_STREAM)    
  public Response exportLinkGraph(@QueryParam("query") String q) throws SolrWaybackServiceException {
   
    //This is also required even if the option is removed on the web-page.
    if (!PropertiesLoaderWeb.ALLOW_EXPORT_CSV){ 
      throw new InvalidArgumentServiceException("Export to csv not allowed!");
    }        
    try {
      log.debug("Export linkgraph. query:"+q);
      InputStream is = Facade.exportLinkGraphStreaming(q);
      return Response.ok(is).header("Content-Disposition", getDisposition("solrwayback_linkgraph_$DATETIME.csv")).build();
    } catch (Exception e) {
      log.error("Error in export linkgraph",e);
      throw handleServiceExceptions(e);
    }    
  }
  
  @GET
  @Path("/export/warc")    
  @Produces(MediaType.APPLICATION_OCTET_STREAM)    
  public Response exportWarc(@QueryParam("query") String q, @QueryParam("fq") List<String> fq, @QueryParam("gzip") boolean gzip) throws SolrWaybackServiceException {
   
    //This is also required even if the option is removed on the web-page.
    if (!PropertiesLoaderWeb.ALLOW_EXPORT_WARC){ 
      throw new InvalidArgumentServiceException("Export to warc not allowed!");
    }    
    return exportWarcImpl(q, fq, gzip, false, false);
  }
  
  @GET
  @Path("/export/warcExpanded")    
  @Produces(MediaType.APPLICATION_OCTET_STREAM)    
  public Response exportWarcExpanded(@QueryParam("query") String q, @QueryParam("fq") List<String> fq,  @QueryParam("gzip") boolean gzip) throws SolrWaybackServiceException {
    //This is also required even if the option is removed on the web-page.
    if (!PropertiesLoaderWeb.ALLOW_EXPORT_WARC){ 
      throw new InvalidArgumentServiceException("Export to warc not allowed!");
    }        
    return exportWarcImpl(q, fq, gzip, true, true);
  }
  
  
  private Response exportWarcImpl(String q,
                                   List<String>  fqList,
                                   boolean gzip,
                                   boolean expandResources,
                                   boolean avoidDuplicates) throws SolrWaybackServiceException {
    InputStream is = null;
    try {
      log.debug("Export warc. gzip="+gzip +" query:"+q +" filterquery:"+fqList);
      DateFormat formatOut= new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
      String dateStr = formatOut.format(new Date());

      //Map FQ List<String> to String[]
      String[] fqArray = fqList.stream().toArray(String[]::new);
      is = Facade.exportWarcStreaming(expandResources, avoidDuplicates, gzip,q, fqArray);
      
      String template = "solrwayback_$DATETIME.warc";
      if (gzip) {
          template += ".gz";
      }
      
      return Response.ok(is).header("Content-Disposition", getDisposition(template)).build();

    } catch (Exception e) {
      if (is != null) { // We cannot use the Closeable-feature as we return the stream(?)
        try {
          is.close();
        } catch (IOException ex) {
          log.error("Error closing export stream", e);
        }
      }
      log.error("Error in export warc",e);
      throw handleServiceExceptions(e);
    }
  }


  @GET
  @Path("/image/pagepreview")
  @Produces("image/png")
  public Response getHtmlPagePreview(@QueryParam("source_file_path") String source_file_path, @QueryParam("offset") long offset)
      throws SolrWaybackServiceException {
      
      if (PropertiesLoader.PLAYBACK_DISABLED) {            
          throw new InvalidArgumentServiceException("Playback has been disabled in configuration");
      }
            
     try {
      log.debug("Getting thumbnail html image from source_file_path:" + source_file_path + " offset:" + offset);
      BufferedImage image = Facade.getHtmlPagePreview(source_file_path, offset);          
      return convertToPng(image);                       
    } catch (Exception e) {
      log.error("error thumbnail html image:"+source_file_path +" offset:"+offset);  
      throw handleServiceExceptions(e);
    }
  }

  
  
  /**
   * 
   * @deprecated
   * This method is no longer acceptable to compute time between versions.
   * Use '/export/fields' instead. 
   * Have frontend switch to this method. 
   *
   */
  @Deprecated   
  @GET
  @Path("/export/csv")    
  public Response exportFull(@QueryParam("query") String q,@QueryParam("fields") String fields, @QueryParam("fq") String... filters) throws SolrWaybackServiceException {
    if (!PropertiesLoaderWeb.ALLOW_EXPORT_CSV){ 
      throw new InvalidArgumentServiceException("Export to csv not allowed!");
    }
    try {               
      log.debug("Csv export. Query:"+q +" filterquery:"+filters);
      InputStream is = Facade.exportFields(fields, false, false, null, false, "csv", false, q, filters);
      return Response.ok(is).header("Content-Disposition", getDisposition("solrwayback_$DATETIME.csv")).build();

    } catch (Exception e) {
      log.error("Error in export full",e);      
      throw handleServiceExceptions(e);
    }

  }

  @GET
  @Path("/export/fields")
  public Response exportFields(@QueryParam("query") String q, 
                               @QueryParam("fields") String fields,
                               @QueryParam("expandResources") Boolean expandResources,
                               @QueryParam("ensureUnique") Boolean ensureUnique,
                               @QueryParam("groupfield") String groupField,
                               @QueryParam("flatten") Boolean flatten,
                               @QueryParam("format") String format,
                               @QueryParam("gzip") Boolean gzip,
                               @QueryParam("fq") String... filters                     
          ) throws SolrWaybackServiceException {
    if (!PropertiesLoaderWeb.ALLOW_EXPORT_CSV){
      throw new InvalidArgumentServiceException("Export to fields not allowed!");
    }
    format = format == null ? "csv" : format;
    gzip = Boolean.TRUE.equals(gzip); // Guard against NullPointerException later on
    try {
      log.debug("{} export. Query:'{}, filterquery:'{}', fields:'{}', expandResources:{}, ensureUnique:{}, flatten:{}, groupfield:{}, gzip:{}",
                format, q, filters, fields,
                Boolean.TRUE.equals(expandResources), Boolean.TRUE.equals(ensureUnique), Boolean.TRUE.equals(flatten),
                groupField, gzip);
      InputStream is = Facade.exportFields(fields, expandResources, ensureUnique, groupField, flatten, format, gzip, q, filters);
      // TODO: Set MIME-type and compression flag
      String filenameTemplate = "solrwayback_$DATETIME." + format + (gzip ? ".gz" : "");
      return Response.ok(is).header("Content-Disposition", getDisposition(filenameTemplate)).build();
    } catch (Exception e) {
      log.error("Error in export full",e);
      throw handleServiceExceptions(e);
    }

  }

  /**
   * Endpoint that delivers a zip file of content present in query of a specific content type.
   * @param query       used to extract WARC entries from solr by.
   * @param filters     appended to the solr query.
   * @return            a zip file of the exported content.
   */
  @GET
  @Path("/export/zip")
  public Response exportZipContent(@QueryParam("query") String query, @QueryParam("fq") String... filters)
          throws InvalidArgumentServiceException, SolrServerException, IOException {
    if (!PropertiesLoaderWeb.ALLOW_EXPORT_ZIP){
      throw new InvalidArgumentServiceException("Zip export is not allowed!");
    }

    StreamingOutput zip = Facade.exportZipContent(query, filters);

    return Response.ok(zip)
            .header("Content-Disposition", getDisposition("solrwayback_$DATETIME.zip"))
            .build();

  }


  /*
//   *  This will be called from solrwayback page views, when resources can not be resolved (not harvested)  
   */    
  @GET
  @Path("/notfound")    
  public Response notfound() throws SolrWaybackServiceException {                      
//    log.debug("not found called");
    throw new NotFoundServiceException("");                  
  }

  
  @GET
  @Path("/getContentType")
  public String getContentType(@QueryParam("source_file_path") String source_file_path, @QueryParam("offset") long offset) throws SolrWaybackServiceException {
    try {               
      return Facade.getEncoding(source_file_path, ""+offset);       
    } catch (Exception e) {
      throw handleServiceExceptions(e);
    }

  }


  /*
   * TODO remove this method and replace with the webProxyLeak method?    
   * 
   */
  @GET
  @Path("/webProxy/{path:.+}")
  public Response waybackProxyAPIResolver(@Context UriInfo uriInfo, @PathParam("path") String path) throws SolrWaybackServiceException {
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
      return viewImpl(doc.getSource_file_path() , doc.getOffset(),false, null); //NO TOOLBAR!
      
                     
    } catch (Exception e) {
      throw handleServiceExceptions(e);
    }

  }

  
  /* Called from sw.js (serviceworker).
   * It is called when a resource leaks to the domain without the /solrwayback/ contextroot.
   * 
   * The referer url can be playback API (often HTML page) or warcfile+offs (javascript or CSS)
   *    
   */
  @GET
  @Path("/webProxyLeak/{path:.+}")
  public Response webProxyLeak(@Context UriInfo uriInfo,  @Context HttpServletRequest httpRequest, @PathParam("path") String path) throws SolrWaybackServiceException {
    try {        
      //For some reason the var regexp does not work with comma (;) and other characters. So I have to grab the full url from uriInfo
     // log.info("/webProxyLeak called with data:"+path);
      String fullUrl = uriInfo.getRequestUri().toString();      
     
      int dataStart=fullUrl.indexOf("/webProxyLeak/");      
      String urlData = fullUrl.substring(dataStart+14);
      
      //log.info("WebProxyLeak urldata:"+urlData);// example https://solrwaybackserver:4000/sites/all/images/horse.png;      
      String copiedReferer = httpRequest.getHeader("serviceworker_referer"); //The serviceworker will set this header with the original referer      
      //log.info("WebProxyLeak referer:"+copiedReferer);
      
      
      String solrwaybackServer=PropertiesLoader.WAYBACK_BASEURL.replace("/solrwayback/", "");
      if (!urlData.startsWith( solrwaybackServer)){
        log.error("WebProxyLeak does not originate from solrwayback server:"+urlData);
        return notfound();        
      }
            
      //This is the relative url that needs to be resolved from the referer
      String leakUrlPart=urlData.substring(solrwaybackServer.length()); 
      //log.info("webProxyLeak leakUrlPart:"+leakUrlPart);
      
      Map<String, String> queryMap = getQueryMap(copiedReferer);
      String source_file_path = queryMap.get("source_file_path");      
      String offsetStr = queryMap.get("offset");
      
      if (source_file_path != null || offsetStr !=  null){
        //log.info("webProxyLeak got offset+warc");
        return viewFromLeakedResource(source_file_path, Long.parseLong(offsetStr), leakUrlPart);
      }

      String[] timeAndUrl = UrlUtils.getCrawltimeAndUrlFromWebProxyLeak(copiedReferer);
      if (timeAndUrl == null) { //Should not happen
        log.error("Can not find warc/offset or crawltime/url from ServiceWorker referer:"+copiedReferer);
        return notfound();
      }
      String crawlTime =  timeAndUrl[0];
      String orgUrl =  timeAndUrl[1];
      
      URL base = new URL( orgUrl);
      String resolvedUrl = new URL(base ,leakUrlPart).toString();
      log.info("webProxyLeak should be located at:"+resolvedUrl);          
      
      String solrDate = DateUtils.convertWaybackDate2SolrDate(crawlTime);
      
      return viewhref(resolvedUrl, solrDate, false);     
                     
    } catch (Exception e) {
      throw handleServiceExceptions(e);
    }

  }

  
  
  /*
   * '/web/' is the same as wayback machine uses. 
   * 
   * JAX-RS syntax to match all after /web/.
   */
  @GET
  @Path("/web/{path:.+}")      
  public Response waybackAPIResolver(@Context UriInfo uriInfo, @Context HttpServletRequest httpRequest,
                                     @PathParam("path") String path) throws SolrWaybackServiceException {
    return PathResolver.waybackAPIResolverHelper(this, "/web/", uriInfo, httpRequest, path, false);
  }
  /*
   * Playback with lenient URL resolving.
   * The last part of the path '/web/' is the same as wayback machine uses.
   *
   * JAX-RS syntax to match all after /lenient/web/.
   */
  @GET
  @Path("/lenient/web/{path:.+}")
  public Response waybackAPIResolverLenient(@Context UriInfo uriInfo, @Context HttpServletRequest httpRequest,
                                     @PathParam("path") String path) throws SolrWaybackServiceException {
    return PathResolver.waybackAPIResolverHelper(this,"/lenient/web/", uriInfo, httpRequest, path, true);
  }
   
  /*
   *  will be called with
   *   pwid/web/urn:pwid:netarkivet.dk:2018-12-10T06:27:01Z:part:https://www.petdreams.dk/katteracer-siameser
   * 
   * Jersey syntax to match all after pwid/web/.
   */
  @GET
  @Path("/pwid/web/{path:.+}")
  public Response waybackPwidAPIResolver(@Context UriInfo uriInfo, @Context HttpServletRequest httpRequest, @PathParam("path") String path) throws SolrWaybackServiceException {
    try {        
      //For some reason the var regexp does not work with comma (;) and other characters. So I have to grab the full url from uriInfo
      log.info("/pwid/web/ called with data:"+path);
      String fullUrl = uriInfo.getRequestUri().toString();
      log.info("full url:"+fullUrl);
      int pwidStart=fullUrl.indexOf("/pwid/web/"); //urn:pwid:netarkivet.dk:2018-12-10T06:27:01Z:part:https://www.petdreams.dk/katteracer-siameser
      String pwid = fullUrl.substring(pwidStart+10);

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

      return viewImpl(doc.getSource_file_path() , doc.getOffset(),true, null);
    } catch (Exception e) {
      throw handleServiceExceptions(e);
    }

  }

  
  
  /*
   * This happens for leaks to solrwayback/services/  
   * The proxy will handle it
   */
  @GET
  @Path("/{path:.+}")
  public Response waybackAPIResolverRoot(@Context UriInfo uriInfo, @Context HttpServletRequest httpRequest, @PathParam("path") String path) throws SolrWaybackServiceException {
    try {
      
      String leakUrlStr = uriInfo.getRequestUri().toString();
      String refererUrl = httpRequest.getHeader("referer");
                           
      
      //Referer: http://teg-desktop.sb.statsbiblioteket.dk:8080/solrwayback/services/view?source_file_path=/media/teg/1200GB_SSD/solrwayback_package_3.2_webrecorder/indexing/warcs/thomas_egense_dk.warc&offset=9485066
      Map<String, String> queryMap = getQueryMap(refererUrl);
      String source_file_path = queryMap.get("source_file_path");      
      String offsetStr = queryMap.get("offset");
     
      if (source_file_path == null || offsetStr ==  null){
        log.warn("Need to fix leak, no source_file/offset for refererUrl:"+refererUrl + " url:"+leakUrlStr);        
        return Response.status(Response.Status.NOT_FOUND).build();
      }
      int leakUrlIndex=leakUrlStr.indexOf("/services/");
      String leakUrlPart=leakUrlStr.substring(leakUrlIndex+10);
      long offset=Long.parseLong(offsetStr);      
      //log.info("leakurlStr:"+leakUrlStr);
      //log.info("leakurlPart:"+leakUrlPart);      
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
  public Response viewForward(@QueryParam("source_file_path") String source_file_path, @QueryParam("offset") long offset,
                              @QueryParam("showToolbar") Boolean showToolbar, @QueryParam("lenient") Boolean lenient)
          throws SolrWaybackServiceException {
    try {
              
       if (PropertiesLoader.PLAYBACK_DISABLED) {            
            throw new InvalidArgumentServiceException("Playback has been disabled in configuration");
        }
       
      IndexDoc arcEntry = NetarchiveSolrClient.getInstance().getArcEntry(source_file_path, offset);

      String url =  arcEntry.getUrl();
      String crawlDate = arcEntry.getCrawlDate();           
      String waybackDate = DateUtils.convertUtcDate2WaybackDate(crawlDate);      
                                             
     //Format is: /web/20080331193533/http://ekstrabladet.dk/112/article990050.ece 
      String newUrl= Boolean.TRUE.equals(lenient) ?
              PropertiesLoader.WAYBACK_BASEURL+"services/lenient/web/"+waybackDate+"/"+url :
              PropertiesLoader.WAYBACK_BASEURL+"services/web/"+waybackDate+"/"+url;
              
      newUrl = newUrl.replace("|", "%7C");//For some unknown reason Java does not accept |, must encode.
      newUrl = newUrl.replace("%2f", "/"); // or url will not rest Rest pattern for method. (not clear why)
      newUrl = newUrl.replace("%2F", "/"); // or url will not rest Rest pattern for method. (not clear why)
      
      //Below is for Open wayback at KB
    // String newUrl="http://kb-test-way-001.kb.dk:8082/jsp/QueryUI/Redirect.jsp?url="+url+"&time="+waybackDate;
      //http://kb-test-way-001.kb.dk:8082/jsp/QueryUI/Redirect.jsp?url=http%3A%2F%2Fwww.stiften.dk%2F&time=20120328044226
            
      URI uri =new URI(newUrl);
      log.info("forwarding to:"+uri.toString());
      return Response.seeOther( uri ).build(); //Jersey way to forward response.
           
    } catch (Exception e) {
      throw handleServiceExceptions(e);
    }
  }


  
  @GET
  @Path("/viewFromLeakedResource")
  public Response viewFromLeakedResource(@QueryParam("source_file_path") String source_file_path, @QueryParam("offset") long offset, @QueryParam("urlPart") String urlPart) throws SolrWaybackServiceException {
    //this method is only called from the tomcat solrwaybackrootproxy if that proxy mode is used.
    try {

      log.info("viewFromLeakedResource called: source_file_path:"+source_file_path +" offset:"+offset +" urlPath:"+urlPart);      

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
  public Response view(@QueryParam("source_file_path") String source_file_path, @QueryParam("offset") long offset,
                       @QueryParam("showToolbar") Boolean showToolbar, @QueryParam("lenient") Boolean lenient)
          throws SolrWaybackServiceException {
    try {

      return viewImpl(source_file_path, offset,showToolbar, lenient);

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
  @Produces(MediaType.APPLICATION_JSON +"; charset=UTF-8")
  public TimestampsForPage timestamps(@QueryParam("source_file_path") String source_file_path, @QueryParam("offset") long offset) throws Exception {
    log.debug("timestamps:" + source_file_path + " offset:" + offset);
    TimestampsForPage ts = Facade.timestampsForPage(source_file_path, offset);                                                                
    return ts;
  }

  /*
  @GET
  @Path("frontend/timestampsforpage")
  @Produces(MediaType.APPLICATION_JSON)
  public String timestampsFrontEnd(@QueryParam("source_file_path") String source_file_path, @QueryParam("offset") long offset) throws Exception {    
    return Facade.proxyBackendResources(source_file_path, ""+offset, "timestampsforpage");      

  }
*/

  public Response viewImpl(String source_file_path, long offset, Boolean showToolbar, Boolean lenient) throws Exception{
    
      if (PropertiesLoader.PLAYBACK_DISABLED) {          
          throw new InvalidArgumentServiceException("Playback has been disabled in the configuration");
      }
      
     log.debug("View from FilePath:" + source_file_path + " offset:" + offset);
    IndexDoc doc = NetarchiveSolrClient.getInstance().getArcEntry(source_file_path, offset); // better way to detect html pages than from arc file
   
    Response redirect = getRedirect(doc, null);
    if (redirect != null){
      return redirect;
    }
    
    ArcEntry arcEntry= Facade.viewResource(source_file_path, offset, doc, showToolbar, lenient);
    
    
    String contentType = arcEntry.getContentType();
    
    
   //log.debug("warc content charset:"+arcEntry.getContentCharset() +" warc content type:"+arcEntry.getContentType());
   if (contentType ==  null){    
    //log.debug("no contenttype, using content_type from tika:"+doc.getContentType());
    contentType=doc.getContentType(); 
   }   
   if (arcEntry.getContentCharset() != null){
     contentType = contentType +"; charset="+arcEntry.getContentCharset();
   }
   else{
     contentType=doc.getContent_type_full();     
   }     
    //ResponseBuilder response = Response.ok((Object) in).type(contentType+"; charset="+arcEntry.getContentEncoding());                 
   //log.debug("setting contentype:"+contentType);
//          
   
   ResponseBuilder response = Response.ok(arcEntry.getBinaryNoChunking()).type(contentType );

    if (arcEntry.isHasBeenDecompressed()){ //Will have if playback (HTML, Twitter, etc.) has replaced the content
    	response.header("Content-Encoding", "identity"); //Not required, but will make it easier to see it has been applied.
    } else {      
    	response.header("Content-Encoding", arcEntry.getContentEncoding());
    }          
         
     return response.build();
  }


  @GET
  @Path("/viewhref")
  public Response viewhref(@QueryParam("url") String url, @QueryParam("crawlDate") String crawlDate,  @QueryParam("showToolbar") Boolean showToolbar  ) throws SolrWaybackServiceException {
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

      //log.debug("Closest harvest to: " +crawlDate +" is "+indexDoc.getCrawlDate());
      return view(indexDoc.getSource_file_path(),indexDoc.getOffset(),showToolbar, null);

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
                  
      String leakUrl = httpRequest.getParameter("url");
      log.info("Resolve leak called for url:"+leakUrl);
      
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
         return new HashMap<String, String>();
       }
       
      url =url.substring(index+1);
      String[] params = url.split("&");
    
      for (String param : params)
      {                   
          String[] keyVal=param.split("=");
          String name = keyVal[0];  
          String value="";
           if(keyVal.length >1) { //Value can be empty: key1=value1&key2=
            value =  keyVal[1];
           }
         map.put(name, value);
      }
      return map;
  }
  
  
  /*
   * Move to common top class for both services
   */
  
  /*
   * This will set the correct status and redirect 
   */
  public static Response getRedirect (IndexDoc doc, ArcEntry arc) throws Exception{
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
  
  private Response convertToPng(BufferedImage image)  throws Exception { 
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(image, "png", baos);
    byte[] imageData = baos.toByteArray();
    baos.flush();
    baos.close();
    ResponseBuilder response = Response.ok(new ByteArrayInputStream(imageData));
    return response.build();
  }

  /**
   * Calls {@link #applyTemplate(String)} with the given template and uses the result to construct a HTTP
   * Content-Disposition for downloading.
   * {@code getDisposition("solrwayback_$DATETIME.warc")} might return
   * {@code attachment; filename="solrwayback_2022-10-14_23-36-04.warc"}.
   * @param template a template to expand. {code $DATETIME} will be replaced with a timestamp.
   * @return a content disposition with filename information for browser download.
   */
  private String getDisposition(String template) {
    return "attachment; filename=\"" + applyTemplate(template) + "\"";
  }

  /**
   * Apply the given template. Currently only {@code $DATETIME} will be expanded to the current datetime:
   * {@code applyTemplate("solrwayback_$DATETIME.warc")} might return {@code solrwayback_2022-10-14_23-36-04.warc}.
   * @param template a template to expand. {code $DATETIME} will be replaced with a timestamp.
   * @return the applied template.
   */
  private String applyTemplate(String template) {
    DateFormat formatOut= new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    String dateStr = formatOut.format(new Date());
    return template.replace("$DATETIME", dateStr);
  }

  public SolrWaybackServiceException handleServiceExceptions(Exception e) {
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
