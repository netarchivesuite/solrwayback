// Global search state.
import { defineStore } from 'pinia'
import { useNotifierStore } from '../store/notifier.store'
import { requestService } from '../services/RequestService'

export const useSearchStore = defineStore('search', {

  state: () => ({
    query: '',
    preNormalizedQuery:null,
    normalizedQuery:null,
    searchAppliedFacets:[],
    results: {},
    facets: {},
    solrSettings:{
      grouping:false,
      offset:0,
      imgSearch:false,
      urlSearch:false,
      sort:'score desc'
    },
    loading:false,
    facetLoading:false,
    extraFacetLoading:false
  }),

  actions: {
    setLoadingStatus( status ) {
      this.loading = status
    },
    setFacetLoadingStatus( status ) {
      this.facetLoading = status
    },
    setExtraFacetLoadingStatus( status ) {
      this.extraFacetLoading = status
    },
    updateQuery ( param ) {
      this.query = param
    },
    updatePreNormalizedQuery ( param )  {
      this.preNormalizedQuery = param
    },
    updateNormalizedQuery ( param ) {
      this.normalizedQuery = param
    },
    updateSolrSettingGrouping ( param ) {
      this.solrSettings.grouping = param
    },
    updateSolrSettingOffset ( param ) {
      this.solrSettings.offset = param
    },
    updateSolrSettingImgSearch ( param ) {
      this.solrSettings.imgSearch = param
    },
    updateSolrSettingUrlSearch ( param ) {
      this.solrSettings.urlSearch = param
    },
    updateSolrSettingSort ( param ) {
      this.solrSettings.sort = param
    },
    addToSearchAppliedFacets ( param ) {
      this.searchAppliedFacets.push(param)
    },
    removeFromSearchAppliedFacets ( position ) {
      this.searchAppliedFacets.splice(position, 1)
    },
    emptySearchAppliedFacets () {
      this.searchAppliedFacets = []
    },
    async addSpecificRequestedFacets ( params ) {
      this.setExtraFacetLoadingStatus(params.facet)

      try {
        const result = await requestService.getMoreFacets(params.facet, params.query, params.appliedFacets)

        let newFacets = JSON.parse(JSON.stringify(this.facets))
        newFacets['facet_fields'][params.facet] = result.facet_counts.facet_fields[params.facet]
        this.facets = newFacets
        // this.extraFacetLoading = false
        this.setExtraFacetLoadingStatus(false)
      } catch (error){
        const notifier = useNotifierStore()
        notifier.setNotification({
          title: 'We are so sorry!',
          text: 'Something went wrong when fetching more facets - please try again',
          srvMessage: error,
          type: 'error',
          timeout: false
        })

        // this.extraFacetLoading = false
        this.setExtraFacetLoadingStatus(false)
      }
    },
    setFacetToInitialAmount ( facetArea ) {
      let newFacets = JSON.parse(JSON.stringify(this.facets))
      newFacets['facet_fields'][facetArea] = newFacets['facet_fields'][facetArea].splice(0,20)
      this.facets = newFacets
      this.extraFacetLoading = false
    },
    clearResults () {
      this.results = {}
      this.facetLoading = false
    },
    clearFacets () {
      this.facets = {}
      this.facetLoading = false
    },
    async requestSearch ( params ) {
      this.setLoadingStatus(true)

      try {
        const result = await requestService.fireSearchRequest(params.query, params.facets, params.options)

        this.results = result.response
        this.setLoadingStatus(false)
      } catch (error){
        const notifier = useNotifierStore()
        notifier.setNotification({
          title: 'We are so sorry!',
          text: 'Something went wrong when searching - please try again',
          srvMessage: error,
          type: 'error',
          timeout: false
        })

        this.setLoadingStatus(false)
      }
    },
    async requestImageSearch ( params ) {
      this.setLoadingStatus(true)

      try {
        const result = await requestService.fireImageSearchRequest(params.query)

        this.results = result.response
        this.setLoadingStatus(false)
      } catch (error){
        const notifier = useNotifierStore()
        notifier.setNotification({
          title: 'We are so sorry!',
          text: 'Something went wrong when searching - please try again',
          srvMessage: error,
          type: 'error',
          timeout: false
        })

        this.setLoadingStatus(false)
      }
    },
    async requestUrlSearch ( params ) {
      this.setLoadingStatus(true)

      try {
        const result = await requestService.getNormalizedUrlSearch(params.query, params.facets, params.options)

        this.results = result.response
        this.query =  params.preNormalizedQuery //data.result.responseHeader.params.q
        this.setLoadingStatus(false)
      } catch (error){
        const notifier = useNotifierStore()
        notifier.setNotification({
          title: 'We are so sorry!',
          text: 'Something went wrong when searching - please try again',
          srvMessage: error,
          type: 'error',
          timeout: false
        })

        this.setLoadingStatus(false)
      }
    },
    async requestNormalizedFacets( params ) {
      // this.setLoadingStatus(true)
      this.setFacetLoadingStatus(true)

      try {
        const result = await requestService.getNormalizedUrlFacets(params.query, params.facets, params.options)

        this.facets = result
        this.setFacetLoadingStatus(false)
        // this.facetLoading = false
      } catch (error){
        const notifier = useNotifierStore()
        notifier.setNotification({
          title: 'We are so sorry!',
          text: 'Something went wrong when searching - please try again',
          srvMessage: error,
          type: 'error',
          timeout: false
        })

        // this.setLoadingStatus(false)
        this.setFacetLoadingStatus(false)
      }
    },
    async requestFacets( params ) {
      // this.setLoadingStatus(true)
      this.setFacetLoadingStatus(true)

      try {
        const result = await requestService.fireFacetRequest(params.query, params.facets, params.options)

        this.facets = result
        // this.facetLoading = false
        this.setFacetLoadingStatus(false)
      } catch (error){
        const notifier = useNotifierStore()
        notifier.setNotification({
          title: 'We are so sorry!',
          text: 'Something went wrong when searching - please try again',
          srvMessage: error,
          type: 'error',
          timeout: false
        })

        this.setFacetLoadingStatus(false)
        // this.setLoadingStatus(false)
      }
    },
    resetState() {
      // commit('resetState')
      // const newState = initialState()
      // Object.keys(newState).forEach(key => {
      //       state[key] = newState[key]
      // })
    }
  }


})
