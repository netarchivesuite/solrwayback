package dk.kb.netarchivesuite.solrwayback.service;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.imageio.ImageIO;
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
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

import dk.kb.netarchivesuite.solrwayback.encoders.Sha1Hash;
import dk.kb.netarchivesuite.solrwayback.facade.Facade;
import dk.kb.netarchivesuite.solrwayback.image.ImageUtils;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntryDescriptor;
import dk.kb.netarchivesuite.solrwayback.service.dto.HarvestDates;
import dk.kb.netarchivesuite.solrwayback.service.dto.ImageUrl;
import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDoc;
import dk.kb.netarchivesuite.solrwayback.service.dto.PagePreview;
import dk.kb.netarchivesuite.solrwayback.service.dto.SearchResult;
import dk.kb.netarchivesuite.solrwayback.service.dto.WeightedArcEntryDescriptor;
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
          ArrayList<ImageUrl> imageUrls = new ArrayList<ImageUrl>();   
          
          ArrayList<? extends ArcEntryDescriptor> img = Facade.findImages(query);
           
          for (ArcEntryDescriptor entry : img){
            ImageUrl imageUrl = new ImageUrl();
            String imageLink = PropertiesLoader.WAYBACK_BASEURL+"services/image?arcFilePath="+entry.getArcFull()+"&offset="+entry.getOffset();
            String downloadLink = PropertiesLoader.WAYBACK_BASEURL+"services/downloadRaw?arcFilePath="+entry.getArcFull()+"&offset="+entry.getOffset();
            imageUrl.setImageUrl(imageLink);
            imageUrl.setDownloadUrl(downloadLink);             
            imageUrl.setHash(entry.getHash());
            imageUrls.add(imageUrl);            
          }
          
          return  imageUrls;
        
                      
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
    public ArrayList<ImageUrl> imagesForPage(@QueryParam("arc_full") String arc_full, @QueryParam("offset") long offset  , @QueryParam("test") boolean test) throws ServiceException {
     log.info("arc_full:"+arc_full);
     log.info("offset:"+offset);
      if (test){
        return imagesForPageTest();
      }
     if (arc_full == null || offset == 0){
       log.error("arc_full and offset queryparams missing");
       throw new InvalidArgumentServiceException("arc_full and offset queryparams missing");
     }
      
      try {                    
          IndexDoc doc = SolrClient.getInstance().getArcEntry(arc_full, offset);
                                
           ArrayList<ImageUrl> imageUrls = new ArrayList<ImageUrl>();           
           ArrayList<WeightedArcEntryDescriptor> imagesFromHtmlPage = Facade.getImagesFromHtmlPage(doc);
           
           for (WeightedArcEntryDescriptor entry : imagesFromHtmlPage){                          
             ImageUrl imageUrl = new ImageUrl();
             String imageLink = PropertiesLoader.WAYBACK_BASEURL+"services/image?arcFilePath="+entry.getArcFull()+"&offset="+entry.getOffset();
             String downloadLink = PropertiesLoader.WAYBACK_BASEURL+"services/downloadRaw?arcFilePath="+entry.getArcFull()+"&offset="+entry.getOffset();
             imageUrl.setImageUrl(imageLink);
             imageUrl.setDownloadUrl(downloadLink);             
             imageUrl.setHash(entry.getHash());
             imageUrls.add(imageUrl);
           }
           return imageUrls;
                      
        } catch (Exception e) {           
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
      
      String arcFilePath = doc.getArc_full();
      long offset = doc.getOffset();
      
      BufferedImage image = Facade.getHtmlPagePreview(arcFilePath, offset);
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
    public Response getHtmlPagePreview(@QueryParam("arcFilePath") String arcFilePath, @QueryParam("offset") long offset)
            throws ServiceException {
        try {
            log.debug("Getting thumbnail html image from ArcfilePath:" + arcFilePath + " offset:" + offset);
            BufferedImage image = Facade.getHtmlPagePreview(arcFilePath, offset);          
            return Response.ok(image).build();                       
        } catch (Exception e) {
            log.error("error thumbnail html image:"+arcFilePath +" offset:"+offset);  
            e.printStackTrace();
            throw handleServiceExceptions(e);
        }
    }
    
    @GET
    @Path("/image")
    @Produces("image/jpeg")
    public Response getImage(@QueryParam("arcFilePath") String arcFilePath, @QueryParam("offset") long offset, @QueryParam("height") int height, @QueryParam("width") int width)
            throws ServiceException {
        try {
            log.debug("Getting image from ArcfilePath:" + arcFilePath + " offset:" + offset + " targetWidth:" + width + " targetHeight:" + height);
                     
            ArcEntry arcEntry= Facade.getArcEntry(arcFilePath, offset);
            
            BufferedImage image = ImageUtils.getImageFromBinary(arcEntry.getBinary());
            
            if (image== null){
                log.error("image is null, arc:"+arcFilePath +" offset:"+offset);
                throw new IllegalArgumentException("image is null, arc:"+arcFilePath +" offset:"+offset);                
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
            log.error("error getImage:"+arcFilePath +" offset:"+offset +" height:"+height +" width:"+width); //Java can not read all images. 
            e.printStackTrace();
            throw handleServiceExceptions(e);
        }
    }

    @GET
    @Path("/downloadRaw")
    public Response downloadRaw(@QueryParam("arcFilePath") String arcFilePath, @QueryParam("offset") long offset) throws ServiceException {
        try {
               
            log.debug("Download from FilePath:" + arcFilePath + " offset:" + offset);
            ArcEntry arcEntry= Facade.getArcEntry(arcFilePath, offset);
                                    
            InputStream in = new ByteArrayInputStream(arcEntry.getBinary());
            ResponseBuilder response = Response.ok((Object) in).type(arcEntry.getContentType());
            if (arcEntry.getFileName() != null){
              response.header("Content-Disposition", "filename=\"" + arcEntry.getFileName() +"\"");
            }
            
            log.debug("Download from ArcfilePath:" + arcFilePath + " offset:" + offset + " is mimetype:" + arcEntry.getContentType() + " and has filename:" + arcEntry.getFileName());
            return response.build();

        } catch (Exception e) {
            log.error("Error download from arcfile:"+ arcFilePath + " offset:" + offset,e);
        	e.printStackTrace();
            throw handleServiceExceptions(e);
        }

    }

    @GET
    @Path("/export/brief")    
    public Response exportBrief(@QueryParam("query") String q, @QueryParam("fq") String fq) throws ServiceException {
        try {
               
            log.debug("Export brief. query:"+q +" filterquery:"+fq);
            DateFormat formatOut= new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");                                                                  
            
            String dateStr = formatOut.format(new Date());
            ResponseBuilder response = Response.ok(Facade.exportBrief(q, fq)).type(MediaType.TEXT_PLAIN);
            response.header("Content-Disposition", "attachment; filename=\"solrwayback_"+dateStr+".csv\"");
                        
            log.debug("Export completed");
            return response.build();

        } catch (Exception e) {
            log.error("Error in exportbrief",e);
            e.printStackTrace();
            throw handleServiceExceptions(e);
        }

    }
    
    
    @GET
    @Path("/export/full")    
    public Response exportFull(@QueryParam("query") String q, @QueryParam("fq") String fq) throws ServiceException {
        try {
               
            log.debug("Export brief. query:"+q +" filterquery:"+fq);
            DateFormat formatOut= new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");                                                                  
            
            String dateStr = formatOut.format(new Date());
            ResponseBuilder response = Response.ok(Facade.exportFull(q, fq)).type(MediaType.TEXT_PLAIN);
            response.header("Content-Disposition", "attachment; filename=\"solrwayback_"+dateStr+".csv\"");
                        
            log.debug("Export completed");
            return response.build();

        } catch (Exception e) {
            log.error("Error in exportbrief",e);
            e.printStackTrace();
            throw handleServiceExceptions(e);
        }

    }
    

    
    @GET
    @Path("/getContentType")
    public String getContentType(@QueryParam("arcFilePath") String arcFilePath, @QueryParam("offset") long offset) throws ServiceException {
        try {
               
            return Facade.getEncoding(arcFilePath, ""+offset);       

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
        return view(doc.getArc_full() , doc.getOffset(),true);        
        	
    } catch (Exception e) {
        e.printStackTrace();
        throw handleServiceExceptions(e);
    }
}
    
    @GET
    @Path("/view")
    public Response view(@QueryParam("arcFilePath") String arcFilePath, @QueryParam("offset") long offset, @QueryParam("showToolbar") Boolean showToolbar) throws ServiceException {
        try {
               
         return viewImpl(arcFilePath, offset,showToolbar);
    
    } catch (Exception e) {
        e.printStackTrace();
        throw handleServiceExceptions(e);
    }
}
    
    
    @GET
    @Path("/generatepwid")
    public String generatePid(@QueryParam("arcFilePath") String arcFilePath, @QueryParam("offset") long offset) throws Exception {
      log.debug("generatepwid:" + arcFilePath + " offset:" + offset);
      String xml =Facade.generatePid(arcFilePath, offset);
                                                                  
      return xml;
    
  }
    
    private Response viewImpl(String arcFilePath, long offset,Boolean showToolbar) throws Exception{    	    	
        log.debug("View from FilePath:" + arcFilePath + " offset:" + offset);
    	ArcEntry arcEntry= Facade.viewHtml(arcFilePath, offset,showToolbar);
                                
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
        	return view(indexDoc.getArc_full(),indexDoc.getOffset(),showToolbar);

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
                              
        //TODO use invoing, facetlimit. with defaults
          return Facade.waybackgraph(domain, fLimit,in,dateStart,dateEnd);        
                                     
        } catch (Exception e) {
            e.printStackTrace();
            throw handleServiceExceptions(e);
        }

    }
    
    
    public ArrayList<ImageUrl> imagesForPageTest(){
      ArrayList<ImageUrl> tests = new ArrayList<ImageUrl>();
      
      ImageUrl im1 = new ImageUrl();
      im1.setDownloadUrl("http://belinda:9721/solrwayback/services/downloadRaw?arcFilePath=/netarkiv/0123/filedir/181110-186-20130604210516-00084-sb-prod-har-001.statsbiblioteket.dk.warc&offset=96865967");
      im1.setImageUrl("http://belinda:9721/solrwayback/services/image?arcFilePath=/netarkiv/0123/filedir/181110-186-20130604210516-00084-sb-prod-har-001.statsbiblioteket.dk.warc&offset=96865967");      
      im1.setHash("sha1:OH6RZFQRZWC2AF6U474C4JRC7SLGKWVX");
      tests.add(im1);
      
      ImageUrl im2 = new ImageUrl();
      im2.setDownloadUrl("http://belinda:9721/solrwayback/services/downloadRaw?arcFilePath=/netarkiv/0105/filedir/252063-244-20160219075443-00026-sb-prod-har-003.statsbiblioteket.dk.warc&offset=715018159");
      im2.setImageUrl("http://belinda:9721/solrwayback/services/image?arcFilePath=/netarkiv/0105/filedir/252063-244-20160219075443-00026-sb-prod-har-003.statsbiblioteket.dk.warc&offset=715018159");      
      im2.setHash("sha1:GN6XGABFJ7VULSXDTBURGK6YHIY7NGO6");
      tests.add(im2);
   
      ImageUrl im3 = new ImageUrl();
      im3.setDownloadUrl("http://belinda:9721/solrwayback/services/downloadRaw?arcFilePath=/netarkiv/0228/filedir/167339-178-20121204215446-00211-kb-prod-har-004.kb.dk.arc&offset=95722660");
      im3.setImageUrl("http://belinda:9721/solrwayback/services/image?arcFilePath=/netarkiv/0228/filedir/167339-178-20121204215446-00211-kb-prod-har-004.kb.dk.arc&offset=95722660");      
      im3.setHash("sha1:CVNVM6MUZE54KKU3SOXG6SVDT632SD2E");
      tests.add(im3);
      
      return tests;
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
