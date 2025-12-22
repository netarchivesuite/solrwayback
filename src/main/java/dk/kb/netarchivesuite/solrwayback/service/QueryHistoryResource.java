package dk.kb.netarchivesuite.solrwayback.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

import static dk.kb.netarchivesuite.solrwayback.util.DateUtils.convertWaybackDate2SolrDate;
import static java.net.URLDecoder.decode;

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
            List<Map<String, Object>> formattedHistory = formatHistoryAsJson(history);
            
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

    /**
     * Format history entries as JSON array
     */
    private List<Map<String, Object>> formatHistoryAsJson(List<Map<String, String>> history) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        int actionNumber = 1;
        String lastUrl = null;
        boolean lastWasPlayback = false;
        
        for (Map<String, String> entry : history) {
            String url = entry.get("url");
            String timestamp = entry.get("timestamp");
            String originalUrl = entry.get("originalUrl");

            // Determine if this is a search query or playback URL
            if (url.contains("/search?query=")) {
                // Only output if URL changed (covers query changes AND facet changes)
                if (!url.equals(lastUrl)) {
                    actionNumber = populateQueryEntry(actionNumber, url, result);
                    lastUrl = url;
                    lastWasPlayback = false;
                }
            } else if (url.contains("/services/web/")) {
                actionNumber = populateResultEntries(actionNumber, lastWasPlayback, timestamp, originalUrl, result, url);
                lastUrl = null;  // Reset so next search is always tracked
                lastWasPlayback = true;
            }
        }
        
        return result;
    }

    /**
     * Populate a playback link entry in the result list. Destinguishes between clicks from search results vs. links within playback.
     */
    private static int populateResultEntries(int actionNumber, boolean lastWasPlayback, String timestamp, String originalUrl, List<Map<String, Object>> result, String url) {
        // Playback URL
        Map<String, Object> jsonEntry = new LinkedHashMap<>();
        jsonEntry.put("number", actionNumber++);

        // Distinguish between clicks from search results vs. links within playback
        if (lastWasPlayback) {
            jsonEntry.put("action", "playback link clicked");
        } else {
            jsonEntry.put("action", "search result clicked");
        }

        jsonEntry.put("date", timestamp);
        jsonEntry.put("url", originalUrl != null ? originalUrl : "unknown");
        jsonEntry.put("archivedUrl", url);

        result.add(jsonEntry);
        return actionNumber;
    }

    /**
     * Populate a query entry in the result list
     */
    private int populateQueryEntry(int actionNumber, String url, List<Map<String, Object>> result) {
        Map<String, Object> jsonEntry = new LinkedHashMap<>();
        jsonEntry.put("number", actionNumber++);
        jsonEntry.put("action", "query");

        // Extract query
        String query = extractQueryFromUrl(url);
        if (query != null) {
            jsonEntry.put("query", query);
        }
        // Extract facets and filter queries
        String facets = extractFacetsFromUrl(url);
        if (facets != null && !facets.isEmpty()) {
            jsonEntry.put("facets", facets);

            // Extract filter queries from facets
            List<String> filterQueries = extractFilterQueries(facets);
            if (!filterQueries.isEmpty()) {
                jsonEntry.put("filterQueries", filterQueries);
            }
        }

        result.add(jsonEntry);
        return actionNumber;
    }

    /**
     * Extract the query parameter from a search URL
     */
    private String extractQueryFromUrl(String url) {
        try {
            int queryStart = url.indexOf("query=");
            if (queryStart == -1) {
                return null;
            }
            
            queryStart += 6; // Skip "query="
            int queryEnd = url.indexOf("&", queryStart);
            if (queryEnd == -1) {
                queryEnd = url.length();
            }
            
            String query = url.substring(queryStart, queryEnd);
            
            // URL decode the query
            query = decode(query, StandardCharsets.UTF_8);
            
            // Replace + with space if not decoded
            query = query.replace("+", " ");
            
            return query;
        } catch (Exception e) {
            log.warn("Failed to extract query from URL: {}", url, e);
            return null;
        }
    }
    
    /**
     * Extract the facets parameter from a search URL
     */
    private String extractFacetsFromUrl(String url) {
        try {
            int facetsStart = url.indexOf("facets=");
            if (facetsStart == -1){
                return null;
            }
            
            facetsStart += 7; // Skip "facets="
            int facetsEnd = url.indexOf("&", facetsStart);
            if (facetsEnd == -1){
                facetsEnd = url.length();
            }
            
            String facets = url.substring(facetsStart, facetsEnd);
            
            // URL decode the facets
            facets = decode(facets, StandardCharsets.UTF_8);
            
            return facets.isEmpty() ? null : facets;
        } catch (Exception e) {
            log.warn("Failed to extract facets from URL: {}", url, e);
            return null;
        }
    }
    
    /**
     * Extract individual filter queries from the facets parameter
     * Example: "&fq=domain:example.com&fq=crawl_year:2020" -> ["domain:example.com", "crawl_year:2020"]
     */
    private List<String> extractFilterQueries(String facets) {
        List<String> filterQueries = new ArrayList<>();
        if (facets == null || facets.isEmpty()) {
            return filterQueries;
        }
        
        try {
            // Split by &fq= to get individual filter queries
            String[] parts = facets.split("&fq=");
            for (String part : parts) {
                if (!part.isEmpty() && !part.equals("fq=")) {
                    // Remove leading "fq=" if present
                    String fq = part.startsWith("fq=") ? part.substring(3) : part;
                    if (!fq.isEmpty()) {
                        filterQueries.add(fq);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract filter queries from facets: {}", facets, e);
        }
        
        return filterQueries;
    }
}
