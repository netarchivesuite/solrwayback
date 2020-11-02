package dk.kb.netarchivesuite.solrwayback.facade;


import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.IDN;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.httpclient.ChunkedInputStream;
import org.apache.commons.io.IOUtils;
import org.brotli.dec.BrotliInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import dk.kb.netarchivesuite.solrwayback.parsers.*;
import dk.kb.netarchivesuite.solrwayback.playback.CssPlayback;
import dk.kb.netarchivesuite.solrwayback.playback.HtmlPlayback;
import dk.kb.netarchivesuite.solrwayback.playback.JodelPlayback;
import dk.kb.netarchivesuite.solrwayback.playback.TwitterPlayback;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoaderWeb;
import dk.kb.netarchivesuite.solrwayback.service.dto.*;
import dk.kb.netarchivesuite.solrwayback.service.dto.graph.D3Graph;
import dk.kb.netarchivesuite.solrwayback.service.dto.graph.Link;
import dk.kb.netarchivesuite.solrwayback.service.dto.graph.Node;
import dk.kb.netarchivesuite.solrwayback.service.dto.smurf.SmurfYearBuckets;
import dk.kb.netarchivesuite.solrwayback.service.dto.statistics.DomainYearStatistics;
import dk.kb.netarchivesuite.solrwayback.service.exception.InvalidArgumentServiceException;
import dk.kb.netarchivesuite.solrwayback.service.exception.NotFoundServiceException;
import dk.kb.netarchivesuite.solrwayback.smurf.NetarchiveYearCountCache;
import dk.kb.netarchivesuite.solrwayback.smurf.SmurfUtil;
import dk.kb.netarchivesuite.solrwayback.solr.*;
import dk.kb.netarchivesuite.solrwayback.util.FileUtil;
import dk.kb.netarchivesuite.solrwayback.util.UrlUtils;
import dk.kb.netarchivesuite.solrwayback.wordcloud.WordCloudImageGenerator;
import dk.kb.netarchivesuite.solrwayback.concurrency.ImageSearchExecutor;
import dk.kb.netarchivesuite.solrwayback.export.StreamingSolrExportBufferedInputStream;
import dk.kb.netarchivesuite.solrwayback.export.StreamingSolrWarcExportBufferedInputStream;


public class Facade {
    private static final Logger log = LoggerFactory.getLogger(Facade.class);
        
    
    public static SearchResult search(String searchText, String filterQuery) throws Exception {
        SearchResult result = NetarchiveSolrClient.getInstance().search(searchText, filterQuery);
        return result;
    }
    
    public static String solrSearchNoFacets(String query, List<String>  filterQueries,boolean grouping, boolean revisits, int start) throws Exception {
    if (start >= 1001) {
      throw new InvalidArgumentServiceException("Pagination (start) must be less than 1001");
    }
      return proxySolrNoFacets(query, filterQueries , grouping, revisits, start);
  }
   
    public static String solrSearchFacetsOnly(String query, List<String> filterQueries, boolean revisits) throws Exception {      
      return proxySolrOnlyFacets(query, filterQueries, revisits);
    }
    
    /*
     * returns json
     */
    public static String solrIdLookup(String id) throws Exception {
      return NetarchiveSolrClient.getInstance().idLookupResponse(id);
   }
    
public static IndexDoc findExactMatchPWID(String url, String utc) throws Exception {  
     IndexDoc doc = NetarchiveSolrClient.getInstance().findExactMatchPWID(url, utc);
     return doc;    
}


public static String getAboutText() throws Exception {  
    String aboutFile = PropertiesLoaderWeb.ABOUT_TEXT_FILE;    
    String aboutText=FileUtil.fetchUTF8(aboutFile);
    return aboutText;
}

public static String getSearchHelpText() throws Exception {  
    String searchHelpFile = PropertiesLoaderWeb.SEARCH_HELP_TEXT_FILE;    
    String searchHelpText=FileUtil.fetchUTF8(searchHelpFile);
    return searchHelpText;    
}



public static String generateDomainResultGraph(@QueryParam("q") String q, @QueryParam("fq") List<String> fq ) throws Exception {   
     String jsonStr  = NetarchiveSolrClient.getInstance().domainStatisticsForQuery(q, fq);              
     HashMap<Integer, List<FacetCount>> domainStatisticsForQuery =  DomainStatisticsForDomainParser.parseDomainStatisticsJson(jsonStr);     
     String matrix = DomainStatisticsForDomainParser.generateDomainQueryStatisticsString(domainStatisticsForQuery);     
     return matrix;              
}
    
    public static ArrayList<ArcEntryDescriptor> findImages(String searchText) throws Exception {        
        SearchResult result = NetarchiveSolrClient.getInstance().search(searchText, "content_type_norm:image OR content_type_norm:html", 500); //only search these two types                        
        //multithreaded call solr to find arc file and offset
        ArrayList<ArcEntryDescriptor> extractImages = ImageSearchExecutor.extractImages(result.getResults(), false);
        return extractImages;      
    }
    
    public static ArrayList<DomainYearStatistics> statisticsDomain(String domain) throws Exception{
      ArrayList<DomainYearStatistics> stats = new ArrayList<DomainYearStatistics>();
      int year = Calendar.getInstance().get(Calendar.YEAR);
      for (int i = 1995;i<=year;i++){
        DomainYearStatistics yearStat=  NetarchiveSolrClient.getInstance().domainStatistics(domain, i);
        stats.add(yearStat);         
      }                  
      return stats;            
    }
        
    
    
        
    public static  ArrayList<ImageUrl> imagesLocationSearch(String searchText,String filter, String results, double latitude, double longitude, double radius, String sort) throws Exception {
      int resultInt=500;
      if (results != null){
        resultInt=Integer.parseInt(results);        
      }
      ArrayList<IndexDoc> docs = NetarchiveSolrClient.getInstance().imagesLocationSearchWithSort(searchText,filter, resultInt, latitude, longitude, radius, sort); //only search these two types            
      return indexDoc2Images(docs);            
    }
    
    public static SmurfYearBuckets generateNetarchiveSmurfData(String tag, String filterQuery, int startyear) throws Exception{

      if (tag == null || tag.length() ==0){
        throw new InvalidArgumentServiceException("tag must not be empty");
      }
      
      log.info("netarchive smurf tag query:"+tag +" for startyear:"+startyear);
      try{

        HashMap<Integer, Long> yearFacetsQuery = NetarchiveSolrClient.getInstance().getYearHtmlFacets(tag);
        HashMap<Integer, Long> yearFacetsAll = NetarchiveYearCountCache.getYearFacetsAllQuery();

        SmurfYearBuckets buckets = SmurfUtil.generateYearBuckets(yearFacetsQuery, yearFacetsAll, startyear, null);      
        return buckets;
        
      }
      catch(Exception e){
        e.printStackTrace();
        throw e;
      }

    }

    public static SmurfYearBuckets generateNetarchiveTextSmurfData(String q, String filterQuery, int startyear) throws Exception{

      //No Little Toke Tabels tricks allowed
      String qReplaced = q.replace("\"", "");
      qReplaced = qReplaced.replace(":", "");
      if (q == null || q.length() ==0){
        throw new InvalidArgumentServiceException("tag must not be empty");
      }
      
      log.info("netarchive content smurf query:"+ qReplaced +" for startyear:"+startyear);
      try{

        HashMap<Integer, Long> yearContentQuery = NetarchiveSolrClient.getInstance().getYearTextHtmlFacets(qReplaced);
        HashMap<Integer, Long> yearFacetsAll = NetarchiveYearCountCache.getYearFacetsAllQuery();

        SmurfYearBuckets buckets = SmurfUtil.generateYearBuckets(yearContentQuery, yearFacetsAll, startyear,null);      
        return buckets;
        
      }
      catch(Exception e){
        e.printStackTrace();
        throw e;
      }

    }

    
    
    
    public static BufferedImage getHtmlPagePreview(String source_file_path, long offset) throws Exception {
      
      String url = PropertiesLoader.WAYBACK_BASEURL+"services/view?source_file_path="+source_file_path +"&offset="+offset+"&showToolbar=false";            
      long now = System.currentTimeMillis();
      
      //String filename = PropertiesLoader.SCREENSHOT_TEMP_IMAGEDIR+source_file_path+"@"+offset+".png"; //Does not work since subfolders must be created before.
      //TODO implement caching for images?
      String filename = PropertiesLoader.SCREENSHOT_TEMP_IMAGEDIR+now+"_"+offset +".png"; //Include offset to avoid hitting same time.
      String chromeCommand = PropertiesLoader.CHROME_COMMAND;
                                 
      log.info("Generating preview-image for url:"+url);

      ProcessBuilder pb  =  null;
      
      //Use proxy. Construct proxy URL from base url and proxy port.
      String proxyUrl = getPreviewUrl();
                              
      int timeoutMillis = PropertiesLoader.SCREENSHOT_PREVIEW_TIMEOUT*1000;            
      log.info("generate temp preview file:"+filename);
//     pb = new ProcessBuilder(chromeCommand, "--headless" ,"--disable-gpu" ,"--ipc-connection-timeout=10000","--timeout="+timeoutMillis,"--screenshot="+filename,"--window-size=1280,1024","--proxy-server="+proxyUrl,  url);
      //no socks proxy
      pb = new ProcessBuilder(chromeCommand, "--headless" ,"--disable-gpu" ,"--ipc-connection-timeout=10000","--timeout="+timeoutMillis,"--screenshot="+filename,"--window-size=1280,1024",  url);
      log.info(chromeCommand+" --headless --disable-gpu --ipc-connection-timeout=10000 --timeout="+timeoutMillis+" --screenshot="+filename+" --window-size=1280,1024", url);
    // chromium-browser --headless  --disable-gpu --ipc-connection-timeout=3000 --screenshot=test.png --window-size=1280,1024   --proxy-server="socks4://localhost:9000" https://www.google.com/        
      Process start = pb.start();      
      //Due to a bug in chromium, the process can hang and never terminate. The timeout is not working.. Also the screenshot will not be written to file.
      if(!start.waitFor(timeoutMillis+5000, TimeUnit.MILLISECONDS)) { // timeout + 5 second before killing.
        //timeout - kill the process. 
        log.info("Timeout generating preview. Due to bug chromium can hang.");
        start.destroyForcibly(); 
        throw new NotFoundServiceException("Timeout generating page preview"); // Just give a nice 404.
      }

      //return image even if timeout.
      InputStream is = start.getInputStream();
        String conlog= getStringFromInputStream(is);
        log.info("conlog:"+conlog); //No need to log this, can be spammy. But usefull when debugging                    
       BufferedImage image =  ImageIO.read(new File(filename));
       return image;  
      
            
    }

    public static HarvestDates getHarvestTimesForUrl(String url) throws Exception {
      log.info("getting harvesttimes for url:"+url);
      HarvestDates datesVO = new HarvestDates();
      datesVO.setUrl(url);
      ArrayList<Date> dates = NetarchiveSolrClient.getInstance().getHarvestTimesForUrl(url);
      
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
  
       ArrayList<IndexDoc> indexDocs = NetarchiveSolrClient.getInstance().getHarvestPreviewsForUrl(url); // Only contains the required fields for this method
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
     
    public static BufferedImage wordCloudForDomain(String domain) throws Exception {    
       log.info("getting wordcloud for url:"+domain);
       String text = NetarchiveSolrClient.getInstance().getTextForDomain(domain); // Only contains the required fields for this method       
       BufferedImage bufferedImage = WordCloudImageGenerator.wordCloudForDomain(text);
       return bufferedImage;       
    }    
    
    public static ArrayList<ImageUrl> getImagesForHtmlPageNew(String source_file_path,long offset) throws Exception {            
      ArrayList<ArcEntryDescriptor> arcs = getImagesForHtmlPageNewThreaded(source_file_path,offset);       

      return arcEntrys2Images(arcs);     
    }
    
    public static String punyCodeAndNormaliseUrl(String url) throws Exception {     
      if (!(url.startsWith("http://") || url.startsWith("https://"))){ 
        throw new Exception("Url not starting with http:// or https://");
      }
      
      URL uri = new URL(url);
      String hostName = uri.getHost();
      String hostNameEncoded = IDN.toASCII(hostName);
      
      String path = uri.getPath();
      if ("".equals(path)){
        path="/";
      }
      String urlQueryPath =  uri.getQuery();
      if(urlQueryPath == null){
        urlQueryPath="";
      }
                
      String urlPunied = "http://"+hostNameEncoded + path +urlQueryPath;
      String urlPuniedAndNormalized = Normalisation.canonicaliseURL(urlPunied);     
      
      log.info("normalizing url:"+url +" url_norm:"+urlPuniedAndNormalized );
       return urlPuniedAndNormalized;        
    }
      
     
    
    /*
     * Find images on a HTML page.
     * 1) Find the doc in solr from source_file_path and offset. (fast)
     * 2) Get image links field
     * 3) For each images try to find that url_norm in solr with harvest time closest to the harvesttime for the HTML page. 
     */
    public static ArrayList<ArcEntryDescriptor>  getImagesForHtmlPageNewThreaded(String source_file_path,long offset) throws Exception {
      
  
      IndexDoc doc = NetarchiveSolrClient.getInstance().getArcEntry(source_file_path, offset);
      ArrayList<String> imageLinks = doc.getImageUrls();          
      if (imageLinks.size() == 0){
        return  new ArrayList<ArcEntryDescriptor> ();
      }
      String queryStr = queryStringForImages(imageLinks);
      ArrayList<ArcEntryDescriptor> imagesFromHtmlPage = NetarchiveSolrClient.getInstance().findImagesForTimestamp(queryStr, doc.getCrawlDate());             
      return imagesFromHtmlPage;                
    }

   public static String queryStringForImages(List<String> imageLinks) {
      if (imageLinks.size() > 1000) {
           imageLinks = imageLinks.subList(0, 1000);
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
      return queryStr;
    }
    
    
    /*
     * Find images on a HTML page. 
     * THIS IS NOT WORKING REALLY. To many searches before enough images with exif loc is found. TODO: Use graph search
     * 1) Find the doc in solr from source_file_path and offset. (fast)
     * 2) Get image links field
     * 3) For each images see if we have the image in index and it has exif location data 
     * 
     */
    public static ArrayList<ArcEntryDescriptor> getImagesWithExifLocationForHtmlPageNewThreaded(String source_file_path,long offset) throws Exception {
        
      IndexDoc arcEntry = NetarchiveSolrClient.getInstance().getArcEntry(source_file_path, offset);
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
      query.append(" url_norm:none) AND exif_location_0_coordinate:*"); //just close last OR, and must have gps data
      String queryStr= query.toString();
      ArrayList<ArcEntryDescriptor> imagesFromHtmlPage = NetarchiveSolrClient.getInstance().findImagesForTimestamp(queryStr, arcEntry.getCrawlDate());
                      
       return imagesFromHtmlPage;                
    }
    
    
    public static String getEncoding(String source_file_path,String offset) throws Exception{
    	            	    
    	SearchResult search = NetarchiveSolrClient.getInstance().search("source_file_path:\""+source_file_path +"\" AND source_file_offset:"+offset, 1);
        if (search.getNumberOfResults() ==0){
          log.warn("No content encoding found for:"+source_file_path +" and offset:"+offset);
          return "UTF-8";         
        }
        else{
          return search.getResults().get(0).getContentEncoding(); //Can still be null.
        }
    }
    
    public static ArcEntry getArcEntry(String source_file_path, long offset) throws Exception{         
        return ArcParserFileResolver.getArcEntry(source_file_path, offset);        
    }
    
    public static ArcEntry getArcEntry(String source_file_path, long offset, boolean loadBinary) throws Exception{         
        return ArcParserFileResolver.getArcEntry(source_file_path, offset,loadBinary);        
    }
    
    
    public static InputStream exportWarcStreaming(
            boolean expandResources, boolean avoidDuplicates, String query, String... filterqueries) {
      SolrGenericStreaming solr = new SolrGenericStreaming(
              PropertiesLoader.SOLR_SERVER, 100, Arrays.asList("source_file_path", "source_file_offset"),
              expandResources, avoidDuplicates, query, filterqueries);

      // TODO: Why do we have a max of 1M?
      return new StreamingSolrWarcExportBufferedInputStream(solr, 1000000); //1M max. results just for now
    }
 
   

    public static InputStream exportLinkGraphStreaming(String q) {        
      SolrStreamingLinkGraphCSVExportClient solr= SolrStreamingLinkGraphCSVExportClient.createExporter(PropertiesLoader.SOLR_SERVER, q);      
      return new StreamingSolrExportBufferedInputStream(solr, 1000000); //1 MIL
    }

    public static InputStream exportCvsStreaming(String q, String fq, String fields) throws Exception {
    //TODO test only allowed fields are selected!
        
       SolrStreamingExportClient solr = SolrStreamingExportClient.createCvsExporter( PropertiesLoader.SOLR_SERVER, q, fields, fq );
      return new StreamingSolrExportBufferedInputStream(solr, 1000000);
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
      
      List<FacetCount>  facets = NetarchiveSolrClient.getInstance().getDomainFacets(domain,facetLimit, ingoing, start, end);
      log.info("Creating graph for domain:"+domain +" ingoing:"+ingoing +" and facetLimit:"+facetLimit);
      
      HashMap<String, List<FacetCount>> domainFacetMap = new HashMap<String, List<FacetCount>>();
      //Also find facet for all facets from first call.
      domainFacetMap.put(domain, facets); //add this center domain
      
      //Do all queries
      for (FacetCount f : facets){
        String facetDomain =f.getValue();                  
        List<FacetCount>  fc = NetarchiveSolrClient.getInstance().getDomainFacets(facetDomain,facetLimit, ingoing,start,end);
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
      ArcEntry arc=ArcParserFileResolver.getArcEntry(source_file_path, offset);           
      arc.setContentEncoding(Facade.getEncoding(source_file_path, ""+offset));
      String collectionName = PropertiesLoader.PID_COLLECTION_NAME;
      StringBuffer parts = new StringBuffer();
      //the original page
      parts.append("<part>\n");
      parts.append("urn:pwid:"+collectionName+":"+arc.getCrawlDate()+":part:"+arc.getUrl() +"\n");
      parts.append("</part>\n");      
      String xmlIncludes = HtmlParserUrlRewriter.generatePwid(arc);//all sub elements            
      parts.append(xmlIncludes);
      return  parts.toString();
    }
    
    
    /*
     * This method does something similar to the new feature from archive.org. See: http://blog.archive.org/2017/10/05/wayback-machine-playback-now-with-timestamps/
     * 
     * Returns information about the harvested HTML page.
     * List all resources on the page and when they were harvested.    
     * Calcuate time difference between html page and each resource.
     * Preview link to html page.
     * 
     */
    
    public static TimestampsForPage timestampsForPage(String source_file_path, long offset) throws Exception{      
      TimestampsForPage ts= new TimestampsForPage();
      ArrayList<PageResource> pageResources = new ArrayList<PageResource>();
      ts.setResources(pageResources);
            
      ArcEntry arc=ArcParserFileResolver.getArcEntry(source_file_path, offset);
      // TODO WHY ? arc.setContentEncoding(Facade.getEncoding(source_file_path, ""+offset));
      
      IndexDoc docPage = NetarchiveSolrClient.getInstance().getArcEntry(source_file_path, offset);
            
      Date pageCrawlDate =new Date(docPage.getCrawlDateLong()); 
      ts.setPageCrawlDate(pageCrawlDate);
      ts.setPageUrl(arc.getUrl());
      
      String previewUrl = PropertiesLoader.WAYBACK_BASEURL+"services/image/pagepreview?source_file_path="+source_file_path +"&offset="+offset+"&showToolbar=false";
      ts.setPagePreviewUrl(previewUrl);      
      
      //the original page REMEMBER      
                                                        
      HashSet<String> resources = HtmlParserUrlRewriter.getResourceLinksForHtmlFromArc(arc);      
      
      ArrayList<IndexDoc> docs = NetarchiveSolrClient.getInstance().findNearestHarvestTimeForMultipleUrlsFullFields(resources,arc.getCrawlDate());
          
      for(IndexDoc doc : docs){ //These are the resources found        
        String docUrl = doc.getUrl_norm();                  
        PageResource pageResource = new PageResource();  
        
        Date resourceDate = new Date(doc.getCrawlDateLong());
        pageResource.setCrawlTime(resourceDate);
        pageResource.setUrl(doc.getUrl());
        pageResource.setContentType(doc.getContentTypeNorm());        
        String downloadUrl = PropertiesLoader.WAYBACK_BASEURL+"services/downloadRaw?source_file_path="+doc.getSource_file_path() +"&offset="+doc.getOffset();
        pageResource.setDownloadUrl(downloadUrl);
        
        long timeDif = resourceDate.getTime()-pageCrawlDate.getTime();
                 
        pageResource.setTimeDifference(millisToDuration(timeDif));
        
        pageResources.add(pageResource);                       
        resources.remove(docUrl);                 
      }
        log.info("Url not matched:"+resources);
        ts.setNotHarvested(new ArrayList<String>(resources));
      
      return ts;
    }
    
    
    public static IndexDoc resolveRelativUrlForResource(String source_file_path, long offset, String leakUrl) throws Exception{
      if (!leakUrl.startsWith("/solrwayback")){
        log.warn("resolveRelativeLeak does not start with /solrwayback:"+leakUrl);
       throw new InvalidArgumentServiceException("resolveRelativeLeak does not start with: /solrwayback");
      }
      //remove the start, and everyting until second / 
      leakUrl=leakUrl.substring(12);
      leakUrl=leakUrl.substring(leakUrl.indexOf("/")+1);
      
      
      IndexDoc doc = NetarchiveSolrClient.getInstance().getArcEntry(source_file_path, offset); 
       URL originalURL = new URL(doc.getUrl());
      String resolvedUrl = new URL(originalURL,leakUrl).toString();
     
      log.info("stripped leakUrl:"+leakUrl);
      log.info("url origin:"+doc.getUrl());      
      log.info("resolved URL:"+ resolvedUrl);

      
      //First see if we have the given URL as excact match.
      IndexDoc docFound = NetarchiveSolrClient.getInstance().findClosestHarvestTimeForUrl(resolvedUrl,doc.getCrawlDate());
      if (docFound != null){
        return docFound;
      }
      String[] tokens= leakUrl.split("/");
      String leakResourceName=tokens[tokens.length-1];
      
      //else just try to lookup resourcename (last part of the url) for that domain.       
      ArrayList<IndexDoc> matches = NetarchiveSolrClient.getInstance().findNearestForResourceNameAndDomain(doc.getDomain(), leakResourceName,doc.getCrawlDate()); 
      for (IndexDoc m : matches){
        if (m.getUrl().endsWith(leakUrl) ){        
        log.info("found leak url:"+m.getUrl());
          return m;          
        }
      }
      log.info("Could not find relative resource:"+leakUrl);
      throw new NotFoundServiceException("Could not find relative resource:"+leakUrl);      
    }
        
    /*
     * This is called then a relative url fails. Try to match the relative url for that domain. (qualified guessing...)
     * 
     */
    public static IndexDoc matchRelativeUrlForDomain(String refererUrl,String url,String solrDate) throws Exception{
      
      log.info("url not with domain:"+url +" referer:"+refererUrl);         
      String orgDomain=UrlUtils.getDomainFromWebApiParameters(refererUrl);
      String resourceName = UrlUtils.getResourceNameFromWebApiParameters(url);
      //resourceLeaked
      //TODO use crawltime from refererUrl
      ArrayList<IndexDoc> matches = NetarchiveSolrClient.getInstance().findNearestForResourceNameAndDomain(orgDomain, resourceName ,solrDate); 
      //log.info("********* org domain:"+orgDomain);
      //log.info("********* resourceLeaked "+resourceName );
     //log.info("********* matches:"+matches.size());
      for (IndexDoc m : matches){        
        if (m.getUrl().endsWith(resourceName)){        
          log.info("found leaked resource for:"+url);
          return m;          
        }
      }
      throw new NotFoundServiceException("Could not find resource for leak:"+url);
    }

    
    public static ArcEntry viewHtml(String source_file_path, long offset, IndexDoc doc, Boolean showToolbar) throws Exception{         
    	if (showToolbar==null){
    	   showToolbar=false;
    	}      
    	ArcEntry arc=ArcParserFileResolver.getArcEntry(source_file_path, offset);    	 
           	
    	log.info("from arch contenttype:"+arc.getContentType());

    	    	        
        String encoding = arc.getContentCharset();
           
        if (encoding == null){
    	  encoding =Facade.getEncoding(source_file_path, ""+offset); //Ask the index
    	}    	
    	if (encoding == null){    	    	  
           encoding="ISO-8859-1"; //Is UTF-8 a better default? 
    	}    	
    	
        arc.setContentCharset(encoding); //Need to help read the binary.
    	
    	if(doc.getType().equals("Twitter Tweet")){    	      	  
          TwitterPlayback twitterPlayback = new TwitterPlayback(arc, doc, showToolbar);
          return twitterPlayback.playback();
    	}
    	else if(doc.getType().equals("Jodel Post") || doc.getType().equals("Jodel Thread")){
    	  JodelPlayback jodelPlayback = new JodelPlayback(arc, doc, showToolbar);
          return jodelPlayback.playback();    	      	  
    	}    	
    	else if ("Web Page".equals(doc.getType()) ||  ( (300<=doc.getStatusCode() && arc.getContentType()!= null && arc.getContentType().equals("text/html") ) ) ){ // We still want the toolbar to show for http moved (302 etc.)
    	  HtmlPlayback htmlPlayback = new HtmlPlayback(arc, doc, showToolbar);
          return htmlPlayback.playback();    		 
        } 
    	else if ("text/css".equals(arc.getContentType()) && arc.getContentEncoding()!= null  && !("br".equalsIgnoreCase(arc.getContentEncoding())) ) {     	        
    	  CssPlayback cssPlayback = new CssPlayback(arc, doc, showToolbar); //toolbar is never shown anyway.
          return cssPlayback.playback();    	  
        }		
		else{ //Serve as it is. (Javascript, images, pdfs etc.)
		  
		  return arc; //dont parse
		}
    }
    
    //For fronted
    public static HashMap<String,String> getPropertiesWeb() throws Exception{         
        HashMap<String,String> props = new HashMap<String,String>();
        props.put(PropertiesLoaderWeb.WAYBACK_SERVER_PROPERTY,PropertiesLoaderWeb.WAYBACK_SERVER);
        props.put(PropertiesLoaderWeb.OPENWAYBACK_SERVER_PROPERTY,PropertiesLoaderWeb.OPENWAYBACK_SERVER);
        props.put(PropertiesLoaderWeb.ALLOW_EXPORT_WARC_PROPERTY,""+PropertiesLoaderWeb.ALLOW_EXPORT_WARC);
        props.put(PropertiesLoaderWeb.ALLOW_EXPORT_CSV_PROPERTY,""+PropertiesLoaderWeb.ALLOW_EXPORT_CSV);
        props.put(PropertiesLoaderWeb.EXPORT_CSV_FIELDS_PROPERTY,PropertiesLoaderWeb.EXPORT_CSV_FIELDS);
        props.put(PropertiesLoaderWeb.MAPS_LATITUDE_PROPERTY,PropertiesLoaderWeb.MAPS_LATITUDE);
        props.put(PropertiesLoaderWeb.MAPS_LONGITUDE_PROPERTY,PropertiesLoaderWeb.MAPS_LONGITUDE);
        props.put(PropertiesLoaderWeb.MAPS_RADIUS_PROPERTY,PropertiesLoaderWeb.MAPS_RADIUS);        
        props.put(PropertiesLoaderWeb.LEAFLET_SOURCE_PROPERTY,PropertiesLoaderWeb.LEAFLET_SOURCE);
        props.put(PropertiesLoaderWeb.LEAFLET_ATTRIBUTION_PROPERTY,PropertiesLoaderWeb.LEAFLET_ATTRIBUTION);
        
        return props;
    }
    
    public static String proxySolrNoFacets( String query, List<String> fq, boolean grouping, boolean revisits, Integer start) throws Exception {          
    	return NetarchiveSolrClient.getInstance().searchJsonResponseNoFacets(query, fq, grouping, revisits, start);    	    	
   }
        
    public static String proxySolrOnlyFacets( String query, List<String> fq, boolean revisits) throws Exception {          
      return NetarchiveSolrClient.getInstance().searchJsonResponseOnlyFacets(query, fq, revisits);             
 }
    
            
    /*
     * Temp solution, make generic query properties
     * 
     */
    
 /*
public static String proxyBackendResources(String source_file_path, String offset, String serviceName) throws Exception{                    
      
      String backendServer= PropertiesLoaderWeb.WAYBACK_SERVER;
  
        
      ClientConfig config = new DefaultClientConfig();
      Client client = Client.create(config);
      WebResource service = client.resource(UriBuilder.fromUri(backendServer).build());
      WebResource queryWs= service.path("services")
                                  .path(serviceName)                                  
                                  .queryParam("source_file_path", source_file_path)                           
                                  .queryParam("offset", offset);
               
                 
      ClientResponse response = queryWs.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
      String responseStr= response.getEntity(String.class);

      return responseStr;
      
  }
   */ 
    
    
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
    
    public static  ArrayList<ImageUrl> indexDoc2Images(ArrayList<IndexDoc> docs){
      ArrayList<ImageUrl> imageUrls = new ArrayList<ImageUrl>();
      for (IndexDoc entry : docs){                          
        ImageUrl imageUrl = new ImageUrl();
        String imageLink = PropertiesLoader.WAYBACK_BASEURL+"services/image?source_file_path="+entry.getSource_file_path()+"&offset="+entry.getOffset();
        String downloadLink = PropertiesLoader.WAYBACK_BASEURL+"services/downloadRaw?source_file_path="+entry.getSource_file_path()+"&offset="+entry.getOffset();
        imageUrl.setImageUrl(imageLink);
        imageUrl.setDownloadUrl(downloadLink);             
        imageUrl.setHash(entry.getHash());
        imageUrl.setUrlNorm(entry.getUrl_norm());
                
        imageUrl.setLastModified(entry.getLastModifiedLong());
        String exifLocation = entry.getExifLocation();
        if (exifLocation != null){
          String[] split = exifLocation.split(",");
          double lat = Double.parseDouble(split[0]);          
          imageUrl.setLatitude(lat);        
          double lon = Double.parseDouble(split[1]);          
          imageUrl.setLongitude(lon);
          imageUrl.setResourceName(entry.getResourceName());  
        }                                
        imageUrls.add(imageUrl);
      }
      return imageUrls;                       
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

   /*
    * Just show the most important  
    */     
  private static String millisToDuration(long millis){ //TODO better... fast impl for demo    
    String sign ="";
    if (millis <0){
      sign="-";
      millis=-millis;
    }
    
    long days = TimeUnit.MILLISECONDS.toDays(millis);
    if (days >0){
      return sign+days +" days";
    }
    long hours = TimeUnit.MILLISECONDS.toHours(millis);
    if(hours >0){
      return sign+hours +" hours";      
    }
    long minutes = TimeUnit.MILLISECONDS.toMinutes(millis); 
    if (minutes >0){
      return sign+minutes +" minutes";
    }
    
    long seconds = TimeUnit.MILLISECONDS.toSeconds(millis); 
    return sign+seconds +" seconds";    
  }
  
  
  
  //takes the wayback_base url and create the proxy url
  // http://localhost:8080/solrwayback/ -> socks4://localhost:9000 
  private static String getPreviewUrl() throws Exception{

    String baseUrl = PropertiesLoader.WAYBACK_BASEURL;
    String port = "8080"; //TODO load   
    URL url = new URL(baseUrl);
    String host = url.getHost();
    String part =url.getPath();        
    String socksUrl= "http://"+host+":"+port; 
    log.info("socks url:"+socksUrl);
    return socksUrl;
   
  }
  
   
}
