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

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.NodeConfig;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
/*
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

 */
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.Assert.*;
import static org.junit.Assert.assertNotNull;

public class TimeMapTest {
    private static final Logger log = LoggerFactory.getLogger(TimeMapTest.class);
    public static final int TEST_DOCS = 20; // Changing this might make some unit tests fail
    private static String SOLR_HOME = "target/test-classes/solr_9";
    private static CoreContainer coreContainer= null;
    private static EmbeddedSolrServer embeddedServer = null;

    @BeforeClass
    public static void setUp() throws Exception {
        log.info("Setting up embedded server");
        PropertiesLoader.initProperties(UnitTestUtils.getFile("properties/solrwayback_unittest.properties").getPath());
        PropertiesLoader.MEMENTO_TIMEMAP_PAGESIZE = 10;
        PropertiesLoader.MEMENTO_TIMEMAP_PAGINGLIMIT = 10000;
        PropertiesLoaderWeb.initProperties(UnitTestUtils.getFile("properties/solrwaybackweb_unittest.properties").getPath());

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
        log.info("Embedded server ready with timemap paginglimit set to: '{}'", PropertiesLoader.MEMENTO_TIMEMAP_PAGINGLIMIT);
        assertEquals(10000, PropertiesLoader.MEMENTO_TIMEMAP_PAGINGLIMIT);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        embeddedServer.close();
        coreContainer.shutdown();
    }

    @Test
    public void serverAvailable() throws SolrServerException, IOException {
        SolrQuery query = new SolrQuery("*:*");
        assertTrue("There should be some results", embeddedServer.query(query).getResults().getNumFound() > 0);
    }

    @Test
    public void timeMapLinkConstruction() throws IOException, URISyntaxException {
        assertEquals(10000, PropertiesLoader.MEMENTO_TIMEMAP_PAGINGLIMIT);
        // Set very high to disable paging
        TimeMap timeMap = new TimeMap();
        StreamingOutput streamingOutput = timeMap.getTimeMap(new URI("http://kb.dk/"), "link", 0);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        streamingOutput.write(output);
        String timeMapString = new String(output.toByteArray(), StandardCharsets.UTF_8);
        
        String timeMapStringLocalhost=UnitTestUtils.replaceHostNameWithLocalHost(timeMapString);        
        assertEquals(testTimeMapLinkFormat, timeMapStringLocalhost);
    }

    @Test
    public void timeMapJsonConstruction() throws IOException, URISyntaxException {
        assertEquals(10000, PropertiesLoader.MEMENTO_TIMEMAP_PAGINGLIMIT);
        // Set very high to disable paging
        TimeMap timeMap = new TimeMap();
        StreamingOutput streamingOutput = timeMap.getTimeMap(new URI("http://kb.dk/"), "spec", 0);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        streamingOutput.write(output);
        String timeMapString = new String(output.toByteArray(), StandardCharsets.UTF_8);
        String timeMapStringLocalhost=UnitTestUtils.replaceHostNameWithLocalHost(timeMapString);        
        assertEquals(testTimeMapJSON, timeMapStringLocalhost);
    }

    /** saveFirstAndLastDate should update first/last wayback dates and derived memento strings. */
    @Test
    public void testSaveFirstAndLastDate_updatesMetadata() throws Exception {
        MementoMetadata meta = new MementoMetadata();

        // Create three documents with different wayback_date values
        SolrDocument doc1 = new SolrDocument();
        doc1.setField("wayback_date", 20000101000000L);

        SolrDocument doc2 = new SolrDocument();
        doc2.setField("wayback_date", 19990101000000L);

        SolrDocument doc3 = new SolrDocument();
        doc3.setField("wayback_date", 20200101000000L);

        Method m = TimeMap.class.getDeclaredMethod("saveFirstAndLastDate", SolrDocument.class, MementoMetadata.class);
        m.setAccessible(true);

        // apply in order doc1, doc2, doc3
        m.invoke(null, doc1, meta);
        assertEquals(20000101000000L, meta.getFirstWaybackDate());
        assertEquals(20000101000000L, meta.getLastWaybackDate());
        // derived strings should be set
        assertNotNull(meta.getFirstMemento());
        assertNotNull(meta.getLastMemento());

        m.invoke(null, doc2, meta);
        // first should now be doc2
        assertEquals(19990101000000L, meta.getFirstWaybackDate());
        assertEquals(20000101000000L, meta.getLastWaybackDate());

        m.invoke(null, doc3, meta);
        // last should now be doc3
        assertEquals(19990101000000L, meta.getFirstWaybackDate());
        assertEquals(20200101000000L, meta.getLastWaybackDate());

        // verify the derived memento strings match DateUtils conversion
        String expectedFirst = DateUtils.convertWaybackdate2Mementodate(meta.getFirstWaybackDate());
        String expectedLast = DateUtils.convertWaybackdate2Mementodate(meta.getLastWaybackDate());
        assertEquals(expectedFirst, meta.getFirstMemento());
        assertEquals(expectedLast, meta.getLastMemento());
    }

    /** getPage should paginate a stream according to configured page size. */
    @Test
    public void testGetPage_paginationBoundaries() {
        // Ensure deterministic page size for this test
        int originalPageSize = PropertiesLoader.MEMENTO_TIMEMAP_PAGESIZE;
        PropertiesLoader.MEMENTO_TIMEMAP_PAGESIZE = 3;
        try {
            // prepare a stream of 7 dummy SolrDocuments
            Stream<SolrDocument> docs = IntStream.range(0, 7).mapToObj(i -> {
                SolrDocument d = new SolrDocument();
                d.setField("id", "doc" + i);
                return d;
            });

            // page 1: expect items 0..2
            TimeMap.Page<SolrDocument> page1 = TimeMap.getPage(docs, 1, 7);
            assertEquals(Integer.valueOf(1), page1.getPageNumber());
            assertEquals(Integer.valueOf(PropertiesLoader.MEMENTO_TIMEMAP_PAGESIZE), page1.getResultsPerPage());
            assertEquals(Long.valueOf(7L), page1.getTotalResults());
            List<SolrDocument> items1 = page1.getItems().collect(Collectors.toList());
            assertEquals(3, items1.size());
            assertEquals("doc0", items1.get(0).getFieldValue("id"));

            // new stream for page 2 (streams cannot be reused)
            Stream<SolrDocument> docs2 = IntStream.range(0, 7).mapToObj(i -> {
                SolrDocument d = new SolrDocument();
                d.setField("id", "doc" + i);
                return d;
            });
            TimeMap.Page<SolrDocument> page2 = TimeMap.getPage(docs2, 2, 7);
            List<SolrDocument> items2 = page2.getItems().collect(Collectors.toList());
            assertEquals(3, items2.size());
            assertEquals("doc3", items2.get(0).getFieldValue("id"));

            // page 3 has only one element
            Stream<SolrDocument> docs3 = IntStream.range(0, 7).mapToObj(i -> {
                SolrDocument d = new SolrDocument();
                d.setField("id", "doc" + i);
                return d;
            });
            TimeMap.Page<SolrDocument> page3 = TimeMap.getPage(docs3, 3, 7);
            List<SolrDocument> items3 = page3.getItems().collect(Collectors.toList());
            assertEquals(1, items3.size());
            assertEquals("doc6", items3.get(0).getFieldValue("id"));
        } finally {
            PropertiesLoader.MEMENTO_TIMEMAP_PAGESIZE = originalPageSize;
        }
    }

    /** Page getters and setters should behave as normal. */
    @Test
    public void testPage_gettersSetters() {
        Stream<SolrDocument> s = Stream.empty();
        TimeMap.Page<SolrDocument> page = new TimeMap.Page<>(2, 10L, s);
        assertEquals(Integer.valueOf(2), page.getPageNumber());
        assertEquals(Long.valueOf(10L), page.getTotalResults());
        page.setPageNumber(5);
        page.setResultsPerPage(11);
        page.setTotalResults(20L);
        assertEquals(Integer.valueOf(5), page.getPageNumber());
        assertEquals(Integer.valueOf(11), page.getResultsPerPage());
        assertEquals(Long.valueOf(20L), page.getTotalResults());
        Stream<SolrDocument> newItems = Stream.of(new SolrDocument());
        page.setItems(newItems);
        assertNotNull(page.getItems());
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
        document.addField("source_file_path", "some.warc_" + id);
        document.addField("links", Arrays.asList("http://example.com/everywhere", "http://example.com/mod10_" + id%10));
        document.addField("status_code", "200");
        document.setField("crawl_date", DateUtils.solrTimestampToJavaDate(solrDate));
        document.addField("wayback_date", DateUtils.convertUtcDate2WaybackDate(solrDate));
        embeddedServer.add(document);
    }

  
    
    final String testTimeMapJSON = "{\"original_uri\":\"http://kb.dk/\",\"timegate_uri\":\"http://localhost:8080/solrwayback/services/memento/http://kb.dk/\"," +
            "\"timemap_uri\":{\"link_format\":\"http://localhost:8080/solrwayback/services/memento/timemap/link/http://kb.dk/\"," +
            "\"json_format\":\"http://localhost:8080/solrwayback/services/memento/timemap/json/http://kb.dk/\"}," +
            "\"mementos\":{\"first\":{\"datetime\":\"Tue, 15 Mar 2005 12:31:51 GMT\",\"uri\":\"http://localhost:8080/solrwayback/services/web/20050315123151/http://kb.dk/\"}," +
            "\"last\":{\"datetime\":\"Wed, 15 Mar 2023 12:31:51 GMT\",\"uri\":\"http://localhost:8080/solrwayback/services/web/20230315123151/http://kb.dk/\"}" +
            ",\"list\":[{\"datetime\":\"Tue, 15 Mar 2005 12:31:51 GMT\",\"uri\":\"http://localhost:8080/solrwayback/services/web/20050315123151/http://kb.dk/\"}" +
            ",{\"datetime\":\"Tue, 15 Mar 2005 12:31:51 GMT\",\"uri\":\"http://localhost:8080/solrwayback/services/web/20050315123151/http://kb.dk/\"}," +
            "{\"datetime\":\"Tue, 15 Mar 2005 12:31:51 GMT\",\"uri\":\"http://localhost:8080/solrwayback/services/web/20050315123151/http://kb.dk/\"}," +
            "{\"datetime\":\"Tue, 15 Mar 2005 12:31:51 GMT\",\"uri\":\"http://localhost:8080/solrwayback/services/web/20050315123151/http://kb.dk/\"}," +
            "{\"datetime\":\"Thu, 15 Mar 2012 12:31:51 GMT\",\"uri\":\"http://localhost:8080/solrwayback/services/web/20120315123151/http://kb.dk/\"}," +
            "{\"datetime\":\"Thu, 15 Mar 2012 12:31:51 GMT\",\"uri\":\"http://localhost:8080/solrwayback/services/web/20120315123151/http://kb.dk/\"}," +
            "{\"datetime\":\"Thu, 15 Mar 2012 12:31:51 GMT\",\"uri\":\"http://localhost:8080/solrwayback/services/web/20120315123151/http://kb.dk/\"}," +
            "{\"datetime\":\"Thu, 15 Mar 2012 12:31:51 GMT\",\"uri\":\"http://localhost:8080/solrwayback/services/web/20120315123151/http://kb.dk/\"}," +
            "{\"datetime\":\"Thu, 15 Mar 2012 12:31:51 GMT\",\"uri\":\"http://localhost:8080/solrwayback/services/web/20120315123151/http://kb.dk/\"}," +
            "{\"datetime\":\"Thu, 15 Mar 2012 12:31:51 GMT\",\"uri\":\"http://localhost:8080/solrwayback/services/web/20120315123151/http://kb.dk/\"}," +
            "{\"datetime\":\"Thu, 15 Mar 2012 12:31:51 GMT\",\"uri\":\"http://localhost:8080/solrwayback/services/web/20120315123151/http://kb.dk/\"}," +
            "{\"datetime\":\"Sun, 15 Mar 2020 12:31:51 GMT\",\"uri\":\"http://localhost:8080/solrwayback/services/web/20200315123151/http://kb.dk/\"}," +
            "{\"datetime\":\"Sun, 15 Mar 2020 12:31:51 GMT\",\"uri\":\"http://localhost:8080/solrwayback/services/web/20200315123151/http://kb.dk/\"}," +
            "{\"datetime\":\"Sun, 15 Mar 2020 12:31:51 GMT\",\"uri\":\"http://localhost:8080/solrwayback/services/web/20200315123151/http://kb.dk/\"}," +
            "{\"datetime\":\"Sun, 15 Mar 2020 12:31:51 GMT\",\"uri\":\"http://localhost:8080/solrwayback/services/web/20200315123151/http://kb.dk/\"}," +
            "{\"datetime\":\"Sun, 15 Mar 2020 12:31:51 GMT\",\"uri\":\"http://localhost:8080/solrwayback/services/web/20200315123151/http://kb.dk/\"}," +
            "{\"datetime\":\"Sun, 15 Mar 2020 12:31:51 GMT\",\"uri\":\"http://localhost:8080/solrwayback/services/web/20200315123151/http://kb.dk/\"}," +
            "{\"datetime\":\"Wed, 15 Mar 2023 12:31:51 GMT\",\"uri\":\"http://localhost:8080/solrwayback/services/web/20230315123151/http://kb.dk/\"}," +
            "{\"datetime\":\"Wed, 15 Mar 2023 12:31:51 GMT\",\"uri\":\"http://localhost:8080/solrwayback/services/web/20230315123151/http://kb.dk/\"}," +
            "{\"datetime\":\"Wed, 15 Mar 2023 12:31:51 GMT\",\"uri\":\"http://localhost:8080/solrwayback/services/web/20230315123151/http://kb.dk/\"}]}}";
    final String testTimeMapLinkFormat = "<http://kb.dk/>;rel=\"original\",\n" +
            "<http://localhost:8080/solrwayback/services/memento/timemap/link/http://kb.dk/>" +
            "; rel=\"self\"; type=\"application/link-format\"" +
            "; from=\"Tue, 15 Mar 2005 12:31:51 GMT\"; until=\"Wed, 15 Mar 2023 12:31:51 GMT\",\n" +
            "<http://localhost:8080/solrwayback/services/memento/http://kb.dk/>" +
            "; rel=\"timegate\",\n" +
            "<http://localhost:8080/solrwayback/services/web/20050315123151/https://kb.dk/>; rel=\"first memento\"; datetime=\"Tue, 15 Mar 2005 12:31:51 GMT\"; collection=\"netarchivebuilder\",\n" +
            "<http://localhost:8080/solrwayback/services/web/20050315123151/https://kb.dk/>; rel=\"memento\"; datetime=\"Tue, 15 Mar 2005 12:31:51 GMT\"; collection=\"netarchivebuilder\",\n" +
            "<http://localhost:8080/solrwayback/services/web/20050315123151/https://kb.dk/>; rel=\"memento\"; datetime=\"Tue, 15 Mar 2005 12:31:51 GMT\"; collection=\"netarchivebuilder\",\n" +
            "<http://localhost:8080/solrwayback/services/web/20050315123151/https://kb.dk/>; rel=\"memento\"; datetime=\"Tue, 15 Mar 2005 12:31:51 GMT\"; collection=\"netarchivebuilder\",\n" +
            "<http://localhost:8080/solrwayback/services/web/20120315123151/https://kb.dk/>; rel=\"memento\"; datetime=\"Thu, 15 Mar 2012 12:31:51 GMT\"; collection=\"netarchivebuilder\",\n" +
            "<http://localhost:8080/solrwayback/services/web/20120315123151/https://kb.dk/>; rel=\"memento\"; datetime=\"Thu, 15 Mar 2012 12:31:51 GMT\"; collection=\"netarchivebuilder\",\n" +
            "<http://localhost:8080/solrwayback/services/web/20120315123151/https://kb.dk/>; rel=\"memento\"; datetime=\"Thu, 15 Mar 2012 12:31:51 GMT\"; collection=\"netarchivebuilder\",\n" +
            "<http://localhost:8080/solrwayback/services/web/20120315123151/https://kb.dk/>; rel=\"memento\"; datetime=\"Thu, 15 Mar 2012 12:31:51 GMT\"; collection=\"netarchivebuilder\",\n" +
            "<http://localhost:8080/solrwayback/services/web/20120315123151/https://kb.dk/>; rel=\"memento\"; datetime=\"Thu, 15 Mar 2012 12:31:51 GMT\"; collection=\"netarchivebuilder\",\n" +
            "<http://localhost:8080/solrwayback/services/web/20120315123151/https://kb.dk/>; rel=\"memento\"; datetime=\"Thu, 15 Mar 2012 12:31:51 GMT\"; collection=\"netarchivebuilder\",\n" +
            "<http://localhost:8080/solrwayback/services/web/20120315123151/https://kb.dk/>; rel=\"memento\"; datetime=\"Thu, 15 Mar 2012 12:31:51 GMT\"; collection=\"netarchivebuilder\",\n" +
            "<http://localhost:8080/solrwayback/services/web/20200315123151/https://kb.dk/>; rel=\"memento\"; datetime=\"Sun, 15 Mar 2020 12:31:51 GMT\"; collection=\"netarchivebuilder\",\n" +
            "<http://localhost:8080/solrwayback/services/web/20200315123151/https://kb.dk/>; rel=\"memento\"; datetime=\"Sun, 15 Mar 2020 12:31:51 GMT\"; collection=\"netarchivebuilder\",\n" +
            "<http://localhost:8080/solrwayback/services/web/20200315123151/https://kb.dk/>; rel=\"memento\"; datetime=\"Sun, 15 Mar 2020 12:31:51 GMT\"; collection=\"netarchivebuilder\",\n" +
            "<http://localhost:8080/solrwayback/services/web/20200315123151/https://kb.dk/>; rel=\"memento\"; datetime=\"Sun, 15 Mar 2020 12:31:51 GMT\"; collection=\"netarchivebuilder\",\n" +
            "<http://localhost:8080/solrwayback/services/web/20200315123151/https://kb.dk/>; rel=\"memento\"; datetime=\"Sun, 15 Mar 2020 12:31:51 GMT\"; collection=\"netarchivebuilder\",\n" +
            "<http://localhost:8080/solrwayback/services/web/20200315123151/https://kb.dk/>; rel=\"memento\"; datetime=\"Sun, 15 Mar 2020 12:31:51 GMT\"; collection=\"netarchivebuilder\",\n" +
            "<http://localhost:8080/solrwayback/services/web/20230315123151/https://kb.dk/>; rel=\"memento\"; datetime=\"Wed, 15 Mar 2023 12:31:51 GMT\"; collection=\"netarchivebuilder\",\n" +
            "<http://localhost:8080/solrwayback/services/web/20230315123151/https://kb.dk/>; rel=\"memento\"; datetime=\"Wed, 15 Mar 2023 12:31:51 GMT\"; collection=\"netarchivebuilder\",\n" +
            "<http://localhost:8080/solrwayback/services/web/20230315123151/https://kb.dk/>; rel=\"last memento\"; datetime=\"Wed, 15 Mar 2023 12:31:51 GMT\"; collection=\"netarchivebuilder\",\n";

}
