package dk.kb.netarchivesuite.solrwayback.service;

import dk.kb.netarchivesuite.solrwayback.UnitTestUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;
import java.util.*;

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
        ArgumentCaptor<List> historyCaptor = ArgumentCaptor.forClass(List.class);
        verify(mockSession).setAttribute(eq(SESSION_KEY), historyCaptor.capture());

        List<Map<String, String>> capturedHistory = historyCaptor.getValue();
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
        when(mockSession.getAttribute(SESSION_KEY)).thenReturn(existingHistory);

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
        ArgumentCaptor<List> historyCaptor = ArgumentCaptor.forClass(List.class);
        verify(mockSession).setAttribute(eq(SESSION_KEY), historyCaptor.capture());

        List<Map<String, String>> capturedHistory = historyCaptor.getValue();
        assertEquals(1, capturedHistory.size());
        assertEquals("http://example.com/page.html", capturedHistory.get(0).get("originalUrl"));
        assertNotNull(capturedHistory.get(0).get("timestamp"));
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
        when(mockSession.getAttribute(SESSION_KEY)).thenReturn(existingHistory);

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
        when(mockSession.getAttribute(SESSION_KEY)).thenReturn(new ArrayList<>());

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
        when(mockSession.getAttribute(SESSION_KEY)).thenReturn(history);

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
        // Setup
        when(mockRequest.getSession(false)).thenReturn(null);

        // Execute
        Response response = resource.downloadHistory(mockRequest);

        // Verify
        assertEquals(200, response.getStatus());
        List<Map<String, Object>> entity = (List<Map<String, Object>>) response.getEntity();
        assertNotNull(entity);
        assertEquals(0, entity.size());
        assertEquals("attachment; filename=\"query_history.json\"",
                     response.getHeaderString("Content-Disposition"));
    }

    /**
     * Test downloading empty history
     */
    @Test
    public void testDownloadHistoryEmptyHistory() {
        // Setup
        when(mockRequest.getSession(false)).thenReturn(mockSession);
        when(mockSession.getAttribute(SESSION_KEY)).thenReturn(new ArrayList<>());

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
        when(mockSession.getAttribute(SESSION_KEY)).thenReturn(history);

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
        assertEquals("query", firstEntry.get("action"));
        assertEquals("test", firstEntry.get("query"));

        // Verify second entry
        Map<String, Object> secondEntry = entity.get(1);
        assertEquals(2, secondEntry.get("number"));
        assertEquals("query", secondEntry.get("action"));
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
        when(mockSession.getAttribute(SESSION_KEY)).thenReturn(history);

        // Execute
        Response response = resource.downloadHistory(mockRequest);

        // Verify
        assertEquals(200, response.getStatus());
        List<Map<String, Object>> entity = (List<Map<String, Object>>) response.getEntity();
        assertEquals(1, entity.size());

        Map<String, Object> entry = entity.get(0);
        assertEquals("query", entry.get("action"));
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
            "2020-12-31 12:00:00"
        ));

        when(mockRequest.getSession(false)).thenReturn(mockSession);
        when(mockSession.getAttribute(SESSION_KEY)).thenReturn(history);

        // Execute
        Response response = resource.downloadHistory(mockRequest);

        // Verify
        assertEquals(200, response.getStatus());
        List<Map<String, Object>> entity = (List<Map<String, Object>>) response.getEntity();
        assertEquals(2, entity.size());

        // Verify search entry
        Map<String, Object> searchEntry = entity.get(0);
        assertEquals(1, searchEntry.get("number"));
        assertEquals("query", searchEntry.get("action"));

        // Verify playback entry
        Map<String, Object> playbackEntry = entity.get(1);
        assertEquals(2, playbackEntry.get("number"));
        assertEquals("search result clicked", playbackEntry.get("action"));
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
        when(mockSession.getAttribute(SESSION_KEY)).thenReturn(history);

        // Execute
        Response response = resource.downloadHistory(mockRequest);

        // Verify
        assertEquals(200, response.getStatus());
        List<Map<String, Object>> entity = (List<Map<String, Object>>) response.getEntity();
        assertEquals(3, entity.size());

        // First playback should be "search result clicked"
        Map<String, Object> firstPlayback = entity.get(1);
        assertEquals("search result clicked", firstPlayback.get("action"));

        // Second playback should be "playback link clicked"
        Map<String, Object> secondPlayback = entity.get(2);
        assertEquals("playback link clicked", secondPlayback.get("action"));
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
        when(mockSession.getAttribute(SESSION_KEY)).thenReturn(history);

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
        when(mockSession.getAttribute(SESSION_KEY)).thenReturn(history);

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
        when(mockSession.getAttribute(SESSION_KEY)).thenReturn(history);

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
        when(mockSession.getAttribute(SESSION_KEY)).thenReturn(history);

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
        when(mockSession.getAttribute(SESSION_KEY)).thenReturn(history);

        // Execute
        Response response = resource.downloadHistory(mockRequest);

        // Verify
        assertEquals(200, response.getStatus());
        List<Map<String, Object>> entity = (List<Map<String, Object>>) response.getEntity();
        assertEquals(4, entity.size());

        // Verify order and action types
        assertEquals("query", entity.get(0).get("action"));
        assertEquals("search result clicked", entity.get(1).get("action"));
        assertEquals("query", entity.get(2).get("action"));
        assertEquals("search result clicked", entity.get(3).get("action"));
    }

    // Helper methods

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
}

