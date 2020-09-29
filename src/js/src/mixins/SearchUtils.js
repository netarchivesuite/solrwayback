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
    validateUrl(testString) {
      return testString.substring(0,7) === 'http://' || 
             testString.substring(0,8) === 'https://' || 
             testString.substring(0,10) === 'url_norm:"'
    },
    deliverSearchRequest(futureQuery) {
      this.requestSearch({query:futureQuery, facets:this.searchAppliedFacets, options:this.solrSettings})
      this.requestFacets({query:futureQuery, facets:this.searchAppliedFacets, options:this.solrSettings})
      this.$_pushSearchHistory('SolrWayback', this.query, this.searchAppliedFacets, this.solrSettings)
    },
    deliverUrlSearchRequest(futureQuery) {
      this.updatePreNormalizedQuery(futureQuery)
      let queryString = this.DisectQueryForNewUrlSearch(futureQuery)
      if(this.validateUrl(queryString)) {
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
    deliverImgSearchRequest() {
      this.requestImageSearch({query:this.query})
      this.$_pushSearchHistory('SolrWayback', this.query, this.searchAppliedFacets, this.solrSettings)
    },
    checkForChangesInSolrSettings() {
      return  this.futureGrouped !== this.solrSettings.grouping ||
              this.futureUrlSearch !== this.solrSettings.urlSearch ||
              this.futureImgSearch !== this.solrSettings.imgSearch
    },
    checkForChangesInQuery(query) {
      return query !== this.query
    },
    setSolrQueryParamsForNewQuery() {
      this.updateSolrSettingGrouping(this.futureSolrSettings.grouping)
      this.updateSolrSettingUrlSearch(this.futureSolrSettings.urlSearch)
      this.updateSolrSettingImgSearch(this.futureSolrSettings.imgSearch)  
    },
    prepareVariablesForNewSearch(futureQuery) {
      this.updatePreNormalizedQuery(null)
      this.clearResults()
      this.updateQuery(futureQuery)
      this.updateSolrSettingOffset(0)
      this.setSolrQueryParamsForNewQuery()
    },
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
    determineNewSearch(futureQuery) {
      this.prepareVariablesForNewSearch(futureQuery)
      if(this.solrSettings.imgSearch) {
         this.deliverImgSearchRequest()
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