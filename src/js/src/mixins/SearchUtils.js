import HistoryRoutingUtils from './HistoryRoutingUtils'
import { mapState, mapActions } from 'vuex'

export default {
  mixins: [HistoryRoutingUtils],
  computed: {
    ...mapState({
      query: state => state.Search.query,
      searchAppliedFacets: state => state.Search.searchAppliedFacets,
      solrSettings: state => state.Search.solrSettings,
    }),
  },
  methods: {
    ...mapActions('Search', {
      updateQuery:'updateQuery',
      updateSolrSettingGrouping:'updateSolrSettingGrouping',
      updateSolrSettingImgSearch:'updateSolrSettingImgSearch',
      updateSolrSettingUrlSearch:'updateSolrSettingUrlSearch',
      updateSolrSettingOffset:'updateSolrSettingOffset',
      updatePreNormalizedQuery:'updatePreNormalizedQuery',
      clearResults:'clearResults',
      clearFacets:'clearFacets',
      requestSearch:'requestSearch',
      requestImageSearch:'requestImageSearch',
      requestUrlSearch:'requestUrlSearch',
      requestNormalizedFacets:'requestNormalizedFacets',
      requestFacets:'requestFacets'

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
    deliverSearchRequest(futureQuery, updateHistory, pagnation) {
      this.requestSearch({query:futureQuery, facets:this.searchAppliedFacets, options:this.solrSettings})
      !pagnation ? this.requestFacets({query:futureQuery, facets:this.searchAppliedFacets, options:this.solrSettings}) : null
      updateHistory ? this.$_pushSearchHistory('Search', futureQuery, this.searchAppliedFacets, this.solrSettings) : null
    },
    //Deliver an URL search
    deliverUrlSearchRequest(futureQuery, updateHistory) {
      this.updatePreNormalizedQuery(futureQuery)
      if(this.$_validateUrlSearchPrefix(this.DisectQueryForNewUrlSearch(futureQuery))) {
        this.requestUrlSearch({query:this.DisectQueryForNewUrlSearch(futureQuery), facets:this.searchAppliedFacets, options:this.solrSettings})
        this.requestNormalizedFacets({query:this.DisectQueryForNewUrlSearch(futureQuery), facets:this.searchAppliedFacets, options:this.solrSettings})
        updateHistory ? this.$_pushSearchHistory('Search', this.DisectQueryForNewUrlSearch(futureQuery), this.searchAppliedFacets, this.solrSettings) : null
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
    deliverImgSearchRequest(futureQuery, updateHistory) {
      this.requestImageSearch({query:futureQuery})
      updateHistory ? this.$_pushSearchHistory('Search', futureQuery, this.searchAppliedFacets, this.solrSettings) : null
    },
    // Check if there has been any changes to the query
    queryHasChanged(query) {
      return query !== this.query
    },
    // Prepare for a new search
    prepareStateForNewSearch(futureQuery, pagnation) {
      this.updatePreNormalizedQuery(null)
      this.clearResults()
      !pagnation ? this.clearFacets() : null
      this.updateQuery(futureQuery)
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
    $_determineNewSearch(futureQuery, updateHistory, pagnation) {
      //console.log('we\'ve accessed the searchfunction with ',futureQuery)
      //console.log('we have these solrsettings: ', this.solrSettings)
      //console.log('and these facets', this.searchAppliedFacets)
      this.prepareStateForNewSearch(futureQuery, pagnation)
      if(this.solrSettings.imgSearch) {
        this.deliverImgSearchRequest(futureQuery ,updateHistory)
      }
      else if(this.solrSettings.urlSearch) {
        this.deliverUrlSearchRequest(futureQuery , updateHistory)
      }
      else {
        this.deliverSearchRequest(futureQuery, updateHistory, pagnation)
      }
    }
  }
}