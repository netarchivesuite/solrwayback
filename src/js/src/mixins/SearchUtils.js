import HistoryRoutingUtils from './HistoryRoutingUtils'
import { mapState, mapActions } from 'vuex'

export default {
  mixins: [HistoryRoutingUtils],
  computed: {
    ...mapState({
      query: state => state.Search.query,
      searchAppliedFacets: state => state.Search.searchAppliedFacets,
      solrSettings: state => state.Search.solrSettings,
      futureSolrSettings: state => state.Search.futureSolrSettings,
    }),
  },
  methods: {
    ...mapActions('Search', {
      updateSearchAppliedFacets:'updateSearchAppliedFacets',
      updateSolrSettingGrouping:'updateSolrSettingGrouping',
      updateSolrSettingImgSearch:'updateSolrSettingImgSearch',
      updateSolrSettingUrlSearch:'updateSolrSettingUrlSearch',
      updateSolrSettingOffset:'updateSolrSettingOffset',
      updateFutureSolrSettingGrouping:'updateFutureSolrSettingGrouping',
      updateFutureSolrSettingImgSearch:'updateFutureSolrSettingImgSearch',
      updateFutureSolrSettingUrlSearch:'updateFutureSolrSettingUrlSearch',
      updatePreNormalizedQuery:'updatePreNormalizedQuery',
      clearResults:'clearResults',
      requestSearch:'requestSearch',
      requestImageSearch:'requestImageSearch',
      requestUrlSearch:'requestUrlSearch',

    }),
    ...mapActions('Notifier', {
      setNotification: 'setNotification'
    }),
    $_validateUrlSearchPrefix(testString) {
      return testString.substring(0,7) === 'http://' || 
             testString.substring(0,8) === 'https://' || 
             testString.substring(0,10) === 'url_norm:"'
    },
    //Deliver a normal search
    deliverSearchRequest(futureQuery) {
      this.requestSearch({query:futureQuery, facets:this.searchAppliedFacets, options:this.solrSettings})
      this.requestFacets({query:futureQuery, facets:this.searchAppliedFacets, options:this.solrSettings})
      this.$_pushSearchHistory('SolrWayback', this.query, this.searchAppliedFacets, this.solrSettings)
    },
    //Deliver an URL search
    deliverUrlSearchRequest(futureQuery) {
      this.updatePreNormalizedQuery(futureQuery)
      let queryString = this.DisectQueryForNewUrlSearch(futureQuery)
      if(this.$_validateUrlSearchPrefix(queryString)) {
        this.requestUrlSearch({query:queryString, facets:this.searchAppliedFacets, options:this.solrSettings})
        this.requestFacets({query:'url_norm:"' + queryString + '"', facets:this.searchAppliedFacets, options:this.solrSettings})
        this.$_pushSearchHistory('SolrWayback', queryString, this.searchAppliedFacets, this.solrSettings)
      }
      else {
        this.setNotification({
          title: 'We are so sorry!',
          text: 'This URL is not valid. the url must start with \'http://\' or \'https://\'',
          type: 'error',
          timeout: false
        })
      }
    },
    // Deliver an image search
    deliverImgSearchRequest(futureQuery) {
      this.requestImageSearch({query:futureQuery})
      this.$_pushSearchHistory('SolrWayback', futureQuery, this.searchAppliedFacets, this.solrSettings)
    },
    // Check if there has been any changes to params
    solrSettingsHaveChanged() {
      return  this.futureSolrSettings.grouping !== this.solrSettings.grouping ||
              this.futureSolrSettings.urlSearch !== this.solrSettings.urlSearch ||
              this.futureSolrSettings.imgSearch !== this.solrSettings.imgSearch
    },
    // Check if there has been any changes to the query
    queryHasChanged(query) {
      return query !== this.query
    },
    // Set the params for a new query
    setSolrQueryParamsForNewQuery() {
      this.updateSolrSettingGrouping(this.futureSolrSettings.grouping)
      this.updateSolrSettingUrlSearch(this.futureSolrSettings.urlSearch)
      this.updateSolrSettingImgSearch(this.futureSolrSettings.imgSearch)  
    },
    // Prepare for a new search
    prepareVariablesForNewSearch(futureQuery) {
      this.updatePreNormalizedQuery(null)
      this.clearResults()
      this.updateQuery(futureQuery)
      this.updateSolrSettingOffset(0)
      this.setSolrQueryParamsForNewQuery()
    },
    // Disect the query for URL searching
    DisectQueryForNewUrlSearch(futureQuery) {
      let queryString = ''
          if(futureQuery.substring(0,10) === 'url_norm:"') {
            queryString = futureQuery.replace('url_norm:"', '')
            queryString.substring(queryString.length-1, queryString.length) === '"' ? queryString = queryString.slice(0,-1) : null
          }
          else {
            queryString = futureQuery
          }
          return queryString
    },
    // Method to fire off a search (and deciding which kind it is)
    $_determineNewSearch(futureQuery) {
      if(this.solrSettingsHaveChanged() || this.queryHasChanged(futureQuery)) {
        this.prepareVariablesForNewSearch(futureQuery)
        if(this.solrSettings.imgSearch) {
          this.deliverImgSearchRequest(futureQuery)
        }
        else if(this.solrSettings.urlSearch) {
          this.deliverUrlSearchRequest(futureQuery)
        }
        else {
          this.deliverSearchRequest(futureQuery)
        }
      }
    }
  }
}