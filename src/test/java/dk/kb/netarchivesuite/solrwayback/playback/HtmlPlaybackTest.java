package dk.kb.netarchivesuite.solrwayback.playback;

import dk.kb.netarchivesuite.solrwayback.normalise.Normalisation;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;
import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDoc;
import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDocShort;
import dk.kb.netarchivesuite.solrwayback.solr.WaybackStatistics;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;

import static dk.kb.netarchivesuite.solrwayback.playback.PlaybackTestData.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests {@link HtmlPlayback} with the Solr backend mocked out using Mockito.mockStatic.
 */
public class HtmlPlaybackTest extends PlaybackTestBase {

    private static final String PAGE_URL = "http://example.com/folder/index.html";

    private static final String TOOLBAR_MARKER = "BEGIN WAYBACK TOOLBAR INSERT";

    @Before
    public void setUpStubs() throws Exception {
        // By default nothing is found in the archive
        when(solrClient.findNearestUrlsShort(anyCollection(), anyString(), anyBoolean())).thenReturn(new ArrayList<>());
        when(solrClient.getArcEntry(SOURCE_FILE, OFFSET)).thenReturn(createIndexDoc("text/html"));
        when(solrClient.getWayBackStatistics(anyInt(), anyString(), anyString(), anyString())).thenReturn(createStats());
    }

    /** Resources found in the archive are rewritten to downloadRaw links. */
    @Test
    public void testResolvedResourceIsRewritten() throws Exception {
        String imageUrl = "http://example.com/folder/image.png";
        when(solrClient.findNearestUrlsShort(anyCollection(), anyString(), anyBoolean()))
                .thenAnswer(invocation -> resolveOnly(invocation.getArgument(0), imageUrl, 42L));

        String html = playback("<html><head></head><body><img src=\"image.png\"></body></html>", "text/html", false);

        assertTrue("Resolved image should point to downloadRaw, but got:\n" + html,
                   html.contains(BASEURL + "services/downloadRaw?source_file_path=somesourcefile&amp;offset=42"));
    }

    /** Resources not found in the archive are rewritten to the notfound link. */
    @Test
    public void testUnresolvedResourceIsNotFound() throws Exception {
        String html = playback("<html><head></head><body><img src=\"missing.png\"></body></html>", "text/html", false);

        assertTrue("Unresolved image should point to notfound, but got:\n" + html,
                   html.contains(BASEURL + "services/notfound/"));
    }

    /** Anchors are rewritten to web playback links without being resolved. */
    @Test
    public void testAnchorIsRewrittenWithoutResolving() throws Exception {
        String html = playback("<html><head></head><body><a href=\"other.html\">x</a></body></html>", "text/html", false);

        assertTrue("Anchor should point to web playback, but got:\n" + html,
                   html.contains(BASEURL + "services/web/" + WAYBACK_DATE + "/http://example.com/folder/other.html"));
    }

    /** Meta refresh URLs are rewritten to web playback links. */
    @Test
    public void testMetaRefreshIsRewritten() throws Exception {
        String html = playback(
                "<html><head><meta http-equiv=\"refresh\" content=\"0;url=http://example.com/new.html\"></head>" +
                "<body></body></html>", "text/html", false);

        assertTrue("Meta refresh should point to web playback, but got:\n" + html,
                   html.contains("url=" + BASEURL + "services/web/" + WAYBACK_DATE + "/http://example.com/new.html"));
    }

    /** No toolbar is injected and Solr is not queried for it when showToolbar is false. */
    @Test
    public void testNoToolbar() throws Exception {
        String html = playback("<html><head></head><body><p>hello</p></body></html>", "text/html", false);

        assertFalse("Toolbar should not be injected", html.contains(TOOLBAR_MARKER));
        assertTrue(html.contains("<p>hello</p>"));
        verify(solrClient, never()).getArcEntry(anyString(), anyLong());
    }

    /** The toolbar is injected when showToolbar is true. */
    @Test
    public void testToolbarInjected() throws Exception {
        String html = playback("<html><head></head><body><p>hello</p></body></html>", "text/html", true);

        assertTrue("Toolbar should be injected, but got:\n" + html, html.contains(TOOLBAR_MARKER));
        assertTrue(html.contains("<p>hello</p>"));
        verify(solrClient).getArcEntry(SOURCE_FILE, OFFSET);
    }

    /** XHTML pages are serialised with XML syntax when the toolbar is injected. */
    @Test
    public void testToolbarInjectedInXhtml() throws Exception {
        // XHTML output uses XML syntax, so void elements must be self-closed
        String html = playback(
                "<html><head></head><body><p>hello<br>world</p></body></html>", "application/xhtml+xml", true);

        assertTrue("Toolbar should be injected, but got:\n" + html, html.contains(TOOLBAR_MARKER));
        assertTrue("Expected self-closed <br />, but got:\n" + html, html.contains("<br />"));
    }

    /** HTML pages are serialised with HTML syntax when the toolbar is injected. */
    @Test
    public void testToolbarInjectedInHtml() throws Exception {
        String html = playback("<html><head></head><body><p>hello<br>world</p></body></html>", "text/html", true);

        assertTrue("Expected plain <br>, but got:\n" + html, html.contains("<br>"));
        assertFalse(html.contains("<br />"));
    }

    /** The page is returned without toolbar if fetching toolbar statistics fails. */
    @Test
    public void testToolbarFailureFallsBackToPage() throws Exception {
        when(solrClient.getWayBackStatistics(anyInt(), anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Solr down"));

        String html = playback("<html><head></head><body><p>hello</p></body></html>", "text/html", true);

        assertFalse(html.contains(TOOLBAR_MARKER));
        assertTrue(html.contains("<p>hello</p>"));
    }

    /** The given ArcEntry is returned and marked as decompressed. */
    @Test
    public void testArcEntryIsMarkedDecompressed() throws Exception {
        ArcEntry arc = createArcEntry("<html><body></body></html>");
        arc.setHasBeenDecompressed(false);

        ArcEntry result = new HtmlPlayback(arc, createIndexDoc("text/html"), false).playback(false);

        assertSame(arc, result);
        assertTrue(result.isHasBeenDecompressed());
    }

    /** The lenient flag is passed on to the Solr URL resolver. */
    @Test
    public void testLenientIsPassedToResolver() throws Exception {
        playback("<html><head></head><body><img src=\"image.png\"></body></html>", "text/html", false, true);

        verify(solrClient).findNearestUrlsShort(anyCollection(), eq(CRAWL_DATE), eq(true));
    }

    private String playback(String html, String contentType, boolean showToolbar) throws Exception {
        return playback(html, contentType, showToolbar, false);
    }

    private String playback(String html, String contentType, boolean showToolbar, boolean lenient) throws Exception {
        ArcEntry arc = createArcEntry(html);
        HtmlPlayback playback = new HtmlPlayback(arc, createIndexDoc(contentType), showToolbar);
        return playback.playback(lenient).getStringContentAsStringSafe();
    }

    private static ArcEntry createArcEntry(String html) {
        ArcEntry arc = new ArcEntry();
        arc.setUrl(PAGE_URL);
        arc.setCrawlDate(CRAWL_DATE);
        arc.setWaybackDate(WAYBACK_DATE);
        arc.setContentType("text/html");
        arc.setStringContent(html);
        return arc;
    }

    private static IndexDoc createIndexDoc(String contentType) {
        IndexDoc doc = new IndexDoc();
        doc.setUrl(PAGE_URL);
        doc.setUrl_norm(Normalisation.canonicaliseURL(PAGE_URL));
        doc.setCrawlDate(CRAWL_DATE);
        doc.setContentType(contentType);
        doc.setSource_file_path(SOURCE_FILE);
        doc.setOffset(OFFSET);
        doc.setStatusCode(200);
        return doc;
    }

    private static WaybackStatistics createStats() {
        WaybackStatistics stats = new WaybackStatistics();
        stats.setUrl(PAGE_URL);
        stats.setUrl_norm(Normalisation.canonicaliseURL(PAGE_URL));
        stats.setStatusCode(200);
        stats.setNumberOfHarvest(3);
        stats.setFirstHarvestDate("2019-01-01T00:00:00Z");
        stats.setLastHarvestDate("2021-01-01T00:00:00Z");
        return stats;
    }

    // Resolves only the given URL, everything else is not found
    private static ArrayList<IndexDocShort> resolveOnly(Collection<String> urls, String url, long offset) {
        ArrayList<IndexDocShort> docs = new ArrayList<>();
        if (urls.contains(url)) {
            IndexDocShort doc = new IndexDocShort();
            doc.setUrl(url);
            doc.setUrl_norm(Normalisation.canonicaliseURL(url));
            doc.setSource_file_path("somesourcefile");
            doc.setOffset(offset);
            docs.add(doc);
        }
        return docs;
    }
}
