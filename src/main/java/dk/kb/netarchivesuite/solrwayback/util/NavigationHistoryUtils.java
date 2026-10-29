package dk.kb.netarchivesuite.solrwayback.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


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
    public static int populateQueryEntry(int actionNumber, String timestamp, String url, List<Map<String, Object>> result) {
        Map<String, Object> jsonEntry = new LinkedHashMap<>();
        jsonEntry.put("number", actionNumber++);
        jsonEntry.put("action", NavigationHistoryAction.QUERY);
        jsonEntry.put("timestamp", timestamp);

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
     * Decide how a playback page was reached, from what the browser reported when the page was shown.
     *
     * @param url            the playback URL, e.g. https://host/solrwayback/services/web/20051102141713/http://example.com/
     * @param referrer       {@code document.referrer} of the playback page, may be null or empty
     * @param navigationType the browser's navigation type: "navigate", "reload" or "back_forward", may be null
     * @param clickSource    "toolbar" or "page link" if the previous page recorded a click on a link to this page, else null
     * @param sameDocument   true if the URL changed without loading a new page (anchor or history.pushState)
     */
    public static NavigationHistoryAction classifyPlayback(String url, String referrer, String navigationType, String clickSource, boolean sameDocument) {
        // Determine how a playback page was reached
        if (sameDocument) {
            return NavigationHistoryAction.PLAYBACK_NAVIGATION;
        }
        if ("back_forward".equals(navigationType)) {
            return NavigationHistoryAction.BROWSER_HISTORY_NAVIGATION;
        }
        if ("reload".equals(navigationType)) {
            return NavigationHistoryAction.PAGE_RELOADED;
        }
        if ("toolbar".equals(clickSource)) {
            return NavigationHistoryAction.TOOLBAR_LINK_CLICKED;
        }
        if ("page link".equals(clickSource)) {
            return NavigationHistoryAction.PLAYBACK_LINK_CLICKED;
        }
        if (referrer == null || referrer.isEmpty()) {
            return NavigationHistoryAction.DIRECT_NAVIGATION;
        }

        // The SolrWayback frontend and playback share the base URL, e.g. https://host/solrwayback/
        int servicesIndex = url == null ? -1 : url.indexOf("services/");
        if (servicesIndex == -1 || !referrer.startsWith(url.substring(0, servicesIndex))) {
            return NavigationHistoryAction.OTHER_NAVIGATION;
        }
        String page = referrer.substring(servicesIndex);
        if (page.startsWith("services/web/") || page.startsWith("services/lenient/web/") || page.startsWith("services/view")) {
            return NavigationHistoryAction.PLAYBACK_NAVIGATION;
        }
        if (page.startsWith("calendar")) {
            return NavigationHistoryAction.CALENDAR_LINK_CLICKED;
        }
        if (page.isEmpty() || page.startsWith("search") || page.startsWith("?")) {
            return NavigationHistoryAction.SEARCH_RESULT_CLICKED;
        }
        return NavigationHistoryAction.OTHER_NAVIGATION;
    }

    /**
     * Populate a playback entry in the result list.
     */
    public static int populateResultEntries(int actionNumber, NavigationHistoryAction action, String timestamp, String date, String originalUrl, List<Map<String, Object>> result, String url) {
        // Playback URL
        Map<String, Object> jsonEntry = new LinkedHashMap<>();
        jsonEntry.put("number", actionNumber++);
        jsonEntry.put("action", action);
        jsonEntry.put("timestamp", timestamp);
        jsonEntry.put("date", date);
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
        EntryLinker linker = new EntryLinker();

        for (Map<String, String> entry : history) {
            String url = entry.get("url");
            String timestamp = entry.get("timestamp");
            String date = entry.get("date");
            String originalUrl = entry.get("originalUrl");
            NavigationHistoryAction storedAction = parseAction(entry.get("action"));

            // Determine if this is a search query or playback URL
            if (url.contains("/search?query=")) {
                // Only output if URL changed (covers query changes AND facet changes)
                if (!url.equals(lastUrl)) {
                    actionNumber = populateQueryEntry(actionNumber, timestamp, url, result);
                    linker.link(result.get(result.size() - 1), entry, NavigationHistoryAction.QUERY);
                    lastUrl = url;
                    lastWasPlayback = false;
                }
            } else if (storedAction != null || url.contains("/services/web/")) {
                // Entries tracked before the action was recorded fall back to guessing from the order:
                // the first playback after a search is a search result, the following ones are links
                NavigationHistoryAction action = storedAction;
                if (action == null) {
                    action = lastWasPlayback ? NavigationHistoryAction.PLAYBACK_LINK_CLICKED : NavigationHistoryAction.SEARCH_RESULT_CLICKED;
                }
                actionNumber = populateResultEntries(actionNumber, action, timestamp, date, originalUrl, result, url);
                linker.link(result.get(result.size() - 1), entry, action);
                lastUrl = null;  // Reset so next search is always tracked
                lastWasPlayback = true;
            }
        }

        return result;
    }

    /**
     * Links exported entries to the browser tab they happened in ("tab", numbered from 1 in order of appearance)
     * and to the earlier entry for the page they were opened from ("from"), found from the stored referrer.
     */
    private static class EntryLinker {
        private final Map<String, Integer> tabNumbers = new HashMap<>();
        // Exported playback entries by playback URL, the most recent one wins
        private final Map<String, Map<String, Object>> playbacks = new HashMap<>();
        // Exported query entries, most recent last
        private final List<Map<String, Object>> queries = new ArrayList<>();

        void link(Map<String, Object> jsonEntry, Map<String, String> entry, NavigationHistoryAction action) {
            // Back/forward and reload report the referrer of the original page load, not where the user came from
            boolean hasParent = action != NavigationHistoryAction.QUERY
                    && action != NavigationHistoryAction.BROWSER_HISTORY_NAVIGATION
                    && action != NavigationHistoryAction.PAGE_RELOADED;
            Map<String, Object> parent = hasParent ? findParent(entry.get("referrer")) : null;
            if (parent != null) {
                jsonEntry.put("from", parent.get("number"));
            }

            // Failed playback has no tab ID of its own, it happened in the tab of the page it was opened from
            Object tab = tabNumber(entry.get("tabId"));
            if (tab == null && parent != null) {
                tab = parent.get("tab");
            }
            if (tab != null) {
                jsonEntry.put("tab", tab);
            }

            if (action == NavigationHistoryAction.QUERY) {
                queries.add(jsonEntry);
            } else {
                playbacks.put(withoutFragment(entry.get("url")), jsonEntry);
            }
        }

        private Integer tabNumber(String tabId) {
            if (tabId == null || tabId.isEmpty()) {
                return null;
            }
            return tabNumbers.computeIfAbsent(tabId, id -> tabNumbers.size() + 1);
        }

        private Map<String, Object> findParent(String referrer) {
            if (referrer == null || referrer.isEmpty()) {
                return null;
            }
            Map<String, Object> playback = playbacks.get(withoutFragment(referrer));
            if (playback != null || referrer.contains("/services/") || !referrer.contains("query=")) {
                return playback;
            }

            // A search page: the most recent search for the same query and facets
            String query = extractQueryFromUrl(referrer);
            String facets = extractFacetsFromUrl(referrer);
            for (int i = queries.size() - 1; i >= 0; i--) {
                Map<String, Object> queryEntry = queries.get(i);
                if (Objects.equals(query, queryEntry.get("query")) && Objects.equals(facets, queryEntry.get("facets"))) {
                    return queryEntry;
                }
            }
            return null;
        }

        private static String withoutFragment(String url) {
            if (url == null) {
                return null;
            }
            int hashIndex = url.indexOf('#');
            return hashIndex == -1 ? url : url.substring(0, hashIndex);
        }
    }

    /**
     * Parse the action stored with a history entry, or null if none or unknown.
     */
    private static NavigationHistoryAction parseAction(String action) {
        if (action == null) {
            return null;
        }
        try {
            return NavigationHistoryAction.valueOf(action);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown navigation history action: {}", action);
            return null;
        }
    }
}
