package dk.kb.netarchivesuite.solrwayback.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import dk.kb.netarchivesuite.solrwayback.normalise.Normalisation;
import dk.kb.netarchivesuite.solrwayback.normalise.Normalisation.NormaliseType;
import dk.kb.netarchivesuite.solrwayback.service.SolrWaybackResource;
import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDoc;
import dk.kb.netarchivesuite.solrwayback.service.exception.SolrWaybackServiceException;
import dk.kb.netarchivesuite.solrwayback.solr.NetarchiveSolrClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

/**
 * Unit tests for {@link PathResolver#waybackAPIResolverHelper}.
 *
 * <p>The Solr lookup is stubbed out (via a {@link NetarchiveSolrClient} test subclass installed as the
 * singleton instance), so these tests focus on the URL handling that happens before the lookup: extracting
 * the URL-R and Wayback date from the request URI and normalising the protocol part (e.g. fixing
 * {@code http:/} → {@code http://}). The URL and date that {@code waybackAPIResolverHelper} would query Solr
 * with are captured and asserted.</p>
 *
 * <p>Note: concrete classes such as {@link SolrWaybackResource} and {@link NetarchiveSolrClient} are not
 * mocked with Mockito here (the inline mock maker cannot instrument them in this JVM); lightweight test
 * subclasses are used instead. Only the JAX-RS interfaces are mocked.</p>
 */
public class PathResolverWaybackAPIResolverTest {

    private static final String BASE_PATH = "/web/";
    private static final String WAYBACK_DATE = "20120101120000";

    private UriInfo uriInfo;
    private HttpServletRequest httpRequest;

    /** Response returned by the stubbed playback; identity is asserted to ensure it is passed through. */
    private final Response viewImplResponse = Response.ok().build();

    /** Captures what waybackAPIResolverHelper passes on to Solr and returns a canned IndexDoc. */
    private static class CapturingSolrClient extends NetarchiveSolrClient {
        String capturedUrl;
        String capturedDate;
        final IndexDoc docToReturn;

        CapturingSolrClient(IndexDoc docToReturn) {
            this.docToReturn = docToReturn;
        }

        @Override
        public IndexDoc findClosestHarvestTimeForUrl(String url, String timeStamp) {
            this.capturedUrl = url;
            this.capturedDate = timeStamp;
            return docToReturn;
        }

        /** Installs this client as the singleton returned by {@link NetarchiveSolrClient#getInstance()}. */
        void install() {
            instance = this; // 'instance' is a protected static field of the superclass.
        }

        static void uninstall() {
            instance = null;
        }
    }

    private CapturingSolrClient solrClient;

    @Before
    public void setup() {
        Normalisation.setType(NormaliseType.NORMAL);

        uriInfo = Mockito.mock(UriInfo.class);
        httpRequest = Mockito.mock(HttpServletRequest.class);

        // A non-HTML doc whose crawl date matches the requested date, so no redirect happens and the
        // helper proceeds directly to playback.
        IndexDoc doc = new IndexDoc();
        doc.setContentTypeNorm("image");
        doc.setCrawlDate("2012-01-01T12:00:00");
        doc.setSource_file_path("test.warc");
        doc.setOffset(0L);

        solrClient = new CapturingSolrClient(doc);
        solrClient.install();
    }

    @After
    public void tearDown() {
        CapturingSolrClient.uninstall();
        Normalisation.setType(NormaliseType.NORMAL);
    }

    /** SolrWaybackResource stub whose playback methods avoid touching any Solr/IO. */
    private SolrWaybackResource stubResource() {
        return new SolrWaybackResource() {
            @Override
            public Response viewImpl(String source_file_path, long offset, Boolean showToolbar, Boolean lenient) {
                return viewImplResponse;
            }

            @Override
            public Response downloadRaw(String source_file_path, long offset) {
                return viewImplResponse;
            }

            @Override
            public SolrWaybackServiceException handleServiceExceptions(Exception e) {
                // Surface the underlying error directly so test failures are easy to diagnose.
                throw new RuntimeException("Unexpected exception in waybackAPIResolverHelper", e);
            }
        };
    }

    /**
     * Drives {@code waybackAPIResolverHelper} with the given full request URI and returns the URL that the helper
     * ends up querying Solr with, i.e. the result of the URL extraction and protocol normalisation.
     */
    private String resolveAndCaptureSolrUrl(String fullRequestUri) throws Exception {
        Mockito.when(uriInfo.getRequestUri()).thenReturn(URI.create(fullRequestUri));

        Response response = PathResolver.waybackAPIResolverHelper(
                stubResource(), BASE_PATH, uriInfo, httpRequest, "ignored", false);

        // The playback response is passed straight through.
        assertSame(viewImplResponse, response);
        return solrClient.capturedUrl;
    }

    @Test
    public void testUrlIsExtractedFromRequestUri() throws Exception {
        String solrUrl = resolveAndCaptureSolrUrl(
                "http://localhost:8080/solrwayback/services" + BASE_PATH + WAYBACK_DATE + "/http://example.com/page.html");

        assertEquals("http://example.com/page.html", solrUrl);
    }

    @Test
    public void testSingleSlashHttpIsNormalisedToDoubleSlash() throws Exception {
        // Some webservers collapse "http://" in the path into "http:/". The helper must repair this.
        String solrUrl = resolveAndCaptureSolrUrl(
                "http://localhost:8080/solrwayback/services" + BASE_PATH + WAYBACK_DATE + "/http:/example.com/page.html");

        assertEquals("http://example.com/page.html", solrUrl);
    }

    @Test
    public void testSingleSlashHttpsIsNormalisedAndRewrittenToHttp() throws Exception {
        // The single slash is repaired to "https://", and canonicalisation then rewrites https → http.
        String solrUrl = resolveAndCaptureSolrUrl(
                "http://localhost:8080/solrwayback/services" + BASE_PATH + WAYBACK_DATE + "/https:/example.com/page.html");

        assertEquals("http://example.com/page.html", solrUrl);
    }

    @Test
    public void testUrlWithQueryParametersIsPreserved() throws Exception {
        String solrUrl = resolveAndCaptureSolrUrl(
                "http://localhost:8080/solrwayback/services" + BASE_PATH + WAYBACK_DATE + "/http://example.com/search?q=foo&x=1");

        assertEquals("http://example.com/search?q=foo&x=1", solrUrl);
    }

    @Test
    public void testUrlWithWWWGetsRemoved() throws Exception {
        String solrUrl = resolveAndCaptureSolrUrl(
                "http://localhost:8080/solrwayback/services" + BASE_PATH + WAYBACK_DATE + "/http://www.elvalledeloscaidos.es:80/img/loteria%202012.txt");

        // www-prefix removed and (in NORMAL mode) the port stripped, matching the canonical url_norm in the index.
        assertEquals("http://elvalledeloscaidos.es/img/loteria%202012.txt", solrUrl);
    }


    /**
     * The Wayback date in the request URI is split off from the URL and converted to a Solr timestamp before the
     * lookup.
     */
    @Test
    public void testWaybackDateIsConvertedToSolrDate() throws Exception {
        resolveAndCaptureSolrUrl(
                "http://localhost:8080/solrwayback/services" + BASE_PATH + WAYBACK_DATE + "/http://example.com/page.html");

        assertEquals("2012-01-01T12:00:00Z", solrClient.capturedDate);
    }
}
