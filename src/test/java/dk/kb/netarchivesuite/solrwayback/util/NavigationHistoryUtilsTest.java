package dk.kb.netarchivesuite.solrwayback.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link NavigationHistoryUtils#classifyPlayback}.
 */
public class NavigationHistoryUtilsTest {

    private static final String BASE = "https://example.org/solrwayback/";
    private static final String PLAYBACK_URL = BASE + "services/web/20051102141713/http://www.yahooligans.com";

    private static NavigationHistoryAction classify(String referrer) {
        return NavigationHistoryUtils.classifyPlayback(PLAYBACK_URL, referrer, "navigate", null, false);
    }

    /** A page opened from the SolrWayback search page is a search result click. */
    @Test
    public void testSearchPageReferrerIsSearchResult() {
        assertEquals(NavigationHistoryAction.SEARCH_RESULT_CLICKED, classify(BASE + "search/yahooligans?query=yahooligans&offset=0"));
        assertEquals(NavigationHistoryAction.SEARCH_RESULT_CLICKED, classify(BASE + "search?query=yahooligans"));
        assertEquals(NavigationHistoryAction.SEARCH_RESULT_CLICKED, classify(BASE));
    }

    /** A page opened from the harvest calendar is a calendar link click. */
    @Test
    public void testCalendarReferrerIsCalendarLink() {
        assertEquals(NavigationHistoryAction.CALENDAR_LINK_CLICKED, classify(BASE + "calendar?url=http%3A%2F%2Fyahooligans.com%2F"));
    }

    /** A page reached from a played back page without a recorded click is playback navigation. */
    @Test
    public void testPlaybackReferrerWithoutClickIsPlaybackNavigation() {
        assertEquals(NavigationHistoryAction.PLAYBACK_NAVIGATION, classify(BASE + "services/web/20051102141713/http://www.yahooligans.com"));
        assertEquals(NavigationHistoryAction.PLAYBACK_NAVIGATION, classify(BASE + "services/lenient/web/20051102141713/http://www.yahooligans.com"));
        // Frames are played back through services/view
        assertEquals(NavigationHistoryAction.PLAYBACK_NAVIGATION, classify(BASE + "services/view?source_file_path=a.warc&offset=1&showToolbar=false"));
    }

    /** A page without a referrer, e.g. a typed URL or bookmark, is direct navigation. */
    @Test
    public void testNoReferrerIsDirectNavigation() {
        assertEquals(NavigationHistoryAction.DIRECT_NAVIGATION, classify(null));
        assertEquals(NavigationHistoryAction.DIRECT_NAVIGATION, classify(""));
    }

    /** A page opened from an external site or another SolrWayback page is other navigation. */
    @Test
    public void testOtherReferrerIsOtherNavigation() {
        assertEquals(NavigationHistoryAction.OTHER_NAVIGATION, classify("https://www.google.com/"));
        assertEquals(NavigationHistoryAction.OTHER_NAVIGATION, classify(BASE + "pwid?source_file_path=a.warc&offset=1"));
    }

    /** A click recorded by the previous page decides the action over the referrer. */
    @Test
    public void testRecordedClickWins() {
        String playbackReferrer = BASE + "services/web/20051102141713/http://www.yahooligans.com";
        assertEquals(NavigationHistoryAction.PLAYBACK_LINK_CLICKED,
                NavigationHistoryUtils.classifyPlayback(PLAYBACK_URL, playbackReferrer, "navigate", "page link", false));
        assertEquals(NavigationHistoryAction.TOOLBAR_LINK_CLICKED,
                NavigationHistoryUtils.classifyPlayback(PLAYBACK_URL, playbackReferrer, "navigate", "toolbar", false));
    }

    /** Back/forward, reload and URL changes within the page decide the action over the referrer. */
    @Test
    public void testNavigationTypes() {
        String searchReferrer = BASE + "search?query=yahooligans";
        assertEquals(NavigationHistoryAction.BROWSER_HISTORY_NAVIGATION,
                NavigationHistoryUtils.classifyPlayback(PLAYBACK_URL, searchReferrer, "back_forward", null, false));
        assertEquals(NavigationHistoryAction.PAGE_RELOADED,
                NavigationHistoryUtils.classifyPlayback(PLAYBACK_URL, searchReferrer, "reload", null, false));
        assertEquals(NavigationHistoryAction.PLAYBACK_NAVIGATION,
                NavigationHistoryUtils.classifyPlayback(PLAYBACK_URL + "#top", null, null, null, true));
    }
}
