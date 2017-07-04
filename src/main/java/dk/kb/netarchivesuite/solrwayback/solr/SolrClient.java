package dk.kb.netarchivesuite.solrwayback.solr;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.TimeZone;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.impl.BinaryRequestWriter;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.FieldStatsInfo;
import org.apache.solr.client.solrj.response.Group;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.collect.Iterables;

import dk.kb.netarchivesuite.solrwayback.export.GenerateCSV;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDoc;
import dk.kb.netarchivesuite.solrwayback.service.dto.SearchResult;
import dk.kb.netarchivesuite.solrwayback.service.dto.WeightedArcEntryDescriptor;

public class SolrClient {

  private static final Logger log = LoggerFactory.getLogger(SolrClient.class);
  private static HttpSolrClient solrServer;
  private static SolrClient instance = null;

  static {
    SolrClient.initialize(PropertiesLoader.SOLR_SERVER);
  }

  private SolrClient() { // private. Singleton
  }

  // Example url with more than 1000 rewrites: http://belinda:9721/webarchivemimetypeservlet/services/wayback?waybackdata=20140119010303%2Fhttp%3A%2F%2Fbillige-skilte.dk%2F%3Fp%3D35


  public static void initialize(String solrServerUrl) {
    solrServer = new HttpSolrClient(solrServerUrl);
    solrServer.setRequestWriter(new BinaryRequestWriter()); // To avoid http error code 413/414, due to monster URI. (and it is faster)


    instance = new SolrClient();
    log.info("SolrClient initialized with solr server url:" + solrServerUrl);
  }

  public static SolrClient getInstance() {
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

    
    String dateStart= getSolrTimeStamp(crawlDateStart);
    String dateEnd = getSolrTimeStamp(crawlDateEnd);
    
    
    SolrQuery solrQuery = new SolrQuery();
    solrQuery.setQuery("links_domains:\""+domain+"\" AND -domain:\""+domain+"\"");
    solrQuery.setRows(0);
    solrQuery.set("facet", "true");       
    solrQuery.add("facet.field","domain");
    solrQuery.add("facet.limit",""+facetLimit);
    solrQuery.addFilterQuery("crawl_date:["+dateStart+ " TO "+dateEnd+"]");
    
    solrQuery.add("fl","id,score,title,arc_full,url, url_norm,source_file_s,content_type_norm,hash,crawl_date,content_type, content_encoding"); //only request fields used

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


    String dateStart= getSolrTimeStamp(crawlDateStart);
    String dateEnd = getSolrTimeStamp(crawlDateEnd);
            
    SolrQuery solrQuery = new SolrQuery();
    solrQuery.setQuery("domain:\""+domain+"\"");  

    solrQuery.setRows(0);
    solrQuery.set("facet", "true");       
    solrQuery.add("facet.field","links_domains");
    solrQuery.add("facet.limit",""+(facetLimit+1)); //+1 because itself will be removed and is almost certain of resultset if self-linking
    solrQuery.addFilterQuery("crawl_date:["+dateStart+ " TO "+dateEnd+"]");
    solrQuery.add("fl","id,score,title,arc_full,url, url_norm,source_file_s,content_type_norm,hash,crawl_date,content_type, content_encoding"); //only request fields used

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
  public WaybackStatistics getWayBackStatistics(String url_norm, String crawlDate)  throws Exception{
    WaybackStatistics stats = new  WaybackStatistics();
    stats.setUrl_norm(url_norm);
    //These will only be set if they are different from input (end points). So set them below
    stats.setLastHarvestDate(crawlDate);
    stats.setFirstHarvestDate(crawlDate);

    //We query for 1 result to get the domain.
    String domain = null;

    stats.setHarvestDate(crawlDate);
    log.info("Getting wayback statistics for solrdate:"+crawlDate); 
    final String statsField= "crawl_date";

    int results=0;

    String urlNormFixed = fixUrlNormQuery(url_norm);
    String query = "(url:\""+url_norm+"\" OR "+ urlNormFixed +") AND crawl_date:{\""+crawlDate+"\" TO *]";    
    
    SolrQuery solrQuery = new SolrQuery(query);            
    log.info("solrQuery2:"+solrQuery.toQueryString());
    
    solrQuery.setRows(1);
    solrQuery.setGetFieldStatistics(true);
    solrQuery.setGetFieldStatistics(statsField);

    QueryResponse rsp = solrServer.query(solrQuery,METHOD.POST);	  	  
    results += rsp.getResults().getNumFound();
    if (rsp.getResults().getNumFound() != 0 ){
      domain=  (String) rsp.getResults().get(0).getFieldValue("domain");
      final FieldStatsInfo fieldStats = rsp.getFieldStatsInfo().get(statsField);       
      if (fieldStats!= null){
        stats.setLastHarvestDate(getSolrTimeStamp((Date)fieldStats.getMax()));        
        String next = getSolrTimeStamp((Date)fieldStats.getMin());            
        if (!crawlDate.equals(next)){
          stats.setNextHarvestDate(next);//Dont want same as next
        }        
      }
    }

     urlNormFixed = fixUrlNormQuery(url_norm);    
    solrQuery = new SolrQuery("(url:\""+url_norm+"\" OR "+urlNormFixed +") AND crawl_date:[* TO \""+crawlDate+"\"}");            
    log.info("solrQuery3:"+solrQuery.toQueryString());
    solrQuery.setRows(1);
    solrQuery.setGetFieldStatistics(true);
    solrQuery.setGetFieldStatistics(statsField);


    rsp = solrServer.query(solrQuery,METHOD.POST);          
    results += rsp.getResults().getNumFound();
    if (rsp.getResults().getNumFound() != 0 ){
      domain=  (String) rsp.getResults().get(0).getFieldValue("domain");
      final FieldStatsInfo fieldStats = rsp.getFieldStatsInfo().get(statsField);       
      if (fieldStats != null){
        stats.setFirstHarvestDate( getSolrTimeStamp((Date)fieldStats.getMin()));        
        String previous =  getSolrTimeStamp((Date)fieldStats.getMax());
        if (!crawlDate.equals(previous)){ //Dont want same as previous
          stats.setPreviousHarvestDate(previous);
        }        
      }      
    }

    stats.setNumberOfHarvest(results+1); //The +1 is the input value that is not included in any of the two result sets.

    if (domain == null){      
      //This can happen if we only have 1 harvest. It will not be include in the {x,*] og [*,x } since x is not included
      solrQuery = new SolrQuery("(url:\""+url_norm+"\" OR "+urlNormFixed +")");            
      log.info(solrQuery.toString());
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
    log.info(solrQuery.toString());
    solrQuery.setRows(1);
    solrQuery.setGetFieldStatistics(true);
    solrQuery.setGetFieldStatistics("content_length");


    rsp = solrServer.query(solrQuery,METHOD.POST);          
    long numberHarvestDomain= rsp.getResults().getNumFound();
    stats.setNumberHarvestDomain(numberHarvestDomain);
    if (numberHarvestDomain != 0 ){    
      final FieldStatsInfo fieldStats = rsp.getFieldStatsInfo().get("content_length");       
      if (fieldStats != null){        
        double totalContentLength = (Double) fieldStats.getSum();
        stats.setDomainHarvestTotalContentLength((long) totalContentLength);         
      }      
    }

    return stats;

  }


  public ArrayList<WeightedArcEntryDescriptor> findImageForTimestamp(
      String searchString, String timeStamp) throws Exception {
    log.info("findImageForTimestamp:" + searchString +" and timeStamp:"+timeStamp);
    ArrayList<WeightedArcEntryDescriptor> images= new ArrayList<>();

    SolrQuery solrQuery = new SolrQuery();
    solrQuery.setQuery(searchString); // only search images
    solrQuery.setRows(50); //get 50 images...

    solrQuery.set("facet", "false"); //very important. Must overwrite to false. Facets are very slow and expensive.
    solrQuery.add("group","true");       
    solrQuery.add("group.field","url_norm");
    solrQuery.add("group.sort","abs(sub(ms("+timeStamp+"), crawl_date)) asc");
    solrQuery.add("fl","id,score,title,arc_full,url,url_norm,source_file_s,content_type_norm,hash,crawl_date,content_type,content_encoding"); //only request fields used

    QueryResponse rsp = solrServer.query(solrQuery,METHOD.POST);

    if (rsp.getGroupResponse()== null){
      log.info("no images found for search:"+searchString);
      return images;
    }


    List<Group> values = rsp.getGroupResponse().getValues().get(0).getValues();
    for (Group current:values){
      SolrDocumentList docs = current.getResult();
      ArrayList<IndexDoc> groupDocs = solrDocList2IndexDoc(docs);

      String source_file_s = groupDocs.get(0).getSource_file_s();//always only 1 due to group
      String arcFull = groupDocs.get(0).getArc_full();
      WeightedArcEntryDescriptor desc= new WeightedArcEntryDescriptor();
      desc.setUrl(groupDocs.get(0).getUrl());
      desc.setArcFull(arcFull);
      desc.setSource_file_s(source_file_s);
      desc.setHash(groupDocs.get(0).getHash());
      desc.setOffset(getOffset(source_file_s));
      desc.setContent_type(groupDocs.get(0).getMimeType());

      images.add(desc);
    }                              

    log.info("resolve images:" + searchString + " found:" + images.size());
    return images;
  }


  public SearchResult search(String searchString, int results) throws Exception {
    return search(searchString,null,results);
  }

  public SearchResult search(String searchString, String filterQuery) throws Exception {
    return search(searchString,filterQuery,50);
  }

  
  public ArrayList<Date> getHarvestTimesForUrl(String url) throws Exception {
    System.out.println("harvesttimes for url:"+url);
    ArrayList<Date> dates = new ArrayList<Date>();
    String urlNormFixed = fixUrlNormQuery(url);    
    SolrQuery solrQuery = new SolrQuery();
    solrQuery = new SolrQuery("(url:\""+url+"\" OR "+urlNormFixed+")");     
    log.info("solrQuery1:"+solrQuery.toQueryString());
    solrQuery.set("facet", "false"); //very important. Must overwrite to false. Facets are very slow and expensive.
    solrQuery.add("fl","id, crawl_date");    
    solrQuery.setRows(1000000);
    

    QueryResponse rsp = solrServer.query(solrQuery,METHOD.POST);
    SolrDocumentList docs = rsp.getResults();

    for (SolrDocument doc : docs) {
      Date date = (Date) doc.get("crawl_date");    
      dates.add(date);
    }           
    return dates;
  }


  public ArrayList<IndexDoc> getHarvestPreviewsForUrl(String url) throws Exception {
       
    String urlNormFixed = fixUrlNormQuery(url);    
    SolrQuery solrQuery = new SolrQuery();
    solrQuery = new SolrQuery("(url:\""+url+"\" OR "+urlNormFixed+")");     
    log.info("solrQuery1:"+solrQuery.toQueryString());
    solrQuery.set("facet", "false"); //very important. Must overwrite to false. Facets are very slow and expensive.
    solrQuery.add("fl","id, crawl_date,arc_full,source_file_s,score");    
    solrQuery.add("sort","crawl_date asc");
    solrQuery.setRows(1000000);
       
    QueryResponse rsp = solrServer.query(solrQuery,METHOD.POST);
    SolrDocumentList docs = rsp.getResults();
       
    ArrayList<IndexDoc> indexDocs  = solrDocList2IndexDoc(docs);                   
    return indexDocs;
  }

  
  
  public IndexDoc getArcEntry(String source_file_s) throws Exception {
   
    SolrQuery solrQuery = new SolrQuery();
    solrQuery.set("facet", "false"); //very important. Must overwrite to false. Facets are very slow and expensive.
    solrQuery.add("fl","id, score, title, arc_full,url,url_norm,source_file_s,content_type_norm, hash,crawl_date,content_type,content_encoding");
    solrQuery.setQuery("source_file_s:\""+ source_file_s +"\"") ;
    solrQuery.setRows(1);

    QueryResponse rsp = solrServer.query(solrQuery,METHOD.POST);
    SolrDocumentList docs = rsp.getResults();

    if (docs.getNumFound() == 0){
      throw new Exception("Could not find arc entry:"+source_file_s);
    }

    ArrayList<IndexDoc> indexDocs = solrDocList2IndexDoc(docs);
   
    return indexDocs.get(0);
  }

  
  
  public SearchResult search(String searchString, String filterQuery, int results) throws Exception {
    log.info("search for:" + searchString +" and filter:"+filterQuery);
    SearchResult result = new SearchResult();
    SolrQuery solrQuery = new SolrQuery();
    solrQuery.set("facet", "false"); //very important. Must overwrite to false. Facets are very slow and expensive.
    solrQuery.add("fl","id, score, title, arc_full,url,url_norm,source_file_s,content_type_norm, hash,crawl_date,content_type,content_encoding");
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
    log.info("search for:" + searchString + " found:" + result.getNumberOfResults());
    return result;
  }

  public ArrayList<IndexDoc> findClosetsHarvestTimeForMultipleUrls(HashSet<String> urls, String timeStamp) throws Exception{
    ArrayList<IndexDoc>  allDocs = new ArrayList<IndexDoc>();
    Iterable<List<String>> splitSets = Iterables.partition(urls, 1000); //split into sets of size max 1000;
    for (List<String> set : splitSets){
      HashSet<String> urlPartSet = new  HashSet<String>();
      urlPartSet.addAll(set);
      ArrayList<IndexDoc> partIndexDocs= findClosetsHarvestTimeForMultipleUrlsMax1000(urlPartSet, timeStamp);
      allDocs.addAll(partIndexDocs);
    }				
    return allDocs;			
  }

  /*
   * Notice here do we not fix url_norm 
   */
  private ArrayList<IndexDoc> findClosetsHarvestTimeForMultipleUrlsMax1000(HashSet<String> urls, String timeStamp) throws Exception{

    if (urls.size() > 1000){
      throw new IllegalArgumentException("More than 1000 different urls in query:"+urls.size() +". Solr does not allow more than 1024 queries");
    }

    //Generate URL string: (url:"A" OR url:"B" OR ....)
    StringBuffer buf = new StringBuffer();
    buf.append("(url:test"); //Just to avoid last OR logic
    int i =0;
    for (String  url : urls) {            
      buf.append(" OR url:\""+url+"\"");        

    }
    buf.append(")");


    String  query = buf.toString();		
    SolrQuery solrQuery = new SolrQuery();
    solrQuery.setQuery(query);

    solrQuery.setRows(urls.size());
    solrQuery.add("group","true");       
    solrQuery.add("group.field","url");
    solrQuery.set("facet", "false"); //very important. Must overwrite to false. Facets are very slow and expensive.
    solrQuery.add("sort","abs(sub(ms("+timeStamp+"), crawl_date)) asc");
    solrQuery.add("fl","id,score,title,arc_full,url, url_norm,source_file_s,content_type_norm,hash,crawl_date,content_type, content_encoding"); //only request fields used

    QueryResponse rsp = solrServer.query(solrQuery,METHOD.POST);        

    ArrayList<IndexDoc>  allDocs = new ArrayList<IndexDoc>();
    List<Group> values = rsp.getGroupResponse().getValues().get(0).getValues();
    for (Group current:values){
      SolrDocumentList docs = current.getResult();
      IndexDoc groupDoc = solrDocList2IndexDoc(docs).get(0);
      allDocs.add(groupDoc);                             
    }                    

    log.info("number URLS in search:" +urls.size() +" number of harvested url found:"  +allDocs.size() +" resultset:"+rsp.getGroupResponse().getValues().get(0).getMatches() +" time:"+rsp.getQTime());
    return allDocs;                     
  }



/*
 * Notice here do we not fix url_norm 
 */
  public IndexDoc findClosestHarvestTimeForUrl(String url,String timeStamp) throws Exception {
    log.info("search for:" + url +" for crawldate:"+timeStamp);

    if (url == null || timeStamp == null){
      throw new IllegalArgumentException("harvestUrl or timeStamp is null"); // Can happen for url-rewrites that are not corrected       
    }
    
    String urlNormFixed = fixUrlNormQuery(url);
    String query = "(url:\""+url+"\" OR "+ urlNormFixed +")";//Seems there is an error with url_norm. WWW is not always removed    
               
    SolrQuery solrQuery = new SolrQuery();
    solrQuery.setQuery(query);

    solrQuery.setRows(1); //get 50 images...

    solrQuery.set("facet", "false"); //very important. Must overwrite to false. Facets are very slow and expensive.
    solrQuery.add("sort","abs(sub(ms("+timeStamp+"), crawl_date)) asc");
    solrQuery.add("fl","id,score,title,arc_full,url,url_norm,source_file_s,content_type_norm,hash,crawl_date,content_type, content_encoding"); //only request fields used

    solrQuery.setRows(1);
    QueryResponse rsp = solrServer.query(solrQuery,METHOD.POST);        

    SolrDocumentList docs = rsp.getResults();
    if (docs == null  || docs.size() ==0){
      return null;
    }
    ArrayList<IndexDoc> indexDocs = solrDocList2IndexDoc(docs);              
    return indexDocs.get(0);     
  }

 /* We dont want to return the solr docs outside this class and making VO for them will double memory imprint,.
  * That is why we do all the CSV generation in the client class. For huge result sets there will also be deep paging.
  *  
  */

  public String exportBrief(String query, String filterQuery, int results) throws Exception {
    log.info("export brief:" + query +" and filter:"+filterQuery);
    
    SolrQuery solrQuery = new SolrQuery();
    solrQuery.set("facet", "false"); //very important. Must overwrite to false. Facets are very slow and expensive.
    solrQuery.add("fl","title, arc_full,url,source_file_s,crawl_date,wayback_date");
    solrQuery.setQuery(query); // only search images
    solrQuery.setRows(results);
    if (filterQuery != null){
      solrQuery.setFilterQueries(filterQuery);
    }

    QueryResponse rsp = solrServer.query(solrQuery,METHOD.POST);
    SolrDocumentList docs = rsp.getResults();
    StringBuffer export = new StringBuffer();
    GenerateCSV.generateFirstLineHeader(export);    
    GenerateCSV.generateSecondLineHeader(export, query, filterQuery);
    GenerateCSV.addHeadlineBrief(export);
            
    long numFound = docs.getNumFound();    
    for ( SolrDocument doc : docs){
     GenerateCSV.generateLineBrief(export, doc);
     doc = null;// For garbage collection 
    }    
    
    return export.toString();
  }
  
  
  
  public String exportFull(String query, String filterQuery, int results) throws Exception {
    log.info("export full:" + query +" and filter:"+filterQuery);
    
    SolrQuery solrQuery = new SolrQuery();
    solrQuery.set("facet", "false"); //very important. Must overwrite to false. Facets are very slow and expensive.
    solrQuery.add("fl","title, host, public_suffix, crawl_year, content_type, content_language url, arc_full,url,source_file_s,crawl_date,wayback_date");
    solrQuery.setQuery(query); // only search images
    solrQuery.setRows(results);
    if (filterQuery != null){
      solrQuery.setFilterQueries(filterQuery);
    }

    QueryResponse rsp = solrServer.query(solrQuery,METHOD.POST);
    SolrDocumentList docs = rsp.getResults();
    StringBuffer export = new StringBuffer();
    GenerateCSV.generateFirstLineHeader(export);    
    GenerateCSV.generateSecondLineHeader(export, query, filterQuery);
    GenerateCSV.addHeadlineFull(export);
            
    long numFound = docs.getNumFound();    
    for ( SolrDocument doc : docs){
     GenerateCSV.generateLineFull(export, doc);
     doc = null;// For garbage collection
    }    
            
    return export.toString();
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
    String arc_full =(String) doc.get("arc_full");
    String source_file_s= (String) doc.get("source_file_s");
    indexDoc.setScore(Double.valueOf((float) doc.getFieldValue("score")));
    indexDoc.setId((String) doc.get("id"));
    indexDoc.setTitle((String) doc.get("title"));
    indexDoc.setArc_full(arc_full);
    indexDoc.setUrl((String) doc.get("url"));
    indexDoc.setUrl_norm((String) doc.get("url_norm"));
    indexDoc.setSource_file_s(source_file_s);
    indexDoc.setContentTypeNorm((String) doc.get("content_type_norm"));
    indexDoc.setContentEncoding((String) doc.get("content_encoding"));

    ArrayList<String> hashList =  (ArrayList<String>) doc.get("hash");
    if (hashList != null){
      indexDoc.setHash(hashList.get(0));     
    }
    
    Date date = (Date) doc.get("crawl_date");        
    indexDoc.setCrawlDateLong(date.getTime());
    indexDoc.setCrawlDate(getSolrTimeStamp(date));  //HACK! demo must be ready for lunch

    ArrayList<String> mimeTypes =  (ArrayList<String>) doc.get("content_type");
    if (mimeTypes != null && mimeTypes.size() >0){
      indexDoc.setMimeType(mimeTypes.get(0));        
    }
    indexDoc.setOffset(getOffset(source_file_s));

    return indexDoc;
  }

  public static long getOffset(String source_file_s){
    String[] split = source_file_s.split("@");
    String offset=split[1];
    return Long.valueOf(offset);
  }

  private static String getSolrTimeStamp(Date date){
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"); //not thread safe, so create new         
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));    
    return dateFormat.format(date)+"Z";

  }


  //www,http,https combinations
  private static String fixUrlNormQuery(String url){
    int index = url.indexOf("://");
    String lastPart = url.substring(index+3);

    if (lastPart.startsWith("www.")){
      lastPart=lastPart.substring(4);
    }    
    StringBuffer query = new StringBuffer(); 
           
      query.append("url_norm:\"http://www."+lastPart +"\"");
      query.append(" OR");
      query.append(" url_norm:\"https://www."+lastPart+"\"");
      query.append(" OR");
      query.append(" url_norm:\"https://"+lastPart+"\"");
      query.append(" OR");
      query.append(" url_norm:\"http://"+lastPart+"\"");      
    return  query.toString(); 
  }

  
  
  
}
