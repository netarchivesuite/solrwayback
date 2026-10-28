package dk.kb.netarchivesuite.solrwayback.service;

import dk.kb.netarchivesuite.solrwayback.UnitTestUtils;
import dk.kb.netarchivesuite.solrwayback.service.NavigationHistoryResource.History;
import dk.kb.netarchivesuite.solrwayback.util.JsonUtils;
import dk.kb.netarchivesuite.solrwayback.util.DateUtils;
import dk.kb.netarchivesuite.solrwayback.util.NavigationHistoryAction;
import org.junit.Before;
import org.junit.Test;

import org.mockito.ArgumentCaptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Test class for NavigationHistoryResource.
 * Tests all public methods including tracking, retrieval, and history management.
 */


public class NavigationHistoryResourceTest extends UnitTestUtils {

    private NavigationHistoryResource resource;
    private HttpServletRequest mockRequest;
    private HttpSession mockSession;

    private static final String SESSION_KEY = "solrwayback_query_history";

    @Before
    public void setUp() {
        resource = new NavigationHistoryResource();
        mockRequest = mock(HttpServletRequest.class);
        mockSession = mock(HttpSession.class);
    }

    /**
     * Test tracking a search query successfully
     */
    @Test
    public void testTrackSearchSuccess() {
        // Setup
        when(mockRequest.getSession(true)).thenReturn(mockSession);
        when(mockSession.getAttribute(SESSION_KEY)).thenReturn(null);

        Map<String, Object> data = new HashMap<>();
        data.put("url", "http://localhost:8080/search?query=test");

        // Execute
        Response response = resource.trackSearch(mockRequest, data);

        // Verify
        assertEquals(200, response.getStatus());
        Map<String, Object> entity = (Map<String, Object>) response.getEntity();
        assertTrue((Boolean) entity.get("success"));
        assertEquals(1, entity.get("count"));

        // Verify session interactions
        verify(mockSession).setMaxInactiveInterval(43200);
        ArgumentCaptor<History> historyCaptor = ArgumentCaptor.forClass(History.class);
        verify(mockSession).setAttribute(eq(SESSION_KEY), historyCaptor.capture());

        List<Map<String, String>> capturedHistory = historyCaptor.getValue().entries();
        assertEquals(1, capturedHistory.size());
        assertEquals("http://localhost:8080/search?query=test", capturedHistory.get(0).get("url"));
        assertNotNull(capturedHistory.get(0).get("timestamp"));
    }


    /**
     * Test tracking multiple search queries
     */
    @Test
    public void testTrackSearchMultipleQueries() {
        // Setup - simulate existing history
        List<Map<String, String>> existingHistory = new ArrayList<>();
        Map<String, String> existingEntry = new HashMap<>();
        existingEntry.put("url", "http://localhost:8080/search?query=first");
        existingEntry.put("timestamp", "2025-12-22 10:00:00");
        existingHistory.add(existingEntry);

        when(mockRequest.getSession(true)).thenReturn(mockSession);
        when(mockSession.getAttribute(SESSION_KEY)).thenReturn(new History(existingHistory));

        Map<String, Object> data = new HashMap<>();
        data.put("url", "http://localhost:8080/search?query=second");

        // Execute
        Response response = resource.trackSearch(mockRequest, data);

        // Verify
        assertEquals(200, response.getStatus());
        Map<String, Object> entity = (Map<String, Object>) response.getEntity();
        assertTrue((Boolean) entity.get("success"));
        assertEquals(2, entity.get("count"));
    }

    /**
     * Test tracking search with null URL
     */
    @Test
    public void testTrackSearchNullUrl() {
        // Setup
        when(mockRequest.getSession(true)).thenReturn(mockSession);
        when(mockSession.getAttribute(SESSION_KEY)).thenReturn(null);

        Map<String, Object> data = new HashMap<>();
        data.put("url", null);

        // Execute - should handle gracefully
        Response response = resource.trackSearch(mockRequest, data);

        // Verify - should still succeed (null is stored)
        assertEquals(200, response.getStatus());
    }

    /**
     * Test tracking playback navigation successfully
     */
    @Test
    public void testTrackPlaybackSuccess() {
        // Setup
        when(mockRequest.getSession(true)).thenReturn(mockSession);
        when(mockSession.getAttribute(SESSION_KEY)).thenReturn(null);

        Map<String, Object> data = new HashMap<>();
        data.put("url", "http://localhost:8080/services/web/20201231120000/http://example.com/page.html");
        data.put("originalUrl", "http://example.com/page.html");
        data.put("waybackDate", "20201231120000");

        // Execute
        Response response = resource.trackPlayback(mockRequest, data);

        // Verify
        assertEquals(200, response.getStatus());
        Map<String, Object> entity = (Map<String, Object>) response.getEntity();
        assertTrue((Boolean) entity.get("success"));
        assertEquals(1, entity.get("count"));

        // Verify session interactions
        verify(mockSession).setMaxInactiveInterval(43200);
        ArgumentCaptor<History> historyCaptor = ArgumentCaptor.forClass(History.class);
        verify(mockSession).setAttribute(eq(SESSION_KEY), historyCaptor.capture());

        List<Map<String, String>> capturedHistory = historyCaptor.getValue().entries();
        assertEquals(1, capturedHistory.size());
        assertEquals("http://example.com/page.html", capturedHistory.get(0).get("originalUrl"));
        assertNotNull(capturedHistory.get(0).get("timestamp"));
     }

    
    /**
     * Test that the action is decided when playback is tracked, from how the page was reached
     */
    @Test
    public void testTrackPlaybackStoresAction() {
        // Setup
        when(mockRequest.getSession(true)).thenReturn(mockSession);
        when(mockSession.getAttribute(SESSION_KEY)).thenReturn(null);

        Map<String, Object> data = new HashMap<>();
        data.put("url", "http://localhost:8080/solrwayback/services/web/20201231120000/http://example.com/page.html");
        data.put("originalUrl", "http://example.com/page.html");
        data.put("waybackDate", "20201231120000");
        data.put("referrer", "http://localhost:8080/solrwayback/search/test?query=test");
        data.put("navigationType", "navigate");
        data.put("clickSource", null);
        data.put("tabId", "tab-a");

        // Execute
        Response response = resource.trackPlayback(mockRequest, data);

        // Verify
        assertEquals(200, response.getStatus());
        ArgumentCaptor<History> historyCaptor = ArgumentCaptor.forClass(History.class);
        verify(mockSession).setAttribute(eq(SESSION_KEY), historyCaptor.capture());
        List<Map<String, String>> capturedHistory = historyCaptor.getValue().entries();
        assertEquals(NavigationHistoryAction.SEARCH_RESULT_CLICKED.name(), capturedHistory.get(0).get("action"));
        assertEquals("tab-a", capturedHistory.get(0).get("tabId"));
        assertEquals("http://localhost:8080/solrwayback/search/test?query=test", capturedHistory.get(0).get("referrer"));
    }

    /**
     * Test that the tab ID sent with a search is stored
     */
    @Test
    public void testTrackSearchStoresTabId() {
        // Setup
        when(mockRequest.getSession(true)).thenReturn(mockSession);
        when(mockSession.getAttribute(SESSION_KEY)).thenReturn(null);

        Map<String, Object> data = new HashMap<>();
        data.put("url", "http://localhost:8080/solrwayback/#/search?query=test");
        data.put("tabId", "tab-a");

        // Execute
        resource.trackSearch(mockRequest, data);

        // Verify
        ArgumentCaptor<History> historyCaptor = ArgumentCaptor.forClass(History.class);
        verify(mockSession).setAttribute(eq(SESSION_KEY), historyCaptor.capture());
        Map<String, String> entry = historyCaptor.getValue().entries().get(0);
        assertEquals("tab-a", entry.get("tabId"));
    }

    /**
     * Test that a failed playback of a page is added to an existing history
     */
    @Test
    public void testTrackFailedPlayback() {
        // Setup
        when(mockRequest.getHeader("Sec-Fetch-Dest")).thenReturn("document");
        when(mockRequest.getHeader("Referer")).thenReturn("http://localhost:8080/solrwayback/services/web/20051102141713/http://www.yahooligans.com");
        when(mockRequest.getSession(false)).thenReturn(mockSession);
        History history = new History();
        when(mockSession.getAttribute(SESSION_KEY)).thenReturn(history);

        // Execute
        NavigationHistoryResource.trackFailedPlayback(mockRequest,
                "http://localhost:8080/solrwayback/services/web/20051102141713/http://yahooligans.yahoo.com/content/news/",
                "20051102141713", "http://yahooligans.yahoo.com/content/news/");

        // Verify
        assertEquals(1, history.size());
        Map<String, String> entry = history.entries().get(0);
        assertEquals(NavigationHistoryAction.PLAYBACK_FAILED.name(), entry.get("action"));
        assertEquals("http://yahooligans.yahoo.com/content/news/", entry.get("originalUrl"));
        assertEquals("2005-11-02T14:17:13Z", entry.get("date"));
        assertEquals("http://localhost:8080/solrwayback/services/web/20051102141713/http://www.yahooligans.com", entry.get("referrer"));
        assertNotNull(entry.get("timestamp"));
    }

    /**
     * Test that missing resources on a page, e.g. images, are not tracked as failed playback
     */
    @Test
    public void testTrackFailedPlaybackIgnoresResources() {
        // Setup
        when(mockRequest.getHeader("Sec-Fetch-Dest")).thenReturn("image");
        when(mockRequest.getSession(false)).thenReturn(mockSession);

        // Execute
        NavigationHistoryResource.trackFailedPlayback(mockRequest,
                "http://localhost:8080/solrwayback/services/web/20051102141713/http://example.com/logo.gif",
                "20051102141713", "http://example.com/logo.gif");

        // Verify
        verify(mockSession, never()).setAttribute(eq(SESSION_KEY), any());
    }

    /**
     * Test that failed playback falls back to the Accept header and does not create a session
     */
    @Test
    public void testTrackFailedPlaybackWithoutSession() {
        // Setup
        when(mockRequest.getHeader("Accept")).thenReturn("text/html,application/xhtml+xml,*/*;q=0.8");
        when(mockRequest.getSession(false)).thenReturn(null);

        // Execute
        NavigationHistoryResource.trackFailedPlayback(mockRequest,
                "http://localhost:8080/solrwayback/services/web/20051102141713/http://example.com/",
                "20051102141713", "http://example.com/");

        // Verify
        verify(mockRequest).getSession(false);
        verify(mockRequest, never()).getSession(true);
    }

    /**
     * Test tracking playback with multiple entries
     */
    @Test
    public void testTrackPlaybackMultipleEntries() {
        // Setup - simulate existing history
        List<Map<String, String>> existingHistory = new ArrayList<>();
        Map<String, String> existingEntry = new HashMap<>();
        existingEntry.put("url", "http://localhost:8080/services/web/20201231120000/http://example.com/first.html");
        existingEntry.put("originalUrl", "http://example.com/first.html");
        existingEntry.put("timestamp", "2020-12-31 12:00:00");
        existingHistory.add(existingEntry);

        when(mockRequest.getSession(true)).thenReturn(mockSession);
        when(mockSession.getAttribute(SESSION_KEY)).thenReturn(new History(existingHistory));

        Map<String, Object> data = new HashMap<>();
        data.put("url", "http://localhost:8080/services/web/20210101120000/http://example.com/second.html");
        data.put("originalUrl", "http://example.com/second.html");
        data.put("waybackDate", "20210101120000");

        // Execute
        Response response = resource.trackPlayback(mockRequest, data);

        // Verify
        assertEquals(200, response.getStatus());
        Map<String, Object> entity = (Map<String, Object>) response.getEntity();
        assertTrue((Boolean) entity.get("success"));
        assertEquals(2, entity.get("count"));
    }

    /**
     * Test tracking playback with invalid wayback date
     */
    @Test
    public void testTrackPlaybackInvalidWaybackDate() {
        // Setup
        when(mockRequest.getSession(true)).thenReturn(mockSession);
        when(mockSession.getAttribute(SESSION_KEY)).thenReturn(null);

        Map<String, Object> data = new HashMap<>();
        data.put("url", "http://localhost:8080/services/web/invalid/http://example.com/page.html");
        data.put("originalUrl", "http://example.com/page.html");
        data.put("waybackDate", "invalid");

        // Execute
        Response response = resource.trackPlayback(mockRequest, data);

        // Verify - should return error due to date conversion failure
        assertEquals(500, response.getStatus());
        Map<String, Object> entity = (Map<String, Object>) response.getEntity();
        assertFalse((Boolean) entity.get("success"));
        assertNotNull(entity.get("error"));
    }

    /**
     * Test getting count with no session
     */
    @Test
    public void testGetCountNoSession() {
        // Setup
        when(mockRequest.getSession(false)).thenReturn(null);

        // Execute
        Response response = resource.getCount(mockRequest);

        // Verify
        assertEquals(200, response.getStatus());
        Map<String, Object> entity = (Map<String, Object>) response.getEntity();
        assertEquals(0, entity.get("count"));
    }

    /**
     * Test getting count with empty history
     */
    @Test
    public void testGetCountEmptyHistory() {
        // Setup
        when(mockRequest.getSession(false)).thenReturn(mockSession);
        when(mockSession.getAttribute(SESSION_KEY)).thenReturn(new History());

        // Execute
        Response response = resource.getCount(mockRequest);

        // Verify
        assertEquals(200, response.getStatus());
        Map<String, Object> entity = (Map<String, Object>) response.getEntity();
        assertEquals(0, entity.get("count"));
    }

    /**
     * Test getting count with existing history
     */
    @Test
    public void testGetCountWithHistory() {
        // Setup
        List<Map<String, String>> history = new ArrayList<>();
        history.add(createHistoryEntry("url1", "2025-12-22 10:00:00"));
        history.add(createHistoryEntry("url2", "2025-12-22 10:01:00"));
        history.add(createHistoryEntry("url3", "2025-12-22 10:02:00"));

        when(mockRequest.getSession(false)).thenReturn(mockSession);
        when(mockSession.getAttribute(SESSION_KEY)).thenReturn(new History(history));

        // Execute
        Response response = resource.getCount(mockRequest);

        // Verify
        assertEquals(200, response.getStatus());
        Map<String, Object> entity = (Map<String, Object>) response.getEntity();
        assertEquals(3, entity.get("count"));
    }

    /**
     * Test downloading history with no session
     */
    @Test
    public void testDownloadHistoryNoSession() {
        String currentDate = DateUtils.currentDateYYYYMMDD();

        // Setup
        when(mockRequest.getSession(false)).thenReturn(null);

        // Execute
        Response response = resource.downloadHistory(mockRequest);

        // Verify
        assertEquals(200, response.getStatus());
        List<Map<String, Object>> entity = (List<Map<String, Object>>) response.getEntity();
        assertNotNull(entity);
        assertEquals(0, entity.size());
        assertEquals("attachment; filename=\"" + currentDate + "_query_history.json\"",
                     response.getHeaderString("Content-Disposition"));
    }

    /**
     * Test downloading empty history
     */
    @Test
    public void testDownloadHistoryEmptyHistory() {
        // Setup
        when(mockRequest.getSession(false)).thenReturn(mockSession);
        when(mockSession.getAttribute(SESSION_KEY)).thenReturn(new History());

        // Execute
        Response response = resource.downloadHistory(mockRequest);

        // Verify
        assertEquals(200, response.getStatus());
        List<Map<String, Object>> entity = (List<Map<String, Object>>) response.getEntity();
        assertNotNull(entity);
        assertEquals(0, entity.size());
    }

    /**
     * Test downloading history with search queries
     */
    @Test
    public void testDownloadHistoryWithSearchQueries() {
        // Setup
        List<Map<String, String>> history = new ArrayList<>();
        history.add(createSearchEntry("http://localhost:8080/search?query=test", "2025-12-22 10:00:00"));
        history.add(createSearchEntry("http://localhost:8080/search?query=example", "2025-12-22 10:01:00"));

        when(mockRequest.getSession(false)).thenReturn(mockSession);
        when(mockSession.getAttribute(SESSION_KEY)).thenReturn(new History(history));

        // Execute
        Response response = resource.downloadHistory(mockRequest);

        // Verify
        assertEquals(200, response.getStatus());
        List<Map<String, Object>> entity = (List<Map<String, Object>>) response.getEntity();
        assertNotNull(entity);
        assertEquals(2, entity.size());

        // Verify first entry
        Map<String, Object> firstEntry = entity.get(0);
        assertEquals(1, firstEntry.get("number"));
        assertEquals(NavigationHistoryAction.QUERY, firstEntry.get("action"));
        assertEquals("test", firstEntry.get("query"));

        // Verify second entry
        Map<String, Object> secondEntry = entity.get(1);
        assertEquals(2, secondEntry.get("number"));
        assertEquals(NavigationHistoryAction.QUERY, secondEntry.get("action"));
        assertEquals("example", secondEntry.get("query"));
    }

    /**
     * Test downloading history with search queries containing facets
     */
    @Test
    public void testDownloadHistoryWithFacets() {
        // Setup
        List<Map<String, String>> history = new ArrayList<>();
        String urlWithFacets = "http://localhost:8080/search?query=test&facets=fq%3Ddomain%3Aexample.com%26fq%3Dcrawl_year%3A2020";
        history.add(createSearchEntry(urlWithFacets, "2025-12-22 10:00:00"));

        when(mockRequest.getSession(false)).thenReturn(mockSession);
        when(mockSession.getAttribute(SESSION_KEY)).thenReturn(new History(history));

        // Execute
        Response response = resource.downloadHistory(mockRequest);

        // Verify
        assertEquals(200, response.getStatus());
        List<Map<String, Object>> entity = (List<Map<String, Object>>) response.getEntity();
        assertEquals(1, entity.size());

        Map<String, Object> entry = entity.get(0);
        assertEquals(NavigationHistoryAction.QUERY, entry.get("action"));
        assertEquals("test", entry.get("query"));
        assertNotNull(entry.get("facets"));

        // Check filter queries
        List<String> filterQueries = (List<String>) entry.get("filterQueries");
        assertNotNull(filterQueries);
        assertTrue(filterQueries.size() >= 1);
    }

    /**
     * Test downloading history with playback entries
     */
    @Test
    public void testDownloadHistoryWithPlaybackEntries() {
        // Setup
        List<Map<String, String>> history = new ArrayList<>();
        history.add(createSearchEntry("http://localhost:8080/search?query=test", "2025-12-22 10:00:00"));
        history.add(createPlaybackEntry(
            "http://localhost:8080/services/web/20201231120000/http://example.com/page.html",
            "http://example.com/page.html",
            "2026-03-02 15:30:00",
            "2020-12-31 12:00:00"
        ));

        when(mockRequest.getSession(false)).thenReturn(mockSession);
        when(mockSession.getAttribute(SESSION_KEY)).thenReturn(new History(history));

        // Execute
        Response response = resource.downloadHistory(mockRequest);

        // Verify
        assertEquals(200, response.getStatus());
        List<Map<String, Object>> entity = (List<Map<String, Object>>) response.getEntity();
        assertEquals(2, entity.size());

        // Verify search entry
        Map<String, Object> searchEntry = entity.get(0);
        assertEquals(1, searchEntry.get("number"));
        assertEquals(NavigationHistoryAction.QUERY, searchEntry.get("action"));

        // Verify playback entry
        Map<String, Object> playbackEntry = entity.get(1);
        assertEquals(2, playbackEntry.get("number"));
        assertEquals(NavigationHistoryAction.SEARCH_RESULT_CLICKED, playbackEntry.get("action"));
        assertEquals("http://example.com/page.html", playbackEntry.get("url"));
        assertEquals("2020-12-31 12:00:00", playbackEntry.get("date"));
    }

    /**
     * Test downloading history with consecutive playback entries
     */
    @Test
    public void testDownloadHistoryConsecutivePlaybackEntries() {
        // Setup
        List<Map<String, String>> history = new ArrayList<>();
        history.add(createSearchEntry("http://localhost:8080/search?query=test", "2025-12-22 10:00:00"));
        history.add(createPlaybackEntry(
            "http://localhost:8080/services/web/20201231120000/http://example.com/page1.html",
            "http://example.com/page1.html",
            "2020-12-31 12:00:00"
        ));
        history.add(createPlaybackEntry(
            "http://localhost:8080/services/web/20201231120100/http://example.com/page2.html",
            "http://example.com/page2.html",
            "2020-12-31 12:01:00"
        ));

        when(mockRequest.getSession(false)).thenReturn(mockSession);
        when(mockSession.getAttribute(SESSION_KEY)).thenReturn(new History(history));

        // Execute
        Response response = resource.downloadHistory(mockRequest);

        // Verify
        assertEquals(200, response.getStatus());
        List<Map<String, Object>> entity = (List<Map<String, Object>>) response.getEntity();
        assertEquals(3, entity.size());

        // First playback should be "search result clicked"
        Map<String, Object> firstPlayback = entity.get(1);
        assertEquals(NavigationHistoryAction.SEARCH_RESULT_CLICKED, firstPlayback.get("action"));

        // Second playback should be "playback link clicked"
        Map<String, Object> secondPlayback = entity.get(2);
        assertEquals(NavigationHistoryAction.PLAYBACK_LINK_CLICKED, secondPlayback.get("action"));
    }

    /**
     * Test that the tracked action is exported instead of guessing from the order. Two results opened from the
     * same search are both search results, and failed playback is exported as its own action.
     */
    @Test
    public void testDownloadHistoryUsesTrackedAction() {
        // Setup
        List<Map<String, String>> history = new ArrayList<>();
        history.add(createSearchEntry("http://localhost:8080/search?query=yahooligans", "2026-03-20T11:15:00Z"));
        history.add(createPlaybackEntry(
            "http://localhost:8080/services/web/20051102141713/http://www.yahooligans.com",
            "http://www.yahooligans.com",
            "2026-03-20T11:16:13Z",
            "2005-11-02T14:17:13Z",
            NavigationHistoryAction.SEARCH_RESULT_CLICKED
        ));
        history.add(createPlaybackEntry(
            "http://localhost:8080/services/web/20051102141713/http://yahooligans.yahoo.com/content/news/",
            "http://yahooligans.yahoo.com/content/news/",
            "2026-03-20T11:17:42Z",
            "2005-11-02T14:17:13Z",
            NavigationHistoryAction.PLAYBACK_FAILED
        ));
        history.add(createPlaybackEntry(
            "http://localhost:8080/services/web/20000607214855/http://www4.yahooligans.com:80/",
            "http://www4.yahooligans.com:80/",
            "2026-03-20T11:17:50Z",
            "2000-06-07T21:48:55Z",
            NavigationHistoryAction.SEARCH_RESULT_CLICKED
        ));

        when(mockRequest.getSession(false)).thenReturn(mockSession);
        when(mockSession.getAttribute(SESSION_KEY)).thenReturn(new History(history));

        // Execute
        Response response = resource.downloadHistory(mockRequest);

        // Verify
        assertEquals(200, response.getStatus());
        List<Map<String, Object>> entity = (List<Map<String, Object>>) response.getEntity();
        assertEquals(4, entity.size());
        assertEquals(NavigationHistoryAction.SEARCH_RESULT_CLICKED, entity.get(1).get("action"));
        assertEquals(NavigationHistoryAction.PLAYBACK_FAILED, entity.get(2).get("action"));
        assertEquals("http://yahooligans.yahoo.com/content/news/", entity.get(2).get("url"));
        assertEquals("2026-03-20T11:17:42Z", entity.get(2).get("timestamp"));
        assertEquals(NavigationHistoryAction.SEARCH_RESULT_CLICKED, entity.get(3).get("action"));
    }

    /**
     * Test that entries are exported with the tab they happened in and the entry they were opened from, when the
     * user opens two results from one search in separate tabs and clicks around in both.
     */
    @Test
    public void testDownloadHistoryLinksTabsAndParents() {
        // Setup
        String base = "http://localhost:8080/solrwayback/";
        String searchPage = base + "search?query=yahooligans&grouping=false&facets=&sort=score%20desc";
        String result1 = base + "services/web/20051102141713/http://www.yahooligans.com";
        String page1 = base + "services/web/20051102141713/http://www.yahooligans.com/Arts/";
        String result2 = base + "services/web/20000607214855/http://www4.yahooligans.com:80/";

        List<Map<String, String>> history = new ArrayList<>();
        history.add(createEntry("url", base + "#/search?query=yahooligans&grouping=false&facets=", "tabId", "search-tab"));
        history.add(createEntry("url", result1, "action", "SEARCH_RESULT_CLICKED", "tabId", "tab-b", "referrer", searchPage));
        history.add(createEntry("url", page1, "action", "PLAYBACK_LINK_CLICKED", "tabId", "tab-b", "referrer", result1 + "#top"));
        history.add(createEntry("url", base + "services/web/20051102141713/http://yahooligans.yahoo.com/content/news/",
                "action", "PLAYBACK_FAILED", "referrer", page1));
        history.add(createEntry("url", result2, "action", "SEARCH_RESULT_CLICKED", "tabId", "tab-c", "referrer", searchPage));
        history.add(createEntry("url", result1, "action", "BROWSER_HISTORY_NAVIGATION", "tabId", "tab-b", "referrer", searchPage));

        when(mockRequest.getSession(false)).thenReturn(mockSession);
        when(mockSession.getAttribute(SESSION_KEY)).thenReturn(new History(history));

        // Execute
        Response response = resource.downloadHistory(mockRequest);

        // Verify
        List<Map<String, Object>> entity = (List<Map<String, Object>>) response.getEntity();
        assertEquals(6, entity.size());
        Integer[] expectedTabs = {1, 2, 2, 2, 3, 2};
        Integer[] expectedFrom = {null, 1, 2, 3, 1, null};
        for (int i = 0; i < entity.size(); i++) {
            assertEquals("tab of entry " + (i + 1), expectedTabs[i], entity.get(i).get("tab"));
            assertEquals("from of entry " + (i + 1), expectedFrom[i], entity.get(i).get("from"));
        }
    }

    /**
     * Test downloading history filters duplicate search queries
     */
    @Test
    public void testDownloadHistoryFiltersDuplicateSearches() {
        // Setup
        List<Map<String, String>> history = new ArrayList<>();
        history.add(createSearchEntry("http://localhost:8080/search?query=test", "2025-12-22 10:00:00"));
        history.add(createSearchEntry("http://localhost:8080/search?query=test", "2025-12-22 10:00:01"));
        history.add(createSearchEntry("http://localhost:8080/search?query=different", "2025-12-22 10:00:02"));

        when(mockRequest.getSession(false)).thenReturn(mockSession);
        when(mockSession.getAttribute(SESSION_KEY)).thenReturn(new History(history));

        // Execute
        Response response = resource.downloadHistory(mockRequest);

        // Verify - duplicate should be filtered
        assertEquals(200, response.getStatus());
        List<Map<String, Object>> entity = (List<Map<String, Object>>) response.getEntity();
        assertEquals(2, entity.size()); // Only 2 unique searches

        assertEquals("test", entity.get(0).get("query"));
        assertEquals("different", entity.get(1).get("query"));
    }

    /**
     * Test downloading history with URL-encoded query
     */
    @Test
    public void testDownloadHistoryURLEncodedQuery() {
        // Setup
        List<Map<String, String>> history = new ArrayList<>();
        history.add(createSearchEntry("http://localhost:8080/search?query=hello+world", "2025-12-22 10:00:00"));

        when(mockRequest.getSession(false)).thenReturn(mockSession);
        when(mockSession.getAttribute(SESSION_KEY)).thenReturn(new History(history));

        // Execute
        Response response = resource.downloadHistory(mockRequest);

        // Verify
        assertEquals(200, response.getStatus());
        List<Map<String, Object>> entity = (List<Map<String, Object>>) response.getEntity();
        assertEquals(1, entity.size());

        Map<String, Object> entry = entity.get(0);
        assertEquals("hello world", entry.get("query"));
    }

    /**
     * Test clearing history with no session
     */
    @Test
    public void testClearHistoryNoSession() {
        // Setup
        when(mockRequest.getSession(false)).thenReturn(null);

        // Execute
        Response response = resource.clearHistory(mockRequest);

        // Verify
        assertEquals(200, response.getStatus());
        Map<String, Object> entity = (Map<String, Object>) response.getEntity();
        assertTrue((Boolean) entity.get("success"));

        // Verify no interactions with session
        verify(mockRequest).getSession(false);
        verifyNoMoreInteractions(mockSession);
    }

    /**
     * Test clearing history successfully
     */
    @Test
    public void testClearHistorySuccess() {
        // Setup
        List<Map<String, String>> history = new ArrayList<>();
        history.add(createHistoryEntry("url1", "2025-12-22 10:00:00"));
        history.add(createHistoryEntry("url2", "2025-12-22 10:01:00"));

        when(mockRequest.getSession(false)).thenReturn(mockSession);
        when(mockSession.getAttribute(SESSION_KEY)).thenReturn(new History(history));

        // Execute
        Response response = resource.clearHistory(mockRequest);

        // Verify
        assertEquals(200, response.getStatus());
        Map<String, Object> entity = (Map<String, Object>) response.getEntity();
        assertTrue((Boolean) entity.get("success"));

        // Verify session attribute was removed
        verify(mockSession).removeAttribute(SESSION_KEY);
    }

    /**
     * Test clearing history handles exceptions
     */
    @Test
    public void testClearHistoryException() {
        // Setup
        when(mockRequest.getSession(false)).thenReturn(mockSession);
        doThrow(new RuntimeException("Session error")).when(mockSession).removeAttribute(SESSION_KEY);

        // Execute
        Response response = resource.clearHistory(mockRequest);

        // Verify
        assertEquals(500, response.getStatus());
        Map<String, Object> entity = (Map<String, Object>) response.getEntity();
        assertFalse((Boolean) entity.get("success"));
        assertNotNull(entity.get("error"));
    }

    /**
     * Test extracting query with special characters
     */
    @Test
    public void testDownloadHistoryQueryWithSpecialCharacters() {
        // Setup
        List<Map<String, String>> history = new ArrayList<>();
        history.add(createSearchEntry("http://localhost:8080/search?query=%22exact+phrase%22", "2025-12-22 10:00:00"));

        when(mockRequest.getSession(false)).thenReturn(mockSession);
        when(mockSession.getAttribute(SESSION_KEY)).thenReturn(new History(history));

        // Execute
        Response response = resource.downloadHistory(mockRequest);

        // Verify
        assertEquals(200, response.getStatus());
        List<Map<String, Object>> entity = (List<Map<String, Object>>) response.getEntity();
        assertEquals(1, entity.size());

        Map<String, Object> entry = entity.get(0);
        String query = (String) entry.get("query");
        assertNotNull(query);
        assertTrue(query.contains("\"") || query.contains("exact") || query.contains("phrase"));
    }

    /**
     * Test mixed search and playback history
     */
    @Test
    public void testDownloadHistoryMixedHistory() {
        // Setup
        List<Map<String, String>> history = new ArrayList<>();
        history.add(createSearchEntry("http://localhost:8080/search?query=first", "2025-12-22 10:00:00"));
        history.add(createPlaybackEntry(
            "http://localhost:8080/services/web/20201231120000/http://example.com/page1.html",
            "http://example.com/page1.html",
            "2020-12-31 12:00:00"
        ));
        history.add(createSearchEntry("http://localhost:8080/search?query=second", "2025-12-22 10:02:00"));
        history.add(createPlaybackEntry(
            "http://localhost:8080/services/web/20201231120100/http://example.com/page2.html",
            "http://example.com/page2.html",
            "2020-12-31 12:01:00"
        ));

        when(mockRequest.getSession(false)).thenReturn(mockSession);
        when(mockSession.getAttribute(SESSION_KEY)).thenReturn(new History(history));

        // Execute
        Response response = resource.downloadHistory(mockRequest);

        // Verify
        assertEquals(200, response.getStatus());
        List<Map<String, Object>> entity = (List<Map<String, Object>>) response.getEntity();
        assertEquals(4, entity.size());

        // Verify order and action types
        assertEquals(NavigationHistoryAction.QUERY, entity.get(0).get("action"));
        assertEquals(NavigationHistoryAction.SEARCH_RESULT_CLICKED, entity.get(1).get("action"));
        assertEquals(NavigationHistoryAction.QUERY, entity.get(2).get("action"));
        assertEquals(NavigationHistoryAction.SEARCH_RESULT_CLICKED, entity.get(3).get("action"));
    }

    /**
     * Test that adding an entry which would make the history exceed the limit returns 413 and does not mutate session.
     */
    @Test
    public void testTrackSearchRejectsWhenHistoryTooLarge() {
        // Setup
        when(mockRequest.getSession(true)).thenReturn(mockSession);
        when(mockSession.getAttribute(SESSION_KEY)).thenReturn(null);

        // Temporarily set MAX_HISTORY_BYTES to a very very small value so a normal entry will exceed it and throw 413
        long original = NavigationHistoryResource.MAX_HISTORY_BYTES;
        NavigationHistoryResource.MAX_HISTORY_BYTES = 1L;

        try {
            Map<String, Object> data = new HashMap<>();
            data.put("url", "tiny");

            // Execute
            Response response = resource.trackSearch(mockRequest, data);

            // Verify
            assertEquals(413, response.getStatus());
            Map<String, Object> entity = (Map<String, Object>) response.getEntity();
            assertFalse((Boolean) entity.get("success"));
            assertEquals("Navigation history size limit exceeded", entity.get("error"));

            // Verify the entry was not stored
            ArgumentCaptor<History> historyCaptor = ArgumentCaptor.forClass(History.class);
            verify(mockSession).setAttribute(eq(SESSION_KEY), historyCaptor.capture());
            assertEquals(0, historyCaptor.getValue().size());
        } finally {
            // Restore original limit
            NavigationHistoryResource.MAX_HISTORY_BYTES = original;
        }
    }

    /**
     * Test that the size of the history is counted as the size of the whole history as JSON
     */
    @Test
    public void testHistorySizeLimitMatchesJsonSize() {
        Map<String, String> first = createHistoryEntry("url1", "2025-12-22 10:00:00");
        Map<String, String> second = createHistoryEntry("url2", "2025-12-22 10:01:00");
        long original = NavigationHistoryResource.MAX_HISTORY_BYTES;
        NavigationHistoryResource.MAX_HISTORY_BYTES = JsonUtils.toJSON(List.of(first, second)).length();

        try {
            History history = new History();
            assertTrue(history.add(first));
            assertTrue(history.add(second));
            assertFalse(history.add(createHistoryEntry("url3", "2025-12-22 10:02:00")));
            assertEquals(2, history.size());
        } finally {
            NavigationHistoryResource.MAX_HISTORY_BYTES = original;
        }
    }

    /**
     * Test that no entries are lost when requests from the same session add to the history at the same time
     */
    @Test
    public void testHistoryConcurrentAdds() throws InterruptedException {
        History history = new History();
        ExecutorService executor = Executors.newFixedThreadPool(8);
        for (int i = 0; i < 1000; i++) {
            String url = "url" + i;
            executor.execute(() -> history.add(createHistoryEntry(url, "2025-12-22 10:00:00")));
        }
        executor.shutdown();
        assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));

        assertEquals(1000, history.size());
    }

    /**
     * Test that a history stored as a plain list by an earlier version is converted and kept
     */
    @Test
    public void testTrackSearchConvertsListHistory() {
        // Setup
        List<Map<String, String>> listHistory = new ArrayList<>();
        listHistory.add(createSearchEntry("http://localhost:8080/search?query=first", "2025-12-22 10:00:00"));
        when(mockRequest.getSession(true)).thenReturn(mockSession);
        when(mockSession.getAttribute(SESSION_KEY)).thenReturn(listHistory);

        Map<String, Object> data = new HashMap<>();
        data.put("url", "http://localhost:8080/search?query=second");

        // Execute
        resource.trackSearch(mockRequest, data);

        // Verify
        ArgumentCaptor<History> historyCaptor = ArgumentCaptor.forClass(History.class);
        verify(mockSession).setAttribute(eq(SESSION_KEY), historyCaptor.capture());
        List<Map<String, String>> entries = historyCaptor.getValue().entries();
        assertEquals(2, entries.size());
        assertEquals("http://localhost:8080/search?query=first", entries.get(0).get("url"));
        assertEquals("http://localhost:8080/search?query=second", entries.get(1).get("url"));
    }

    // Helper methods
    private Map<String, String> createEntry(String... keysAndValues) {
        Map<String, String> entry = new HashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            entry.put(keysAndValues[i], keysAndValues[i + 1]);
        }
        return entry;
    }

    private Map<String, String> createHistoryEntry(String url, String timestamp) {
        Map<String, String> entry = new HashMap<>();
        entry.put("url", url);
        entry.put("timestamp", timestamp);
        return entry;
    }

    private Map<String, String> createSearchEntry(String url, String timestamp) {
        return createHistoryEntry(url, timestamp);
    }

    private Map<String, String> createPlaybackEntry(String url, String originalUrl, String timestamp) {
        Map<String, String> entry = new HashMap<>();
        entry.put("url", url);
        entry.put("originalUrl", originalUrl);
        entry.put("timestamp", timestamp);
        return entry;
    }

    private Map<String, String> createPlaybackEntry(String url, String originalUrl, String timestamp, String date) {
        Map<String, String> entry = new HashMap<>();
        entry.put("url", url);
        entry.put("originalUrl", originalUrl);
        entry.put("timestamp", timestamp);
        entry.put("date", date);
        return entry;
    }

    private Map<String, String> createPlaybackEntry(String url, String originalUrl, String timestamp, String date, NavigationHistoryAction action) {
        Map<String, String> entry = createPlaybackEntry(url, originalUrl, timestamp, date);
        entry.put("action", action.name());
        return entry;
    }
}

