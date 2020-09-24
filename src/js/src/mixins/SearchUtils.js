import HistoryRoutingUtils from './HistoryRoutingUtils'
import { mapState, mapActions } from 'vuex'

export default {
  mixins: [HistoryRoutingUtils],
  computed: {
    ...mapState({
      query: state => state.Search.query,
      searchAppliedFacets: state => state.Search.searchAppliedFacets,
      solrSettings: state => state.Search.solrSettings,
      futoreSolrSettings: state => state.Search.futureSolrSettings,
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
      clearResults:'clearResults',
      requestSearch:'requestSearch',
      requestImageSearch:'requestImageSearch',
      requestUrlSearch:'requestUrlSearch',

    }),
    handleSubmit(futureQuery) {
      if (futureQuery !== this.query ||
          this.futureGrouped !== this.solrSettings.grouping ||
          this.futureUrlSearch !== this.solrSettings.urlSearch ||
          this.futureImgSearch !== this.solrSettings.imgSearch) 
        {
        console.log('search params changed!')
        this.preNormalizeQuery = null
        this.clearResults()
        this.updateQuery(futureQuery)
        this.updateSolrSettingOffset(0)
        this.updateSolrSettingGrouping(this.futureGrouped)
        this.updateSolrSettingUrlSearch(this.futureUrlSearch)
        this.updateSolrSettingImgSearch(this.futureImgSearch)
        if(this.solrSettings.imgSearch) {
           this.requestImageSearch({query:this.query})
           this.$_pushSearchHistory('SolrWayback', this.query, this.searchAppliedFacets, this.solrSettings)
        }
        else if(this.solrSettings.urlSearch) {
          this.preNormalizeQuery = futureQuery
          let queryString = ''
          if(futureQuery.substring(0,10) === 'url_norm:"') {
            queryString = futureQuery.replace('url_norm:"', '')
            queryString.substring(queryString.length-1, queryString.length) === '"' ? queryString = queryString.slice(0,-1) : null
          }
          else {
            queryString = futureQuery
          }
          if(this.validateUrl(queryString)) {
            this.requestUrlSearch({query:queryString, facets:this.searchAppliedFacets, options:this.solrSettings})
            this.requestFacets({query:'url_norm:"' + queryString + '"', facets:this.searchAppliedFacets, options:this.solrSettings})
            this.$_pushSearchHistory('SolrWayback', queryString, this.searchAppliedFacets, this.solrSettings)
          }
          else {
            this.setNotification({
          	title: 'We are so sorry!',
            text: 'This query is not valid. the url must start with \'http://\' or \'https://\'',
            type: 'error',
            timeout: false
          })
          }
        }
        else {
          this.requestSearch({query:futureQuery, facets:this.searchAppliedFacets, options:this.solrSettings})
          this.requestFacets({query:futureQuery, facets:this.searchAppliedFacets, options:this.solrSettings})
          this.$_pushSearchHistory('SolrWayback', this.query, this.searchAppliedFacets, this.solrSettings)
        }
      }
    },
  }
}