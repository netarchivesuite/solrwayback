package dk.kb.netarchivesuite.solrwayback;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.impl.BinaryRequestWriter;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;

import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.service.dto.SearchResult;
import dk.kb.netarchivesuite.solrwayback.solr.FacetCount;
import dk.kb.netarchivesuite.solrwayback.solr.SolrClient;
import dk.kb.netarchivesuite.solrwayback.solr.WaybackStatistics;

public class SolrClientTest {

	
	
	/*
	 * Dette var en test for at bundle forepørgsler på URL'er til en crawl-date
	 * Desværre fejler tomcat pga. for lang query streng,  nogle gange når der bare er 50 url'er.
	 * Så denne bundlede løsning var ikke så god som den kunne have været. 
	 */
	public static void main (String[] args) throws Exception{
	        PropertiesLoader.initProperties();
	       
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
	       // testWaybackStats();
	      	  
	        testFacetLinks();
	       // testSolrDate();
	}
	
	public static void testWaybackStats() throws Exception{
      
         SolrClient solr = SolrClient.getInstance();
         
         
         WaybackStatistics stats = solr.getWayBackStatistics(" http://jpexplorer.jp.dk/explorer/500dage/500dage_logbog/index.html", "2008-12-04T18:52:50Z"); //vi
         
         System.out.println(stats);
         
                   
   }

	
	
	public static void testSolrDate() throws Exception{
      
      SolrClient solr = SolrClient.getInstance();
      SearchResult search = solr.search("crawl_date:\"2015-09-17T17:02:03Z\"", 10);      
      System.out.println(search.getResults().get(0).getCrawlDate());
                
}
	
	
public static void testFacetLinks() throws Exception{
      
      SolrClient solr = SolrClient.getInstance();
      
      Date start= new Date(System.currentTimeMillis()-25L*365*86400*1000); //25 years ago
      Date end = new Date();
      
      List<FacetCount>  fc = solr.getDomainFacets("jp.dk",20, true, start,end);      
      
      for (FacetCount f : fc){
        System.out.println(f.getValue() +" : " +f.getCount());
        
      }
                
}
    
	
	public static void test1() throws Exception{
	   HttpSolrClient solrServer;
       SolrClient instance = null;
       
          solrServer = new HttpSolrClient("http://ariel:52300/solr/");
          solrServer.setRequestWriter(new BinaryRequestWriter());

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
          solrQuery2.add("fl","id,score,title,arc_full,url,source_file_s,content_type_norm,hash,crawl_date,content_type, content_encoding"); //only request fields used
       
          solrQuery2.setRows(1000);
          long start=System.currentTimeMillis();
          rsp = solrServer.query(solrQuery2, METHOD.POST);        
          System.out.println("results:"+rsp.getResults().size()); 
          System.out.println("querytime:"+(System.currentTimeMillis()-start));     

          
          
          
	}
	
}
