package dk.kb.netarchivesuite.solrwayback.memento;

import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoaderWeb;
import dk.kb.netarchivesuite.solrwayback.service.SolrWaybackMementoAPI;
import dk.kb.netarchivesuite.solrwayback.solr.NetarchiveSolrClient;
import dk.kb.netarchivesuite.solrwayback.solr.NetarchiveSolrTestClient;
import dk.kb.netarchivesuite.solrwayback.solr.SRequest;
import dk.kb.netarchivesuite.solrwayback.solr.SolrGenericStreaming;
import dk.kb.netarchivesuite.solrwayback.solr.SolrGenericStreamingTest;
import dk.kb.netarchivesuite.solrwayback.util.DateUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.CoreContainer;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TimeMapTest {

    private static final Logger log = LoggerFactory.getLogger(TimeMapTest.class);

    public static final int TEST_DOCS = 100; // Changing this might make some unit tests fail
    private static final String SOLR_HOME = "target/test-classes/solr";
    private static CoreContainer coreContainer= null;
    private static EmbeddedSolrServer embeddedServer = null;

    @BeforeClass
    public static void setUp() throws Exception {
        log.info("Setting up embedded server");

        PropertiesLoader.initProperties();

        coreContainer = new CoreContainer(SOLR_HOME);
        coreContainer.load();
        embeddedServer = new EmbeddedSolrServer(coreContainer,"netarchivebuilder");
        NetarchiveSolrTestClient.initializeOverLoadUnitTest(embeddedServer);

        // Remove any items from previous executions:
        embeddedServer.deleteByQuery("*:*"); //This is not on the NetarchiveSolrClient API!

        fillSolr();
        SolrGenericStreaming.setDefaultSolrClient(embeddedServer);
        log.info("Embedded server ready");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        coreContainer.shutdown();
        embeddedServer.close();
    }


    @Test
    public void serverAvailable() throws SolrServerException, IOException {
        SolrQuery query = new SolrQuery("*:*");
        assertTrue("There should be some results", embeddedServer.query(query).getResults().getNumFound() > 0);
    }

    @Test
    public void basicStreaming() {
        log.debug("Testing basic streaming");
        List<SolrDocument> docs = SolrGenericStreaming.create(
                        SRequest.builder().
                                query("title:title_5").
                                fields("id").
                                pageSize(2)).
                stream().collect(Collectors.toList());
        assertFalse("Basic streaming should return some documents", docs.isEmpty());
    }


    @Test
    public void testTimeMapLinkConstruction() throws IOException, URISyntaxException {
        // Set very high to disable paging
        StreamingOutput timeMap = TimeMap.getTimeMap(new URI("http://example.com/"), "application/link-format", null);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        timeMap.write(output);
        String timeMapString = new String(output.toByteArray(), StandardCharsets.UTF_8);

        assertEquals(testTimeMap, timeMapString);
    }
    /*
    @Before
    public void setUp(){
        PropertiesLoader.MEMENTO_TIMEMAP_PAGINGLIMIT = 999999;
        NetarchiveSolrClient.initialize("http://localhost:8983/solr/netarchivebuilder/");
        PropertiesLoaderWeb.initProperties();
    }

    @Test
    public void testTimeMapLinkConstruction() throws IOException, URISyntaxException {
        // Set very high to disable paging
        StreamingOutput timeMap = TimeMap.getTimeMap(new URI("http://kb.dk/"), "application/link-format", null);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        timeMap.write(output);
        String timeMapString = new String(output.toByteArray(), StandardCharsets.UTF_8);

        assertEquals(testTimeMap, timeMapString);
    }

     */

    final String testTimeMap = "<http://kb.dk/>;rel=\"original\",\n" +
            "<http://localhost:8080/solrwayback/services/memento/timemap/link/http://kb.dk/>" +
            "; rel=\"self\"; type=\"application/link-format\"" +
            "; from=\"Thu, 23 Mar 2023 14:05:57 GMT\"" +
            "; until=\"Wed, 26 Jul 2023 09:53:12 GMT\",\n" +
            "<http://localhost:8080/solrwayback/services/memento/http://kb.dk/>" +
            "; rel=\"timegate\",\n" +
            "<http://localhost:8080/solrwayback/services/web/20230323140557/https://www.kb.dk/>" +
            "; rel=\"first memento\"; datetime=\"Thu, 23 Mar 2023 14:05:57 GMT\",\n" +
            "<http://localhost:8080/solrwayback/services/web/20230323140557/http://www.kb.dk/>" +
            "; rel=\"memento\"; datetime=\"Thu, 23 Mar 2023 14:05:57 GMT\",\n" +
            "<http://localhost:8080/solrwayback/services/web/20230721064503/http://www.kb.dk/>" +
            "; rel=\"memento\"; datetime=\"Fri, 21 Jul 2023 06:45:03 GMT\",\n" +
            "<http://localhost:8080/solrwayback/services/web/20230721064504/https://www.kb.dk/>" +
            "; rel=\"memento\"; datetime=\"Fri, 21 Jul 2023 06:45:04 GMT\",\n" +
            "<http://localhost:8080/solrwayback/services/web/20230726095312/https://www.kb.dk/>" +
            "; rel=\"memento\"; datetime=\"Wed, 26 Jul 2023 09:53:12 GMT\",\n" +
            "<http://localhost:8080/solrwayback/services/web/20230726095312/http://www.kb.dk/>" +
            "; rel=\"last memento\"; datetime=\"Wed, 26 Jul 2023 09:53:12 GMT\",\n";


    private static void fillSolr() throws SolrServerException, IOException {
        log.info("Filling embedded server with {} documents", TEST_DOCS);
        final Random r = new Random(87); // Random but not too random
        for (int i = 0 ; i < TEST_DOCS ; i++) {
            addDoc(i, r);
        }
        embeddedServer.commit();
    }

    private static void addDoc(int id, Random r) throws SolrServerException, IOException {
        final String[] CRAWL_TIMES = new String[]{
                "2018-03-15T12:31:51Z",
                "2019-03-15T12:31:51Z",
                "2020-03-15T12:31:51Z",
                "2021-03-15T12:31:51Z"
        };
        SolrInputDocument document = new SolrInputDocument();

        document.setField("id", "doc_" + id);
        document.addField("source_file_offset", id);
        document.addField("title", "title_" + id%10);
        document.addField("url", "https://example.COM/" + id%10); // %10 to get duplicates
        document.addField("url_norm", "http://example.com/" + id%10);
        document.addField("record_type","response");
        document.addField("source_file_path", "some.warc_" + id);
        document.addField("links", Arrays.asList("http://example.com/everywhere", "http://example.com/mod10_" + id%10));
        document.addField("status_code", "200");
        document.setField("crawl_date", DateUtils.solrTimestampToJavaDate(CRAWL_TIMES[r.nextInt(CRAWL_TIMES.length)]));
        document.addField("wayback_date", 20220406122323L);
        embeddedServer.add(document);
    }
}
