package dk.kb.netarchivesuite.solrwayback.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


import static java.net.URLDecoder.decode;

public class NavigationHistoryUtils {
    private static final Logger log = LoggerFactory.getLogger(NavigationHistoryUtils.class);


    /**
     * Extract the facets parameter from a search URL
     */
    public static String extractFacetsFromUrl(String url) {
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
    public static List<String> extractFilterQueries(String facets) {
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

    /**
     * Populate a query entry in the result list
     */
    public static int populateQueryEntry(int actionNumber, String url, List<Map<String, Object>> result) {
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
    private static String extractQueryFromUrl(String url) {
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
     * Populate a playback link entry in the result list. Destinguishes between clicks from search results vs. links within playback.
     */
    public static int populateResultEntries(int actionNumber, boolean lastWasPlayback, String timestamp, String originalUrl, List<Map<String, Object>> result, String url) {
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
     * Format history entries as JSON array
     */
    public static List<Map<String, Object>> formatHistoryAsJson(List<Map<String, String>> history) {
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
}
