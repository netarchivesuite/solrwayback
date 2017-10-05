package dk.kb.netarchivesuite.solrwayback.facade;


import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.imageio.ImageIO;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import dk.kb.netarchivesuite.solrwayback.parsers.WaybackToolbarInjecter;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoaderWeb;
import dk.kb.netarchivesuite.solrwayback.service.dto.*;
import dk.kb.netarchivesuite.solrwayback.service.dto.graph.D3Graph;
import dk.kb.netarchivesuite.solrwayback.service.dto.graph.Link;
import dk.kb.netarchivesuite.solrwayback.service.dto.graph.Node;
import dk.kb.netarchivesuite.solrwayback.service.exception.InvalidArgumentServiceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

import dk.kb.netarchivesuite.solrwayback.concurrency.ImageSearchExecutor;
import dk.kb.netarchivesuite.solrwayback.parsers.HtmlParserUrlRewriter;
import dk.kb.netarchivesuite.solrwayback.parsers.FileParserFactory;
import dk.kb.netarchivesuite.solrwayback.solr.FacetCount;
import dk.kb.netarchivesuite.solrwayback.solr.SolrClient;

public class Facade {
    private static final Logger log = LoggerFactory.getLogger(Facade.class);

    public static SearchResult search(String searchText, String filterQuery) throws Exception {
        SearchResult result = SolrClient.getInstance().search(searchText, filterQuery);
        return result;
    }
    
    public static String solrSearch(String query, String filterQuery,boolean revisits, int start) throws Exception {
      return proxySolr(query, filterQuery , revisits, start);
  }
    

    public static ArrayList<ArcEntryDescriptor> findImages(String searchText) throws Exception {
        long start = System.currentTimeMillis();
        SearchResult result = SolrClient.getInstance().search(searchText, "content_type_norm:image OR content_type_norm:html", 500); //only search these two types
        
                
        //multithreaded call solr to find arc file and offset
        ArrayList<ArcEntryDescriptor> extractImages = ImageSearchExecutor.extractImages(result.getResults());
        return extractImages;      
    }
    
    
    public static BufferedImage getHtmlPagePreview(String source_file_path, long offset) throws Exception {
      
      String url = PropertiesLoader.WAYBACK_BASEURL+"services/view?source_file_path="+source_file_path +"&offset="+offset+"&showToolbar=false";            
      String filename = PropertiesLoader.PHANTOMJS_TEMP_IMAGEDIR+source_file_path+"@"+offset+".png";
      String scriptFile = PropertiesLoader.PHANTOMJS_RASTERIZE_FILE;
      
      log.info("generate temp preview file:"+filename);
     ProcessBuilder pb =
         new ProcessBuilder("phantomjs", scriptFile,url,filename,"1280px*1024px");
         log.info("phantomjs"+" "+scriptFile +" "+"\""+url+"\""+" "+filename +"\"1280px*1024px\"");
     
    
     Process start = pb.start();
      InputStream is = start.getInputStream();
      String conlog= getStringFromInputStream(is);
      log.info("conlog:"+conlog);
      start.waitFor(); //Wait until completed. 5 seconds timeout in script
   
      
     BufferedImage image =  ImageIO.read(new File(filename));
     return image;
    }

    public static HarvestDates getHarvestTimesForUrl(String url) throws Exception {
      log.info("getting harvesttimes for url:"+url);
      HarvestDates datesVO = new HarvestDates();
      datesVO.setUrl(url);
      ArrayList<Date> dates = SolrClient.getInstance().getHarvestTimesForUrl(url);
      
      ArrayList<Long> crawltimes= new ArrayList<Long>(); // only YYYYMMDD part of day
      
      for (Date d : dates ){
        crawltimes.add(d.getTime());
        
      }
      datesVO.setDates(crawltimes);    
      Collections.sort(crawltimes);
      
      datesVO.setNumberOfHarvests(crawltimes.size());
      return  datesVO;      
    }
    
    public static ArrayList<PagePreview> getPagePreviewsForUrl(String url) throws Exception {
      log.info("getting pagePreviews for url:"+url);
  
       ArrayList<IndexDoc> indexDocs = SolrClient.getInstance().getHarvestPreviewsForUrl(url); // Only contains the required fields for this method
       //Convert to PagePreview      
       ArrayList<PagePreview> previews =  new ArrayList<PagePreview>();
       
       for (IndexDoc doc : indexDocs){
         PagePreview pp = new PagePreview();
         pp.setCrawlDate(doc.getCrawlDateLong());
         String source_file_path=doc.getSource_file_path();
         long offset = doc.getOffset();         
         String previewUrl = PropertiesLoader.WAYBACK_BASEURL+"services/image/pagepreview?source_file_path="+source_file_path +"&offset="+offset+"&showToolbar=false"; 
         String solrWaybackUrl = PropertiesLoader.WAYBACK_BASEURL+"services/view?source_file_path="+source_file_path +"&offset="+offset;
         pp.setPagePreviewUrl(previewUrl);
         pp.setSolrWaybackUrl(solrWaybackUrl);                
         previews.add(pp);
       }
              
       return previews;
            
    }
     
    
    public static ArrayList<ImageUrl> getImagesForHtmlPageNew(String source_file_path,long offset) throws Exception {            
      ArrayList<ArcEntryDescriptor> arcs = getImagesForHtmlPageNewThreaded(source_file_path,offset);       
      return arcEntrys2Images(arcs);
      
    }
    

    /*
     * Find images on a HTML page.
     * 1) Find the doc in solr from source_file_path and offset. (fast)
     * 2) Get image links field
     * 3) For each images try to find that url_norm in solr with harvest time closest to the harvesttime for the HTML page. 
     */
    public static ArrayList<ArcEntryDescriptor>  getImagesForHtmlPageNewThreaded(String source_file_path,long offset) throws Exception {
      
  
      IndexDoc arcEntry = SolrClient.getInstance().getArcEntry(source_file_path, offset);
      ArrayList<String> imageLinks = arcEntry.getImageUrls();          
      if (imageLinks.size() == 0){
        return  new ArrayList<ArcEntryDescriptor> ();
      }
      StringBuilder query = new StringBuilder();
      query.append("(");
      for (String imageUrl : imageLinks ){         
        //fix https!
        String fixedUrl = imageUrl;
        if (imageUrl.startsWith("https:")){
          fixedUrl = "http:"+imageUrl.substring(6); // because image_links are not normlized as url_norm
        }                       
        query.append(" url_norm:\""+fixedUrl+"\" OR");            
      }
      query.append(" url_norm:none)"); //just close last OR
      String queryStr= query.toString();
      ArrayList<ArcEntryDescriptor> imagesFromHtmlPage = SolrClient.getInstance().findImagesForTimestamp(queryStr, arcEntry.getCrawlDate());
                      
       return imagesFromHtmlPage;                
    }
    
         
    public static String getEncoding(String source_file_path,String offset) throws Exception{
    	            	    
    	SearchResult search = SolrClient.getInstance().search("source_file_path:\""+source_file_path +"\" AND source_file_offset:"+offset, 1);
        if (search.getNumberOfResults() ==0){
          log.warn("No content encoding found for:"+source_file_path +" and offset:"+offset);
          return "UTF-8";         
        }
        else{
          String encoding = search.getResults().get(0).getContentEncoding();                    
          return encoding; //Can still be null. 
        }
    }
    
    public static ArcEntry getArcEntry(String source_file_path, long offset) throws Exception{         
        return FileParserFactory.getArcEntry(source_file_path, offset);        
    }
    
    
    
    public static String exportBrief(String q, String fg) throws Exception{         
      return SolrClient.getInstance().exportBrief(q, fg, 100000);         
    }
  
    
    public static String exportFull(String q, String fg) throws Exception{         
      return SolrClient.getInstance().exportFull(q, fg, 1000000);         
    }
    
    
    
    public static D3Graph waybackgraph(String domain, int facetLimit, boolean ingoing , String dateStart, String dateEnd) throws Exception{
      
      //Default dates
      Date start = new Date(System.currentTimeMillis()-25L*365*86400*1000L); // 25 years ago
      Date end = new Date();
      
      if (dateStart != null){
        start = new Date(Long.valueOf(dateStart));
      }
      if (dateEnd != null){
        end = new Date(Long.valueOf(dateEnd));
      }
      
      List<FacetCount>  facets = SolrClient.getInstance().getDomainFacets(domain,facetLimit, ingoing, start, end);
      log.info("Creating graph for domain:"+domain +" ingoing:"+ingoing +" and facetLimit:"+facetLimit);
      
      HashMap<String, List<FacetCount>> domainFacetMap = new HashMap<String, List<FacetCount>>();
      //Also find facet for all facets from first call.
      domainFacetMap.put(domain, facets); //add this center domain
      
      //Do all queries
      for (FacetCount f : facets){
        String facetDomain =f.getValue();                  
        List<FacetCount>  fc = SolrClient.getInstance().getDomainFacets(facetDomain,facetLimit, ingoing,start,end);
        domainFacetMap.put(f.getValue(),fc);        
      }
      
      //Just build a HashSet with all domains
      HashSet<String> allDomains = new HashSet<String>(); //Same domain can be from different queries, but must be same node.
      for (String current : domainFacetMap.keySet()){
        allDomains.add(current);
        List<FacetCount> list = domainFacetMap.get(current);
          for (FacetCount f : list){
            allDomains.add(f.getValue());
          }                
      }
      log.info("Total number of nodes:"+allDomains.size());
                  

      
      //First map all urls to a number due to the graph id naming contraints.
      HashMap<String, Integer> domainNumberMap = new HashMap<String, Integer>();
      int number=0; //start number
            
      for (String d: allDomains){
        domainNumberMap.put(d, number++);
      }      
      
      //Notice we add same egde multiple times, but d3 has no problem with this.
      
      D3Graph g = new D3Graph();
      List<Node> nodes = new ArrayList<Node>();
      g.setNodes(nodes);
      List<Link> links = new ArrayList<Link>();
      g.setLinks(links);
      
      
      //All all nodes
      for (String d :allDomains){
        if (d.equals(domain)){ //Center node
          nodes.add(new Node(d,domainNumberMap.get(d),16,"red")); //size 16 and red
        }else{
          nodes.add(new Node(d,domainNumberMap.get(d),5)); //black default color          
        }
          
      }
      
      
      //All all edges (links)
      for (String c : domainFacetMap.keySet()){
        List<FacetCount> list = domainFacetMap.get(c);
 
        for (FacetCount f: list){
          if (ingoing){
            links.add(new Link(domainNumberMap.get(f.getValue()),domainNumberMap.get(c),5)); //Link from input url to all facets
          }
          else{
            links.add(new Link(domainNumberMap.get(c),domainNumberMap.get(f.getValue()),5)); //Link from input url to all facets
            
          }
        }
        
      }
             
      
      return  g;
    }
    
    
    public static String generatePid(String source_file_path, long offset) throws Exception{      
      ArcEntry arc=FileParserFactory.getArcEntry(source_file_path, offset);           
      arc.setContentEncoding(Facade.getEncoding(source_file_path, ""+offset));
      StringBuffer parts = new StringBuffer();
      //the original page
      parts.append("<part>\n");
      parts.append("pwid:netarkivet.dk:"+arc.getCrawlDate()+":part:"+arc.getUrl() +"\n");
      parts.append("</part>\n");      
      String xmlIncludes = HtmlParserUrlRewriter.generatePwid(arc);//all sub elements            
      parts.append(xmlIncludes);
      return  parts.toString();
    }
    
    public static ArcEntry viewHtml(String source_file_s, long offset, Boolean showToolbar) throws Exception{         
    	
    	ArcEntry arc=FileParserFactory.getArcEntry(source_file_s, offset);    	 

    	arc.setContentEncoding(Facade.getEncoding(source_file_s, ""+offset));
    	if (("text/html".equals(arc.getContentType()))){
    		long start = System.currentTimeMillis();
        	log.debug(" Generate webpage from FilePath:" + source_file_s + " offset:" + offset);
        	String textReplaced = HtmlParserUrlRewriter.replaceLinks(arc);    	 
        	
        	//Inject tooolbar
        	if (showToolbar!=Boolean.FALSE ){ //If true or null. 
        	  textReplaced = WaybackToolbarInjecter.injectWaybacktoolBar(source_file_s,offset,textReplaced);
        	}
        	
        	arc.setBinary(textReplaced.getBytes(arc.getContentEncoding()));    	
            log.info("Generating webpage total processing:"+(System.currentTimeMillis()-start));
        	return arc;
    		 
        }else if (("text/css".equals(arc.getContentType()))){ 
    		long start = System.currentTimeMillis();
        	log.debug(" Generate css from FilePath:" + source_file_s + " offset:" + offset);
        	String textReplaced = HtmlParserUrlRewriter.replaceLinksCss(arc)    	 ;        
        	
        	arc.setBinary(textReplaced.getBytes(arc.getContentEncoding()));    	
            log.info("Generating css total processing:"+(System.currentTimeMillis()-start));
        	return arc;
        	
        }

		log.info("skipping html url rewrite for contentype:"+arc.getContentType());
    	return arc; //dont parse
            
    }
    
    //For fronted
    public static HashMap<String,String> getPropertiesWeb() throws Exception{         
        HashMap<String,String> props = new HashMap<String,String>();
        props.put(PropertiesLoaderWeb.WAYBACK_SERVER_PROPERTY,PropertiesLoaderWeb.WAYBACK_SERVER);
        return props;
    }
    
    
    
    public static String proxySolr( String query, String fq, boolean revisits, Integer start) throws Exception{                    
      
      String startStr ="0";
      if (start != null){
        startStr=start.toString();
      }

      log.info("query "+query +" revisits:"+revisits);
      String solrUrl =PropertiesLoader.SOLR_SERVER;  
      ClientConfig config = new DefaultClientConfig();
      Client client = Client.create(config);
      WebResource service = client.resource(UriBuilder.fromUri(solrUrl).build());
      WebResource queryWs= service.path("select")                                    
                                  .queryParam("rows", "20") //Hardcoded pt.
                                  .queryParam("start", startStr)
                                  .queryParam("q", query) 
                                  .queryParam("wt", "json")
                                  .queryParam("q.op", "AND")
                                  .queryParam("indent", "true")                      
                                  .queryParam("facet", "true")
                                  .queryParam("facet.field", "domain")
                                  .queryParam("facet.field", "content_type_norm")
                                  .queryParam("facet.field", "crawl_year")                           
                                  .queryParam("f.crawl_year.facet.sort","index")
                                  .queryParam("facet.field", "public_suffix");
               
      if ( fq != null && fq.length() > 0){
        queryWs = queryWs.queryParam("fq",fq);                        
       }
      
      if (!revisits){
        queryWs = queryWs.queryParam("fq", "record_type:response");
      }
                 
      ClientResponse response = queryWs.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
      String responseStr= response.getEntity(String.class);

      log.info(responseStr.substring(0, Math.min(500, responseStr.length()-1)));
      
      
      return responseStr;
      
  }
    
 // convert InputStream to String
    private static String getStringFromInputStream(InputStream is) {

        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();

        String line;
        try {

            br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return sb.toString();

    }
    
   public static  ArrayList<ImageUrl> arcEntrys2Images(ArrayList<ArcEntryDescriptor> arcs){
      ArrayList<ImageUrl> imageUrls = new ArrayList<ImageUrl>();
      for (ArcEntryDescriptor entry : arcs){                          
        ImageUrl imageUrl = new ImageUrl();
        String imageLink = PropertiesLoader.WAYBACK_BASEURL+"services/image?source_file_path="+entry.getSource_file_path()+"&offset="+entry.getOffset();
        String downloadLink = PropertiesLoader.WAYBACK_BASEURL+"services/downloadRaw?source_file_path="+entry.getSource_file_path()+"&offset="+entry.getOffset();
        imageUrl.setImageUrl(imageLink);
        imageUrl.setDownloadUrl(downloadLink);             
        imageUrl.setHash(entry.getHash());
        imageUrl.setUrlNorm(entry.getUrl_norm());
        imageUrls.add(imageUrl);         
      }
      return imageUrls;                 
      
    }
    
}
