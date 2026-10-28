package dk.kb.netarchivesuite.solrwayback.playback;

import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;
import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDoc;

import org.junit.Test;

import static dk.kb.netarchivesuite.solrwayback.playback.PlaybackTestData.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests {@link CssPlayback} with the Solr backend mocked out using Mockito.mockStatic.
 */
public class CssPlaybackTest extends PlaybackTestBase {

    private static final String CSS_URL = "http://example.com/css/main.css";

    /** Leading @import found in the archive is rewritten to a downloadRaw link. */
    @Test
    public void testResolvedImportIsRewritten() throws Exception {
        when(solrClient.findClosestHarvestTimeForUrl("http://example.com/css/base.css", WAYBACK_DATE))
                .thenReturn(createIndexDoc("somesourcefile", 42L));

        String css = playback("@import url(\"base.css\");\nbody { color: red; }\n", false);

        assertTrue("Resolved import should point to downloadRaw, but got:\n" + css,
                   css.startsWith("@import url(\"" + BASEURL + "services/downloadRaw?source_file_path=somesourcefile&offset=42\");"));
        assertTrue(css.contains("body { color: red; }"));
    }

    /** Leading @import not found in the archive is rewritten to the notfound link. */
    @Test
    public void testUnresolvedImportIsNotFound() throws Exception {
        String css = playback("@import url(\"missing.css\");\nbody { color: red; }\n", false);

        assertTrue("Unresolved import should point to notfound, but got:\n" + css,
                   css.startsWith("@import url(\"" + BASEURL + "services/notfound/\");"));
    }

    /** Relative @import URLs are resolved against the CSS URL and looked up at the wayback date. */
    @Test
    public void testImportIsResolvedAgainstCssUrl() throws Exception {
        playback("@import \"../shared/theme.css\";\n", false);

        verify(solrClient).findClosestHarvestTimeForUrl("http://example.com/shared/theme.css", WAYBACK_DATE);
    }

    /** All consecutive @import lines at the start of the CSS are rewritten. */
    @Test
    public void testMultipleLeadingImportsAreRewritten() throws Exception {
        when(solrClient.findClosestHarvestTimeForUrl("http://example.com/css/a.css", WAYBACK_DATE))
                .thenReturn(createIndexDoc("somesourcefile", 1L));
        when(solrClient.findClosestHarvestTimeForUrl("http://example.com/css/b.css", WAYBACK_DATE))
                .thenReturn(createIndexDoc("somesourcefile", 2L));

        String css = playback("@import url(\"a.css\");\n@import url(\"b.css\");\nbody {}\n", false);

        assertTrue(css.contains(BASEURL + "services/downloadRaw?source_file_path=somesourcefile&offset=1"));
        assertTrue(css.contains(BASEURL + "services/downloadRaw?source_file_path=somesourcefile&offset=2"));
    }

    /** An @import after other rules is not rewritten, as only leading @import lines are processed. */
    @Test
    public void testNonLeadingImportIsUntouched() throws Exception {
        String input = "body { color: red; }\n@import url(\"late.css\");\n";

        assertEquals(input, playback(input, false));
        verifyNoInteractions(solrClient);
    }

    /** url() references in rules are currently not rewritten. */
    @Test
    public void testUrlInRuleIsUntouched() throws Exception {
        String input = "body { background: url(\"bg.png\"); }\n";

        assertEquals(input, playback(input, false));
        verifyNoInteractions(solrClient);
    }

    /** Non-ASCII content survives the round trip through the ArcEntry. */
    @Test
    public void testNonAsciiContentIsPreserved() throws Exception {
        String input = "/* æøå ÆØÅ € */\nbody { font-family: \"Ærø\"; }\n";

        assertEquals(input, playback(input, false));
    }

    /** No toolbar is ever injected into CSS, even when showToolbar is true. */
    @Test
    public void testToolbarIsNeverInjected() throws Exception {
        String input = "body { color: red; }\n";

        ArcEntry result = new CssPlayback(createArcEntry(input), createPageDoc(), true).playback(false);

        assertEquals(input, result.getStringContentAsStringSafe());
        verify(solrClient, never()).getArcEntry(anyString(), anyLong());
    }

    /** The lenient flag is currently ignored, so the same lookup is made either way. */
    @Test
    public void testLenientIsIgnored() throws Exception {
        String input = "@import url(\"base.css\");\n";

        assertEquals(playback(input, false), playback(input, true));
        verify(solrClient, times(2)).findClosestHarvestTimeForUrl("http://example.com/css/base.css", WAYBACK_DATE);
        verify(solrClient, never()).findNearestUrlsShort(anyCollection(), anyString(), anyBoolean());
    }

    /** The given ArcEntry is returned and marked as decompressed. */
    @Test
    public void testArcEntryIsMarkedDecompressed() throws Exception {
        ArcEntry arc = createArcEntry("body {}\n");
        arc.setHasBeenDecompressed(false);

        ArcEntry result = new CssPlayback(arc, createPageDoc(), false).playback(false);

        assertSame(arc, result);
        assertTrue(result.isHasBeenDecompressed());
    }

    private String playback(String css, boolean lenient) throws Exception {
        CssPlayback playback = new CssPlayback(createArcEntry(css), createPageDoc(), false);
        return playback.playback(lenient).getStringContentAsStringSafe();
    }

    private static ArcEntry createArcEntry(String css) {
        ArcEntry arc = new ArcEntry();
        arc.setUrl(CSS_URL);
        arc.setCrawlDate(CRAWL_DATE);
        arc.setWaybackDate(WAYBACK_DATE);
        arc.setContentType("text/css");
        arc.setStringContent(css);
        return arc;
    }

    private static IndexDoc createPageDoc() {
        IndexDoc doc = new IndexDoc();
        doc.setUrl(CSS_URL);
        doc.setCrawlDate(CRAWL_DATE);
        doc.setContentType("text/css");
        doc.setSource_file_path(SOURCE_FILE);
        doc.setOffset(OFFSET);
        return doc;
    }

    private static IndexDoc createIndexDoc(String sourceFile, long offset) {
        IndexDoc doc = new IndexDoc();
        doc.setSource_file_path(sourceFile);
        doc.setOffset(offset);
        return doc;
    }
}
