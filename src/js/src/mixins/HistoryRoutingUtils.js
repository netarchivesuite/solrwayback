import { trackSearch } from '../utils/queryHistoryTracker'

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
      // Track search in session storage (runs after routing completes)
      setTimeout(() => {
        try {
          trackSearch(query, appliedFacets, solrSettings, window.location.origin)
        } catch (e) {
          console.warn('Query history tracking failed:', e)
        }
      }, 0)
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
