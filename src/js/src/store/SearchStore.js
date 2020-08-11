// Global search state.

import { searchService } from '../services/SearchService'

const state = {
  query: "",
  results: {},
  facets: {},
  error: "",
  loading:false,
  facetLoading:false,
}

const actions = {
  setLoadingStatus( {commit}, param) {
    commit('setLoadingStatus', param)
  },
  updateQuery ( {commit}, param ) {
    commit('updateQuerySuccess', param)
  },
  clearResults ( {commit} ) {
    commit('clearResultsSuccess')
  },
  search ({ commit }, params) {
    commit('setLoadingStatus',true)
    searchService
      .fireSearch(params)
      .then(result => commit('doSearchSuccess', result), error =>
        commit('doSearchError', error))
  },
  facets( {commit}, params) {
    commit('setFacetLoadingStatus')
    searchService
      .fireFacetRequest(params)
      .then(result => commit('facetRequestSuccess', result), error =>
        commit('facetRequestError', error))
  },
}

const mutations = {
  updateQuerySuccess(state, param) {
    state.query = param
  },
  facetRequestSuccess(state, result) {
    state.facets = result.response
  },
  facetRequestError(state, message) {
    state.error = message;
  },
  doSearchSuccess(state, result) {
    state.results = result.response
  },
  doSearchError(state, message) {
    state.error = message;
  },
  setLoadingStatus(state, status) {
    state.loading = status
  },
  setFacetLoadingStatus(state, status) {
    state.facetLoading = status
  },
  clearResultsSuccess(state) {
    state.results = {}
  }

}

export const searchStore = {
  namespaced: true,
  state,
  actions,
  mutations
}
