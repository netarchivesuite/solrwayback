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
     * Format history entries as text file content in the SolrWaybackQueryHistory format
     */
    private String formatHistoryAsText(List<Map<String, String>> history) {
        StringBuilder sb = new StringBuilder();
        sb.append("SolrWayback Query History\n");
        sb.append("=========================\n\n");
        
        int actionNumber = 1;
        String lastQuery = null;
        boolean lastWasPlayback = false;
        
        for (Map<String, String> entry : history) {
            String url = entry.get("url");
            
            // Determine if this is a search query or playback URL
            if (url.contains("/search?query=")) {
                // Extract query from search URL
                String query = extractQueryFromUrl(url);
                
                // Only output if query changed
                if (query != null && !query.equals(lastQuery)) {
                    sb.append("Action Number: ").append(actionNumber++).append("\n");
                    sb.append("SolrWayback Query changed.\n");
                    sb.append("Query: ").append(query).append("\n");
                    sb.append("-----------------------\n");
                    lastQuery = query;
                    lastWasPlayback = false;
                }
            } else if (url.contains("/services/web/") && entry.containsKey("waybackDate")) {
                // Playback URL
                String waybackDate = entry.get("waybackDate");
                String originalUrl = entry.get("originalUrl");
                
                sb.append("Action Number: ").append(actionNumber++).append("\n");
                
                // Distinguish between clicks from search results vs. links within playback
                if (lastWasPlayback) {
                    sb.append("Followed a link in playback:\n");
                } else {
                    sb.append("Found interesting search result and clicked it from search results:\n");
                }
                
                sb.append("SolrWayback Playback URL clicked.\n");
                sb.append("Archive Date: ").append(waybackDate).append("\n");
                sb.append("Original URL: ").append(originalUrl != null ? originalUrl : "unknown").append("\n");
                sb.append("-----------------------\n");
                
                lastWasPlayback = true;
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Extract the query parameter from a search URL
     */
    private String extractQueryFromUrl(String url) {
        try {
            int queryStart = url.indexOf("query=");
            if (queryStart == -1) return null;
            
            queryStart += 6; // Skip "query="
            int queryEnd = url.indexOf("&", queryStart);
            if (queryEnd == -1) queryEnd = url.length();
            
            String query = url.substring(queryStart, queryEnd);
            
            // URL decode the query
            query = java.net.URLDecoder.decode(query, "UTF-8");
            
            // Replace + with space if not decoded
            query = query.replace("+", " ");
            
            return query;
        } catch (Exception e) {
            log.warn("Failed to extract query from URL: {}", url, e);
            return null;
        }
    }
}
