export default {
  methods: {
    $_pushSearchHistory(destination, query, appliedFacets, solrSettings) {
    // Should be deleted before production.
    //console.log('We about to push a history.', appliedFacets)
    let currentQuery = this.$router.history.current.query
    if(query !== '' && query !== undefined && (currentQuery.query !== query ||
       currentQuery.grouping !== solrSettings.grouping ||
       currentQuery.urlSearch !== solrSettings.urlSearch ||
       currentQuery.imgSearch !== solrSettings.imgSearch ||
       currentQuery.fileSearch !== solrSettings.fileSearch ||
       currentQuery.facets !== appliedFacets.join('') ||
       currentQuery.offset !== solrSettings.offset)) {
      this.$router.push({name: destination, query: { query: query,
                                                     grouping: solrSettings.grouping,
                                                     imgSearch: solrSettings.imgSearch,
                                                     offset: solrSettings.offset,
                                                     urlSearch: solrSettings.urlSearch,
                                                     fileSearch: solrSettings.fileSearch,
                                                     facets:appliedFacets.join('')
                                                     } })
      //console.log(this.$router, 'we pushed route yey')
      }
      else {
        //console.log('No changes, no dice!')
      }
    },
    $_pushCleanHistory(destination) {
      // Should be deleted before production.
      //console.log('We pushed a history.')
        this.$router.push({name: destination })
      //console.log(this.$router, 'we pushed route yey')
      }
  }
}
