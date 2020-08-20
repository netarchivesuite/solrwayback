// Global search state.

import { searchService } from '../../services/SearchService'

const initialState = () => ({
  query: '',
  searchAppliedFacets:'',
  results: {},
  facets: {},
  error: '',
  loading:false,
  facetLoading:false,
})

const state = initialState()

const actions = {
  setLoadingStatus( {commit}, param) {
    commit('setLoadingStatus', param)
  },
  updateQuery ( {commit}, param ) {
    commit('updateQuerySuccess', param)
  },
  updateSearchAppliedFacets ( {commit}, param ) {
    commit('updateSearchAppliedFacetsSuccess', param)
  },
  clearResults ( {commit} ) {
    commit('clearResultsSuccess')
  },
  search ({ commit }, params) {
    commit('setLoadingStatus',true)
    searchService
      .fireSearchRequest(params.query, params.facets)
      .then(result => commit('doSearchSuccess', result), error =>
        commit('doSearchError', error))
  },
  requestFacets({commit}, params) {
    commit('setFacetLoadingStatus')
    searchService
      .fireFacetRequest(params.query, params.facets)
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
  updateSearchAppliedFacetsSuccess(state, param) {
    state.searchAppliedFacets = param
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
    state.error = message
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
