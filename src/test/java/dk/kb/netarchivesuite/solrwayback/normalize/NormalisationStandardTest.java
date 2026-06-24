package dk.kb.netarchivesuite.solrwayback.normalize;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import dk.kb.netarchivesuite.solrwayback.normalise.Normalisation;
import dk.kb.netarchivesuite.solrwayback.normalise.Normalisation.NormaliseType;
import dk.kb.netarchivesuite.solrwayback.normalise.NormalisationStandard;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link NormalisationStandard#canonicaliseURL(String)} and
 * {@link NormalisationStandard#canonicaliseURL(String, boolean, boolean)}.
 *
 * Note that port handling in {@code canonicaliseURL} depends on the globally configured
 * {@link NormaliseType}, so each test sets the type explicitly.
 */
public class NormalisationStandardTest {

    @Before
    public void setup() {
        // The default and most common configuration.
        Normalisation.setType(NormaliseType.NORMAL);
    }

    @After
    public void tearDown() {
        // Restore the default so other test classes are not affected by the LEGACY tests below.
        Normalisation.setType(NormaliseType.NORMAL);
    }

    // --- Null and empty input ---

    @Test
    public void testNullIsReturnedUnchanged() {
        assertNull(NormalisationStandard.canonicaliseURL(null));
    }

    @Test
    public void testEmptyIsReturnedUnchanged() {
        assertEquals("", NormalisationStandard.canonicaliseURL(""));
    }

    // --- Protocol and host normalisation ---

    @Test
    public void testHttpsIsRewrittenToHttp() {
        assertEquals("http://example.com/", NormalisationStandard.canonicaliseURL("https://example.com/"));
    }

    @Test
    public void testHostAndPathAreLowercased() {
        assertEquals("http://example.com/foo", NormalisationStandard.canonicaliseURL("http://Example.COM/Foo"));
    }

    @Test
    public void testMissingProtocolGetsHttpPrefix() {
        assertEquals("http://example.com/", NormalisationStandard.canonicaliseURL("example.com"));
    }

    // --- www prefix handling ---

    @Test
    public void testWwwPrefixRemovedWhenCreateUnambiguous() {
        assertEquals("http://example.com/foo",
                NormalisationStandard.canonicaliseURL("http://www.example.com/foo", true, true));
    }

    @Test
    public void testTwoLetterWwwPrefixRemovedWhenCreateUnambiguous() {
        // "ww2" (two letters) is not handled by the underlying canonicaliser, only by the custom WWW_PREFIX block.
        assertEquals("http://example.com/foo",
                NormalisationStandard.canonicaliseURL("http://ww2.example.com/foo", true, true));
    }

    @Test
    public void testTwoLetterWwwPrefixKeptWhenNotCreateUnambiguous() {
        // Plain "www." is stripped by the underlying AggressiveUrlCanonicalizer regardless of createUnambiguous.
        // The createUnambiguous flag only governs the custom prefix handling for variants like ww/ww2.
        assertEquals("http://ww2.example.com/foo",
                NormalisationStandard.canonicaliseURL("http://ww2.example.com/foo", true, false));
    }

    // --- Trailing slash handling ---

    @Test
    public void testTrailingSlashRemovedFromPath() {
        assertEquals("http://example.com/foo", NormalisationStandard.canonicaliseURL("http://example.com/foo/"));
    }

    @Test
    public void testDomainOnlyKeepsSingleTrailingSlash() {
        assertEquals("http://example.com/", NormalisationStandard.canonicaliseURL("http://example.com"));
    }

    @Test
    public void testMultipleTrailingSlashesCollapseToDomainSlash() {
        assertEquals("http://example.com/", NormalisationStandard.canonicaliseURL("http://example.com///"));
    }

    // --- Port handling (depends on NormaliseType) ---

    @Test
    public void testPortIsRemovedInNormalMode() {
        Normalisation.setType(NormaliseType.NORMAL);
        assertEquals("http://example.com/foo",
                NormalisationStandard.canonicaliseURL("http://example.com:8080/foo"));
    }

    @Test
    public void testPortIsKeptInLegacyMode() {
        Normalisation.setType(NormaliseType.LEGACY);
        assertEquals("http://example.com:8080/foo",
                NormalisationStandard.canonicaliseURL("http://example.com:8080/foo"));
    }

    // --- %-escape handling ---

    @Test
    public void testRedundantEscapeNormalisedWhenCreateUnambiguous() {
        assertEquals("http://example.com/*.html",
                NormalisationStandard.canonicaliseURL("http://example.com/%2A.html", true, true));
    }

    @Test
    public void testRedundantEscapeKeptWhenNotCreateUnambiguous() {
        assertEquals("http://example.com/%2a.html",
                NormalisationStandard.canonicaliseURL("http://example.com/%2A.html", true, false));
    }

    @Test
    public void testSpaceIsAlwaysEscaped() {
        assertEquals("http://example.com/a%20b",
                NormalisationStandard.canonicaliseURL("http://example.com/a b"));
    }

    // --- High-order Unicode handling ---

    @Test
    public void testHighOrderUnicodeKeptRawWhenAllowed() {
        assertEquals("http://example.com/rosé",
                NormalisationStandard.canonicaliseURL("http://example.com/rosé", true, true));
    }

    @Test
    public void testHighOrderUnicodeEscapedWhenNotAllowed() {
        assertEquals("http://example.com/ros%c3%a9",
                NormalisationStandard.canonicaliseURL("http://example.com/rosé", false, true));
    }

    // --- Single-argument shorthand ---

    @Test
    public void testShorthandDelegatesToAggressiveNormalisation() {
        // Shorthand is documented as equivalent to canonicaliseURL(url, true, true).
        assertEquals(NormalisationStandard.canonicaliseURL("http://www.Example.com/%2A.html", true, true),
                NormalisationStandard.canonicaliseURL("http://www.Example.com/%2A.html"));
    }

    @ Test
    public void test(){
        System.out.println(NormalisationStandard.canonicaliseURL("http://www.elvalledeloscaidos.es:80/img/loteria 2012.txt"));
    }
}
