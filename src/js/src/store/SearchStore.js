// Global search state.

import { searchService } from '../services/SearchService'

const state = {
  query: "",
  filters:"",
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
  updateFilters ( {commit}, param ) {
    commit('updateFiltersSuccess', param)
  },
  clearResults ( {commit} ) {
    commit('clearResultsSuccess')
  },
  search ({ commit }, params) {
    commit('setLoadingStatus',true)
    searchService
      .fireSearch(params.query, params.filters)
      .then(result => commit('doSearchSuccess', result), error =>
        commit('doSearchError', error))
  },
  requestFacets({commit}, params) {
    commit('setFacetLoadingStatus')
    searchService
      .fireFacetRequest(params.query, params.filters)
      .then(result => commit('facetRequestSuccess', result), error =>
        commit('facetRequestError', error))
  },
}

const mutations = {
  updateQuerySuccess(state, param) {
    state.query = param
  },
  updateFiltersSuccess(state, param) {
    state.filters = param
  },
  facetRequestSuccess(state, result) {
    state.facets = result
  },
  facetRequestError(state, message) {
    state.error = message
  },
  doSearchSuccess(state, result) {
    state.results = result.response
    state.loading = false
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
    state.facetLoading = false
  }

}

export const searchStore = {
  namespaced: true,
  state,
  actions,
  mutations
}
