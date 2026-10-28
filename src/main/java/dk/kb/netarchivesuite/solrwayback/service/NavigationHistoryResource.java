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

            List<Map<String, String>> history = getHistory(session);

            String url = (String) data.get("url");
            // Truncate to seconds so we don't include fractional seconds (e.g. 2026-03-02T18:05:57Z)
            String timestamp = DATE_FORMAT.format(Instant.now().truncatedTo(ChronoUnit.SECONDS));

            Map<String, String> entry = new HashMap<>();
            entry.put("url", url);
            entry.put("timestamp", timestamp);

            if (!addToHistory(session, history, entry)) {
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
            List<Map<String, String>> history = getHistory(session);

            String url = (String) data.get("url");
            String originalUrl = (String) data.get("originalUrl");
            String archivalDate = convertWaybackDate2SolrDate((String) data.get("waybackDate"));
            // Truncate to seconds so we don't include fractional seconds (e.g. 2026-03-02T18:05:57Z)
            String timestamp = DATE_FORMAT.format(Instant.now().truncatedTo(ChronoUnit.SECONDS));
            NavigationHistoryAction action = NavigationHistoryUtils.classifyPlayback(
                    url,
                    (String) data.get("referrer"),
                    (String) data.get("navigationType"),
                    (String) data.get("clickSource"),
                    Boolean.TRUE.equals(data.get("sameDocument")));

            Map<String, String> entry = new HashMap<>();
            entry.put("url", url);
            entry.put("originalUrl", originalUrl);
            entry.put("date", archivalDate);
            entry.put("timestamp", timestamp);
            entry.put("action", action.name());

            if (!addToHistory(session, history, entry)) {
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
     * missing images, stylesheets, frames etc., and only for sessions that already have a navigation history.
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

            if (addToHistory(session, getHistory(session), entry)) {
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
            int count = 0;
            if (session != null) {
                List<Map<String, String>> history = getHistory(session);
                count = history.size();
            }
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

            List<Map<String, String>> history = getHistory(session);
            List<Map<String, Object>> formattedHistory = NavigationHistoryUtils.formatHistoryAsJson(history);

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
     * Get history list from session, creating if necessary
     */
    @SuppressWarnings("unchecked")
    private static List<Map<String, String>> getHistory(HttpSession session) {
        Object historyObj = session.getAttribute(SESSION_KEY);
        if (historyObj instanceof List) {
            return (List<Map<String, String>>) historyObj;
        }
        // Return a new empty list; the caller is responsible for persisting it via setAttribute
        return new ArrayList<>();
    }

    /**
     * Add an entry to the history stored in the session, unless that would make the history too large.
     *
     * @return false if the entry was rejected because of the size limit
     */
    private static boolean addToHistory(HttpSession session, List<Map<String, String>> history, Map<String, String> entry) {
        // Check size before mutating the session-stored list
        List<Map<String, String>> updatedHistory = new ArrayList<>(history);
        updatedHistory.add(entry);
        if (isTooLarge(updatedHistory)) {
            log.warn("Navigation history would exceed {} bytes, rejecting new entry", MAX_HISTORY_BYTES);
            return false;
        }

        history.add(entry);
        session.setAttribute(SESSION_KEY, history);
        return true;
    }

    private static Response historyTooLargeResponse() {
        return Response.status(413)
                .entity(Map.of("success", false, "error", "Navigation history size limit exceeded"))
                .build();
    }

    /**
     * Check whether the serialized history exceeds the maximum allowed size.
     * Uses the project's JsonUtils to convert to JSON and measures UTF-8 bytes.
     */
    private static boolean isTooLarge(List<Map<String, String>> history) {
        try {
            String json = JsonUtils.toJSON(history);
            long bytes = json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
            return bytes > MAX_HISTORY_BYTES;
        } catch (RuntimeException e) {
            // If we cannot measure size for some reason, log a warning and allow the operation.
            log.warn("Could not determine size of navigation history, allowing update", e);
            return false;
        }
    }

}
