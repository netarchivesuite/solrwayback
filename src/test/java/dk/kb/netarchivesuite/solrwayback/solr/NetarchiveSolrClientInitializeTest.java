package dk.kb.netarchivesuite.solrwayback.solr;

import dk.kb.netarchivesuite.solrwayback.UnitTestUtils;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoaderWeb;
import org.apache.solr.client.solrj.SolrClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for {@link NetarchiveSolrClient#initialize(String)}
 */
public class NetarchiveSolrClientInitializeTest {

    @Before
    public void setUp() throws Exception {
        // Load unit test properties so PropertiesLoader.SOLR_SERVER etc. are populated
        PropertiesLoader.initProperties(UnitTestUtils.getFile("properties/solrwayback_unittest.properties").getPath());
        PropertiesLoaderWeb.initProperties(UnitTestUtils.getFile("properties/solrwaybackweb_unittest.properties").getPath());
        // Disable IndexWatcher during unit tests to avoid background timer threads
        PropertiesLoader.SOLR_SERVER_CHECK_INTERVAL = 0;
    }

    @After
    public void tearDown() {
        // Reset static fields to avoid leaking state between tests
        NetarchiveSolrClient.solrServer = null;
        NetarchiveSolrClient.noCacheSolrServer = null;
        NetarchiveSolrClient.instance = null;
        NetarchiveSolrClient.indexWatcher = null;
    }

    @Test
    public void testInitialize_withCachingEnabled() {
        // Ensure caching is enabled in properties
        PropertiesLoader.SOLR_SERVER_CACHING = true;

        NetarchiveSolrClient.initialize(PropertiesLoader.SOLR_SERVER);

        assertNotNull("Instance should be initialized", NetarchiveSolrClient.instance);
        assertNotNull("solrServer should be set", NetarchiveSolrClient.solrServer);
        assertNotNull("noCacheSolrServer should be set", NetarchiveSolrClient.noCacheSolrServer);

        // When caching is enabled, solrServer should be a CachingSolrClient and noCacheSolrServer should be the inner client
        assertTrue("solrServer should be a CachingSolrClient when caching enabled",
                NetarchiveSolrClient.solrServer instanceof dk.kb.netarchivesuite.solrwayback.solr.CachingSolrClient);
        assertTrue("noCacheSolrServer should be a RestrictedSolrClient",
                NetarchiveSolrClient.noCacheSolrServer instanceof dk.kb.netarchivesuite.solrwayback.solr.RestrictedSolrClient);

        // IndexWatcher should not be created because we set check interval to 0
        assertNull("indexWatcher should be null when check interval is 0", NetarchiveSolrClient.indexWatcher);
    }

    @Test
    public void testInitialize_withCachingDisabled() {
        // Ensure caching is disabled
        PropertiesLoader.SOLR_SERVER_CACHING = false;

        NetarchiveSolrClient.initialize(PropertiesLoader.SOLR_SERVER);

        assertNotNull("Instance should be initialized", NetarchiveSolrClient.instance);
        assertNotNull("solrServer should be set", NetarchiveSolrClient.solrServer);
        assertNotNull("noCacheSolrServer should be set", NetarchiveSolrClient.noCacheSolrServer);

        // When caching is disabled, solrServer should be the same type as noCacheSolrServer (RestrictedSolrClient)
        assertTrue("solrServer should be a RestrictedSolrClient when caching disabled",
                NetarchiveSolrClient.solrServer instanceof dk.kb.netarchivesuite.solrwayback.solr.RestrictedSolrClient);
        assertTrue("noCacheSolrServer should be a RestrictedSolrClient",
                NetarchiveSolrClient.noCacheSolrServer instanceof dk.kb.netarchivesuite.solrwayback.solr.RestrictedSolrClient);

        // IndexWatcher should not be created because we set check interval to 0
        assertNull("indexWatcher should be null when check interval is 0", NetarchiveSolrClient.indexWatcher);
    }
}

