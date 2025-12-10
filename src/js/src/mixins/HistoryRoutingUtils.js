export default {
  methods: {
    $_pushSearchHistory(destination, query, appliedFacets, solrSettings) {
    const currentParams = this.$route.query
    if(this.checkForChangesInQuery(query, currentParams, solrSettings, appliedFacets)) {
      this.$router.push({name: destination, query: { query: query,
                                                     grouping: solrSettings.grouping,
                                                     imgSearch: solrSettings.imgSearch,
                                                     offset: solrSettings.imgSearch === true ? 0 : solrSettings.offset,
                                                     urlSearch: solrSettings.urlSearch,
                                                     facets:appliedFacets.join(''),
                                                     sort:solrSettings.sort
                                                     } })
      // Track search on server (runs after routing completes)
      setTimeout(() => {
        this.$_trackSearchOnServer(query, appliedFacets, solrSettings)
      }, 0)
      }
    },
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
        await fetch(`${window.location.origin}/solrwayback/services/queryhistory/track/search`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          credentials: 'same-origin',
          body: JSON.stringify({ url })
        })
      } catch (e) {
        console.warn('Query history tracking failed:', e)
      }
    },
    $_pushCleanHistory(destination) {
        this.$router.push({name: destination })
      },
    checkForChangesInQuery(query, currentParams, solrSettings, appliedFacets) {
      return query !== '' && query !== undefined && (currentParams.query !== query ||
        currentParams.grouping !== solrSettings.grouping ||
        currentParams.urlSearch !== solrSettings.urlSearch ||
        currentParams.imgSearch !== solrSettings.imgSearch ||
        currentParams.facets !== appliedFacets.join('') ||
        currentParams.offset !== solrSettings.offset ||
        currentParams.sort !== solrSettings.sort)
    },
    $_getResolvedUrl(destination, query, appliedFacets, solrSettings) {
      const route = this.$router.resolve({name: destination, query: { query: query,
        grouping: solrSettings.grouping,
        imgSearch: solrSettings.imgSearch,
        offset: solrSettings.imgSearch === true ? 0 : solrSettings.offset,
        urlSearch: solrSettings.urlSearch,
        facets:appliedFacets.join(''),
        sort:solrSettings.sort
        } })
        return route
    }
  },
}
