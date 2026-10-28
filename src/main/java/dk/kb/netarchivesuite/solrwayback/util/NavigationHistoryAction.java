package dk.kb.netarchivesuite.solrwayback.util;

/**
 * Represents the type of action recorded in a user's navigation history.
 */
public enum NavigationHistoryAction {
    QUERY("QUERY"),
    // Playback opened from the SolrWayback search page
    SEARCH_RESULT_CLICKED("SEARCH RESULT CLICKED"),
    // A link was clicked in a played back page
    PLAYBACK_LINK_CLICKED("PLAYBACK LINK CLICKED"),
    // First/previous/next/last capture link clicked in the playback toolbar
    TOOLBAR_LINK_CLICKED("TOOLBAR LINK CLICKED"),
    // Playback opened from the harvest calendar
    CALENDAR_LINK_CLICKED("CALENDAR LINK CLICKED"),
    // Left a played back page without a recorded link click: redirect, script, form, link opened from the context menu
    // or a URL change within the page
    PLAYBACK_NAVIGATION("PLAYBACK NAVIGATION"),
    // No referrer: URL typed, pasted or opened from a bookmark
    DIRECT_NAVIGATION("DIRECT NAVIGATION"),
    // Browser back/forward
    BROWSER_HISTORY_NAVIGATION("BROWSER HISTORY NAVIGATION"),
    PAGE_RELOADED("PAGE RELOADED"),
    // Opened from another page than the ones above, e.g. an external site
    OTHER_NAVIGATION("OTHER NAVIGATION"),
    // A page was requested for playback but has never been harvested
    PLAYBACK_FAILED("PLAYBACK FAILED");

    private final String label;

    NavigationHistoryAction(String label) {
        this.label = label.toUpperCase();
    }

    @Override
    public String toString() {
        return label;
    }
}
