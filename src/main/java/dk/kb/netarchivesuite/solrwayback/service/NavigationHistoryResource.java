package dk.kb.netarchivesuite.solrwayback.service;

import dk.kb.netarchivesuite.solrwayback.util.NavigationHistoryUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.SimpleDateFormat;
import java.util.*;

import static dk.kb.netarchivesuite.solrwayback.util.DateUtils.convertWaybackDate2SolrDate;
import static java.net.URLDecoder.decode;

/**
 * REST resource for tracking query history in session storage.
 * Provides endpoints for tracking search queries, playback navigation, and downloading history.
 */
@Path("/navigationhistory/")
public class NavigationHistoryResource {

    private static final Logger log = LoggerFactory.getLogger(NavigationHistoryResource.class);
    private static final String SESSION_KEY = "solrwayback_query_history";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

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
            String timestamp = DATE_FORMAT.format(new Date());
            
            Map<String, String> entry = new HashMap<>();
            entry.put("url", url);
            entry.put("timestamp", timestamp);
            
            history.add(entry);
            session.setAttribute(SESSION_KEY, history);
            
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
            String timestamp = convertWaybackDate2SolrDate((String) data.get("waybackDate"));

            Map<String, String> entry = new HashMap<>();
            entry.put("url", url);
            entry.put("originalUrl", originalUrl);
            entry.put("timestamp", timestamp);
            
            history.add(entry);
            session.setAttribute(SESSION_KEY, history);
            log.debug("Tracked playback: {}", url);
            
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
        try {
            HttpSession session = request.getSession(false);
            if (session == null) {
                List<Map<String, Object>> emptyHistory = new ArrayList<>();
                return Response.ok(emptyHistory)
                        .header("Content-Disposition", "attachment; filename=\"query_history.json\"")
                        .build();
            }
            
            List<Map<String, String>> history = getHistory(session);
            List<Map<String, Object>> formattedHistory = NavigationHistoryUtils.formatHistoryAsJson(history);
            
            log.info("Downloading query history with {} entries", formattedHistory.size());
            
            return Response.ok(formattedHistory)
                    .header("Content-Disposition", "attachment; filename=\"query_history.json\"")
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
    private List<Map<String, String>> getHistory(HttpSession session) {
        Object historyObj = session.getAttribute(SESSION_KEY);
        if (historyObj instanceof List) {
            return (List<Map<String, String>>) historyObj;
        }
        return new ArrayList<>();
    }

}
