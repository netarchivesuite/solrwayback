export default {
  methods: {
    $_pushSearchHistory(destination, query, appliedFacets, solrSettings) {
    // Should be deleted before production.
    console.log('We pushed a history.')
    let newFacetUrl = appliedFacets !== '' ? '&facets=' + encodeURIComponent(appliedFacets) : ''
    let newOptionsUrl = '&offset=' + solrSettings.offset + '&grouping=' + solrSettings.grouping + '&imgSearch=' + solrSettings.imgSearch + '&urlSearch=' + solrSettings.urlSearch
    history.pushState({name: destination}, destination, '?q=' + query + newFacetUrl + newOptionsUrl)
    },
  }
}
