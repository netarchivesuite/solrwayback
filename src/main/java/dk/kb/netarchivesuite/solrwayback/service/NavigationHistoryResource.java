package dk.kb.netarchivesuite.solrwayback.service;

import dk.kb.netarchivesuite.solrwayback.util.DateUtils;
import dk.kb.netarchivesuite.solrwayback.util.JsonUtils;
import dk.kb.netarchivesuite.solrwayback.util.NavigationHistoryAction;
import dk.kb.netarchivesuite.solrwayback.util.NavigationHistoryUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static dk.kb.netarchivesuite.solrwayback.util.DateUtils.convertWaybackDate2SolrDate;

/**
 * REST resource for tracking query history in session storage.
 * Provides endpoints for tracking search queries, playback navigation, and downloading history.
 */
@Path("/navigationhistory/")
public class NavigationHistoryResource {

    private static final Logger log = LoggerFactory.getLogger(NavigationHistoryResource.class);
    private static final String SESSION_KEY = "solrwayback_query_history";
    // Guards creating the history of a session
    private static final Object SESSION_LOCK = new Object();

    // Use ISO-8601 UTC format, e.g. 2005-01-01T12:00:00Z
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_INSTANT;

    // Maximum allowed size of the stored history in bytes (25 MB)
    public static long MAX_HISTORY_BYTES = 25L * 1024L * 1024L;

    /**
     * Track a search query
     */
    @POST
    @Path("track/search")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response trackSearch(@Context HttpServletRequest request, Map<String, Object> data) {
        try {
            HttpSession session = request.getSession(true);
            session.setMaxInactiveInterval(43200); // 12 hours

            History history = getHistory(session, true);

            String url = (String) data.get("url");
            // Truncate to seconds so we don't include fractional seconds (e.g. 2026-03-02T18:05:57Z)
            String timestamp = DATE_FORMAT.format(Instant.now().truncatedTo(ChronoUnit.SECONDS));

            Map<String, String> entry = new HashMap<>();
            entry.put("url", url);
            entry.put("timestamp", timestamp);
            entry.put("tabId", (String) data.get("tabId"));

            if (!history.add(entry)) {
                return historyTooLargeResponse();
            }

            log.debug("Tracked search query: {}", url);

            return Response.ok()
                    .entity(Map.of("success", true, "count", history.size()))
                    .build();
        } catch (Exception e) {
            log.error("Error tracking search query", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("success", false, "error", e.getMessage()))
                    .build();
        }
    }

    /**
     * Track playback navigation
     */
    @POST
    @Path("track/playback")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response trackPlayback(@Context HttpServletRequest request, Map<String, Object> data) {
        try {
            HttpSession session = request.getSession(true);
            session.setMaxInactiveInterval(43200); // 12 hours
            History history = getHistory(session, true);

            String url = (String) data.get("url");
            String originalUrl = (String) data.get("originalUrl");
            String archivalDate = convertWaybackDate2SolrDate((String) data.get("waybackDate"));
            // Truncate to seconds so we don't include fractional seconds (e.g. 2026-03-02T18:05:57Z)
            String timestamp = DATE_FORMAT.format(Instant.now().truncatedTo(ChronoUnit.SECONDS));
            String referrer = (String) data.get("referrer");
            NavigationHistoryAction action = NavigationHistoryUtils.classifyPlayback(
                    url,
                    referrer,
                    (String) data.get("navigationType"),
                    (String) data.get("clickSource"),
                    Boolean.TRUE.equals(data.get("sameDocument")));

            Map<String, String> entry = new HashMap<>();
            entry.put("url", url);
            entry.put("originalUrl", originalUrl);
            entry.put("date", archivalDate);
            entry.put("timestamp", timestamp);
            entry.put("action", action.name());
            entry.put("tabId", (String) data.get("tabId"));
            entry.put("referrer", referrer);

            if (!history.add(entry)) {
                return historyTooLargeResponse();
            }
            log.debug("Tracked playback ({}): {}", action, url);

            return Response.ok()
                    .entity(Map.of("success", true, "count", history.size()))
                    .build();
        } catch (Exception e) {
            log.error("Error tracking playback for url", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("success", false, "error", e.getMessage()))
                    .build();
        }
    }

    /**
     * Track a page that was requested for playback but has never been harvested. Called from playback itself,
     * as the tracking script is not part of the not found page. Only requests for a whole page are tracked, not
     * missing images, stylesheets, frames etc., and only for sessions that exist.
     *
     * @param request     the playback request
     * @param archivedUrl the requested playback URL
     * @param waybackDate the requested capture date, e.g. 20051102141713
     * @param originalUrl the requested URL of the original resource
     */
    public static void trackFailedPlayback(HttpServletRequest request, String archivedUrl, String waybackDate, String originalUrl) {
        try {
            if (!isPageRequest(request)) {
                return;
            }
            HttpSession session = request.getSession(false);
            if (session == null) {
                return;
            }

            Map<String, String> entry = new HashMap<>();
            entry.put("url", archivedUrl);
            entry.put("originalUrl", originalUrl);
            entry.put("date", convertWaybackDate2SolrDate(waybackDate));
            entry.put("timestamp", DATE_FORMAT.format(Instant.now().truncatedTo(ChronoUnit.SECONDS)));
            entry.put("action", NavigationHistoryAction.PLAYBACK_FAILED.name());
            // The not found page cannot report its tab, the export takes the tab of the referring page instead
            entry.put("referrer", request.getHeader("Referer"));

            if (getHistory(session, true).add(entry)) {
                log.debug("Tracked failed playback: {}", archivedUrl);
            }
        } catch (Exception e) {
            // Tracking must never break playback
            log.warn("Error tracking failed playback for url: {}", archivedUrl, e);
        }
    }

    /**
     * Whether the request is for a page shown in the browser window, as opposed to a resource on a page.
     * Uses the Sec-Fetch-Dest header and falls back to the Accept header for browsers without it.
     */
    static boolean isPageRequest(HttpServletRequest request) {
        // Sec-Fetch-Header describes what the browser is doing with the request.
        String fetchDest = request.getHeader("Sec-Fetch-Dest");
        if (fetchDest != null) {
            return "document".equals(fetchDest);
        }
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("text/html");
    }

    /**
     * Get current history count
     */
    @GET
    @Path("count")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCount(@Context HttpServletRequest request) {
        try {
            HttpSession session = request.getSession(false);
            History history = session == null ? null : getHistory(session, false);
            int count = history == null ? 0 : history.size();
            return Response.ok()
                    .entity(Map.of("count", count))
                    .build();
        } catch (Exception e) {
            log.error("Error getting history count", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    /**
     * Download history as JSON file
     */
    @GET
    @Path("download")
    @Produces(MediaType.APPLICATION_JSON)
    public Response downloadHistory(@Context HttpServletRequest request) {
        String currentDate = DateUtils.currentDateYYYYMMDD();

        try {
            HttpSession session = request.getSession(false);
            if (session == null) {
                List<Map<String, Object>> emptyHistory = new ArrayList<>();
                return Response.ok(emptyHistory)
                        .header("Content-Disposition", "attachment; filename=\"" + currentDate + "_query_history.json\"")
                        .build();
            }

            History history = getHistory(session, false);
            List<Map<String, String>> entries = history == null ? new ArrayList<>() : history.entries();
            List<Map<String, Object>> formattedHistory = NavigationHistoryUtils.formatHistoryAsJson(entries);

            log.info("Downloading query history with {} entries", formattedHistory.size());

            return Response.ok(formattedHistory)
                    .header("Content-Disposition", "attachment; filename=\"" + currentDate + "_query_history.json\"")
                    .build();
        } catch (Exception e) {
            log.error("Error downloading history", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Error generating history file: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Clear history from session
     */
    @POST
    @Path("clear")
    @Produces(MediaType.APPLICATION_JSON)
    public Response clearHistory(@Context HttpServletRequest request) {
        try {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.removeAttribute(SESSION_KEY);
            }
            return Response.ok()
                    .entity(Map.of("success", true))
                    .build();
        } catch (Exception e) {
            log.error("Error clearing history", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("success", false, "error", e.getMessage()))
                    .build();
        }
    }

    /**
     * Get history from session.
     *
     * @param create whether to create an empty history if the session has none
     * @return the history, or null if the session has none and create is false
     */
    @SuppressWarnings("unchecked")
    private static History getHistory(HttpSession session, boolean create) {
        // Locked so that concurrent requests from a new session do not each store a history of their own
        synchronized (SESSION_LOCK) {
            Object historyObj = session.getAttribute(SESSION_KEY);
            if (historyObj instanceof History) {
                return (History) historyObj;
            }
            if (historyObj == null && !create) {
                return null;
            }
            History history = historyObj instanceof List
                    ? new History((List<Map<String, String>>) historyObj)
                    : new History();
            session.setAttribute(SESSION_KEY, history);
            return history;
        }
    }

    private static Response historyTooLargeResponse() {
        return Response.status(413)
                .entity(Map.of("success", false, "error", "Navigation history size limit exceeded"))
                .build();
    }

    /**
     * The navigation history of one session. 
     * Multiple requests from a session can run at the same time, e.g. from
     * several tabs, so the entries are only accessed through the synchronized methods of this class.
     */
    public static class History implements Serializable {
        private static final long serialVersionUID = 1L;

        private final List<Map<String, String>> entries = new ArrayList<>();
        // Size of the entries as a JSON array, kept up to date so the whole history is not serialized on every add
        private long bytes = 2; // []

        History() {
        }

        History(List<Map<String, String>> existing) {
            for (Map<String, String> entry : existing) {
                append(entry, entryBytes(entry));
            }
        }

        /**
         * Add an entry, unless that would make the history too large.
         *
         * @return false if the entry was rejected because of the size limit
         */
        synchronized boolean add(Map<String, String> entry) {
            long entryBytes = entryBytes(entry);
            if (bytes + entryBytes > MAX_HISTORY_BYTES) {
                log.warn("Navigation history would exceed {} bytes, rejecting new entry", MAX_HISTORY_BYTES);
                return false;
            }
            append(entry, entryBytes);
            return true;
        }

        public synchronized int size() {
            return entries.size();
        }

        /**
         * @return a copy of the entries, safe to use while other requests add to the history
         */
        public synchronized List<Map<String, String>> entries() {
            return new ArrayList<>(entries);
        }

        private void append(Map<String, String> entry, long entryBytes) {
            entries.add(entry);
            bytes += entryBytes;
        }

        /**
         * The UTF-8 size of the entry as JSON.
         * This includes the separating comma if it is not the first entry.
         */
        private long entryBytes(Map<String, String> entry) {
            long separator = entries.isEmpty() ? 0 : 1;
            try {
                return JsonUtils.toJSON(entry).getBytes(StandardCharsets.UTF_8).length + separator;
            } catch (RuntimeException e) {
                // If we cannot measure size for some reason, log a warning and allow the operation.
                log.warn("Could not determine size of navigation history entry, allowing update", e);
                return separator;
            }
        }
    }

}
