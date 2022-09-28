export default {
  methods: {
    $_pushSearchHistory(destination, query, appliedFacets, solrSettings) {
    const currentParams = this.$router.history.current.query
    if(this.checkForChangesInQuery(query, currentParams, solrSettings, appliedFacets)) {
      this.$router.push({name: destination, query: { query: query,
                                                     grouping: solrSettings.grouping,
                                                     imgSearch: solrSettings.imgSearch,
                                                     offset: solrSettings.imgSearch === true ? 0 : solrSettings.offset,
                                                     urlSearch: solrSettings.urlSearch,
                                                     facets:appliedFacets.join('')
                                                     } })
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
        currentParams.offset !== solrSettings.offset)
    }
  },
}
