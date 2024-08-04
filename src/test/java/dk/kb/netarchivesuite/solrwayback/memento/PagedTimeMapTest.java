package dk.kb.netarchivesuite.solrwayback.memento;

import dk.kb.netarchivesuite.solrwayback.UnitTestUtils;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoaderWeb;
import dk.kb.netarchivesuite.solrwayback.solr.NetarchiveSolrTestClient;
import dk.kb.netarchivesuite.solrwayback.solr.SolrStreamDirect;
import dk.kb.netarchivesuite.solrwayback.util.DateUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.NodeConfig;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PagedTimeMapTest {
    private static final Logger log = LoggerFactory.getLogger(PagedTimeMapTest.class);
    public static final int TEST_DOCS = 100; // Changing this might make some unit tests fail
    private static String SOLR_HOME = "target/test-classes/solr_9";
    private static CoreContainer coreContainer= null;
    private static EmbeddedSolrServer embeddedServer = null;

    @BeforeClass
    public static void setUp() throws Exception {
        log.info("Setting up embedded server");
        PropertiesLoader.MEMENTO_TIMEMAP_PAGINGLIMIT = 1;
        PropertiesLoader.MEMENTO_TIMEMAP_PAGESIZE = 5;
        PropertiesLoaderWeb.initProperties();

        // Embedded Solr 9.1+ must have absolute home both as env and explicit param
        Path solrHome = Path.of(SOLR_HOME).toAbsolutePath();
        System.setProperty("solr.install.dir", solrHome.toString());
        NodeConfig nodeConfig = new NodeConfig.NodeConfigBuilder("netarchivebuilder", solrHome).build();
        coreContainer = new CoreContainer(nodeConfig);
        coreContainer.load();
        embeddedServer = new EmbeddedSolrServer(coreContainer,"netarchivebuilder");
        NetarchiveSolrTestClient.initializeOverLoadUnitTest(embeddedServer);
                
        // Remove any items from previous executions:
        embeddedServer.deleteByQuery("*:*"); //This is not on the NetarchiveSolrClient API!

        fillSolr();
        SolrStreamDirect.setDefaultSolrClient(embeddedServer);
        log.info("Embedded server ready with timemap paging limit '{}'.", PropertiesLoader.MEMENTO_TIMEMAP_PAGINGLIMIT);
        assertEquals(1, PropertiesLoader.MEMENTO_TIMEMAP_PAGINGLIMIT);
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
    public void testTimeMapLinkLastConstruction() throws IOException, URISyntaxException {
        StreamingOutput timeMap = TimeMap.getTimeMap(new URI("http://kb.dk/"), "application/link-format", 20);

        String timeMapString = convertStreamingTimeMapToString(timeMap);
        
        String timeMapStringLocalhost=UnitTestUtils.replaceHostNameWithLocalHost(timeMapString);              
        assertEquals(testPagedTimeMapLinkLastPage, timeMapStringLocalhost);
    }

    @Test
    public void testTimeMapLinkNoPageConstruction() throws IOException, URISyntaxException {
        StreamingOutput timeMap = TimeMap.getTimeMap(new URI("http://kb.dk/"), "application/link-format", null);

        String timeMapString = convertStreamingTimeMapToString(timeMap);
        String timeMapStringLocalhost=UnitTestUtils.replaceHostNameWithLocalHost(timeMapString);
        
        assertEquals(testPagedTimeMapLinkFirstPage, timeMapStringLocalhost);
    }
    @Test
    public void testTimeMapLinkFirstConstruction() throws IOException, URISyntaxException {
        StreamingOutput timeMap = TimeMap.getTimeMap(new URI("http://kb.dk/"), "application/link-format", 1);

        String timeMapString = convertStreamingTimeMapToString(timeMap);
        String timeMapStringLocalhost=UnitTestUtils.replaceHostNameWithLocalHost(timeMapString);
        assertEquals(testPagedTimeMapLinkFirstPage, timeMapStringLocalhost);
    }


    @Test
    public void testTimeMapLinkRandomConstruction() throws IOException, URISyntaxException {
        StreamingOutput timeMap = TimeMap.getTimeMap(new URI("http://kb.dk/"), "application/link-format", 5);

        String timeMapString = convertStreamingTimeMapToString(timeMap);
        String timeMapStringLocalhost=UnitTestUtils.replaceHostNameWithLocalHost(timeMapString);
        assertEquals(testPagedTimeMapLinkFifthPage, timeMapStringLocalhost);
    }

    @Test
    public void testPagedTimeMapFirstJSONConstruction() throws IOException, URISyntaxException {
        StreamingOutput timeMap = TimeMap.getTimeMap(new URI("http://kb.dk/"), "application/json", 1);

        String timeMapString = convertStreamingTimeMapToString(timeMap);
        String timeMapStringLocalhost=UnitTestUtils.replaceHostNameWithLocalHost(timeMapString);
        assertEquals(testPagedTimeMapJSONFirstPage, timeMapStringLocalhost);
    }

    @Test
    public void testPagedTimeMapRandomJSONConstruction() throws IOException, URISyntaxException {
        StreamingOutput timeMap = TimeMap.getTimeMap(new URI("http://kb.dk/"), "application/json", 12);

        String timeMapString = convertStreamingTimeMapToString(timeMap);
        String timeMapStringLocalhost=UnitTestUtils.replaceHostNameWithLocalHost(timeMapString);
        assertEquals(testPagedTimeMapJSONTwelthPage, timeMapStringLocalhost);
    }

    @Test
    public void testPagedTimeMapLastJSONConstruction() throws IOException, URISyntaxException {
        StreamingOutput timeMap = TimeMap.getTimeMap(new URI("http://kb.dk/"), "application/json", 20);

        String timeMapString = convertStreamingTimeMapToString(timeMap);
        String timeMapStringLocalhost=UnitTestUtils.replaceHostNameWithLocalHost(timeMapString);
        assertEquals(testPagedTimeMapJSONLastPage, timeMapStringLocalhost);
    }

    @Test
    public void testPagedTimeMapNoPageJSONConstruction() throws IOException, URISyntaxException {
        StreamingOutput timeMap = TimeMap.getTimeMap(new URI("http://kb.dk/"), "application/json", null);

        String timeMapString = convertStreamingTimeMapToString(timeMap);
        String timeMapStringLocalhost=UnitTestUtils.replaceHostNameWithLocalHost(timeMapString);
        assertEquals(testPagedTimeMapJSONFirstPage, timeMapStringLocalhost);
    }

    private static String convertStreamingTimeMapToString(StreamingOutput timeMap) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        timeMap.write(output);
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }

    final String testPagedTimeMapJSONFirstPage = "{\"original_uri\":\"http://kb.dk/\"," +
            "\"timegate_uri\":\"http://localhost:8080/solrwayback/services/memento/http://kb.dk/\"," +
            "\"timemap_uri\":{\"link_format\":\"http://localhost:8080/solrwayback/services/memento/timemap/1/link/http://kb.dk/\"," +
                             "\"json_format\":\"http://localhost:8080/solrwayback/services/memento/timemap/1/json/http://kb.dk/\"}," +
            "\"mementos\":" +
            "{\"first\":{\"datetime\":\"Tue, 15 Mar 2005 12:31:51 GMT\",\"uri\":\"http://localhost:8080/solrwayback/services/web/20050315123151/http://kb.dk/\"}," +
              "\"last\":{\"datetime\":\"Wed, 15 Mar 2023 12:31:51 GMT\",\"uri\":\"http://localhost:8080/solrwayback/services/web/20230315123151/http://kb.dk/\"}," +
            "\"list\":[{\"datetime\":\"Tue, 15 Mar 2005 12:31:51 GMT\",\"uri\":\"http://localhost:8080/solrwayback/services/web/20050315123151/http://kb.dk/\"}," +
                      "{\"datetime\":\"Tue, 15 Mar 2005 12:31:51 GMT\",\"uri\":\"http://localhost:8080/solrwayback/services/web/20050315123151/http://kb.dk/\"}," +
                      "{\"datetime\":\"Tue, 15 Mar 2005 12:31:51 GMT\",\"uri\":\"http://localhost:8080/solrwayback/services/web/20050315123151/http://kb.dk/\"}," +
                      "{\"datetime\":\"Tue, 15 Mar 2005 12:31:51 GMT\",\"uri\":\"http://localhost:8080/solrwayback/services/web/20050315123151/http://kb.dk/\"}," +
                      "{\"datetime\":\"Tue, 15 Mar 2005 12:31:51 GMT\",\"uri\":\"http://localhost:8080/solrwayback/services/web/20050315123151/http://kb.dk/\"}]}," +
            "\"pages\":" +
                "{\"next\":{\"uri\":\"http://localhost:8080/solrwayback/services/memento/timemap/2/json/http://kb.dk/\"}}}";

    final String testPagedTimeMapJSONTwelthPage = "{\"original_uri\":\"http://kb.dk/\"," +
            "\"timegate_uri\":\"http://localhost:8080/solrwayback/services/memento/http://kb.dk/\"," +
            "\"timemap_uri\":{\"link_format\":\"http://localhost:8080/solrwayback/services/memento/timemap/12/link/http://kb.dk/\"," +
                             "\"json_format\":\"http://localhost:8080/solrwayback/services/memento/timemap/12/json/http://kb.dk/\"}," +
            "\"mementos\":" +
            "{\"first\":{\"datetime\":\"Tue, 15 Mar 2005 12:31:51 GMT\",\"uri\":\"http://localhost:8080/solrwayback/services/web/20050315123151/http://kb.dk/\"}," +
             "\"last\":{\"datetime\":\"Wed, 15 Mar 2023 12:31:51 GMT\",\"uri\":\"http://localhost:8080/solrwayback/services/web/20230315123151/http://kb.dk/\"}," +
            "\"list\":[{\"datetime\":\"Sun, 15 Mar 2020 12:31:51 GMT\",\"uri\":\"http://localhost:8080/solrwayback/services/web/20200315123151/http://kb.dk/\"}," +
                     "{\"datetime\":\"Sun, 15 Mar 2020 12:31:51 GMT\",\"uri\":\"http://localhost:8080/solrwayback/services/web/20200315123151/http://kb.dk/\"}," +
                     "{\"datetime\":\"Sun, 15 Mar 2020 12:31:51 GMT\",\"uri\":\"http://localhost:8080/solrwayback/services/web/20200315123151/http://kb.dk/\"}," +
                     "{\"datetime\":\"Sun, 15 Mar 2020 12:31:51 GMT\",\"uri\":\"http://localhost:8080/solrwayback/services/web/20200315123151/http://kb.dk/\"}," +
                     "{\"datetime\":\"Sun, 15 Mar 2020 12:31:51 GMT\",\"uri\":\"http://localhost:8080/solrwayback/services/web/20200315123151/http://kb.dk/\"}]}," +
            "\"pages\":{\"prev\":{\"uri\":\"http://localhost:8080/solrwayback/services/memento/timemap/11/json/http://kb.dk/\"}," +
                       "\"next\":{\"uri\":\"http://localhost:8080/solrwayback/services/memento/timemap/13/json/http://kb.dk/\"}}}";

    final String testPagedTimeMapJSONLastPage =
            "{\"original_uri\":\"http://kb.dk/\",\"timegate_uri\":\"http://localhost:8080/solrwayback/services/memento/http://kb.dk/\"," +
                    "\"timemap_uri\":{\"link_format\":\"http://localhost:8080/solrwayback/services/memento/timemap/20/link/http://kb.dk/\"," +
                    "\"json_format\":\"http://localhost:8080/solrwayback/services/memento/timemap/20/json/http://kb.dk/\"}," +
                    "\"mementos\":{\"first\":{\"datetime\":\"Tue, 15 Mar 2005 12:31:51 GMT\",\"uri\":\"http://localhost:8080/solrwayback/services/web/20050315123151/http://kb.dk/\"}," +
                                  "\"last\":{\"datetime\":\"Wed, 15 Mar 2023 12:31:51 GMT\",\"uri\":\"http://localhost:8080/solrwayback/services/web/20230315123151/http://kb.dk/\"}," +
                    "\"list\":[" +
                    "{\"datetime\":\"Wed, 15 Mar 2023 12:31:51 GMT\",\"uri\":\"http://localhost:8080/solrwayback/services/web/20230315123151/http://kb.dk/\"}," +
                    "{\"datetime\":\"Wed, 15 Mar 2023 12:31:51 GMT\",\"uri\":\"http://localhost:8080/solrwayback/services/web/20230315123151/http://kb.dk/\"}," +
                    "{\"datetime\":\"Wed, 15 Mar 2023 12:31:51 GMT\",\"uri\":\"http://localhost:8080/solrwayback/services/web/20230315123151/http://kb.dk/\"}," +
                    "{\"datetime\":\"Wed, 15 Mar 2023 12:31:51 GMT\",\"uri\":\"http://localhost:8080/solrwayback/services/web/20230315123151/http://kb.dk/\"}," +
                    "{\"datetime\":\"Wed, 15 Mar 2023 12:31:51 GMT\",\"uri\":\"http://localhost:8080/solrwayback/services/web/20230315123151/http://kb.dk/\"}]}," +
                    "\"pages\":{\"prev\":{\"uri\":\"http://localhost:8080/solrwayback/services/memento/timemap/19/json/http://kb.dk/\"}}}";

    final String testPagedTimeMapLinkFirstPage = "<http://kb.dk/>;rel=\"original\",\n" +
            "<http://localhost:8080/solrwayback/services/memento/timemap/1/link/http://kb.dk/>; rel=\"self\"; type=\"application/link-format\"; from=\"Tue, 15 Mar 2005 12:31:51 GMT\"; until=\"Wed, 15 Mar 2023 12:31:51 GMT\",\n" +
            "<http://localhost:8080/solrwayback/services/memento/http://kb.dk/>; rel=\"timegate\",\n" +
            "<http://localhost:8080/solrwayback/services/web/20050315123151/https://kb.dk/>; rel=\"first memento\"; datetime=\"Tue, 15 Mar 2005 12:31:51 GMT\",\n" +
            "<http://localhost:8080/solrwayback/services/web/20050315123151/https://kb.dk/>; rel=\"memento\"; datetime=\"Tue, 15 Mar 2005 12:31:51 GMT\",\n" +
            "<http://localhost:8080/solrwayback/services/web/20050315123151/https://kb.dk/>; rel=\"memento\"; datetime=\"Tue, 15 Mar 2005 12:31:51 GMT\",\n" +
            "<http://localhost:8080/solrwayback/services/web/20050315123151/https://kb.dk/>; rel=\"memento\"; datetime=\"Tue, 15 Mar 2005 12:31:51 GMT\",\n" +
            "<http://localhost:8080/solrwayback/services/web/20050315123151/https://kb.dk/>; rel=\"memento\"; datetime=\"Tue, 15 Mar 2005 12:31:51 GMT\",\n" +
            "<http://localhost:8080/solrwayback/services/memento/timemap/2/link/http://kb.dk/>; rel=\"next\"; type=\"application/link-format\"\n";

    final String testPagedTimeMapLinkFifthPage = "<http://kb.dk/>;rel=\"original\",\n" +
            "<http://localhost:8080/solrwayback/services/memento/timemap/5/link/http://kb.dk/>; rel=\"self\"; type=\"application/link-format\"; from=\"Tue, 15 Mar 2005 12:31:51 GMT\"; until=\"Wed, 15 Mar 2023 12:31:51 GMT\",\n" +
            "<http://localhost:8080/solrwayback/services/memento/http://kb.dk/>; rel=\"timegate\",\n" +
            "<http://localhost:8080/solrwayback/services/web/20050315123151/https://kb.dk/>; rel=\"memento\"; datetime=\"Tue, 15 Mar 2005 12:31:51 GMT\",\n" +
            "<http://localhost:8080/solrwayback/services/web/20050315123151/https://kb.dk/>; rel=\"memento\"; datetime=\"Tue, 15 Mar 2005 12:31:51 GMT\",\n" +
            "<http://localhost:8080/solrwayback/services/web/20050315123151/https://kb.dk/>; rel=\"memento\"; datetime=\"Tue, 15 Mar 2005 12:31:51 GMT\",\n" +
            "<http://localhost:8080/solrwayback/services/web/20050315123151/https://kb.dk/>; rel=\"memento\"; datetime=\"Tue, 15 Mar 2005 12:31:51 GMT\",\n" +
            "<http://localhost:8080/solrwayback/services/web/20120315123151/https://kb.dk/>; rel=\"memento\"; datetime=\"Thu, 15 Mar 2012 12:31:51 GMT\",\n" +
            "<http://localhost:8080/solrwayback/services/memento/timemap/4/link/http://kb.dk/>; rel=\"prev\"; type=\"application/link-format\"\n" +
            "<http://localhost:8080/solrwayback/services/memento/timemap/6/link/http://kb.dk/>; rel=\"next\"; type=\"application/link-format\"\n";
    final String testPagedTimeMapLinkLastPage = "<http://kb.dk/>;rel=\"original\",\n" +
            "<http://localhost:8080/solrwayback/services/memento/timemap/20/link/http://kb.dk/>; rel=\"self\"; type=\"application/link-format\"; from=\"Tue, 15 Mar 2005 12:31:51 GMT\"; until=\"Wed, 15 Mar 2023 12:31:51 GMT\",\n"+
            "<http://localhost:8080/solrwayback/services/memento/http://kb.dk/>; rel=\"timegate\",\n" +
            "<http://localhost:8080/solrwayback/services/web/20230315123151/https://kb.dk/>; rel=\"memento\"; datetime=\"Wed, 15 Mar 2023 12:31:51 GMT\",\n" +
            "<http://localhost:8080/solrwayback/services/web/20230315123151/https://kb.dk/>; rel=\"memento\"; datetime=\"Wed, 15 Mar 2023 12:31:51 GMT\",\n" +
            "<http://localhost:8080/solrwayback/services/web/20230315123151/https://kb.dk/>; rel=\"memento\"; datetime=\"Wed, 15 Mar 2023 12:31:51 GMT\",\n" +
            "<http://localhost:8080/solrwayback/services/web/20230315123151/https://kb.dk/>; rel=\"memento\"; datetime=\"Wed, 15 Mar 2023 12:31:51 GMT\",\n" +
            "<http://localhost:8080/solrwayback/services/web/20230315123151/https://kb.dk/>; rel=\"last memento\"; datetime=\"Wed, 15 Mar 2023 12:31:51 GMT\",\n" +
            "<http://localhost:8080/solrwayback/services/memento/timemap/19/link/http://kb.dk/>; rel=\"prev\"; type=\"application/link-format\"\n";


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
        document.addField("source_file_path", "some.warc_" + id);
        document.addField("links", Arrays.asList("http://example.com/everywhere", "http://example.com/mod10_" + id%10));
        document.addField("status_code", "200");
        document.setField("crawl_date", DateUtils.solrTimestampToJavaDate(solrDate));
        document.addField("wayback_date", DateUtils.convertUtcDate2WaybackDate(solrDate));
        embeddedServer.add(document);
    }
}
