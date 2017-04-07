package dk.kb.netarchivesuite.solrwayback.facade;


import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

import dk.kb.netarchivesuite.solrwayback.parsers.ProximityHtmlParser;
import dk.kb.netarchivesuite.solrwayback.parsers.WaybackToolbarInjecter;
import dk.kb.netarchivesuite.solrwayback.service.dto.*;
import dk.kb.netarchivesuite.solrwayback.service.dto.graph.D3Graph;
import dk.kb.netarchivesuite.solrwayback.service.dto.graph.Link;
import dk.kb.netarchivesuite.solrwayback.service.dto.graph.Node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.netarchivesuite.solrwayback.concurrency.ImageFromArcFileExtractorExecutor;
import dk.kb.netarchivesuite.solrwayback.parsers.HtmlParserUrlRewriter;
import dk.kb.netarchivesuite.solrwayback.parsers.FileParserFactory;
import dk.kb.netarchivesuite.solrwayback.solr.FacetCount;
import dk.kb.netarchivesuite.solrwayback.solr.SolrClient;

public class Facade {
    private static final Logger log = LoggerFactory.getLogger(Facade.class);

    // Maximum distance, measured in images on the page, from matching terms
    // Only relevant if there are matching terms
    public static final int MAX_DISTANCE = 2;

    public static SearchResult search(String searchText, String filterQuery) throws Exception {

        SearchResult result = SolrClient.getInstance().search(searchText, filterQuery);
        return result;
    }

    public static ArrayList<? extends ArcEntryDescriptor> findImages(String searchText) throws Exception {

        long start = System.currentTimeMillis();
        SearchResult result = SolrClient.getInstance().search(searchText, "content_type_norm:image OR content_type_norm:html", 20); //only search these two types
        
        //multithreaded load arc/warc files and parse html
        ArrayList<? extends ArcEntryDescriptor> extractImages =
                ImageFromArcFileExtractorExecutor.extractImages(result.getResults());

        return extractImages;
        
        /* This is the old non multithreaded html read and parsing.
        HashSet<ArcEntryDescriptor> imagesHash = new HashSet<ArcEntryDescriptor>(); 
        
        for (IndexDoc current : result.getResults()){
            if ("html".equals(current.getContentTypeNorm())){                           
                ArrayList<ArcEntryDescriptor> images = getImagesFromHtmlPage(current);                                
                imagesHash.addAll(images);                          
            }
            else if ("image".equals(current.getContentTypeNorm())){ 
                String source_file_s = current.getSource_file_s();//always only 1 due to group
                String arcFull = current.getArc_full();
                ArcEntryDescriptor desc= new ArcEntryDescriptor();
                desc.setArcFull(arcFull);
                desc.setSource_file_s(source_file_s);
                desc.setHash(current.getHash());
                desc.setOffset(SolrClient.getOffset(source_file_s));
                imagesHash.add(desc);
            }

        }        
        System.out.println("image search for:"+searchText +" milis:"+(System.currentTimeMillis()-start));

        return new ArrayList<ArcEntryDescriptor>(imagesHash);
    */
    
    }


    public static   HarvestDates getHarvestTimesForUrl(String url) throws Exception {
      log.info("getting harvesttimes for url:"+url);
      HarvestDates datesVO = new HarvestDates();
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
    
    public static ArrayList<WeightedArcEntryDescriptor> getImagesFromHtmlPage(IndexDoc indexDoc) throws Exception{
        if (!"html".equals(indexDoc.getContentTypeNorm())){
            throw new IllegalArgumentException("Not html doc:"+indexDoc.getContentTypeNorm());
        }
        
        String arc_full = indexDoc.getArc_full();
        String crawlDate=indexDoc.getCrawlDate();
        String url = indexDoc.getUrl();               
        ArcEntry arcEntry = getArcEntry(arc_full, indexDoc.getOffset());
        String html = new String(arcEntry.getBinary());
        
        //ArrayList<String> imageUrls = HtmlParser.getImageUrls( url, html);
        // TODO: Extract terms from query
        Set<String> queryTerms = Collections.emptySet();
        List<ProximityHtmlParser.WeightedImage> imageUrls =
                ProximityHtmlParser.getImageUrls(new URL(url), indexDoc.getScore(), html, queryTerms, MAX_DISTANCE);

        log.info("image urls:"+imageUrls);
        if (imageUrls.size() == 0){
            return new ArrayList<>();
        }

        //TODO maybe images will have width/size in index in a future version
        //only check first 10

        StringBuilder query = new StringBuilder();
        //query.append("content_type_norm:image AND content_length:[2000 TO *] AND ("); //was just a test to find large images. No problems with 20MB images
        query.append("content_type_norm:image  AND (");
        for (int i=0;i<10 && i <imageUrls.size();i++ ){
            String orgUrl=imageUrls.get(i).getImageURL().toExternalForm();
            String fixedUrl= orgUrl.replaceAll("[\\\\]", "/");            
            query.append(" url:\""+fixedUrl+"\" OR");
          
        }
        query.append(" url:none)"); //just close last OR
        String queryStr= query.toString();
        log.info("image query:"+queryStr);
        ArrayList<WeightedArcEntryDescriptor> resolved = null;
        try{
         resolved = SolrClient.getInstance().findImageForTimestamp(queryStr, crawlDate);
        }
        catch(Exception e){
        	log.error("Solr error for query:"+queryStr); 
        }
        
        // Enrich with the right weights
        Map<String, ProximityHtmlParser.WeightedImage> imageMap = new HashMap<>(imageUrls.size());
        for (ProximityHtmlParser.WeightedImage image: imageUrls) {
            imageMap.put(image.getImageURL().toExternalForm(), image);
        }
        ProximityHtmlParser.WeightedImage image;
        for (WeightedArcEntryDescriptor descriptor: resolved) {
            if ((image = imageMap.get(descriptor.getUrl())) != null) {
                descriptor.setWeight(image.getWeight());
            } else {
                log.debug("Unable to match ArcEntryDescriptor url '" + descriptor.getUrl() + "' to weighted image");
            }
        }
        return resolved;
    }
    
    
    public static String getEncoding(String arcFilePath,String offset) throws Exception{
    	
        String paths[] = arcFilePath.split("/");
        String fileName = paths[paths.length - 1];
            	    
    	SearchResult search = SolrClient.getInstance().search("source_file_s:"+ fileName+"@"+offset, 1);
        if (search.getNumberOfResults() ==0){
        	return "UTF-8";
        }
        else{
        	return search.getResults().get(0).getContentEncoding();
        }
    }
    
    public static ArcEntry getArcEntry(String arcFilePath, long offset) throws Exception{         
        return FileParserFactory.getArcEntry(arcFilePath, offset);        
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
    
    public static ArcEntry viewHtml(String arcFilePath, long offset, Boolean showToolbar) throws Exception{         
    	
    	ArcEntry arc=FileParserFactory.getArcEntry(arcFilePath, offset);    	 
        arc.setContentEncoding(Facade.getEncoding(arcFilePath, ""+offset));
    	if (("text/html".equals(arc.getContentType()))){
    		long start = System.currentTimeMillis();
        	log.debug(" Generate webpage from FilePath:" + arcFilePath + " offset:" + offset);
        	String textReplaced = HtmlParserUrlRewriter.replaceLinks(arc);    	 
        	
        	//Inject tooolbar
        	if (showToolbar!=Boolean.FALSE ){ //If true or null. 
        	  textReplaced = WaybackToolbarInjecter.injectWaybacktoolBar(arcFilePath,offset,textReplaced);
        	}
        	
        	arc.setBinary(textReplaced.getBytes(arc.getContentEncoding()));    	
            log.info("Generating webpage total processing:"+(System.currentTimeMillis()-start));
        	return arc;
    		 
        }else if (("text/css".equals(arc.getContentType()))){ 
    		long start = System.currentTimeMillis();
        	log.debug(" Generate css from FilePath:" + arcFilePath + " offset:" + offset);
        	String textReplaced = HtmlParserUrlRewriter.replaceLinksCss(arc)    	 ;        
        	
        	arc.setBinary(textReplaced.getBytes(arc.getContentEncoding()));    	
            log.info("Generating css total processing:"+(System.currentTimeMillis()-start));
        	return arc;
        	
        }

		log.info("skipping html url rewrite for contentype:"+arc.getContentType());
    	return arc; //dont parse
            
    }
    
    
}
