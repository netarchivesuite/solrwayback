package dk.kb.netarchivesuite.solrwayback.playback;

import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;
import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDoc;

import org.junit.Test;

import static dk.kb.netarchivesuite.solrwayback.playback.PlaybackTestData.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Tests {@link JavascriptPlayback} with the Solr backend mocked out using Mockito.mockStatic.
 * <p>
 * JavascriptPlayback delegates to the CSS rewriter, which only handles leading {@code @import} lines,
 * so in practice JavaScript is passed through unchanged.
 */
public class JavascriptPlaybackTest extends PlaybackTestBase {

    private static final String JS_URL = "http://example.com/js/main.js";

    /** JavaScript is passed through unchanged without querying Solr. */
    @Test
    public void testJavascriptIsUnchanged() throws Exception {
        String input = "function hello() {\n  console.log('hello');\n}\n";

        assertEquals(input, playback(input, false));
        verifyNoInteractions(solrClient);
    }

    /** URLs in JavaScript are currently not rewritten. */
    @Test
    public void testUrlsAreUntouched() throws Exception {
        String input =
                "var css = \"http://example.com/css/main.css\";\n" +
                "fetch('/api/data.json').then(r => r.json());\n" +
                "document.write('<script src=\"lib.js\"></script>');\n";

        assertEquals(input, playback(input, false));
        verifyNoInteractions(solrClient);
    }

    /** Minified JavaScript on a single line is passed through unchanged. */
    @Test
    public void testMinifiedJavascriptIsUnchanged() throws Exception {
        String input = "!function(e){var t={};function n(r){return t[r]}n.p=\"/static/\"}([]);";

        assertEquals(input, playback(input, false));
    }

    /** Non-ASCII content survives the round trip through the ArcEntry. */
    @Test
    public void testNonAsciiContentIsPreserved() throws Exception {
        String input = "var greeting = \"Hej æøå ÆØÅ € \\u00e6\";\n";

        assertEquals(input, playback(input, false));
    }

    /** No toolbar is ever injected into JavaScript, even when showToolbar is true. */
    @Test
    public void testToolbarIsNeverInjected() throws Exception {
        String input = "var x = 1;\n";

        ArcEntry result = new JavascriptPlayback(createArcEntry(input), createPageDoc(), true).playback(false);

        assertEquals(input, result.getStringContentAsStringSafe());
        verifyNoInteractions(solrClient);
    }

    /** The lenient flag is currently ignored. */
    @Test
    public void testLenientIsIgnored() throws Exception {
        String input = "var x = 1;\n";

        assertEquals(playback(input, false), playback(input, true));
        verifyNoInteractions(solrClient);
    }

    /** The given ArcEntry is returned and marked as decompressed. */
    @Test
    public void testArcEntryIsMarkedDecompressed() throws Exception {
        ArcEntry arc = createArcEntry("var x = 1;\n");
        arc.setHasBeenDecompressed(false);

        ArcEntry result = new JavascriptPlayback(arc, createPageDoc(), false).playback(false);

        assertSame(arc, result);
        assertTrue(result.isHasBeenDecompressed());
    }

    private String playback(String js, boolean lenient) throws Exception {
        JavascriptPlayback playback = new JavascriptPlayback(createArcEntry(js), createPageDoc(), false);
        return playback.playback(lenient).getStringContentAsStringSafe();
    }

    private static ArcEntry createArcEntry(String js) {
        ArcEntry arc = new ArcEntry();
        arc.setUrl(JS_URL);
        arc.setCrawlDate(CRAWL_DATE);
        arc.setWaybackDate(WAYBACK_DATE);
        arc.setContentType("application/javascript");
        arc.setStringContent(js);
        return arc;
    }

    private static IndexDoc createPageDoc() {
        IndexDoc doc = new IndexDoc();
        doc.setUrl(JS_URL);
        doc.setCrawlDate(CRAWL_DATE);
        doc.setContentType("application/javascript");
        doc.setSource_file_path(SOURCE_FILE);
        doc.setOffset(OFFSET);
        return doc;
    }
}
