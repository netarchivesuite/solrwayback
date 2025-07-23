// Global search state.
import { defineStore } from 'pinia';

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
      solrSettings.urlSearch = param
    },
    updateSolrSettingSort ( param ) {
      this.solrSettings.sort = param
    },
    addToSearchAppliedFacets ( param ) {
      searchAppliedFacets.push(param)
    },
    removeFromSearchAppliedFacets ( position ) {
      this.searchAppliedFacets.splice(position, 1)
    },
    emptySearchAppliedFacets () {
      searchAppliedFacets = []
    },
    addSpecificRequestedFacets ( params ) {
      this.setExtraFacetLoadingStatus(params.facet)
      //commit('setExtraFacetLoadingStatus', params.facet)
      requestService
        .getMoreFacets(params.facet, params.query, params.appliedFacets)
        .then(
          result => {
            let newFacets = JSON.parse(JSON.stringify(this.facets))
            newFacets['facet_fields'][params.facet] = result.facet_counts.facet_fields[params.facet]
            this.facets = newFacets
            this.extraFacetLoading = false
          },
          error => {
            this.dispatch('Notifier/setNotification', {
              title: 'We are so sorry!',
              text: 'Something went wrong when fetching more facets - please try again',
              srvMessage: 'Facets not found.',
              type: 'error',
              timeout: false
            })
            this.extraFacetLoading = false
          })
          // commit('loadMorefacetsRequestSuccess', {result:result, selectedFacet:params.facet}), error =>
          // commit('loadMorefacetsRequestError', error))
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
      state.facets = {}
      state.facetLoading = false
    },
    requestSearch ( params ) {
      this.setLoadingStatus(true)
      // commit('setLoadingStatus', true)
      requestService
        .fireSearchRequest(params.query, params.facets, params.options)
        .then(
          result => {
            this.results = result.response
            this.loading = false
          },
          error => {
            this.dispatch('Notifier/setNotification', {
              title: 'We are so sorry!',
              text: 'Something went wrong when searching - please try again',
              srvMessage: error,
              type: 'error',
              timeout: false
            })
            this.loading = false
          }
        )
        // commit('doSearchSuccess', result), error =>
        // commit('doSearchError', error))
    },
    requestImageSearch ( params ) {
      // commit('setLoadingStatus', true)
      this.setLoadingStatus(true)
      requestService
        .fireImageSearchRequest(params.query)
        .then(
          result => {
            this.results = result.response
            this.loading = false
          },
          error =>{
            this.dispatch('Notifier/setNotification', {
              title: 'We are so sorry!',
              text: 'Something went wrong when searching for images - please try again',
              srvMessage: error,
              type: 'error',
              timeout: false
            })
            this.loading = false
          })     
          // commit('doImageSearchSuccess', result), error =>
          // commit('doImageSearchError', error))
    },
    requestUrlSearch ( params ) {
      // commit('setLoadingStatus',true)
      this.setLoadingStatus(true)
      requestService
        .getNormalizedUrlSearch(params.query, params.facets, params.options)
        .then(
          result => {
            this.results = result.response
            this.query =  params.preNormalizedQuery //data.result.responseHeader.params.q
            this.loading = false
          },
          error => {
            this.dispatch('Notifier/setNotification', {
              title: 'We are so sorry!',
              text: 'Something went wrong when searching for URLs - please try again',
              srvMessage: error,
              type: 'error',
              timeout: false
            })
            this.loading = false
          })
          // commit('doUrlSearchSuccess', {result, params}), error =>
          // commit('doUrlSearchError', error))
    },
    requestNormalizedFacets( params ) {
      // commit('setLoadingStatus', true)
      this.setLoadingStatus(true)
      requestService
        .getNormalizedUrlFacets(params.query, params.facets, params.options)
        .then(
          result => {
            this.facets = result
            this.facetLoading = false
          },
          error => {
            this.dispatch('Notifier/setNotification', {
              title: 'We are so sorry!',
              text: 'Something went wrong when requesting the facets - please try again',
              srvMessage: error,
              type: 'error',
              timeout: false
            })
            this.loading = false
          })
          // commit('facetRequestSuccess', result), error =>
          // commit('facetRequestError', error))
    },
    requestFacets( params ) {
      // commit('setFacetLoadingStatus', true)
      this.setLoadingStatus(true)
      requestService
        .fireFacetRequest(params.query, params.facets, params.options)
        .then(
          result => {
            this.facets = result
            this.facetLoading = false
          },
          error => {
            this.dispatch('Notifier/setNotification', {
              title: 'We are so sorry!',
              text: 'Something went wrong when requesting the facets - please try again',
              srvMessage: error,
              type: 'error',
              timeout: false
            })
            this.loading = false
          })
          // commit('facetRequestSuccess', result), error =>
          // commit('facetRequestError', error))
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



// const mutations = {
  // updateQuerySuccess(state, param) {
  //   state.query = param
  // },
  // updatePreNormalizedQuerySuccess(state, param) {
  //   state.preNormalizedQuery = param
  // },
  // updateNormalizedQuerySuccess(state, param) {
  //   state.normalizedQuery = param
  // },

  // updateSolrSettingGroupingSuccess(state, param) {
  //   state.solrSettings.grouping = param
  // },
  // updateSolrSettingOffsetSuccess(state, param) {
  //   state.solrSettings.offset = param
  // },
  // updateSolrSettingImgSearchSuccess(state, param) {
  //   state.solrSettings.imgSearch = param
  // },
  // updateSolrSettingUrlSearchSuccess(state, param) {
  //   state.solrSettings.urlSearch = param
  // },
  // updateSolrSettingSortSuccess(state, param) {
  //   state.solrSettings.sort = param
  // },
  // addToSearchAppliedFacetsSuccess(state, param) {
  //   state.searchAppliedFacets.push(param)
  // },
  // removeFromSearchAppliedFacetsSuccess(state, position) {
  //   state.searchAppliedFacets.splice(position, 1)
  // },
  // emptySearchAppliedFacetsSuccess(state) {
  //   state.searchAppliedFacets = []
  // },
  // loadMorefacetsRequestSuccess(state, param) {
  //   let newFacets = JSON.parse(JSON.stringify(state.facets))
  //   newFacets['facet_fields'][param.selectedFacet] = param.result.facet_counts.facet_fields[param.selectedFacet]
  //   state.facets = newFacets
  //   state.extraFacetLoading = false
  // },
  // setFacetsToInitialAmountSuccess(state, facetArea) {
  //   let newFacets = JSON.parse(JSON.stringify(state.facets))
  //   newFacets['facet_fields'][facetArea] = newFacets['facet_fields'][facetArea].splice(0,20)
  //   state.facets = newFacets
  //   state.extraFacetLoading = false

  // },
  // loadMorefacetsRequestError() {
  //   this.dispatch('Notifier/setNotification', {
  //     title: 'We are so sorry!',
  //     text: 'Something went wrong when fetching more facets - please try again',
  //     srvMessage: 'Facets not found.',
  //     type: 'error',
  //     timeout: false
  //   })
  //   state.extraFacetLoading = false

  // },
  // facetRequestSuccess(state, result) {
  //     state.facets = result
  //     state.facetLoading = false
  // },
  // facetRequestError(state, message) {
  //   this.dispatch('Notifier/setNotification', {
  //     title: 'We are so sorry!',
  //     text: 'Something went wrong when requesting the facets - please try again',
  //     srvMessage: message,
  //     type: 'error',
  //     timeout: false
  //   })
  //   state.loading = false
  // },
  // doSearchSuccess(state, result) {
  //   state.results = result.response
  //   state.loading = false
  // },
  // doSearchError(state, message) {
  //   this.dispatch('Notifier/setNotification', {
  //     title: 'We are so sorry!',
  //     text: 'Something went wrong when searching - please try again',
  //     srvMessage: message,
  //     type: 'error',
  //     timeout: false
  //   })
  //   state.loading = false
  // },
  // doImageSearchSuccess(state, result) {
  //   state.results = result.response
  //   state.loading = false
  // },
  // doImageSearchError(state, message) {
  //   this.dispatch('Notifier/setNotification', {
  //     title: 'We are so sorry!',
  //     text: 'Something went wrong when searching for images - please try again',
  //     srvMessage: message,
  //     type: 'error',
  //     timeout: false
  //   })
  //   state.loading = false
  // },
  // doUrlSearchSuccess(state, data) {
  //   state.results = data.result.response
  //   state.query =  data.params.preNormalizedQuery //data.result.responseHeader.params.q
  //   state.loading = false
  // },
  // doUrlSearchError(state, message) {
  //   this.dispatch('Notifier/setNotification', {
  //     title: 'We are so sorry!',
  //     text: 'Something went wrong when searching for URLs - please try again',
  //     srvMessage: message,
  //     type: 'error',
  //     timeout: false
  //   })
  //   state.loading = false
  // },
  // setLoadingStatus(state, status) {
  //   state.loading = status
  // },
  // setFacetLoadingStatus(state, status) {
  //   state.facetLoading = status
  // },
  // setExtraFacetLoadingStatus(state, status) {
  //   state.extraFacetLoading = status
  // },
  // clearResultsSuccess(state) {
  //   state.results = {}
  //   state.facetLoading = false
  // },
  // clearFacetsSuccess(state) {
  //   state.facets = {}
  //   state.facetLoading = false
  // },
  // resetState(state) {
  //   const newState = initialState()
  //   Object.keys(newState).forEach(key => {
  //         state[key] = newState[key]
  //   })
  // },

// }

// export default {
//   namespaced: true,
//   state,
//   actions,
//   mutations
// }
