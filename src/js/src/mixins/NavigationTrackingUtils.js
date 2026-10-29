import axios from 'axios'

// Per-tab ID kept in sessionStorage, shared with the playback tracker (playback-tracker.js)
const TAB_ID_KEY = 'solrwaybackTabId'
let tabId = null

// The ID of this browser tab, so entries from different tabs can be told apart in the history
function getTabId() {
  if (tabId) {
    return tabId
  }
  try {
    let id = sessionStorage.getItem(TAB_ID_KEY)
    try {
      // Tabs opened by script get a copy of the opener's sessionStorage, and with it the opener's ID
      if (id && window.opener && window.opener.sessionStorage.getItem(TAB_ID_KEY) === id) {
        id = null
      }
    } catch (e) {
      // Opener on another origin
    }
    if (!id) {
      id = Date.now().toString(36) + Math.random().toString(36).substring(2, 10)
      sessionStorage.setItem(TAB_ID_KEY, id)
    }
    tabId = id
    return id
  } catch (e) {
    return null
  }
}

export default {
  methods: {
    async $_trackSearchOnServer(query, appliedFacets, solrSettings) {
      try {
        // Build the search URL
        const params = new URLSearchParams({
          query: query,
          grouping: solrSettings.grouping,
          imgSearch: solrSettings.imgSearch,
          offset: solrSettings.imgSearch === true ? 0 : solrSettings.offset,
          urlSearch: solrSettings.urlSearch,
          facets: appliedFacets.join(''),
          sort: solrSettings.sort
        })
        const url = `${window.location.origin}${window.location.pathname}#/search?${params.toString()}`

        // Send to server
        await axios.post('services/navigationhistory/track/search', { url, tabId: getTabId() }, {
          headers: { 'Content-Type': 'application/json' }
        })
      } catch (e) {
        console.warn('Query history tracking failed:', e)
      }
    }
  }
}
