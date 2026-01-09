import axios from 'axios'

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
        await axios.post('services/navigationhistory/track/search', { url }, {
          headers: { 'Content-Type': 'application/json' }
        })
      } catch (e) {
        console.warn('Query history tracking failed:', e)
      }
    }
  }
}
