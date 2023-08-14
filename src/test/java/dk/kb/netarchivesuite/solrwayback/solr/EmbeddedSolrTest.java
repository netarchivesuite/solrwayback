package dk.kb.netarchivesuite.solrwayback.solr;

import static org.junit.Assert.assertEquals;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDoc;


public class EmbeddedSolrTest {

    private static final Logger log = LoggerFactory.getLogger(EmbeddedSolrTest.class);        
    private static String solr_home= "target/test-classes/solr_9";
    private static NetarchiveSolrClient server = null;
    private static  CoreContainer coreContainer= null;
    private static EmbeddedSolrServer embeddedServer = null;
    
    @Before
    public void setUp() throws Exception {
        System.setProperty("solr.install.dir", Path.of(solr_home).toAbsolutePath().toString());
       coreContainer = CoreContainer.createAndLoad(Path.of(solr_home).toAbsolutePath()); //new CoreContainer(solr_home);
       coreContainer.load();
       embeddedServer = new EmbeddedSolrServer(coreContainer,"netarchivebuilder");
       NetarchiveSolrTestClient.initializeOverLoadUnitTest(embeddedServer);
       server = NetarchiveSolrClient.getInstance();
       
        // Remove any items from previous executions:
       embeddedServer.deleteByQuery("*:*"); //This is not on the NetarchiveSolrClient API!
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
      coreContainer.shutdown(); 
      embeddedServer.close();
    }
    
  
    @Test
    public void testDateSortBug() throws Exception {
    
       String url = "http://testurl.dk/test";
      
       ArrayList<String> crawlTimes = new ArrayList<String>();
       crawlTimes.add("2018-03-15T12:31:51Z");
       crawlTimes.add("2018-03-15T12:34:37Z");
       crawlTimes.add("2018-03-15T12:35:56Z");
       crawlTimes.add("2018-03-15T12:36:14Z");
       crawlTimes.add("2018-03-15T12:36:43Z"); //  <-- Excact match test #1
       crawlTimes.add("2018-03-15T12:37:32Z");//   <-- nearest for test #3
       crawlTimes.add("2018-03-15T12:37:52Z"); //  <-- nearest for test #2 
       crawlTimes.add("2018-03-15T12:39:15Z");
       crawlTimes.add("2018-03-15T12:40:09Z");
             
       int i =1;
       for (String crawl : crawlTimes){
         SolrInputDocument document = new SolrInputDocument();
         String id = ""+i++; 
         String title = "title "+i;                
         document.addField("source_file_offset", i+"");
         document.addField("id", id);
         document.addField("title", title);
         document.addField( "url", url);
         document.addField( "url_norm", url);
         document.addField("record_type","response");
         document.addField("source_file_path", "some.warc");
         document.addField("status_code", "200");
         document.setField("crawl_date", crawl); 
         embeddedServer.add(document);
         
       }              
       embeddedServer.commit();    
       
       
       String dateToSearchFor="2018-03-15T12:36:43Z";
       IndexDoc result = server.findClosestHarvestTimeForUrl(url, dateToSearchFor);
       assertEquals("2018-03-15T12:36:43Z", result.getCrawlDate());
       
       
       dateToSearchFor="2018-03-15T12:37:45Z"; // 7 seconds from match          
       result = server.findClosestHarvestTimeForUrl(url, dateToSearchFor);
       assertEquals("2018-03-15T12:37:52Z", result.getCrawlDate());
       
       dateToSearchFor="2018-03-15T12:37:40Z"; // 8 seconds from match          
       result = server.findClosestHarvestTimeForUrl(url, dateToSearchFor);
       assertEquals("2018-03-15T12:37:32Z", result.getCrawlDate());       
    }
    

}
