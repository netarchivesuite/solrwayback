# Query History Feature

This feature allows users to track and download their search history for the current browser session.

## Components

### QueryHistoryParser.vue
A utility component that parses search URLs from session storage and formats them into a readable text format. It handles:
- Parsing search query parameters (query, facets, filter queries)
- Parsing playback URLs (archived content clicks)
- Formatting entries for download
- Generating timestamped text files

### QueryHistoryButton.vue
A UI button component displayed in the search tools menu that:
- Shows the current number of history entries as a badge
- Enables download when history is available
- Integrates with the QueryHistoryParser to generate downloads
- Updates dynamically when new searches are performed

### queryHistoryTracker.js
A utility module that:
- Tracks search URLs in session storage (not localStorage, so history is session-only)
- Provides functions to add, retrieve, and clear history
- Dispatches custom events to notify components of history updates

## How It Works

1. **Tracking**: When a user performs a search, the `trackSearch` function in `queryHistoryTracker.js` is called from `HistoryRoutingUtils.js`, storing the full search URL in session storage.

2. **Display**: The `QueryHistoryButton` component monitors session storage and displays the count of history entries as a badge on the button.

3. **Download**: When the user clicks the button, the `QueryHistoryParser` reads all entries from session storage, formats them (similar to the Python script in https://github.com/WEB-CHILD/SolrWaybackQueryHistory), and triggers a download of a timestamped text file.

## Session Storage vs Local Storage

The implementation uses **session storage** instead of local storage, meaning:
- History is only available for the current browser tab/window session
- History is automatically cleared when the browser tab is closed
- No persistent tracking across sessions
- Better for privacy and reduced storage usage

## Integration Points

- **SearchBox.vue**: Contains the QueryHistoryButton in the tools menu
- **HistoryRoutingUtils.js**: Calls `trackSearch()` when pushing search history to the router
- **Session Storage Key**: `solrwayback_query_history`

## Output Format

The downloaded text file follows the format from the SolrWaybackQueryHistory tool:

```
SolrWayback Query History
=========================

Action Number: 1
SolrWayback Query changed.
Query: example search term
Facets: &fq=type:"Web Page"
Filter Query 1: type:"Web Page"
-----------------------

Action Number: 2
Clicked on a link from search results:
SolrWayback Playback URL clicked.
Archive Date: 20201215120000
Original URL: http://example.com
-----------------------
```

## Future Enhancements

Possible improvements:
- Add option to clear history manually
- Filter history by search type (normal/URL/image search)
- Export in different formats (JSON, CSV)
- Search within history
- Statistics/analytics view
