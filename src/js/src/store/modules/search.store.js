// Global search state.

import { requestService } from '../../services/RequestService'

const initialState = () => ({
  query: '',
  preNormalizedQuery:null,
  searchAppliedFacets:'',
  results: {},
  facets: {},
  solrSettings:{
    grouping:false,
    offset:0,
    imgSearch:false,
    urlSearch:false
  },
  loading:false,
})

const state = initialState()

const actions = {
  setLoadingStatus( {commit}, param) {
    commit('setLoadingStatus', param)
  },
  updateQuery ( {commit}, param) {
    commit('updateQuerySuccess', param)
  },
  updatePreNormalizedQuery ( {commit}, param) {
    commit('updateQuerySuccess', param)
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
  updateSearchAppliedFacets ( {commit}, param ) {
    commit('updateSearchAppliedFacetsSuccess', param)
  },
  clearResults ( {commit} ) {
    commit('clearResultsSuccess')
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
    commit('setLoadingStatus', true)
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
    state.query = param
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
  updateSearchAppliedFacetsSuccess(state, param) {
    state.searchAppliedFacets = param
  },
  facetRequestSuccess(state, result) {
    state.facets = result
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
  },
  setLoadingStatus(state, status) {
    state.loading = status
  },
  clearResultsSuccess(state) {
    state.results = {}
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
