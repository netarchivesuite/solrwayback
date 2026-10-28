package dk.kb.netarchivesuite.solrwayback.playback;

import dk.kb.netarchivesuite.solrwayback.UnitTestUtils;
import dk.kb.netarchivesuite.solrwayback.normalise.Normalisation;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoaderWeb;
import dk.kb.netarchivesuite.solrwayback.solr.NetarchiveSolrClient;

import org.junit.After;
import org.junit.Before;
import org.mockito.MockedStatic;

import static dk.kb.netarchivesuite.solrwayback.playback.PlaybackTestData.BASEURL;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

/**
 * Common setup for the {@link PlaybackHandler} tests: loads the unit test properties and replaces
 * {@link NetarchiveSolrClient#getInstance()} with {@link #solrClient} using Mockito.mockStatic.
 * <p>
 * Subclasses can add stubs in their own {@code @Before} method, which JUnit runs after {@link #setUpPlayback()}.
 */
abstract class PlaybackTestBase {

    private MockedStatic<NetarchiveSolrClient> solrStatic;

    /** Mocked Solr client returned by {@link NetarchiveSolrClient#getInstance()}. Finds nothing unless stubbed. */
    protected NetarchiveSolrClient solrClient;

    @Before
    public void setUpPlayback() throws Exception {
        PropertiesLoader.initProperties(UnitTestUtils.getFile("properties/solrwayback_unittest.properties").getPath());
        PropertiesLoaderWeb.initProperties(UnitTestUtils.getFile("properties/solrwaybackweb_unittest.properties").getPath());
        Normalisation.setTypeFromConfig();
        PropertiesLoader.WAYBACK_BASEURL = BASEURL;

        solrClient = mock(NetarchiveSolrClient.class);
        solrStatic = mockStatic(NetarchiveSolrClient.class);
        solrStatic.when(NetarchiveSolrClient::getInstance).thenReturn(solrClient);
    }

    @After
    public void tearDownPlayback() {
        solrStatic.close();
    }
}
