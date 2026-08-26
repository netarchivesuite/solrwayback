package dk.kb.netarchivesuite.solrwayback.solr;

import static org.junit.Assert.assertEquals;

import java.nio.file.Path;
import java.util.ArrayList;

import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.*;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDoc;


public class EmbeddedSolrTest {

    private static final Logger log = LoggerFactory.getLogger(EmbeddedSolrTest.class);        
    private static String SOLR_HOME = "target/test-classes/solr_9";
    private static NetarchiveSolrClient server = null;
    private static  CoreContainer coreContainer= null;
    private static EmbeddedSolrServer embeddedServer = null;
    
    @Before
    public void setUp() throws Exception {
        // Embedded Solr 9.1+ must have absolute home both as env and explicit param
        Path solrHome = Path.of(SOLR_HOME).toAbsolutePath();
        System.setProperty("solr.install.dir", solrHome.toString());
        NodeConfig nodeConfig = new NodeConfig.NodeConfigBuilder("netarchivebuilder", solrHome).build();
        coreContainer = new CoreContainer(nodeConfig);
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
    
    
    @Test
    public void testRedirectChainResolvesToFinalDocument() throws Exception {
      
        indexRedirectChainTestData();

        //Notice crawldate is 1 day before page harvested for testing.
        IndexDoc result = server.findClosestHarvestTimeForUrl("http://test.domain/redirect1", "2026-08-25T00:00:0Z"); 
     
        System.out.println(result.getUrl());
        System.out.println(result.getCrawlDate());
        System.out.println(result.getStatusCode());
        assertEquals("http://test.domain/final", result.getUrl()); //Resolved through 2 redirects.
        assertEquals(200, result.getStatusCode());        
    }
    

    private static void indexRedirectChainTestData() throws Exception {
        String url1 = "http://test.domain/redirect1";
        String url2 = "http://test.domain/redirect2";
        String urlFinal = "http://test.domain/final";

        // 1) redirect1 -> redirect2
        SolrInputDocument redirect1 = new SolrInputDocument();
        redirect1.addField("source_file_offset", 1000);
        redirect1.addField("id", "redirect1");
        redirect1.addField("title", "redirect1");
        redirect1.addField("url", url1);
        redirect1.addField("url_norm", url1);
        redirect1.addField("record_type", "response");
        redirect1.addField("source_file_path", "some.warc");
        redirect1.addField("status_code", "302");
        redirect1.addField("crawl_date", "2026-08-26T00:00:00Z");
        redirect1.addField("redirect_to_norm", url2); // points to the NEXT record, not itself

        // 2) redirect2 -> final
        SolrInputDocument redirect2 = new SolrInputDocument();
        redirect2.addField("source_file_offset", 2000);
        redirect2.addField("id", "redirect2");
        redirect2.addField("title", "redirect2");
        redirect2.addField("url", url2);
        redirect2.addField("url_norm", url2);
        redirect2.addField("record_type", "response");
        redirect2.addField("source_file_path", "some.warc");
        redirect2.addField("status_code", "302");
        redirect2.addField("crawl_date", "2026-08-26T00:00:01Z");
        redirect2.addField("redirect_to_norm", urlFinal);

        // 3) final 200 response — the actual page content
        SolrInputDocument finalDoc = new SolrInputDocument();
        finalDoc.addField("source_file_offset", 3000);
        finalDoc.addField("id", "final");
        finalDoc.addField("title", "final");
        finalDoc.addField("url", urlFinal);
        finalDoc.addField("url_norm", urlFinal);
        finalDoc.addField("record_type", "response");
        finalDoc.addField("source_file_path", "some.warc");
        finalDoc.addField("status_code", "200");
        finalDoc.addField("crawl_date", "2026-08-26T00:00:02Z");
        // no redirect_to_norm on a 200 — nothing to redirect to

        embeddedServer.add(redirect1);
        embeddedServer.add(redirect2);
        embeddedServer.add(finalDoc);
        embeddedServer.commit();
    }

}
