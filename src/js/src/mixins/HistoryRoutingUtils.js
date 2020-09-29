export default {
  methods: {
    $_pushSearchHistory(destination, query, appliedFacets, solrSettings) {
    // Should be deleted before production.
    console.log('We pushed a history.')
    let newFacetUrl = appliedFacets !== '' ? '&facets=' + encodeURIComponent(appliedFacets) : ''
    let newOptionsUrl = '&offset=' + solrSettings.offset + '&grouping=' + solrSettings.grouping + '&imgSearch=' + solrSettings.imgSearch + '&urlSearch=' + solrSettings.urlSearch
    //history.pushState({name: destination}, destination, '?q=' + query + newFacetUrl + newOptionsUrl)
      this.$router.push({name: destination, query: { query: query,
                                                     grouping: solrSettings.grouping,
                                                     imgSearch: solrSettings.imgSearch,
                                                     offset: solrSettings.offset,
                                                     urlSearch: solrSettings.urlSearch,
                                                     facets:encodeURIComponent(appliedFacets)
                                                     } })
    console.log(this.$router, 'we pushed route yey')
    }
  }
  
  /*  console.log('We pushed a history.', this.$router)
    console.log('query', query)
    console.log('appliedFacets', appliedFacets)
    console.log('solrSettings', solrSettings)
    console.log('path', this.$route.path)

    let newFacetUrl = appliedFacets !== '' ? '&facets=' + encodeURIComponent(appliedFacets) : ''
    let newOptionsUrl = '&offset=' + solrSettings.offset + '&grouping=' + solrSettings.grouping + '&imgSearch=' + solrSettings.imgSearch + '&urlSearch=' + solrSettings.urlSearch
    //history.pushState({name: destination}, destination, '?q=' + query + newFacetUrl + newOptionsUrl)
    // this.$router.push({name: destination}, destination, '?q=' + query + newFacetUrl + newOptionsUrl)
    //destination = 'about'
    if (this.$route.path ===  destination) {
      console.log('destination is the same', destination)
      this.$router.replace({ path: this.$route.path, query: { q: query} })
    } else {
      console.log('destination is not the same', destination)
      this.$router.push({name: destination, query: {q: query}})
    
    }  */
}
