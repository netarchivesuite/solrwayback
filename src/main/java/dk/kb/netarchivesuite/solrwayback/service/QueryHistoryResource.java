package dk.kb.netarchivesuite.solrwayback.service;

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

/**
 * REST resource for tracking query history in session storage.
 * Provides endpoints for tracking search queries, playback navigation, and downloading history.
 */
@Path("/queryhistory/")
public class QueryHistoryResource {

    private static final Logger log = LoggerFactory.getLogger(QueryHistoryResource.class);
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
            List<Map<String, String>> history = getHistory(session);
            
            String url = (String) data.get("url");
            String waybackDate = (String) data.get("waybackDate");
            String originalUrl = (String) data.get("originalUrl");
            String timestamp = DATE_FORMAT.format(new Date());
            
            Map<String, String> entry = new HashMap<>();
            entry.put("url", url);
            entry.put("timestamp", timestamp);
            if (waybackDate != null) {
                entry.put("waybackDate", waybackDate);
            }
            if (originalUrl != null) {
                entry.put("originalUrl", originalUrl);
            }
            
            history.add(entry);
            session.setAttribute(SESSION_KEY, history);
            
            log.debug("Tracked playback: {} ({})", originalUrl, waybackDate);
            
            return Response.ok()
                    .entity(Map.of("success", true, "count", history.size()))
                    .build();
        } catch (Exception e) {
            log.error("Error tracking playback", e);
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
     * Download history as text file
     */
    @GET
    @Path("download")
    @Produces(MediaType.TEXT_PLAIN)
    public Response downloadHistory(@Context HttpServletRequest request) {
        try {
            HttpSession session = request.getSession(false);
            if (session == null) {
                return Response.ok("# No query history available\n")
                        .header("Content-Disposition", "attachment; filename=\"query_history.txt\"")
                        .build();
            }
            
            List<Map<String, String>> history = getHistory(session);
            String content = formatHistoryAsText(history);
            
            log.info("Downloading query history with {} entries", history.size());
            
            return Response.ok(content)
                    .header("Content-Disposition", "attachment; filename=\"query_history.txt\"")
                    .build();
        } catch (Exception e) {
            log.error("Error downloading history", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error generating history file: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Clear history (optional endpoint for testing)
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
    private List<Map<String, String>> getHistory(HttpSession session) {
        Object historyObj = session.getAttribute(SESSION_KEY);
        if (historyObj instanceof List) {
            return (List<Map<String, String>>) historyObj;
        }
        return new ArrayList<>();
    }

    /**
     * Format history entries as text file content
     */
    private String formatHistoryAsText(List<Map<String, String>> history) {
        StringBuilder sb = new StringBuilder();
        sb.append("# SolrWayback Query History\n");
        sb.append("# Generated: ").append(DATE_FORMAT.format(new Date())).append("\n");
        sb.append("# Total entries: ").append(history.size()).append("\n");
        sb.append("#\n");
        sb.append("# Format: [timestamp] URL [waybackDate] [originalUrl]\n");
        sb.append("#\n\n");
        
        for (Map<String, String> entry : history) {
            sb.append("[").append(entry.get("timestamp")).append("] ");
            sb.append(entry.get("url"));
            
            if (entry.containsKey("waybackDate")) {
                sb.append(" [").append(entry.get("waybackDate")).append("]");
            }
            if (entry.containsKey("originalUrl")) {
                sb.append(" [").append(entry.get("originalUrl")).append("]");
            }
            
            sb.append("\n");
        }
        
        return sb.toString();
    }
}
