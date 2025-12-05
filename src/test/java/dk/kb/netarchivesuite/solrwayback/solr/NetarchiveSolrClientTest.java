package dk.kb.netarchivesuite.solrwayback.solr;

import dk.kb.netarchivesuite.solrwayback.UnitTestUtils;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoaderWeb;
import dk.kb.netarchivesuite.solrwayback.service.dto.FacetCount;
import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDoc;
import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDocShort;
import dk.kb.netarchivesuite.solrwayback.service.dto.SearchResult;
import dk.kb.netarchivesuite.solrwayback.util.DateUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.NodeConfig;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for central {@link NetarchiveSolrClient}.
 */
public class NetarchiveSolrClientTest {
    private static final String SOLR_HOME = "target/test-classes/solr_9";
    private static CoreContainer coreContainer = null;
    private static EmbeddedSolrServer embeddedServer = null;
    private static final int TEST_DOCS = 50;

    @BeforeClass
    public static void setUp() throws Exception {
        PropertiesLoader.initProperties(UnitTestUtils.getFile("properties/solrwayback_unittest.properties").getPath());
        PropertiesLoaderWeb.initProperties(UnitTestUtils.getFile("properties/solrwaybackweb_unittest.properties").getPath());

        Path solrHome = Path.of(SOLR_HOME).toAbsolutePath();
        System.setProperty("solr.install.dir", solrHome.toString());
        NodeConfig nodeConfig = new NodeConfig.NodeConfigBuilder("netarchivebuilder", solrHome).build();
        coreContainer = new CoreContainer(nodeConfig);
        coreContainer.load();
        embeddedServer = new EmbeddedSolrServer(coreContainer, "netarchivebuilder");

        // Initialize static test docs
        new SolrTestUtils();

        // Initialize NetarchiveSolrClient with embedded server for tests
        NetarchiveSolrTestClient.initializeOverLoadUnitTest(embeddedServer);
    }

    @Before
    public void setUpEach() throws Exception {
        // This runs before every @Test method
        embeddedServer.deleteByQuery("*:*");
        fillSolr();
        embeddedServer.commit();
        // any lightweight per-test setup goes here
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (embeddedServer != null) embeddedServer.close();
        if (coreContainer != null) coreContainer.shutdown();
    }

    private static void fillSolr() throws SolrServerException, IOException {
        for (int i = 0; i < TEST_DOCS; i++) {
            SolrInputDocument doc = new SolrInputDocument();
            doc.setField("id", "doc_" + i);
            doc.setField("title", "title_" + (i % 5));
            doc.setField("url", "http://example.com/page/" + i);
            doc.setField("url_norm", "http://example.com/page/" + i);
            doc.setField("source_file_path", "file" + (i % 3));
            doc.setField("source_file_offset", (long) i);
            doc.setField("content_type", "text/html");
            doc.setField("crawl_date", DateUtils.solrTimestampToJavaDate("2019-03-15T12:31:5" + (i%9) + "Z"));
            doc.setField("domain", "example.com");
            embeddedServer.add(doc);
        }
    }

    /**
     * Test for {@link NetarchiveSolrClient#search(String, String, int)}: ensure it returns results for a title query.
     */
    @Test
    public void testSearchByTitle() throws Exception {
        NetarchiveSolrClient client = NetarchiveSolrClient.getInstance();
        assertNotNull("NetarchiveSolrClient instance should be initialized", client);

        // search for documents with title_3
        SearchResult result = client.search("title:title_3", null, 100);
        assertNotNull(result);
        assertTrue("Search should return at least one result", result.getNumberOfResults() > 0);
        assertFalse("Result list should not be empty", result.getResults().isEmpty());

        // All returned docs should have title 'title_3'
        boolean allMatch = result.getResults().stream().allMatch(d -> "title_3".equals(d.getTitle()));
        assertTrue("All returned IndexDocs should have title_3", allMatch);
    }

    /**
     * Test for {@link NetarchiveSolrClient#numberOfDocuments()}: ensure it reports the number of docs in the index.
     */
    @Test
    public void testNumberOfDocuments() throws Exception {
        NetarchiveSolrClient client = NetarchiveSolrClient.getInstance();
        assertNotNull("NetarchiveSolrClient instance should be initialized", client);

        long count = client.numberOfDocuments();
        assertEquals("numberOfDocuments should match inserted docs", TEST_DOCS, count);
    }

    /**
     * Test for {@link NetarchiveSolrClient#idLookupResponse(String, String)}:
     * ensure a JSON response is returned for a known id and contains the requested fields.
     */
    @Test
    public void testIdLookupResponse() throws Exception {
        String id = "lookup_doc_1";
        String title = "Lookup Title";
        String domain = "lookup.example.com";

        SolrInputDocument doc = new SolrInputDocument();
        doc.setField("id", id);
        doc.setField("title", title);
        doc.setField("domain", domain);
        doc.setField("url", "http://lookup.example.com/page");
        doc.setField("url_norm", "http://lookup.example.com/page");
        doc.setField("crawl_date", DateUtils.solrTimestampToJavaDate("2021-11-01T00:00:00Z"));
        embeddedServer.add(doc);
        embeddedServer.commit();

        NetarchiveSolrClient client = NetarchiveSolrClient.getInstance();
        assertNotNull(client);

        // Request only id and title in the field list
        String jsonResponse = client.idLookupResponse(id, "id,title,domain");

        System.out.println("JSON Response: " + jsonResponse);
        assertNotNull("idLookupResponse should not return null", jsonResponse);

        // Parse JSON using Jackson and assert structure/contents
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonResponse);
        assertTrue("Root should be an object", root.isObject());
        JsonNode docs = root.path("docs");
        assertTrue("'docs' should be an array", docs.isArray());
        assertFalse("There should be at least one doc", docs.isEmpty());

        JsonNode first = docs.get(0);
        assertEquals("id should match", id, first.path("id").asText());
        assertEquals("title should match", title, first.path("title").asText());
        assertEquals("domain should match", domain, first.path("domain").asText());
    }

    @Test
    public void testFindNearestHarvestTimeForMultipleUrlsFullFields() throws Exception {
        // Prepare a clean index with two URLs, each having two harvests at different times
        embeddedServer.deleteByQuery("*:*");

        embeddedServer.add(SolrTestUtils.doc1);
        embeddedServer.add(SolrTestUtils.doc2);
        embeddedServer.add(SolrTestUtils.doc3);
        embeddedServer.add(SolrTestUtils.doc4);
        embeddedServer.commit();
        NetarchiveSolrClient client = NetarchiveSolrClient.getInstance();
        ArrayList<IndexDoc> results = client.findNearestHarvestTimeForMultipleUrlsFullFields(
                Arrays.asList(SolrTestUtils.url1, SolrTestUtils.url2), "2019-03-15T12:11:00Z");

        // Expect two results, one per URL
        assertEquals(2, results.size());

        // Map url -> crawl_date for easy assertions
        Map<String, String> map = new HashMap<>();
        for (IndexDoc idx : results) {
            map.put(idx.getUrl(), idx.getCrawlDate());
        }

        // url1: nearest to 12:11 is 12:00 (diff 11m) vs 12:30 (19m)
        assertEquals("2019-03-15T12:00:00Z", map.get(SolrTestUtils.url1));
        // url2: nearest to 12:11 is 12:45 (diff 34m) vs 11:00 (71m)
        assertEquals("2019-03-15T12:45:00Z", map.get(SolrTestUtils.url2));

        // Assert that the returned IndexDocs contain the expected fields
        Map<String, String> srcMap = new HashMap<>();
        for (IndexDoc idx : results) {
            assertNotNull("id should be present", idx.getId());
            assertNotNull("url should be present", idx.getUrl());
            assertNotNull("url_norm should be present", idx.getUrl_norm());
            assertNotNull("source_file_path should be present", idx.getSource_file_path());
            assertTrue("offset should be >= 0", idx.getOffset() >= 0);
            assertNotNull("crawlDate should be present", idx.getCrawlDate());
            assertNotNull("contentType should be present", idx.getContentType());
            srcMap.put(idx.getUrl(), idx.getSource_file_path());
        }
        assertEquals("file1_offset", srcMap.get(SolrTestUtils.url1));
        assertEquals("file4_offset", srcMap.get(SolrTestUtils.url2));
    }

    @Test
    public void testFindNearestHarvestTimeForMultipleUrlsFewFields() throws Exception {
        // Prepare a clean index with two URLs, each having two harvests at different times
        embeddedServer.deleteByQuery("*:*");

        embeddedServer.add(SolrTestUtils.doc1);
        embeddedServer.add(SolrTestUtils.doc2);
        embeddedServer.add(SolrTestUtils.doc3);
        embeddedServer.add(SolrTestUtils.doc4);
        embeddedServer.commit();

        NetarchiveSolrClient client = NetarchiveSolrClient.getInstance();
        ArrayList<IndexDocShort> results = client.findNearestHarvestTimeForMultipleUrlsFewFields(
                Arrays.asList(SolrTestUtils.url1, SolrTestUtils.url2), "2019-03-15T12:11:00Z");

        // Expect two results, one per URL
        assertEquals(2, results.size());

        // Map url -> crawl_date for easy assertions
        Map<String, String> map = new HashMap<>();
        for (IndexDocShort idx : results) {
            map.put(idx.getUrl(), idx.getCrawlDate());
        }

        // url1: nearest to 12:11 is 12:00:00
        assertEquals("2019-03-15T12:00:00Z", map.get(SolrTestUtils.url1));
        // url2: nearest to 12:11 is 12:45:00
        assertEquals("2019-03-15T12:45:00Z", map.get(SolrTestUtils.url2));

        // Assert short fields are returned and correct
        Map<String, String> srcMapShort = new HashMap<>();
        for (IndexDocShort idx : results) {
            assertNotNull("url should be present", idx.getUrl());
            assertNotNull("url_norm should be present", idx.getUrl_norm());
            assertNotNull("source_file_path should be present", idx.getSource_file_path());
            assertTrue("offset should be >= 0", idx.getOffset() >= 0);
            assertNotNull("crawlDate should be present", idx.getCrawlDate());
            srcMapShort.put(idx.getUrl(), idx.getSource_file_path());
        }
        assertEquals("file1_offset", srcMapShort.get(SolrTestUtils.url1));
        assertEquals("file4_offset", srcMapShort.get(SolrTestUtils.url2));
    }

    @Test
    public void testIsSolrAvailable() {
        NetarchiveSolrClient client = NetarchiveSolrClient.getInstance();
        // By default (test initialization) there is no IndexWatcher, so availability should be undetermined (null)
        assertNull("isSolrAvailable should be null when IndexWatcher is not started", client.isSolrAvailable());

        // Simulate index status changes by toggling the protected field directly (same package access)
        client.solrAvailable = Boolean.TRUE;
        assertTrue("isSolrAvailable should return true after setting solrAvailable to true", client.isSolrAvailable());

        client.solrAvailable = Boolean.FALSE;
        assertFalse("isSolrAvailable should return false after setting solrAvailable to false", client.isSolrAvailable());

        // Reset to null to avoid side effects for other tests
        client.solrAvailable = null;
    }

    @Test
    public void testGetDomainFacets() throws Exception {
        // Clear index and add documents demonstrating ingoing and outgoing links
        embeddedServer.deleteByQuery("*:*");

        // Doc A: domain alpha links to beta and gamma
        SolrInputDocument a = new SolrInputDocument();
        a.setField("id", "a");
        a.setField("domain", "alpha.com");
        a.setField("links_domains", Arrays.asList("beta.com", "gamma.com"));
        a.setField("crawl_date", DateUtils.solrTimestampToJavaDate("2020-01-15T12:00:00Z"));
        embeddedServer.add(a);

        // Doc B: domain beta links to alpha
        SolrInputDocument b = new SolrInputDocument();
        b.setField("id", "b");
        b.setField("domain", "beta.com");
        b.setField("links_domains", Arrays.asList("alpha.com"));
        b.setField("crawl_date", DateUtils.solrTimestampToJavaDate("2020-01-16T12:00:00Z"));
        embeddedServer.add(b);

        // Doc C: domain gamma links to alpha
        SolrInputDocument c = new SolrInputDocument();
        c.setField("id", "c");
        c.setField("domain", "gamma.com");
        c.setField("links_domains", Arrays.asList("alpha.com"));
        c.setField("crawl_date", DateUtils.solrTimestampToJavaDate("2020-01-17T12:00:00Z"));
        embeddedServer.add(c);

        // Commit
        embeddedServer.commit();

        NetarchiveSolrClient client = NetarchiveSolrClient.getInstance();

        Date start = DateUtils.solrTimestampToJavaDate("2020-01-01T00:00:00Z");
        Date end = DateUtils.solrTimestampToJavaDate("2020-12-31T23:59:59Z");

        // Outgoing for alpha.com should include beta.com and gamma.com
        List<FacetCount> outgoing = client.getDomainFacets("alpha.com", 10, false, start, end);
        assertNotNull(outgoing);
        // Convert to map for easy lookup
        Map<String, Long> outMap = new HashMap<>();
        for (FacetCount fc : outgoing) {
            outMap.put(fc.getValue(), fc.getCount());
        }
        assertEquals(Long.valueOf(1), outMap.get("beta.com"));
        assertEquals(Long.valueOf(1), outMap.get("gamma.com"));

        // Ingoing for alpha.com should include beta.com and gamma.com (both link to alpha)
        List<FacetCount> ingoing = client.getDomainFacets("alpha.com", 10, true, start, end);
        assertNotNull(ingoing);
        Map<String, Long> inMap = new HashMap<>();
        for (FacetCount fc : ingoing) {
            inMap.put(fc.getValue(), fc.getCount());
        }
        // beta and gamma should be present
        assertEquals(Long.valueOf(1), inMap.get("beta.com"));
        assertEquals(Long.valueOf(1), inMap.get("gamma.com"));
    }

    @Test
    public void testGetWayBackStatistics() throws Exception {
        // Prepare a clean index with two harvests for same url_norm in the same domain
        embeddedServer.deleteByQuery("*:*");

        String urlNorm = "http://example.com/page/wayback";
        String domain = "example.com";

        SolrInputDocument before = new SolrInputDocument();
        before.setField("id", "wb_before");
        before.setField("url_norm", urlNorm);
        before.setField("url", "http://example.com/page/wayback");
        before.setField("domain", domain);
        before.setField("content_length", 1000);
        before.setField("crawl_date", DateUtils.solrTimestampToJavaDate("2019-03-15T11:00:00Z"));

        SolrInputDocument after = new SolrInputDocument();
        after.setField("id", "wb_after");
        after.setField("url_norm", urlNorm);
        after.setField("url", "http://example.com/page/wayback");
        after.setField("domain", domain);
        after.setField("content_length", 3000);
        after.setField("crawl_date", DateUtils.solrTimestampToJavaDate("2019-03-15T12:30:00Z"));

        embeddedServer.add(before);
        embeddedServer.add(after);
        embeddedServer.commit();

        NetarchiveSolrClient client = NetarchiveSolrClient.getInstance();

        // Call the method for a timestamp between the two harvests
        WaybackStatistics stats = client.getWayBackStatistics(200, "http://example.com/page/wayback", urlNorm, "2019-03-15T12:00:00Z");

        assertNotNull(stats);
        // Url and url_norm should be set
        assertEquals(urlNorm, stats.getUrl_norm());
        assertEquals("http://example.com/page/wayback", stats.getUrl());

        // The earliest and latest harvests should be the ones we inserted
        assertEquals("2019-03-15T11:00:00Z", stats.getFirstHarvestDate());
        assertEquals("2019-03-15T12:30:00Z", stats.getLastHarvestDate());

        // Next (after the input date) should be 12:30 and previous should be 11:00
        assertEquals("2019-03-15T12:30:00Z", stats.getNextHarvestDate());
        assertEquals("2019-03-15T11:00:00Z", stats.getPreviousHarvestDate());

        // Number of harvests should be after+before+1 (as implemented)
        assertEquals(3L, stats.getNumberOfHarvest());

        // Domain aggregates
        assertEquals(domain, stats.getDomain());
        assertEquals(2L, stats.getNumberHarvestDomain());
        assertEquals(4000L, stats.getDomainHarvestTotalContentLength());
    }
}
