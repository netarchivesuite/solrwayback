package dk.kb.netarchivesuite.solrwayback.solr;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.*;
import org.apache.solr.client.solrj.response.FacetField.Count;

import org.apache.solr.client.solrj.response.FieldStatsInfo;
import org.apache.solr.client.solrj.response.Group;
import org.apache.solr.client.solrj.response.GroupCommand;
import org.apache.solr.client.solrj.response.QueryResponse;

import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.collect.Iterables;

import dk.kb.netarchivesuite.solrwayback.parsers.Normalisation;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntryDescriptor;
import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDoc;
import dk.kb.netarchivesuite.solrwayback.service.dto.SearchResult;
import dk.kb.netarchivesuite.solrwayback.service.dto.statistics.DomainYearStatistics;
import dk.kb.netarchivesuite.solrwayback.service.exception.InvalidArgumentServiceException;
import dk.kb.netarchivesuite.solrwayback.solr.FacetCount;
import dk.kb.netarchivesuite.solrwayback.util.DateUtils;


public class NetarchiveSolrClient {

  private static final Logger log = LoggerFactory.getLogger(NetarchiveSolrClient.class);
  protected static SolrClient solrServer;
  protected  static NetarchiveSolrClient instance = null;
  protected  static Pattern TAGS_VALID_PATTERN = Pattern.compile("[-_.a-zA-Z0-9Ã¦Ã¸Ã¥Ã†Ã˜Ã…]+"); 

  protected static String indexDocFieldList = "id,score,title,url,url_norm,links_images,source_file_path,source_file,source_file_offset,domain,resourcename,content_type,content_type_norm,hash,type,crawl_date,content_encoding,exif_location, status_code";

  protected NetarchiveSolrClient() { // private. Singleton
  }

  // Example url with more than 1000 rewrites: http://belinda:9721/webarchivemimetypeservlet/services/wayback?waybackdata=20140119010303%2Fhttp%3A%2F%2Fbillige-skilte.dk%2F%3Fp%3D35


   /*
   * Called from initialcontextlistener when tomcat is starting up.
   * 
   */
  public static void initialize(String solrServerUrl) {
    solrServer =  new HttpSolrClient.Builder(solrServerUrl).build();
    //  solrServer.setRequestWriter(new BinaryRequestWriter()); // To avoid http error code 413/414, due to monster URI. (and it is faster)

    instance = new NetarchiveSolrClient();
    log.info("SolrClient initialized with solr server url:" + solrServerUrl);
  }

  
  
  public static NetarchiveSolrClient getInstance() {
    if (instance == null) {
      throw new IllegalArgumentException("SolrClient not initialized");
    }
    return instance;
  }


  /*
   * Delegate 
   */
  public  List<FacetCount> getDomainFacets(String domain, int facetLimit, boolean ingoing, Date crawlDateStart, Date crawlDateEnd) throws Exception{

    if (ingoing){
      return getDomainFacetsIngoing(domain, facetLimit, crawlDateStart,crawlDateEnd);
    }
    else{
      return getDomainFacetsOutgoing(domain, facetLimit, crawlDateStart,crawlDateEnd);
    }       
  }

  /*
   * Get other domains linking to this domain
   * 
   */
  public  List<FacetCount> getDomainFacetsIngoing(String domain, int facetLimit, Date crawlDateStart, Date crawlDateEnd) throws Exception{


    String dateStart= DateUtils.getSolrDate(crawlDateStart);
    String dateEnd = DateUtils.getSolrDate(crawlDateEnd);


    SolrQuery solrQuery = new SolrQuery();
    solrQuery.setQuery("links_domains:\""+domain+"\" AND -domain:\""+domain+"\"");
    solrQuery.setRows(0);
    solrQuery.set("facet", "true");       
    solrQuery.add("facet.field","domain");
    solrQuery.add("facet.limit",""+facetLimit);
    solrQuery.addFilterQuery("crawl_date:["+dateStart+ " TO "+dateEnd+"]");

    solrQuery.add("fl","id,score,title,source_file, source_file_path,source_file_offset,url, url_norm,content_type_norm,hash,crawl_date,content_type, content_encoding"); //only request fields used

    QueryResponse rsp = solrServer.query(solrQuery,METHOD.POST);      
    List<FacetCount> facetList = new ArrayList<FacetCount>();
    FacetField facet = rsp.getFacetField("domain");
    for (Count c: facet.getValues()){
      FacetCount fc = new FacetCount();
      fc.setValue(c.getName());
      fc.setCount(c.getCount());      
      facetList.add(fc);
    }
    return facetList;    
  }

  /* 
   *Get the domains this domain links to this domain 
   */
  public  List<FacetCount> getDomainFacetsOutgoing(String domain, int facetLimit, Date crawlDateStart, Date crawlDateEnd) throws Exception{


    String dateStart= DateUtils.getSolrDate(crawlDateStart);
    String dateEnd = DateUtils.getSolrDate(crawlDateEnd);

    SolrQuery solrQuery = new SolrQuery();
    solrQuery.setQuery("domain:\""+domain+"\"");  

    solrQuery.setRows(0);
    solrQuery.set("facet", "true");       
    solrQuery.add("facet.field","links_domains");
    solrQuery.add("facet.limit",""+(facetLimit+1)); //+1 because itself will be removed and is almost certain of resultset if self-linking
    solrQuery.addFilterQuery("crawl_date:["+dateStart+ " TO "+dateEnd+"]");
    solrQuery.add("fl","id,score,title,source_file,source_file_path,source_file_offset,url, url_norm,content_type_norm,hash,crawl_date,content_type, content_encoding"); //only request fields used

    QueryResponse rsp = solrServer.query(solrQuery,METHOD.POST);      
    List<FacetCount> facetList = new ArrayList<FacetCount>();
    FacetField facet = rsp.getFacetField("links_domains");

    //We have to remove the domain itself.
    for (Count c: facet.getValues()){
      if (!c.getName().equalsIgnoreCase(domain)){
        FacetCount fc = new FacetCount();
        fc.setValue(c.getName());
        fc.setCount(c.getCount());      
        facetList.add(fc);
      }
    }
    return facetList;    
  }




  /*
   * The logic for getting the 4 dates in 2 queries is too complicated, and only gives small performance boost... 
   */
  public WaybackStatistics getWayBackStatistics(int statusCode, String url, String url_norm, String crawlDate)  throws Exception{    
    WaybackStatistics stats = new  WaybackStatistics();
    stats.setStatusCode(statusCode); //this is know when calling the method, so no need to extract it from Solr.
    stats.setUrl(url);
    stats.setUrl_norm(url_norm);
    //These will only be set if they are different from input (end points). So set them below
    stats.setLastHarvestDate(crawlDate);
    stats.setFirstHarvestDate(crawlDate);

    //We query for 1 result to get the domain.
    String domain = null;
     long start = System.currentTimeMillis();
    stats.setHarvestDate(crawlDate);
    log.info("Getting wayback statistics for solrdate:"+crawlDate); 
    final String statsField= "crawl_date";

    int results=0;
    
    String query = "url_norm:\""+url_norm+"\" AND crawl_date:{\""+crawlDate+"\" TO *]";    

    SolrQuery solrQuery = new SolrQuery(query);            

    solrQuery.setRows(1);
    solrQuery.setGetFieldStatistics(true);
    solrQuery.setGetFieldStatistics(statsField);

    QueryResponse rsp = solrServer.query(solrQuery,METHOD.POST);	  	  
    log.info("Stats call part 1:"+(System.currentTimeMillis()-start));
    results += rsp.getResults().getNumFound();
    if (rsp.getResults().getNumFound() != 0 ){
      domain=  (String) rsp.getResults().get(0).getFieldValue("domain");
      final FieldStatsInfo fieldStats = rsp.getFieldStatsInfo().get(statsField);       
      if (fieldStats!= null){
        stats.setLastHarvestDate(DateUtils.getSolrDate((Date)fieldStats.getMax()));        
        String next = DateUtils.getSolrDate((Date)fieldStats.getMin());            
        if (!crawlDate.equals(next)){
          stats.setNextHarvestDate(next);//Dont want same as next          
        }        
      }
    }

    
    solrQuery = new SolrQuery("(url_norm:\""+url_norm+"\") AND crawl_date:[* TO \""+crawlDate+"\"}");                
    solrQuery.setRows(1);
    solrQuery.add("fl","domain");
    solrQuery.setGetFieldStatistics(true);
    solrQuery.setGetFieldStatistics(statsField);


    rsp = solrServer.query(solrQuery,METHOD.POST);          
    log.info("Stats call part 2:"+(System.currentTimeMillis()-start));
    results += rsp.getResults().getNumFound();
    if (rsp.getResults().getNumFound() != 0 ){
      domain=  (String) rsp.getResults().get(0).getFieldValue("domain");
      final FieldStatsInfo fieldStats = rsp.getFieldStatsInfo().get(statsField);       
      if (fieldStats != null){
        stats.setFirstHarvestDate( DateUtils.getSolrDate((Date)fieldStats.getMin()));        
        String previous =  DateUtils.getSolrDate((Date)fieldStats.getMax());
        if (!crawlDate.equals(previous)){ //Dont want same as previous
          stats.setPreviousHarvestDate(previous);
        }        
      }      
    }

    stats.setNumberOfHarvest(results+1); //The +1 is the input value that is not included in any of the two result sets.

    if (domain == null){      
      //This can happen if we only have 1 harvest. It will not be include in the {x,*] og [*,x } since x is not included
      solrQuery = new SolrQuery("url_norm:\""+url_norm+"\"");            
      solrQuery.setRows(1);
      solrQuery.setGetFieldStatistics(true);
      solrQuery.setGetFieldStatistics(statsField);

      rsp = solrServer.query(solrQuery,METHOD.POST);
      if (rsp.getResults().size() == 0){        
        return stats; //url never found. 
      }
      domain=(String) rsp.getResults().get(0).getFieldValue("domain");    
    }    
    stats.setDomain(domain);
    solrQuery = new SolrQuery("domain:\""+domain+"\"");            
    solrQuery.setRows(0);
    solrQuery.setGetFieldStatistics(true);
    solrQuery.setGetFieldStatistics("content_length");


    rsp = solrServer.query(solrQuery,METHOD.POST);
    log.info("Stats call part 3:"+(System.currentTimeMillis()-start));
    long numberHarvestDomain= rsp.getResults().getNumFound();
    stats.setNumberHarvestDomain(numberHarvestDomain);
    if (numberHarvestDomain != 0 ){    
      final FieldStatsInfo fieldStats = rsp.getFieldStatsInfo().get("content_length");       
      if (fieldStats != null){        
        double totalContentLength = (Double) fieldStats.getSum();
        stats.setDomainHarvestTotalContentLength((long) totalContentLength);         
      }      
    }
    log.info("total time getting wayback statistics for solrdate:"+crawlDate +" was :"+(System.currentTimeMillis()-start)); 
    return stats;

  }


  public ArrayList<ArcEntryDescriptor> findImagesForTimestamp(String searchString, String timeStamp) throws Exception {    
    ArrayList<ArcEntryDescriptor> images= new ArrayList<>();

    SolrQuery solrQuery = new SolrQuery();
    solrQuery.setQuery(searchString); // only search images
    solrQuery.setRows(50); //get 50 images...

    solrQuery.set("facet", "false"); //very important. Must overwrite to false. Facets are very slow and expensive.
    solrQuery.add("group","true");       
    solrQuery.add("group.field","url_norm");
    solrQuery.add("group.sort","abs(sub(ms("+timeStamp+"), crawl_date)) asc");
    solrQuery.setFilterQueries("content_type_norm:image"); //only images
    solrQuery.setFilterQueries("record_type:response"); //No binary for revists.       //TODO record_type arc missing!

    solrQuery.setFilterQueries("image_size:[2000 TO *]"); //No small images. (fillers etc.) 
    solrQuery.add("fl", indexDocFieldList);

    QueryResponse rsp = solrServer.query(solrQuery,METHOD.POST);

    if (rsp.getGroupResponse()== null){
      // log.info("no images found for search:"+searchString);
      return images;
    }

    List<Group> values = rsp.getGroupResponse().getValues().get(0).getValues();
    for (Group current:values){
      SolrDocumentList docs = current.getResult();
      ArrayList<IndexDoc> groupDocs = solrDocList2IndexDoc(docs);
      String source_file_path= groupDocs.get(0).getSource_file_path();
      ArcEntryDescriptor desc= new ArcEntryDescriptor();
      desc.setUrl(groupDocs.get(0).getUrl());
      desc.setUrl_norm(groupDocs.get(0).getUrl_norm());
      desc.setSource_file_path(source_file_path);
      desc.setHash(groupDocs.get(0).getHash());
      desc.setOffset(groupDocs.get(0).getOffset());
      desc.setContent_type(groupDocs.get(0).getMimeType());

      images.add(desc);
    }                              

    //    log.info("resolve images:" + searchString + " found:" + images.size());
    return images;
  }


  public SearchResult search(String searchString, int results) throws Exception {
    return search(searchString,null,results);
  }

  public SearchResult search(String searchString, String filterQuery) throws Exception {
    return search(searchString,filterQuery,50);
  }


  public ArrayList<Date> getHarvestTimesForUrl(String url) throws Exception {
    ArrayList<Date> dates = new ArrayList<Date>();
    String urlNormFixed = normalizeUrl(url);    
    SolrQuery solrQuery = new SolrQuery();
    solrQuery = new SolrQuery("url_norm:\""+urlNormFixed+"\"");     
    solrQuery.set("facet", "false"); //very important. Must overwrite to false. Facets are very slow and expensive.
    solrQuery.add("fl","id,crawl_date");    
    solrQuery.setRows(1000000);

    log.info("HarvestTimeForUrl query:"+solrQuery);
    QueryResponse rsp = solrServer.query(solrQuery,METHOD.POST);
    SolrDocumentList docs = rsp.getResults();

    for (SolrDocument doc : docs) {
      Date date = (Date) doc.get("crawl_date");    
      dates.add(date);
    }           
    return dates;
  }


  

  public String getTextForDomain(String domain) throws Exception {    
    SolrQuery solrQuery = new SolrQuery();
    solrQuery = new SolrQuery("(domain:\""+domain+"\"");     

    solrQuery.add("fl","id, content_text_length, content" );        
    solrQuery.setFilterQueries("content_type_norm:html" , "content_text_length:[1000 TO *]"); //only html pages and pages with many words.
    solrQuery.setRows(10000);

    QueryResponse rsp = solrServer.query(solrQuery,METHOD.POST);
    SolrDocumentList docs = rsp.getResults();
    StringBuilder b= new StringBuilder();
    long totaltLength=0;
    for (SolrDocument doc : docs){
      b.append(doc.getFieldValue("content"));
      b.append(doc.getFieldValue(" "));//Space between next document.
      totaltLength +=  ((int)  doc.getFieldValue("content_text_length"));
    }    
    log.info("Total extracted content length for wordCloud:"+totaltLength +" total hits:"+rsp.getResults().getNumFound()  +" only using first 10000 hits");
    return b.toString();
  }
  
  public ArrayList<IndexDoc> getHarvestPreviewsForUrl(String url) throws Exception {

    String urlNormFixed = normalizeUrl(url);    
    SolrQuery solrQuery = new SolrQuery();
    solrQuery = new SolrQuery("(url_norm:\""+urlNormFixed+"\"");     
    solrQuery.set("facet", "false"); //very important. Must overwrite to false. Facets are very slow and expensive.
    solrQuery.add("fl","id, crawl_date,source_file_path, source_file, source_file_offset, score");    
    solrQuery.add("sort","crawl_date asc");
    solrQuery.setRows(1000000);

    QueryResponse rsp = solrServer.query(solrQuery,METHOD.POST);
    SolrDocumentList docs = rsp.getResults();

    ArrayList<IndexDoc> indexDocs  = solrDocList2IndexDoc(docs);                   
    return indexDocs;
  }



  public IndexDoc getArcEntry(String source_file_path, long offset) throws Exception {

    SolrQuery solrQuery = new SolrQuery();
    solrQuery.set("facet", "false"); //very important. Must overwrite to false. Facets are very slow and expensive.
    solrQuery.add("fl", indexDocFieldList);   

    String query = null;
        
    //This is due to windows path in solr field source_file_offset. For linux the escape does nothing
    String pathEscaped= ClientUtils.escapeQueryChars(source_file_path);
    
    query = "source_file_path:\""+pathEscaped+"\" AND source_file_offset:"+offset ;         
    log.info("getArcEntry query:"+ query);    
    solrQuery.setQuery(query) ;
    solrQuery.setRows(1);

    QueryResponse rsp = solrServer.query(solrQuery,METHOD.POST);
    SolrDocumentList docs = rsp.getResults();

    if (docs.getNumFound() == 0){
      throw new Exception("Could not find arc entry in index:"+source_file_path +" offset:"+offset);
    }

    ArrayList<IndexDoc> indexDocs = solrDocList2IndexDoc(docs);

    return indexDocs.get(0);
  }

  /*

  public SearchResult imageLocationSearch(String searchString, int results) throws Exception {
    log.info("imageLocationsearch for:" + searchString);
    SearchResult result = new SearchResult();
    SolrQuery solrQuery = new SolrQuery();
    solrQuery.set("facet", "false"); //very important. Must overwrite to false. Facets are very slow and expensive.
    solrQuery.add("fl", indexDocFieldList);
    solrQuery.setQuery(searchString); // only search images
    solrQuery.setRows(results);    
    solrQuery.setFilterQueries( +" and filter:"+filterQuery););


    QueryResponse rsp = solrServer.query(solrQuery,METHOD.POST);
    SolrDocumentList docs = rsp.getResults();


    result.setNumberOfResults(docs.getNumFound());
    ArrayList<IndexDoc> indexDocs = solrDocList2IndexDoc(docs);
    result.setResults(indexDocs);
    log.info("search for:" + searchString + " found:" + result.getNumberOfResults());
    return result;
  }

   */




  public ArrayList<IndexDoc> imagesLocationSearch(String searchText, String filterQuery,int results,double latitude, double longitude, double radius) throws Exception {
    log.info("imagesLocationSearch:" + searchText +" coordinates:"+latitude+","+longitude +" radius:"+radius);

    SolrQuery solrQuery = new SolrQuery();
    solrQuery.set("facet", "false"); //very important. Must overwrite to false. Facets are very slow and expensive.
    solrQuery.add("fl", indexDocFieldList);
    solrQuery.add("group","true");       
    solrQuery.add("group.field","hash"); //Notice not using url_norm. We want really unique images.
    solrQuery.add("group.format","simple");
    solrQuery.add("group.limit","1");    
    solrQuery.setRows(results);
    //The 3 lines defines geospatial search. The ( ) are required if you want to AND with another query
    solrQuery.setQuery("({!geofilt sfield=exif_location}) AND "+searchText);       
    solrQuery.setParam("pt", latitude+","+longitude);
    solrQuery.setParam("d", ""+radius);

    if (filterQuery != null){
      solrQuery.setFilterQueries(filterQuery);
    }

    QueryResponse rsp = solrServer.query(solrQuery);


    //SolrDocumentList docs = rsp.getResults();
    SolrDocumentList docs =  rsp.getGroupResponse().getValues().get(0).getValues().get(0).getResult();     
    ArrayList<IndexDoc> indexDocs = solrDocList2IndexDoc(docs);
   
    return indexDocs;
  }


  public SearchResult search(String searchString, String filterQuery, int results) throws Exception {
    SearchResult result = new SearchResult();
    SolrQuery solrQuery = new SolrQuery();
    solrQuery.set("facet", "false"); //very important. Must overwrite to false. Facets are very slow and expensive.
    solrQuery.add("fl", indexDocFieldList);
    solrQuery.setQuery(searchString); // only search images
    solrQuery.setRows(results);
    if (filterQuery != null){
      solrQuery.setFilterQueries(filterQuery);
    }

    QueryResponse rsp = solrServer.query(solrQuery,METHOD.POST);
    SolrDocumentList docs = rsp.getResults();


    result.setNumberOfResults(docs.getNumFound());
    ArrayList<IndexDoc> indexDocs = solrDocList2IndexDoc(docs);
    result.setResults(indexDocs);
    return result;
  }
  

  public long numberOfDocuments() throws Exception {    
    SolrQuery solrQuery = new SolrQuery();
    solrQuery.setQuery("*:*"); 
    QueryResponse rsp = solrServer.query(solrQuery,METHOD.POST);
    SolrDocumentList docs = rsp.getResults();
    return docs.getNumFound();    
  }
  
  
  public ArrayList<IndexDoc> findNearestHarvestTimeForMultipleUrls(HashSet<String> urls, String timeStamp) throws Exception{
    ArrayList<IndexDoc>  allDocs = new ArrayList<IndexDoc>();
    Iterable<List<String>> splitSets = Iterables.partition(urls, 1000); //split into sets of size max 1000;
    for (List<String> set : splitSets){
      HashSet<String> urlPartSet = new  HashSet<String>();
      urlPartSet.addAll(set);
      List<IndexDoc> partIndexDocs= findNearestHarvestTimeForMultipleUrlsMax1000(urlPartSet, timeStamp);
      allDocs.addAll(partIndexDocs);
    }               
    return allDocs;         
}

  private List<IndexDoc> findNearestHarvestTimeForMultipleUrlsMax1000(HashSet<String> urls, String timeStamp) throws Exception{
    SolrDocumentList docs = findNearestDocuments(urls, timeStamp, indexDocFieldList);

    ArrayList<IndexDoc>  allDocs = new ArrayList<IndexDoc>(docs.size());
    for (SolrDocument current:docs){
      IndexDoc groupDoc = solrDocument2IndexDoc(current);
      allDocs.add(groupDoc);                             
    }                    

    return allDocs;
  }

  public SolrDocumentList findNearestDocuments(HashSet<String> urls, String timeStamp, String fieldList)
      throws SolrServerException, IOException {
final int chunkSize = 1000;

if (urls.size() > chunkSize){
  SolrDocumentList allDocs = new SolrDocumentList();
  Iterable<List<String>> splitSets = Iterables.partition(urls, chunkSize); //split into sets of size max chunkSize;
  for (List<String> chunk : splitSets){
    SolrDocumentList chunkDocs = findNearestDocuments(new HashSet<>(chunk), timeStamp, fieldList);
    mergeInto(allDocs, chunkDocs);
    // What is allDocs.start and should we care?
  }
  return allDocs;
}

SolrQuery solrQuery = new SolrQuery();
solrQuery.setQuery(urlQueryJoin("url_norm", "OR", urls));

solrQuery.setFacet(false);
solrQuery.setGetFieldStatistics(false);
solrQuery.setRows(urls.size());
solrQuery.set("group", "true");
solrQuery.set("group.field", "url_norm");
solrQuery.set("group.size", "1");
solrQuery.set("group.sort","abs(sub(ms("+timeStamp+"), crawl_date)) asc");
solrQuery.add("fl", fieldList);

solrQuery.setFilterQueries("record_type:response OR record_type:arc"); //No binary for revists.

QueryResponse rsp = solrServer.query(solrQuery, METHOD.POST);
SolrDocumentList docs = groupsToDoc(rsp);
log.info("number URLS in search:" +urls.size() +" number of harvested url found:"  + docs.size() +
         " time:"+rsp.getQTime());
return docs;
}

 

  public static void mergeInto(SolrDocumentList main, SolrDocumentList additional) {
    main.addAll(additional);
    if (additional.getMaxScore() != null) {
      main.setMaxScore(main.getMaxScore() == null ? additional.getMaxScore() :
                               Math.max(main.getMaxScore(), additional.getMaxScore()));
    }
    main.setNumFound(main.getNumFound() + additional.getNumFound());
  }

  
    
  private SolrDocumentList groupsToDoc(QueryResponse rsp) {
    SolrDocumentList docs = new SolrDocumentList();
    if (rsp == null || rsp.getGroupResponse() == null || rsp.getGroupResponse().getValues() == null ||
            rsp.getGroupResponse().getValues().isEmpty()) {
      return docs;
    }
    for (GroupCommand groupCommand: rsp.getGroupResponse().getValues()) {
      for (Group group: groupCommand.getValues()) {
        mergeInto(docs, group.getResult());
      }
    }
    return docs;
  }


  @SuppressWarnings("SameParameterValue")
  private String urlQueryJoin(String field, String operator, Iterable<String> urls) {
    StringBuilder sb = new StringBuilder();
    boolean first = true;
    sb.append(field).append(":(");
    for (String url: urls) {
      if (!first) {
        sb.append(" ").append(operator).append(" ");
      }
      first = false;
      sb.append("\"").append(normalizeUrl(url)).append("\"");
    }
    sb.append(")");
    return sb.toString();
  }

  /*
   * Notice here do we not fix url_norm 
   */
  public IndexDoc findClosestHarvestTimeForUrl(String url,String timeStamp) throws Exception {
    log.info("search for:" + url +" for crawldate:"+timeStamp);

    if (url == null || timeStamp == null){
      throw new IllegalArgumentException("harvestUrl or timeStamp is null"); // Can happen for url-rewrites that are not corrected       
    }

    String urlNormFixed = normalizeUrl(url);
    String query = "url_norm:\""+ urlNormFixed +"\"";                
    SolrQuery solrQuery = new SolrQuery();
    solrQuery.setQuery(query);
 
    solrQuery.setFilterQueries("record_type:response OR record_type:arc"); //No binary for revists. 

    solrQuery.set("facet", "false"); //very important. Must overwrite to false. Facets are very slow and expensive.
    solrQuery.add("sort","abs(sub(ms("+timeStamp+"), crawl_date)) asc");
    solrQuery.add("fl", indexDocFieldList);
    //solrQuery.setRows(1);
    //code below is temporary fix for the solr bug. Get the nearest and find which one is nearest.
    //The solr sort is bugged when timestamps are close. The bug is also present in other methods in this class, but not as critical there.
    //Hoping for a solr fix....
    solrQuery.setRows(10);
    
    
    QueryResponse rsp = solrServer.query(solrQuery,METHOD.POST);        

    SolrDocumentList docs = rsp.getResults();
    if (docs == null  || docs.size() ==0){
      return null;
    }
    ArrayList<IndexDoc> indexDocs = solrDocList2IndexDoc(docs);              

     //Return the one nearest
    int bestIndex = 0; //This would be correct if solr could sort correct. 
    // Solr uses a precisionsStep you can define in schema.xml if you want precision to seconds. But this is not done in warc-indexer 3.0 schema.
    //Instead we extract the top 10 and find the nearest but checking against all.

    
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"); 
    long inputCrawlDate= dateFormat.parse(timeStamp).getTime(); //From the input
    long bestMatchDifference = Long.MAX_VALUE;
    
    for (int i =0 ;i<indexDocs.size() ;i++){           
            
      IndexDoc doc = indexDocs.get(i);

      //small hack to make sure http/https not are mixed. Protocol is not into the schema yet. Would be nice if protocol was a field in schema
      if ( (url.startsWith("http://") && doc.getUrl().startsWith("http://")) ||  (url.startsWith("https://") && doc.getUrl().startsWith("https://") ) ) {       
        //log.info("same protocol:"+url + ":"+doc.getUrl());        
      }
      else{        
        //Not a problem just need to see how often it happens for now.
        log.info("Same url has been harvests for both HTTP and HTTPS: "+url + " and "+doc.getUrl());
        continue; //Skip
      }      
      
      String crawlDateDoc = doc.getCrawlDate();      
      long crawlDateForDocument=  dateFormat.parse(crawlDateDoc).getTime();  //For this document      
      long thisMatch = Math.abs(inputCrawlDate - crawlDateForDocument);
      if (thisMatch<bestMatchDifference){
        bestIndex = i;
        bestMatchDifference = thisMatch;      
      }            
    }
    
    if (bestIndex != 0){
      log.warn("Fixed Solr time sort bug, found a better match, # result:"+bestIndex);     
    }    
    return indexDocs.get(bestIndex);     
  }



  public HashMap<Integer, Long> getYearHtmlFacets(String query) throws Exception {
    //facet=true&facet.field=crawl_year&facet.sort=index&facet.limit=500    
    if (!TAGS_VALID_PATTERN.matcher(query).matches()) {
      throw new InvalidArgumentServiceException("Tag syntax not accepted:"+query);        
    }


    SolrQuery solrQuery = new SolrQuery();
    solrQuery.setQuery("elements_used:\""+query+"\""); 
    solrQuery.setFilterQueries("content_type_norm:html"); //only html pages
    solrQuery.setRows(0); //1 page only
    solrQuery.add("fl","id");//rows are 0 anyway
    solrQuery.set("facet", "true");
    solrQuery.set("facet.field", "crawl_year");
    solrQuery.set("facet.sort", "index");
    solrQuery.set("facet.limit", "500"); //500 is higher than number of different years



    QueryResponse rsp = solrServer.query(solrQuery,METHOD.POST);

    FacetField facetField = rsp.getFacetField("crawl_year");

    HashMap<Integer, Long> allCount = new HashMap<Integer, Long>();

    for (FacetField.Count c :facetField.getValues()){
      allCount.put(Integer.parseInt(c.getName()), c.getCount());                    
    }        
    return allCount;  
  }

  public HashMap<Integer, Long> getYearFacetsHtmlAll() throws Exception {
    //facet=true&facet.field=crawl_year&facet.sort=index&facet.limit=500    

    SolrQuery solrQuery = new SolrQuery();
    solrQuery.setQuery("*:*"); 
    solrQuery.setFilterQueries("content_type_norm:html"); //only html pages
    solrQuery.setRows(0); //1 page only
    solrQuery.add("fl","id");//rows are 0 anyway
    solrQuery.set("facet", "true");
    solrQuery.set("facet.field", "crawl_year");
    solrQuery.set("facet.sort", "index");
    solrQuery.set("facet.limit", "500"); //500 is higher than number of different years

    QueryResponse rsp = solrServer.query(solrQuery,METHOD.POST);

    FacetField facetField = rsp.getFacetField("crawl_year");

    HashMap<Integer, Long> allCount = new HashMap<Integer, Long>();

    for (FacetField.Count c :facetField.getValues()){
      allCount.put(Integer.parseInt(c.getName()), c.getCount());                    
    }        
    return allCount;  
  }



  public HashMap<Integer, Long> getYearTextHtmlFacets(String query) throws Exception {
    //facet=true&facet.field=crawl_year&facet.sort=index&facet.limit=500    


    /*
      if (!OK.matcher(query).matches()) {
          throw new InvalidArgumentServiceException("Tag syntax not accepted:"+query);        
        }
     */

    SolrQuery solrQuery = new SolrQuery();
    solrQuery.setQuery("text:\""+query+"\""); 
    solrQuery.setFilterQueries("content_type_norm:html"); //only html pages
    solrQuery.setRows(0); //1 page only
    solrQuery.add("fl","id");//rows are 0 anyway
    solrQuery.set("facet", "true");
    solrQuery.set("facet.field", "crawl_year");
    solrQuery.set("facet.sort", "index");
    solrQuery.set("facet.limit", "500"); //500 is higher than number of different years



    QueryResponse rsp = solrServer.query(solrQuery,METHOD.POST);

    FacetField facetField = rsp.getFacetField("crawl_year");

    HashMap<Integer, Long> allCount = new HashMap<Integer, Long>();

    for (FacetField.Count c :facetField.getValues()){
      allCount.put(Integer.parseInt(c.getName()), c.getCount());                    
    }        
    return allCount;  
  }


  public ArrayList<IndexDoc> findNearestForResourceNameAndDomain(String domain, String resourcename,String timeStamp) throws Exception{    
    String searchString="domain:\""+domain+"\" AND resourcename:\""+resourcename+"\"";
    SolrQuery solrQuery = new SolrQuery();
    solrQuery.setQuery(searchString); 
    solrQuery.set("facet", "false"); 
    solrQuery.set("group", "true");
    solrQuery.set("group.field", "domain");
    solrQuery.set("group.size", "10");
    solrQuery.set("group.sort","abs(sub(ms("+timeStamp+"), crawl_date)) asc");
    solrQuery.add("fl", indexDocFieldList);
    QueryResponse rsp = solrServer.query(solrQuery, METHOD.POST);
    SolrDocumentList docs = groupsToDoc(rsp);
    return solrDocList2IndexDoc(docs);    
  }
  
  /*
   * Uses the stats component and hyperloglog for ultra fast performance instead of grouping, which does not work well over many shards.
   * 
   * Extract statistics for a given domain and year.
   * Number of unique pages (very precise due to hyperloglog)
   * Number of ingoing links (very precise due to hyperloglog)
   * Total size (of the unique pages). (not so precise due, tests show max 10% error, less for if there are many pages)
   */
  public DomainYearStatistics domainStatistics(String domain, int year) throws Exception{

    DomainYearStatistics stats = new DomainYearStatistics();
    stats.setYear(year);
    stats.setDomain(domain);

    String searchString="domain:\""+domain+"\"";

    SolrQuery solrQuery = new SolrQuery();

    solrQuery.setQuery(searchString); 
    solrQuery.set("facet", "false"); 
    solrQuery.addFilterQuery("content_type_norm:html AND status_code:200");
    solrQuery.addFilterQuery("crawl_year:"+year);            
    solrQuery.setRows(0);
    solrQuery.add("fl","id");
    solrQuery.add("stats","true");
    solrQuery.add("stats.field","{!count=true cardinality=true}url_norm"); //Important, use cardinality and not unique.
    solrQuery.add("stats.field","{!sum=true}content_length");

    QueryResponse rsp = solrServer.query(solrQuery);  

    Map<String, FieldStatsInfo> statsMap = rsp.getFieldStatsInfo();
    FieldStatsInfo statsUrl_norm = statsMap.get("url_norm");
    long url_norm_cardinality = statsUrl_norm.getCardinality();
    long url_norm_total = statsUrl_norm.getCount();         

    FieldStatsInfo statsContent_length = statsMap.get("content_length");
    Double sum = (Double) statsContent_length.getSum();


    //estimate content_length for the uniqie pages by fraction of total.
    double size = sum*(url_norm_cardinality*1d/url_norm_total)*1d/1024d;                  
    stats.setSizeInKb((int)size);         
    stats.setTotalPages((int)url_norm_cardinality); 

    //Links         
    solrQuery = new SolrQuery();
    solrQuery.setQuery("links_domains:\""+domain+"\" -"+searchString); //links to, but not from same domain   
    solrQuery.addFilterQuery("content_type_norm:html AND status_code:200");
    solrQuery.addFilterQuery("crawl_year:"+year);            
    solrQuery.setRows(0);
    solrQuery.add("stats","true");
    solrQuery.add("fl","id");
    solrQuery.add("stats.field","{!cardinality=true}domain"); //Important, use cardinality and not unique.

    rsp = solrServer.query(solrQuery);
    Map<String, FieldStatsInfo> stats2 = rsp.getFieldStatsInfo();


    FieldStatsInfo statsLinks = stats2.get("domain");         
    long links_cardinality = statsLinks.getCardinality();         
    stats.setIngoingLinks((int)links_cardinality);         
    return stats;
  }



  private static ArrayList<IndexDoc> solrDocList2IndexDoc(SolrDocumentList docs) {
    ArrayList<IndexDoc> earchives = new ArrayList<IndexDoc>();
    for (SolrDocument current : docs) {
      earchives.add(solrDocument2IndexDoc(current));
    }
    return earchives;
  }

  private static IndexDoc solrDocument2IndexDoc(SolrDocument doc) {
    IndexDoc indexDoc = new IndexDoc();
    indexDoc.setScore(Double.valueOf((float) doc.getFieldValue("score")));
    indexDoc.setId((String) doc.get("id"));
    indexDoc.setTitle((String) doc.get("title"));
    indexDoc.setSource_file_path((String) doc.get("source_file_path"));
    indexDoc.setResourceName((String) doc.get("resourcename"));        
    indexDoc.setDomain((String) doc.get("domain"));
    indexDoc.setUrl((String) doc.get("url"));
    indexDoc.setUrl_norm((String) doc.get("url_norm"));
    indexDoc.setOffset(getOffset(doc));
    indexDoc.setContentType((String) doc.get("content_type"));    
    indexDoc.setContentTypeNorm((String) doc.get("content_type_norm"));
    indexDoc.setContentEncoding((String) doc.get("content_encoding"));
    indexDoc.setType((String) doc.get("type"));
    indexDoc.setExifLocation((String) doc.get("exif_location"));
    Object statusCodeObj = doc.get("status_code");
    if (statusCodeObj != null){         
      indexDoc.setStatusCode((Integer) statusCodeObj);     
    }
    String hash = (String) doc.get("hash");
    indexDoc.setHash((String) hash);      

    Date date = (Date) doc.get("crawl_date");        
    indexDoc.setCrawlDateLong(date.getTime());
    indexDoc.setCrawlDate(DateUtils.getSolrDate(date));

    indexDoc.setMimeType((String) doc.get("content_type"));         

    indexDoc.setOffset(getOffset(doc));

    Object o =  doc.getFieldValue("links_images");
    if (o != null){
      indexDoc.setImageUrls((ArrayList<String>) o);
    }

    return indexDoc;
  }

  //TO, remove method and inline 
  public static long getOffset(SolrDocument doc){
    return  (Long) doc.get("source_file_offset");

  }


  private static String normalizeUrl(String url){
    return Normalisation.canonicaliseURL(url);          
  }

  

}
