package dk.kb.netarchivesuite.solrwayback.memento;

import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoaderWeb;
import dk.kb.netarchivesuite.solrwayback.solr.NetarchiveSolrTestClient;
import dk.kb.netarchivesuite.solrwayback.solr.SolrGenericStreaming;
import dk.kb.netarchivesuite.solrwayback.util.DateUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.CoreContainer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


public class DatetimeNegotiationTest {
    private static final Logger log = LoggerFactory.getLogger(DatetimeNegotiationTest.class);
    public static final int TEST_DOCS = 20; // Changing this might make some unit tests fail
    private static String solr_home= "target/test-classes/solr_9";
    private static CoreContainer coreContainer= null;
    private static EmbeddedSolrServer embeddedServer = null;

    @Before
    public void setUp() throws Exception {
        log.info("Setting up embedded server");
        PropertiesLoader.initProperties();
        PropertiesLoaderWeb.initProperties();
        
        // Embedded Solr 9.1+ must have absolute home both as env and explicit param
        System.setProperty("solr.install.dir", Path.of(solr_home).toAbsolutePath().toString());
        coreContainer = CoreContainer.createAndLoad(Path.of(solr_home).toAbsolutePath());
        coreContainer.load();
        embeddedServer = new EmbeddedSolrServer(coreContainer,"netarchivebuilder");
        NetarchiveSolrTestClient.initializeOverLoadUnitTest(embeddedServer);
                
        // Remove any items from previous executions:
        embeddedServer.deleteByQuery("*:*"); //This is not on the NetarchiveSolrClient API!


        fillSolr();
        SolrGenericStreaming.setDefaultSolrClient(embeddedServer);
        log.info("Embedded server ready with timemap paginglimit set to: '{}'", PropertiesLoader.MEMENTO_TIMEMAP_PAGINGLIMIT);
    }

    @After
    public void tearDown() throws Exception {
        embeddedServer.close();
        coreContainer.shutdown();
    }

    
    @Test
    public void testHeadersForPatternTwoPointTwo() throws Exception {
        PropertiesLoader.PLAYBACK_DISABLED = true;

        Response timeGate = DatetimeNegotiation.getMemento("http://kb.dk/", "Thu, 23 Mar 2019 14:05:57 GMT");
        MultivaluedMap<String, Object> headers = timeGate.getHeaders();

        assertEquals("accept-datetime", headers.get("Vary").get(0));
        String contentLocation=(String) headers.get("Content-Location").get(0);
        assertTrue(contentLocation.endsWith("/20200315123151/https://kb.dk/"));
        assertNotNull(headers.get("Memento-Datetime"));
        assertTrue(headers.get("Link").get(0).toString().contains("rel=\"original\""));
        assertNotNull(headers.get("Content-Length").get(0));    
        assertEquals("text/html;charset=UTF-8", headers.get("Content-Type").get(0));
    }



    @Test
    public void testHeadersForPatternTwoPointOne() throws Exception {
        PropertiesLoader.PLAYBACK_DISABLED = true;
        PropertiesLoader.MEMENTO_REDIRECT = true;

        Response timeGate = DatetimeNegotiation.getMemento("http://kb.dk/", "Thu, 23 Mar 2019 14:05:57 GMT");
        MultivaluedMap<String, Object> headers = timeGate.getHeaders();

        assertEquals("accept-datetime", headers.get("Vary").get(0));
        assertNotNull(headers.get("Location"));
        assertFalse(headers.containsKey("Memento-Datetime"));
        assertTrue(headers.get("Link").get(0).toString().contains("rel=\"original\""));
        assertEquals(0, headers.get("Content-Length").get(0));
        assertEquals("text/html;charset=UTF-8", headers.get("Content-Type").get(0));
    }

    public static void fillSolr() throws SolrServerException, IOException {
        log.info("Filling embedded server with {} documents", TEST_DOCS);
        final Random r = new Random(87); // Random but not too random
        for (int i = 0 ; i < TEST_DOCS ; i++) {
            addDoc(i, r);
        }
        embeddedServer.commit();
    }

    private static void addDoc(int id, Random r) throws SolrServerException, IOException {
        final String[] CRAWL_TIMES = new String[]{
                "2005-03-15T12:31:51Z",
                "2012-03-15T12:31:51Z",
                "2020-03-15T12:31:51Z",
                "2023-03-15T12:31:51Z"
        };
        SolrInputDocument document = new SolrInputDocument();

        String solrDate = CRAWL_TIMES[r.nextInt(CRAWL_TIMES.length)];

        document.setField("id", DateUtils.convertUtcDate2WaybackDate(solrDate) + "_" + id);
        document.addField("source_file_offset", id);
        document.addField("title", "title_" + id%10);
        document.addField("url", "https://kb.dk/"); // %10 to get duplicates
        document.addField("url_norm", "http://kb.dk/");
        document.addField("record_type","response");
        document.addField("source_file_path", "some.warc");
        document.addField("links", Arrays.asList("http://example.com/everywhere", "http://example.com/mod10_" + id%10));
        document.addField("status_code", "200");
        document.setField("crawl_date", DateUtils.solrTimestampToJavaDate(solrDate));
        document.addField("wayback_date", DateUtils.convertUtcDate2WaybackDate(solrDate));
        document.addField("content_length", 5000);
        document.addField("content_type_served", "text/html;charset=UTF-8");
        document.addField("type", "text/html;charset=UTF-8");
        embeddedServer.add(document);
    }


    //TODO: Do embedded solr testing instead. Use same docs as timemaptest



}
