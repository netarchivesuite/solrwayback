package dk.kb.netarchivesuite.solrwayback.service;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

import dk.kb.netarchivesuite.solrwayback.encoders.Sha1Hash;
import dk.kb.netarchivesuite.solrwayback.facade.Facade;
import dk.kb.netarchivesuite.solrwayback.image.ImageUtils;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntryDescriptor;
import dk.kb.netarchivesuite.solrwayback.service.dto.HarvestDates;
import dk.kb.netarchivesuite.solrwayback.service.dto.ImageUrl;
import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDoc;
import dk.kb.netarchivesuite.solrwayback.service.dto.PagePreview;
import dk.kb.netarchivesuite.solrwayback.service.dto.TimestampsForPage;
import dk.kb.netarchivesuite.solrwayback.service.dto.graph.*;
import dk.kb.netarchivesuite.solrwayback.service.exception.InternalServiceException;
import dk.kb.netarchivesuite.solrwayback.service.exception.InvalidArgumentServiceException;
import dk.kb.netarchivesuite.solrwayback.service.exception.NotFoundServiceException;
import dk.kb.netarchivesuite.solrwayback.service.exception.ServiceException;
import dk.kb.netarchivesuite.solrwayback.solr.SolrClient;

//No path except the context root+servletpath for the application. Example http://localhost:8080/officemood/services 

@Path("/")
public class SolrWaybackResource {

	public static void main(String[] args) throws Exception{
		
	    SimpleDateFormat waybackDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");		
		String waybackDate="19990914144635";
				
		
		  Date date = waybackDateFormat.parse(waybackDate);

				
     	DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"); //not thread safe, so create new        	        
        String solrDate = dateFormat.format(date)+"Z";
	System.out.println(solrDate);
	
	}
			
    private static final Logger log = LoggerFactory.getLogger(SolrWaybackResource.class);
   
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
       
    /*
    //TODO virker ikke. Skal være graph query
    @GET
    @Path("/images/search/location")
    @Produces(MediaType.APPLICATION_JSON +"; charset=UTF-8")
    public  ArrayList<ImageUrl> imagesLocatoinSearch(@QueryParam("query") String query) throws ServiceException {
        try {                                          
          ArrayList<ArcEntryDescriptor> img = Facade.findImagesWithLocation(query);
          return Facade.arcEntrys2Images(img);                                                            
        } catch (Exception e) {           
            throw handleServiceExceptions(e);
        }
    }
    */
    
    @GET
    @Path("/images/search/location")
    @Produces(MediaType.APPLICATION_JSON +"; charset=UTF-8")
    public  ArrayList<ImageUrl> imagesLocationSearch(@QueryParam("query") String query, @QueryParam("fq") String fq, @QueryParam("results") String results,@QueryParam("latitude") double latitude, @QueryParam("longitude") double longitude, @QueryParam("d") int d) throws ServiceException {
        try {                                          
          ArrayList<ImageUrl> images = Facade.imagesLocationSearch(query,fq, results, latitude, longitude, d);
          return images;                                                            
        } catch (Exception e) {           
            throw handleServiceExceptions(e);
        }
    }
    
    @GET
    @Path("solr/search")
    @Produces(MediaType.APPLICATION_JSON +"; charset=UTF-8")
    public String  solrSearch(@QueryParam("query") String query, @QueryParam("fq") String filterQuery ,  @QueryParam("revisits") boolean revisits , @QueryParam("start") int start) throws ServiceException {
        try {                    
          String res = Facade.solrSearch(query,filterQuery, revisits, start);          
          return res;
        } catch (Exception e) {
          log.error("error for search:"+query, e);
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
    
     if (source_file_path == null || offset == 0){
       log.error("arc_full and offset queryparams missing");
       throw new InvalidArgumentServiceException("arc_full and offset queryparams missing");
     }
                  
     try {                    
          return Facade.getImagesForHtmlPageNew(source_file_path, offset);  
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

      SimpleDateFormat waybackDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");            
      Date date = waybackDateFormat.parse(waybackDate);
                  
      DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"); //not thread safe, so create new                 
      String solrDate = dateFormat.format(date)+"Z";
    
      //log.info("solrDate="+solrDate +" , url="+url);
      IndexDoc doc = SolrClient.getInstance().findClosestHarvestTimeForUrl(url, solrDate);
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
            e.printStackTrace();
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
            e.printStackTrace();
            throw handleServiceExceptions(e);
        }
    }
    
    @GET
    @Path("/image")
    @Produces("image/jpeg")
    public Response getImage(@QueryParam("source_file_path") String source_file_path, @QueryParam("offset") long offset, @QueryParam("height") int height, @QueryParam("width") int width)
            throws ServiceException {
        try {
            log.debug("Getting image from source_file_path:" + source_file_path + " offset:" + offset + " targetWidth:" + width + " targetHeight:" + height);
                     
            ArcEntry arcEntry= Facade.getArcEntry(source_file_path, offset);
            
            BufferedImage image = ImageUtils.getImageFromBinary(arcEntry.getBinary());
            
            if (image== null){
                log.error("image is null, source_file_path:"+source_file_path +" offset:"+offset);
                throw new IllegalArgumentException("image is null, source_file_path:"+source_file_path +" offset:"+offset);                
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
            e.printStackTrace();
            throw handleServiceExceptions(e);
        }
    }

    @GET
    @Path("/downloadRaw")
    public Response downloadRaw(@QueryParam("source_file_path") String source_file_path, @QueryParam("offset") long offset) throws ServiceException {
        try {
               
            log.debug("Download from FilePath:" + source_file_path + " offset:" + offset);
            ArcEntry arcEntry= Facade.getArcEntry(source_file_path, offset);
                                    
            InputStream in = new ByteArrayInputStream(arcEntry.getBinary());
            ResponseBuilder response = Response.ok((Object) in).type(arcEntry.getContentType());
            if (arcEntry.getFileName() != null){
              response.header("Content-Disposition", "filename=\"" + arcEntry.getFileName() +"\"");
            }
            
            log.debug("Download from source_file_path:" + source_file_path + " offset:" + offset + " is mimetype:" + arcEntry.getContentType() + " and has filename:" + arcEntry.getFileName());
            return response.build();

        } catch (Exception e) {
            log.error("Error download from source_file_path:"+ source_file_path + " offset:" + offset,e);
        	e.printStackTrace();
            throw handleServiceExceptions(e);
        }

    }

  
    
    @GET
    @Path("/export/brief")    
    @Produces(MediaType.TEXT_PLAIN)
    public Response exportBrief(@QueryParam("query") String q, @QueryParam("fq") String fq) throws ServiceException {
        try {              
            log.debug("Export brief. query:"+q +" filterquery:"+fq);
            DateFormat formatOut= new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");                                                                              
            String dateStr = formatOut.format(new Date());                        
            InputStream is = Facade.exportBriefStreaming(q, fq);
            return Response.ok(is).header("Content-Disposition", "attachment; filename=\"solrwayback_"+dateStr+".csv\"").build();

        } catch (Exception e) {
            log.error("Error in export brief",e);
            e.printStackTrace();
            throw handleServiceExceptions(e);
        }
    }
        
    
    @GET
    @Path("/export/full")    
    public Response exportFull(@QueryParam("query") String q, @QueryParam("fq") String fq) throws ServiceException {
        try {               
            log.debug("Export full. query:"+q +" filterquery:"+fq);
            DateFormat formatOut= new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");                                                                                                                                                         
            String dateStr = formatOut.format(new Date());                        
            InputStream is = Facade.exportFullStreaming(q, fq);
            return Response.ok(is).header("Content-Disposition", "attachment; filename=\"solrwayback_"+dateStr+".csv\"").build();
            
        } catch (Exception e) {
            log.error("Error in export full",e);
            e.printStackTrace();
            throw handleServiceExceptions(e);
        }

    }
    

    /*
     *  This will be called from solrwayback page views, when resources can not be resolved (not harvested)  
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
            e.printStackTrace();
            throw handleServiceExceptions(e);
        }

    }
    
    
    /*
     * Example call:
     * wayback?waybackdata=19990914144635/http://209.130.118.14/novelle/novelle.asp?id=478&grp=3
     * Since the URL part is not url encoded we can not use a jersey queryparam for the string
     * The part after 'waybackdata=' is same syntax as the (archive.org) wayback machine. (not url encoded).
     * Also supports URL encoding of the parameters as fallback if above syntax does not validate   
     */
    
    @GET
    @Path("/wayback")
    public Response wayback(@Context UriInfo uriInfo) throws ServiceException {      
        //Get the full request url and find the waybackdata object
        try {    		
        String fullUrl = uriInfo.getRequestUri().toString();
        int dataStart=fullUrl.indexOf("/wayback?waybackdata=");
        if (dataStart <0){
          throw new InvalidArgumentServiceException("no waybackdata parameter in call. Syntax is: wayback?waybackdata={time}/{url}");
        }
        
        String waybackDataObject = fullUrl.substring(dataStart+21);
        log.info("Waybackdata object:"+waybackDataObject);
        
        int indexFirstSlash = waybackDataObject.indexOf("/");  
        if (indexFirstSlash == -1){ //Fallback, try URL decode
          waybackDataObject = java.net.URLDecoder.decode(waybackDataObject, "UTF-8");
          log.info("urldecoded wayback dataobject:"+waybackDataObject);
          indexFirstSlash = waybackDataObject.indexOf("/");          
        }
        
        
        String waybackDate = waybackDataObject.substring(0,indexFirstSlash);
        String url = waybackDataObject.substring(indexFirstSlash+1);

	    SimpleDateFormat waybackDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");	    	
	    Date date = waybackDateFormat.parse(waybackDate);
					
   	    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"); //not thread safe, so create new        	        
        String solrDate = dateFormat.format(date)+"Z";
	  
        //log.info("solrDate="+solrDate +" , url="+url);
        IndexDoc doc = SolrClient.getInstance().findClosestHarvestTimeForUrl(url, solrDate);
        if (doc == null){
         log.info("Url has never been harvested:"+url);
        	throw new IllegalArgumentException("Url has never been harvested:"+url);
        }
        //log.info("Found url with harvesttime:"+doc.getUrl() +" and arc:"+doc.getArc_full());        
        return view(doc.getSource_file_path() , doc.getOffset(),true);        
        	
    } catch (Exception e) {
        e.printStackTrace();
        throw handleServiceExceptions(e);
    }
}
    
    @GET
    @Path("/view")
    public Response view(@QueryParam("source_file_path") String source_file_path, @QueryParam("offset") long offset, @QueryParam("showToolbar") Boolean showToolbar) throws ServiceException {
        try {
               
         return viewImpl(source_file_path, offset,showToolbar);
    
    } catch (Exception e) {
        e.printStackTrace();
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
    	ArcEntry arcEntry= Facade.viewHtml(source_file_path, offset,showToolbar);
                                
        InputStream in = new ByteArrayInputStream(arcEntry.getBinary());
        ResponseBuilder response = Response.ok((Object) in).type(arcEntry.getContentType());                 
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
                    
        	IndexDoc indexDoc = SolrClient.getInstance().findClosestHarvestTimeForUrl(url, crawlDate);
        	if (indexDoc == null){
        		throw new NotFoundServiceException("Url has never been harvested:"+url);
        	}
        	
        	log.info("Closest harvest to: " +crawlDate +" is "+indexDoc.getCrawlDate());
        	return view(indexDoc.getSource_file_path(),indexDoc.getOffset(),showToolbar);

        } catch (Exception e) {
            e.printStackTrace();
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
            e.printStackTrace();
            throw handleServiceExceptions(e);
        }

    }
      
    private ServiceException handleServiceExceptions(Exception e) {
        if (e instanceof ServiceException) {
            log.info("Handling serviceException:" + e.getMessage());
            return (ServiceException) e; // Do nothing, exception already correct
        } else if (e instanceof IllegalArgumentException) {
            log.error("ServiceException(HTTP 400) in Service:", e.getMessage());
            return new InvalidArgumentServiceException(e.getMessage());
        } else {// SQL and other unforseen exceptions.... should not happen.
            log.error("ServiceException(HTTP 500) in Service:", e);
            return new InternalServiceException(e.getMessage());
        }
    }
}
