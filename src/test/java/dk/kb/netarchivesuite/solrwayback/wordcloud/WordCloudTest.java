package dk.kb.netarchivesuite.solrwayback.wordcloud;

import dk.kb.netarchivesuite.solrwayback.facade.Facade;
import dk.kb.netarchivesuite.solrwayback.service.dto.WordCloudWordAndCount;
import dk.kb.netarchivesuite.solrwayback.solr.NetarchiveSolrTestClient;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.NodeConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Test class for word cloud functionality in SolrWayback.
 * Tests the Facade methods that generate word clouds and word frequency data using embedded Solr.
 */
public class WordCloudTest {
    private static final String SOLR_HOME = Path.of("target/test-classes/solr_9").toAbsolutePath().toString();
    private static CoreContainer coreContainer = null;
    private static EmbeddedSolrServer embeddedServer = null;

    @Before
    public void setUp() throws Exception {
        // Embedded Solr 9.1+ must have absolute home both as env and explicit param
        Path solrHome = Path.of(SOLR_HOME).toAbsolutePath();
        System.setProperty("solr.install.dir", solrHome.toString());
        NodeConfig nodeConfig = new NodeConfig.NodeConfigBuilder("netarchivebuilder", solrHome).build();
        coreContainer = new CoreContainer(nodeConfig);
        coreContainer.load();
        embeddedServer = new EmbeddedSolrServer(coreContainer, "netarchivebuilder");
        NetarchiveSolrTestClient.initializeOverLoadUnitTest(embeddedServer);

        // Remove any items from previous executions
        embeddedServer.deleteByQuery("*:*");

        // Create test documents with meaningful text content
        createTestDocument(1, "word word word word test test sample", "example.com");
        createTestDocument(2, "test word cloud generation with some sample text for testing", "example.com");
        createTestDocument(3, "word frequency analysis and word cloud image generation", "example.com");
        createTestDocument(4, "the quick brown fox jumps over the lazy dog and the cat", "test.com");
        createTestDocument(5, "cloud computing with word processing software", "example.com");

        embeddedServer.commit();
    }

    @After
    public void tearDown() throws Exception {
        if (coreContainer != null) {
            coreContainer.shutdown();
        }
        if (embeddedServer != null) {
            embeddedServer.close();
        }
    }

    /**
     * Helper method to create and add a test document to the embedded Solr server.
     *
     * @param id          Document ID.
     * @param textContent Text content of the document.
     * @param domain      Domain associated with the document.
     * @throws Exception if there is an error adding the document.
     */
    private void createTestDocument(int id, String textContent, String domain) throws Exception {
        SolrInputDocument document = new SolrInputDocument();
        document.addField("id", "doc" + id);
        document.addField("url", "http://" + domain + "/page" + id);
        document.addField("url_norm", "http://" + domain + "/page" + id);
        document.addField("domain", domain);
        document.addField("content_type_norm", "html");
        document.addField("record_type", "response");
        document.addField("source_file_path", "test.warc");
        document.addField("source_file_offset", id);
        document.addField("crawl_date", "2023-01-15T12:00:00Z");
        // Ensure content_text_length meets the minimum threshold used by wordcloud (>=1000)
        int contentLengthForWordcloud = Math.max(1000, textContent.length());
        document.addField("content_text_length", contentLengthForWordcloud);
        // WordCloud generator expects 'content' field (used by NetarchiveSolrClient), include it
        document.addField("content", textContent);
        // Keep original text field as well
        document.addField("text", textContent);
        document.addField("status_code", "200");
        
        embeddedServer.add(document);
    }

    /**
     * Test the deprecated wordCloudForDomain method.
     * Verifies that a BufferedImage is generated for a domain query.
     */
    @Test
    public void testWordCloudForDomain() throws Exception {
        String domain = "example.com";
        BufferedImage result = Facade.wordCloudForDomain(domain);

        // Verify image was generated with correct size
        assertEquals("Image should have expected width", 800, result.getWidth());
        assertEquals("Image should have expected height", 600, result.getHeight());
    }

    /**
     * Test wordCloudForQuery with query and filter.
     * Verifies that a BufferedImage is generated for a custom query.
     */
    @Test
    public void testWordCloudForQuery_WithFilter() throws Exception {
        String query = "domain:example.com";
        String filter = "content_type_norm:html";
        
        BufferedImage result = Facade.wordCloudForQuery(query, filter);

        // Verify image was generated with correct size
        assertEquals("Image should have expected width", 800, result.getWidth());
        assertEquals("Image should have expected height", 600, result.getHeight());
    }

    /**
     * Test wordCloudForQuery without filter (null filter).
     * Verifies that a BufferedImage is generated when no filter is provided.
     */
    @Test
    public void testWordCloudForQuery_WithoutFilter() throws Exception {
        String query = "domain:example.com";
        
        BufferedImage result = Facade.wordCloudForQuery(query, null);

        // Verify image was generated
        assertNotNull("Word cloud image should not be null", result);
        assertTrue("Image width should be greater than 0", result.getWidth() > 0);
        assertTrue("Image height should be greater than 0", result.getHeight() > 0);
    }

    /**
     * Test wordCloudWordFrequency with query and filter.
     * Verifies that word frequency data is correctly generated.
     */
    @Test
    public void testWordCloudWordFrequency_WithFilter() throws Exception {
        String query = "domain:example.com";
        String filter = "content_type_norm:html";
        
        List<WordCloudWordAndCount> result = Facade.wordCloudWordFrequency(query, filter);

        // Verify result is not null and contains data
        assertNotNull("Word frequency list should not be null", result);
        assertFalse("Word frequency list should not be empty", result.isEmpty());

        // Verify each entry has valid data
        for (WordCloudWordAndCount entry : result) {
            assertNotNull("Word should not be null", entry.getWord());
            assertFalse("Word should not be empty", entry.getWord().isEmpty());
            assertTrue("Count should be positive", entry.getCount() > 0);
        }

        // Verify words are from our test documents
        boolean foundTestWord = result.stream()
                .anyMatch(w -> "word".equals(w.getWord()) || "test".equals(w.getWord()) || "cloud".equals(w.getWord()));
        assertTrue("Should find at least one word from sample text", foundTestWord);
    }

    /**
     * Test wordCloudWordFrequency without filter.
     * Verifies that word frequency works correctly without a filter query.
     */
    @Test
    public void testWordCloudWordFrequency_WithoutFilter() throws Exception {
        String query = "domain:example.com";
        
        List<WordCloudWordAndCount> result = Facade.wordCloudWordFrequency(query, null);

        // Verify result is not null and contains data
        assertNotNull("Word frequency list should not be null", result);
        assertFalse("Word frequency list should not be empty", result.isEmpty());
    }

    /**
     * Test word frequency results are sorted by count (descending).
     * Verifies that the most frequent words appear first.
     */
    @Test
    public void testWordCloudWordFrequency_SortedByCount() throws Exception {
        String query = "domain:example.com";
        
        List<WordCloudWordAndCount> result = Facade.wordCloudWordFrequency(query, null);

        // Verify results are sorted by count descending
        assertNotNull("Result should not be null", result);
        assertFalse("Result should not be empty", result.isEmpty());

        // Check that counts are in descending order
        for (int i = 0; i < result.size() - 1; i++) {
            assertTrue(
                    "Words should be sorted by count (descending)",
                    result.get(i).getCount() >= result.get(i + 1).getCount()
            );
        }

        // The most frequent word should be "word" since it appears multiple times in our test data
        WordCloudWordAndCount topWord = result.get(0);
        assertEquals("Most frequent word should be 'word'", "word", topWord.getWord());
        assertTrue("'word' should appear at least 4 times", topWord.getCount() >= 4);
    }

    /**
     * Test handling of query with no results.
     * Verifies graceful handling when query returns no documents.
     */
    @Test
    public void testWordCloudForQuery_NoResults() throws Exception {
        String query = "domain:nonexistent.com";
        
        BufferedImage result = Facade.wordCloudForQuery(query, null);

        // Even with no results, should return a valid (possibly blank) image
        assertNotNull("Word cloud image should not be null even for empty results", result);
    }

    /**
     * Test handling of query with no results for word frequency.
     * Verifies that empty result is returned when query has no matches.
     */
    @Test
    public void testWordCloudWordFrequency_NoResults() throws Exception {
        String query = "domain:nonexistent.com";
        
        List<WordCloudWordAndCount> result = Facade.wordCloudWordFrequency(query, null);

        // Should return empty list for no results
        assertNotNull("Result should not be null", result);
        assertTrue("Result should be empty for no results", result.isEmpty());
    }

    /**
     * Test that stopwords are filtered out in word frequency.
     * Common words like "the", "and" should not appear in results if configured as stopwords.
     */
    @Test
    public void testWordCloudWordFrequency_StopwordsFiltered() throws Exception {
        String query = "domain:test.com"; // This domain has "the", "and", "over" in text
        
        List<WordCloudWordAndCount> result = Facade.wordCloudWordFrequency(query, null);

        // Results might be empty if all words are stopwords or too short
        assertNotNull("Result should not be null", result);
        
        // If there are results, verify stopwords are filtered
        if (!result.isEmpty()) {
            boolean hasCommonStopword = result.stream()
                    .anyMatch(w -> "the".equals(w.getWord()) || "and".equals(w.getWord()));
            
            // Note: This might pass if stopwords are configured in properties
            // The test verifies the filtering mechanism works
            assertFalse("Common stopwords should be filtered out if configured", hasCommonStopword);
        }
    }

    /**
     * Test word cloud generation with specific domain query.
     * Verifies that domain filtering works correctly.
     */
    @Test
    public void testWordCloudForDomain_SpecificDomain() throws Exception {
        // Add a document with very distinctive text for a different domain
        embeddedServer.deleteByQuery("*:*");
        createTestDocument(100, "unique special distinctive remarkable extraordinary", "unique.com");
        embeddedServer.commit();
        
        String domain = "unique.com";
        BufferedImage result = Facade.wordCloudForDomain(domain);

        // Verify image was generated
        assertNotNull("Word cloud image should not be null", result);
        assertTrue("Image width should be greater than 0", result.getWidth() > 0);
        
        // Verify word frequency for the same domain
        List<WordCloudWordAndCount> wordFreq = Facade.wordCloudWordFrequency("domain:\"" + domain + "\"", null);
        assertNotNull("Word frequency should not be null", wordFreq);
        
        // Should find at least one of our distinctive words
        boolean hasDistinctiveWord = wordFreq.stream()
                .anyMatch(w -> "unique".equals(w.getWord()) || 
                               "special".equals(w.getWord()) || 
                               "distinctive".equals(w.getWord()));
        assertTrue("Should find distinctive words from the domain", hasDistinctiveWord);
    }
}
