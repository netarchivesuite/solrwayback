// Global search state.

import { requestService } from '../../services/RequestService'

const initialState = () => ({
  query: '',
  preNormalizedQuery:null,
  searchAppliedFacets:[],
  results: {},
  facets: {},
  solrSettings:{
    grouping:false,
    offset:0,
    imgSearch:false,
    urlSearch:false,
  },
  loading:false,
  facetLoading:false,
  extraFacetLoading:false
})

const state = initialState()

const actions = {
  setLoadingStatus( {commit}, param) {
    commit('setLoadingStatus', param)
  },
  setFacetLoadingStatus( {commit}, param) {
    commit('setFacetLoadingStatus', param)
  },
  setExtraFacetLoadingStatus( {commit}, param) {
    commit('setExtraFacetLoadingStatus', param)
  },
  updateQuery ( {commit}, param) {
    commit('updateQuerySuccess', param)
  },
  updatePreNormalizedQuery ( {commit}, param) {
    commit('updatePreNormalizedQuerySuccess', param)
  },
  updateSolrSettingGrouping ( {commit}, param ) {
    commit('updateSolrSettingGroupingSuccess', param)
  },
  updateSolrSettingOffset ( {commit}, param ) {
    commit('updateSolrSettingOffsetSuccess', param)
  },
  updateSolrSettingImgSearch ( {commit}, param ) {
    commit('updateSolrSettingImgSearchSuccess', param)
  },
  updateSolrSettingUrlSearch ( {commit}, param ) {
    commit('updateSolrSettingUrlSearchSuccess', param)
  },
  addToSearchAppliedFacets ( {commit}, param ) {
    commit('addToSearchAppliedFacetsSuccess', param)
  },
  removeFromSearchAppliedFacets ( {commit}, param ) {
    commit('removeFromSearchAppliedFacetsSuccess', param)
  },
  emptySearchAppliedFacets ({commit}) {
    commit('emptySearchAppliedFacetsSuccess')
  },
  addSpecificRequestedFacets ( {commit}, params ) {
    commit('setExtraFacetLoadingStatus', params.facet)
    requestService
      .getMoreFacets(params.facet, params.query)
      .then(result => commit('loadMorefacetsRequestSuccess', {result:result, selectedFacet:params.facet}), error =>
        commit('loadMorefacetsRequestError', error))
  },
  setFacetToInitialAmount ( {commit}, param) {
    commit('setFacetsToInitialAmountSuccess', param)
  },
  clearResults ( {commit} ) {
    commit('clearResultsSuccess')
  },
  clearFacets ( {commit} ) {
    commit('clearFacetsSuccess')
  },
  requestSearch ({ commit }, params) {
    commit('setLoadingStatus', true)
    requestService
      .fireSearchRequest(params.query, params.facets, params.options)
      .then(result => commit('doSearchSuccess', result), error =>
        commit('doSearchError', error))
  },
  requestImageSearch ({ commit }, params) {
    commit('setLoadingStatus', true)
    requestService
      .fireImageSearchRequest(params.query)
      .then(result => commit('doImageSearchSuccess', result), error =>
        commit('doImageSearchError', error))
  },
  requestUrlSearch ({ commit }, params) {
    commit('setLoadingStatus',true)
    requestService
      .getNormalizedUrlSearch(params.query, params.facets, params.options)
      .then(result => commit('doUrlSearchSuccess', result), error =>
        commit('doUrlSearchError', error))
  },
  requestNormalizedFacets({commit}, params) {
    commit('setLoadingStatus', true)
    requestService
      .getNormalizedUrlFacets(params.query, params.facets, params.options)
      .then(result => commit('facetRequestSuccess', result), error =>
        commit('facetRequestError', error))
  },
  requestFacets({commit}, params) {
    commit('setFacetLoadingStatus', true)
    requestService
      .fireFacetRequest(params.query, params.facets, params.options)
      .then(result => commit('facetRequestSuccess', result), error =>
        commit('facetRequestError', error))
  },
  resetState({ commit }) {
    commit('resetState')
  }
}

const mutations = {
  updateQuerySuccess(state, param) {
    state.query = param
  },
  updatePreNormalizedQuerySuccess(state, param) {
    state.preNormalizedQuery = param
  },
  updateSolrSettingGroupingSuccess(state, param) {
    state.solrSettings.grouping = param
  },
  updateSolrSettingOffsetSuccess(state, param) {
    state.solrSettings.offset = param
  },
  updateSolrSettingImgSearchSuccess(state, param) {
    state.solrSettings.imgSearch = param
  },
  updateSolrSettingUrlSearchSuccess(state, param) {
    state.solrSettings.urlSearch = param
  },
  addToSearchAppliedFacetsSuccess(state, param) {
    state.searchAppliedFacets.push(param)
  },
  removeFromSearchAppliedFacetsSuccess(state, position) {
    state.searchAppliedFacets.splice(position, 1)
  },
  emptySearchAppliedFacetsSuccess(state) {
    state.searchAppliedFacets = []
  },
  loadMorefacetsRequestSuccess(state, param) {
    let newFacets = JSON.parse(JSON.stringify(state.facets))
    newFacets['facet_fields'][param.selectedFacet] = param.result.facet_counts.facet_fields[param.selectedFacet]
    state.facets = newFacets
    state.extraFacetLoading = false
  },
  setFacetsToInitialAmountSuccess(state, facetArea) {
    let newFacets = JSON.parse(JSON.stringify(state.facets))
    newFacets['facet_fields'][facetArea] = newFacets['facet_fields'][facetArea].splice(0,20)
    state.facets = newFacets
    state.extraFacetLoading = false

  },
  loadMorefacetsRequestError() {
    this.dispatch('Notifier/setNotification', {
      title: 'We are so sorry!',
      text: 'Something went wrong when fetching more facets - please try again',
      srvMessage: 'Facets not found.',
      type: 'error',
      timeout: false
    })
    state.extraFacetLoading = false

  },
  facetRequestSuccess(state, result) {
      state.facets = result
      state.facetLoading = false
  },
  facetRequestError(state, message) {
    this.dispatch('Notifier/setNotification', {
      title: 'We are so sorry!',
      text: 'Something went wrong when requesting the facets - please try again',
      srvMessage: message,
      type: 'error',
      timeout: false
    })
    state.loading = false
  },
  doSearchSuccess(state, result) {
    state.results = result.response
    state.loading = false
  },
  doSearchError(state, message) {
    this.dispatch('Notifier/setNotification', {
      title: 'We are so sorry!',
      text: 'Something went wrong when searching - please try again',
      srvMessage: message,
      type: 'error',
      timeout: false
    })
    state.loading = false
  },
  doImageSearchSuccess(state, result) {
    state.results = result.response
    state.loading = false
  },
  doImageSearchError(state, message) {
    this.dispatch('Notifier/setNotification', {
      title: 'We are so sorry!',
      text: 'Something went wrong when searching for images - please try again',
      srvMessage: message,
      type: 'error',
      timeout: false
    })
    state.loading = false
  },
  doUrlSearchSuccess(state, result) {
    state.results = result.response
    state.query = result.responseHeader.params.q
    state.loading = false
  },
  doUrlSearchError(state, message) {
    this.dispatch('Notifier/setNotification', {
      title: 'We are so sorry!',
      text: 'Something went wrong when searching for URLs - please try again',
      srvMessage: message,
      type: 'error',
      timeout: false
    })
    state.loading = false
  },
  setLoadingStatus(state, status) {
    state.loading = status
  },
  setFacetLoadingStatus(state, status) {
    state.facetLoading = status
  },
  setExtraFacetLoadingStatus(state, status) {
    console.log('setting ', status)
    state.extraFacetLoading = status
  },
  clearResultsSuccess(state) {
    state.results = {}
    state.facetLoading = false
  },
  clearFacetsSuccess(state) {
    state.facets = {}
    state.facetLoading = false
  },
  resetState(state) {
    const newState = initialState()
    Object.keys(newState).forEach(key => {
          state[key] = newState[key]
    })
  },

}

export default {
  namespaced: true,
  state,
  actions,
  mutations
}
