package dk.kb.netarchivesuite.solrwayback.solr;

import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeSet;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.impl.BinaryRequestWriter;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.FieldStatsInfo;
import org.apache.solr.client.solrj.response.Group;
import org.apache.solr.client.solrj.response.GroupCommand;
import org.apache.solr.client.solrj.response.GroupResponse;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.service.dto.FacetCount;
import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDoc;
import dk.kb.netarchivesuite.solrwayback.service.dto.SearchResult;

import dk.kb.netarchivesuite.solrwayback.solr.NetarchiveSolrClient;
import dk.kb.netarchivesuite.solrwayback.solr.WaybackStatistics;

public class SolrClientTest {



  //integration tests. Not run as unittests. Require a solr server with data.
  // Just an easy class to test methods in  NetarchiveSolrClient class.
  
  public static void main (String[] args) throws Exception{
    

      NetarchiveSolrClient.initialize("http://localhost:8983/solr/netarchivebuilder/");

    /*
	        String dateSolr ="2015-09-17T17:02:03Z";
	        System.out.println(dateSolr);
	        DateFormat utcFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");    
	        Date date = utcFormat.parse(dateSolr);
	        System.out.println(date);
	        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	        String format = dateFormat.format(date);
	        System.out.println(format+"Z");
     */  	        
    //	        testWaybackStats();
    //testexif();
    //testImagesLocationSearch(); 
    //	        testGetImages();
    //testHarvestTimesForUrl(); 
    //testIngoingLinks();
    //testFacetLinks();
    //testSolrDate();
    //   testHarvestPreviewsForUrl();
    //domainStatistics();
    //testGroup();
  }

  public static void testWaybackStats() throws Exception{

    NetarchiveSolrClient solr = NetarchiveSolrClient.getInstance();


    WaybackStatistics stats = solr.getWayBackStatistics(200,"https://denstoredanske.dk/","http://denstoredanske.dk/", "2008-12-04T18:52:50Z"); //vi

    System.out.println(stats);


  }


  public static void testHarvestTimesForUrl() throws Exception{

    NetarchiveSolrClient solr = NetarchiveSolrClient.getInstance();           
    ArrayList<Date> harvestTimesForUrl = solr.getHarvestTimesForUrl("http://jp.dk/");

    System.out.println(harvestTimesForUrl.size());

  }

  public static void testHarvestPreviewsForUrl() throws Exception{

    NetarchiveSolrClient solr = NetarchiveSolrClient.getInstance();


    ArrayList<IndexDoc> indexDocs = solr.getHarvestPreviewsForUrl(2019,"http://denstoredanske.dk/");                 

    System.out.println(indexDocs.size());
  }


  public static void testexif() throws Exception{

    NetarchiveSolrClient solr = NetarchiveSolrClient.getInstance();


    List<IndexDoc> indexDocs = solr.search("exif_location_0_coordinate:*",6000).getResults();                 





    System.out.println(indexDocs.size());
  }


  // AND content_length:[10000 TO *]
  public static void testGetImages() throws Exception{

    NetarchiveSolrClient solr = NetarchiveSolrClient.getInstance();


    HashSet<String> used = new HashSet<String>();

    FileWriter fw = new FileWriter("out.txt");

    for (int year=2006;year<2017; year++){
      SearchResult search = solr.search("url_norm:http\\:\\/\\/img.ekstrabladet.dk\\/images\\/* AND content_length:[10000 TO *] AND crawl_year:"+year, 100000);
      List<IndexDoc> results = search.getResults();

      for (IndexDoc current : results){
        if (!used.contains(current.getHash())){

          //http://belinda:9721/solrwayback/services/downloadRaw?arcFilePath=/netarkiv/0201/filedir/977-14-20050824040722-00002-sb-prod-har-002.statsbiblioteket.dk.arc&offset=78335287
          String url = "http://belinda:9721/solrwayback/services/downloadRaw?source_file_path="+current.getSource_file_path()+"&offset="+current.getOffset();
          fw.write(url+"\n");
          used.add(current.getHash());
        }
        else{
          System.out.println("skipping:"+current.getHash());
        }
      }

      //        

    }
    fw.close();
  }
  
  public static void testImagesLocationSearch() throws Exception{

    NetarchiveSolrClient solr = NetarchiveSolrClient.getInstance();//pt=56.431,9.431&d=500
    ArrayList<IndexDoc> docs = null;
    docs = solr.imagesLocationSearchWithSort("*:*",null,500, 56.4319d,9.431d , 100 , null);          
    System.out.println(docs.size());
    docs = solr.imagesLocationSearchWithSort("*:*",null,500, 56.4319d,9.431d , 100 , "crawl_date asc");              
  }



  public static void testSolrDate() throws Exception{

    NetarchiveSolrClient solr = NetarchiveSolrClient.getInstance();
    SearchResult search = solr.search("crawl_date:\"2015-09-17T17:02:03Z\"", 10);      
    //     System.out.println(search.getResults().get(0).getCrawlDate());

  }



  public static void testFacetLinks() throws Exception{

    NetarchiveSolrClient solr = NetarchiveSolrClient.getInstance();

    Date start= new Date(System.currentTimeMillis()-25L*365*86400*1000); //25 years ago
    Date end = new Date();

    List<FacetCount>  fc = solr.getDomainFacets("denstoredanske.dk",20, true, start,end);      

    for (FacetCount f : fc){
      System.out.println(f.getValue() +" : " +f.getCount());

    }

  }

  public static void testIngoingLinks() throws Exception{

    NetarchiveSolrClient solr = NetarchiveSolrClient.getInstance();

    Date start= new Date(System.currentTimeMillis()-25L*365*86400*1000); //25 years ago
    Date end = new Date();

    List<FacetCount>  fc = solr.getDomainFacetsIngoing("denstoredanske.dk",10000, start,end);      

    for (FacetCount f : fc){
      System.out.println(f.getValue() +" : " +f.getCount());

    }

  }



  public static void test1() throws Exception{
    HttpSolrClient solrServer;
    NetarchiveSolrClient instance = null;

    solrServer =  new HttpSolrClient.Builder("http://ariel:52300/solr/").build();

    //solrServer.setRequestWriter(new BinaryRequestWriter());

    String searchString="domain:jp.dk AND økonomi";

    SolrQuery solrQuery = new SolrQuery();
    solrQuery.setQuery(searchString); // only search images
    solrQuery.setRows(50); 

    solrQuery.set("facet", "false"); //very important. Must overwrite to false. Facets are very slow and expensive.

    //    solrQuery.add("group.sort","abs(sub(ms("+timeStamp+"), crawl_date)) asc");

    solrQuery.add("fl","url");
    QueryResponse rsp = solrServer.query(solrQuery);

    StringBuffer buf = new StringBuffer();
    buf.append("(url:test");
    int i =0;
    for (SolrDocument current : rsp.getResults()) {
      String url =(String) current.get("url");
      buf.append(" OR url:\""+url+"\"");
      System.out.println(i++  +" "+current.get("url"));
    }
    buf.append(")");


    //now make the bundlet query
    String query2 = buf.toString();
    System.out.println(query2);
    SolrQuery solrQuery2 = new SolrQuery(); 

    solrQuery2.setQuery(query2);

    solrQuery2.set("facet", "false");
    solrQuery2.add("sort","abs(sub(ms(2014-01-03T11:56:58Z), crawl_date)) asc");
    solrQuery2.add("fl","id,score,title,url,source_file_path, source_file,source_file_offset,content_type_norm,hash,crawl_date,content_type, content_encoding"); //only request fields used

    solrQuery2.setRows(1000);
    long start=System.currentTimeMillis();
    rsp = solrServer.query(solrQuery2, METHOD.POST);        
    System.out.println("results:"+rsp.getResults().size()); 
    System.out.println("querytime:"+(System.currentTimeMillis()-start));     


  }

  

  
  
  public static void domainStatistics() throws Exception{
    int maxRows = 20000;// it is faster than grouping to extract all
    String domain ="ekot.dk";
    HttpSolrClient solrServer;
    NetarchiveSolrClient instance = null;

    solrServer =  new HttpSolrClient.Builder("http://localhost:8983/solr/netarchivebuilder").build();

    //solrServer.setRequestWriter(new BinaryRequestWriter());

    String searchString="domain:\""+domain+"\"";      
    SolrQuery solrQuery = new SolrQuery();
    solrQuery.setQuery(searchString); 
    solrQuery.set("facet", "false"); 
    solrQuery.addFilterQuery("content_type_norm:html AND status_code:200");
    solrQuery.addFilterQuery("crawl_year:2020");            
    solrQuery.setRows(0);
    solrQuery.add("fl","id");
    solrQuery.add("stats","true");
    solrQuery.add("stats.field","{!count=true cardinality=true}url_norm"); //Important, use cardinality and not unique.
    solrQuery.add("stats.field","{!sum=true}content_length");


    QueryResponse rsp = solrServer.query(solrQuery);


    Map<String, FieldStatsInfo> stats = rsp.getFieldStatsInfo();
    FieldStatsInfo statsUrl_norm = stats.get("url_norm");
    long url_norm_cardinality = statsUrl_norm.getCardinality();
    long url_norm_total = statsUrl_norm.getCount();         

    FieldStatsInfo statsContent_length = stats.get("content_length");
    Double sum = (Double) statsContent_length.getSum();

    System.out.println("cardinality:"+url_norm_cardinality);
    System.out.println("total:"+url_norm_total);
    System.out.println("length:"+sum);

    //estimate content_length for the uniqie pages by fraction of total.
    double size = sum*(url_norm_cardinality*1d/url_norm_total)*1d/1024d;
    System.out.println("size:"+size);




    solrQuery = new SolrQuery();
    solrQuery.setQuery("links_domains:\""+domain+"\" -"+searchString); //links to, but not from same domain   
    solrQuery.addFilterQuery("content_type_norm:html AND status_code:200");
    solrQuery.addFilterQuery("crawl_year:2011");            
    solrQuery.setRows(0);
    solrQuery.add("stats","true");
    solrQuery.add("fl","id");
    solrQuery.add("stats.field","{!cardinality=true}domain"); //Important, use cardinality and not unique.

    rsp = solrServer.query(solrQuery);
    Map<String, FieldStatsInfo> stats2 = rsp.getFieldStatsInfo();


    FieldStatsInfo statsLinks = stats2.get("domain");         
    long links_cardinality = statsLinks.getCardinality();





  }
  public static void testGroup() throws Exception{

    SolrClient solrServer;


    solrServer =  new HttpSolrClient.Builder("http://localhost:8983/solr/netarchivebuilder/").build();

    //Generate URL string: (url_norm:"A" OR url_norm:"B" OR ....)
    StringBuffer buf = new StringBuffer();
    buf.append("url_norm:test OR url_norm:\"http://opasia.dk/kultur/boeger/mestervaerker/glasnoeglen.shtml\"");    


    String  query = buf.toString();     
    SolrQuery solrQuery = new SolrQuery();
    solrQuery.setQuery(query);

    solrQuery.setRows(10);
    solrQuery.add("facet", "false"); //very important. Must overwrite to false. Facets are very slow and expensive.
    solrQuery.add("sort","abs(sub(ms('2012-08-18T23:18:10Z'), crawl_date)) asc");            
    solrQuery.add("group","true"); //Default only 1 value
    solrQuery.add("group.field","url_norm");
    solrQuery.add("group.format","simple");

    solrQuery.setRows(1000);
    solrQuery.setFilterQueries("record_type:response OR record_type:arc"); //No binary for revists. 

    QueryResponse rsp = solrServer.query(solrQuery,METHOD.POST);        

    ArrayList<IndexDoc>  allDocs = new ArrayList<IndexDoc>();
    SolrDocumentList docs = rsp.getGroupResponse().getValues().get(0).getValues().get(0).getResult();

    for (SolrDocument current:docs){
      System.out.println("doc found");

    }                    


  }

  
}

