package dk.kb.netarchivesuite.solrwayback.solr;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.Group;
import org.apache.solr.client.solrj.response.GroupResponse;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class EmbeddedSolrTest {

    private static final Logger log = LoggerFactory.getLogger(EmbeddedSolrTest.class);        

    private static String solr_home= "target/test-classes/solr"; 


    private static CoreContainer coreContainer =  null;
    private static EmbeddedSolrServer server =   null;
    
    
    @Before
    public void setUp() throws Exception {
        
       coreContainer = new CoreContainer(solr_home);
       coreContainer.load();
       server = new EmbeddedSolrServer(coreContainer,"netarchivebuilder");
             
        // Remove any items from previous executions:
        server.deleteByQuery("*:*");
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
      coreContainer.shutdown(); 
      server.close();
    }
    
    
  


    @Test
    public void testDateSortBug() throws Exception {
    
       ArrayList<String> crawlTimes = new ArrayList<String>();
       crawlTimes.add("2018-03-15T12:31:51Z");
       crawlTimes.add("2018-03-15T12:34:37Z");
       crawlTimes.add("2018-03-15T12:35:56Z");
       crawlTimes.add("2018-03-15T12:36:14Z");
       crawlTimes.add("2018-03-15T12:36:43Z");
       crawlTimes.add("2018-03-15T12:37:32Z");
       crawlTimes.add("2018-03-15T12:37:52Z");     
       crawlTimes.add("2018-03-15T12:39:15Z");
       crawlTimes.add("2018-03-15T12:40:09Z");
       
       

       int i =1;
       for (String crawl : crawlTimes){
         SolrInputDocument document = new SolrInputDocument();
         String id = ""+i++; 
         String title = "title "+i;
         document.addField("id", id);
         document.addField("title", title);
         document.addField( "url", "http://testurl.dk/");
         document.addField("source_file", "some.warc");
         document.setField("crawl_date", crawl); 
         server.add(document);
         
       }
       server.commit();    
       
       
       SolrQuery query = new SolrQuery();
       query.setQuery("url:\"http://testurl.dk/\"");
       query.add("group","true");      
       query.add("group.limit","10"); 
       query.add("group.field","url");
       query.add("group.sort","abs(sub(ms(2018-03-15T12:36:43Z), crawl_date)) asc"); //This is #5, excact match
       QueryResponse response = server.query(query);
       assertEquals(1L, response.getGroupResponse().getValues().size()); // grouping.
        GroupResponse groupResponse = response.getGroupResponse();
       
        List<Group> values = groupResponse.getValues().get(0).getValues();
        for (Group group:values){
          SolrDocumentList groupList = group.getResult();
          
           for (SolrDocument doc : groupList){
             Date crawl_date = (Date) doc.getFieldValue("crawl_date");
             System.out.println(crawl_date);
             
           }          
          
        }                          
    }
    
    
    
    



}
