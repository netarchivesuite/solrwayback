package dk.kb.netarchivesuite.solrwayback.solr;

import dk.kb.netarchivesuite.solrwayback.UnitTestUtils;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoaderWeb;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.NodeConfig;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.Assert.*;

/**
 * Tests for {@link SolrStreamingExportClient} using an embedded Solr instance.
 */
public class SolrStreamingExportClientTest {
    private static final String SOLR_HOME = "target/test-classes/solr_9";
    private static CoreContainer coreContainer = null;
    private static EmbeddedSolrServer embeddedServer = null;
    private static final int DOCS = 10;

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
        NetarchiveSolrTestClient.initializeOverLoadUnitTest(embeddedServer);

        // Ensure clean state
        embeddedServer.deleteByQuery("*:*");

        fillSolr();

        embeddedServer.commit();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (embeddedServer != null) {
            embeddedServer.close();
        }
        if (coreContainer != null) {
            coreContainer.shutdown();
        }
    }

    private static void fillSolr() throws SolrServerException, IOException {
        for (int i = 0; i < DOCS; i++) {
            SolrInputDocument document = new SolrInputDocument();
            document.setField("id", "doc_" + i);
            document.setField("domain", "example.com");
            document.setField("title", "title_" + (i % 5));
            document.setField("url", "http://example.com/page/" + i);
            embeddedServer.add(document);
        }
    }

    /**
     * Create exporter and assert default page size.
     */
    @Test
    public void testCreateExporterAndGetPageSize() {
        SolrStreamingExportClient exporter = SolrStreamingExportClient.createCvsExporter(embeddedServer, "*:*", "id,domain");
        assertNotNull(exporter);
        assertEquals("Page size should be the default", SolrStreamingExportClient.DEFAULT_PAGE_SIZE, exporter.getPageSize());
    }

    /**
     * Export CSV and assert header plus one data row per document.
     */
    @Test
    public void testExportNextAndContent() throws Exception {
        SolrStreamingExportClient exporter = SolrStreamingExportClient.createCvsExporter(embeddedServer, "*:*", "id,domain");

        StringBuilder all = new StringBuilder();
        String part;
        int loops = 0;
        // iterate until exporter returns empty strings
        while ((part = exporter.next()) != null && !part.isEmpty() && loops < DOCS + 5) {
            all.append(part);
            loops++;
        }

        String csv = all.toString();
        assertTrue("CSV should contain header fields", csv.startsWith("id,domain\n"));

        // Remove trailing newline(s) added by GenerateCSV
        while (csv.endsWith("\n") || csv.endsWith("\r")) {
            csv = csv.substring(0, csv.length() - 1);
        }

        String[] lines = csv.split("\n");
        // header + DOCS data lines
        assertEquals("CSV should have header + number of docs lines", DOCS + 1, lines.length);

        // Validate every data line: parse two CSV fields (handles quoted values) and assert contents.
        for (int row = 1; row < lines.length; row++) {
            String line = lines[row];
            String[] parsed = parseTwoCsvColumns(line);
            assertEquals("Each data row should have 2 columns", 2, parsed.length);
            String id = parsed[0];
            String domain = parsed[1];
            assertTrue("id should start with doc_ but was: " + id, id.startsWith("doc_"));
            assertEquals("domain should equal example.com", "example.com", domain);
        }
    }

    /**
     * Parse a CSV line with exactly two columns where fields may be quoted and internal quotes are doubled.
     */
    private static String[] parseTwoCsvColumns(String line) {
        if (line == null) return new String[0];
        String first;
        String second;
        line = line.trim();
        if (line.startsWith("\"")) {
            // find closing quote that is not a doubled quote
            int i = 1;
            while (i < line.length()) {
                int idx = line.indexOf('"', i);
                if (idx == -1) break;
                // if next char is also '"', it's escaped quote
                if (idx + 1 < line.length() && line.charAt(idx + 1) == '"') {
                    i = idx + 2; // skip escaped quote
                    continue;
                }
                // found closing
                first = line.substring(1, idx).replaceAll("\"\"", "\"");
                // remainder should start with comma
                int rem = idx + 1;
                if (rem < line.length() && line.charAt(rem) == ',') {
                    second = line.substring(rem + 1).trim();
                    second = unquote(second);
                    return new String[]{first, second};
                } else {
                    // malformed, fall back
                    break;
                }
            }
        }
        // fallback: split on first comma
        int comma = line.indexOf(',');
        if (comma == -1) return new String[]{line};
        first = line.substring(0, comma).trim();
        second = line.substring(comma + 1).trim();
        first = unquote(first);
        second = unquote(second);
        return new String[]{first, second};
    }

    private static String unquote(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1).replaceAll("\"\"", "\"");
        }
        return s;
    }

    /**
     * Constructor should throw when fields are empty.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorValidatesFields() {
        // Empty field list should throw IllegalArgumentException before trying to iterate
        new SolrStreamingExportClient(embeddedServer, 10, "", "", false, false, "*:*");
    }
}
