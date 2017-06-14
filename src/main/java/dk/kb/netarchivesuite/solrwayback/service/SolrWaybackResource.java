package dk.kb.netarchivesuite.solrwayback.service;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.netarchivesuite.solrwayback.facade.Facade;
import dk.kb.netarchivesuite.solrwayback.image.ImageUtils;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntryDescriptor;
import dk.kb.netarchivesuite.solrwayback.service.dto.HarvestDates;
import dk.kb.netarchivesuite.solrwayback.service.dto.ImageUrl;
import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDoc;
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
    @Path("/findimages")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    public ArrayList<ArcEntryDescriptor> findImages(@QueryParam("searchText") String searchText) throws ServiceException {
        try {                    
            ArrayList<? extends ArcEntryDescriptor> img = Facade.findImages(searchText);
            return  (ArrayList<ArcEntryDescriptor>) img; 
        } catch (Exception e) {           
            throw handleServiceExceptions(e);
        }
    }
    
    @GET
    @Path("/search")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    public SearchResult search(@QueryParam("searchText") String searchText, @QueryParam("filterQuery") String filterQuery) throws ServiceException {
        try {                    
            return Facade.search(searchText,filterQuery);
        } catch (Exception e) {           
            throw handleServiceExceptions(e);
        }
    }
    
    
    @GET
    @Path("solr/search")
    @Produces(MediaType.APPLICATION_JSON +"; charset=UTF-8")
    public String  solrSearch(@QueryParam("query") String query, @QueryParam("fq") String filterQuery , @QueryParam("start") int start) throws ServiceException {
        try {                    
            return Facade.solrSearch(query,filterQuery,start);
        } catch (Exception e) {           
            throw handleServiceExceptions(e);
        }
    }
    
    @GET
    @Path("images/htmlpage")
    @Produces(MediaType.APPLICATION_JSON +"; charset=UTF-8")
    public ArrayList<ImageUrl> imagesForPage(@QueryParam("source_file_s") String source_file_s) throws ServiceException {
        try {                    
          IndexDoc doc = SolrClient.getInstance().getArcEntry(source_file_s);
                                
           ArrayList<ImageUrl> imageUrls = new ArrayList<ImageUrl>();           
           ArrayList<WeightedArcEntryDescriptor> imagesFromHtmlPage = Facade.getImagesFromHtmlPage(doc);
           
           for (WeightedArcEntryDescriptor entry : imagesFromHtmlPage){                          
             ImageUrl imageUrl = new ImageUrl();
             String imageLink = PropertiesLoader.WAYBACK_BASEURL+"services/image?arcFilePath="+entry.getArcFull()+"&offset="+entry.getOffset();
             String downloadLink = PropertiesLoader.WAYBACK_BASEURL+"services/downloadRaw?arcFilePath="+entry.getArcFull()+"&offset="+entry.getOffset();
             imageUrl.setImageUrl(imageLink);
             imageUrl.setDownloadUrl(downloadLink);             
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
    public HarvestDates search(@QueryParam("url") String url) throws ServiceException {
        try {                    
            return Facade.getHarvestTimesForUrl(url);
        } catch (Exception e) {           
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
    @Path("/getContentType")
    public String getContentType(@QueryParam("arcFilePath") String arcFilePath, @QueryParam("offset") long offset) throws ServiceException {
        try {
               
            return Facade.getEncoding(arcFilePath, ""+offset);       

        } catch (Exception e) {
            e.printStackTrace();
            throw handleServiceExceptions(e);
        }

    }
    
    
    @GET
    @Path("/wayback/")
    public Response wayback(@QueryParam("waybackdata") String data) throws ServiceException {
    
    	// data is in format : 19990914144635/http://209.130.118.14/novelle/novelle.asp?id=478&grp=3
    	//First is time, second is url
    	try {    		
        log.info("Wayback emulator called with:"+data);
        int indexFirstSlash = data.indexOf("/");
        String waybackDate = data.substring(0,indexFirstSlash);
        String url = data.substring(indexFirstSlash+1);

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
