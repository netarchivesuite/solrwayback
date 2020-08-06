// Global search state.

import { searchService } from '../services/SearchService'

const state = {
  query: "",
  results: {},
  facets: {},
  error: "",
  loading:false,
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
    console.log(params)
    searchService
      .fireSearch(params)
      .then(result => commit('doSearchSuccess', result), error =>
        commit('doSearchError', error))
  },
}

const mutations = {
  updateQuerySuccess(state, param) {
    console.log("we updating", param)
    state.query = param
  },
  doSearchSuccess(state, result) {
    //console.log("we got results", result.response)
    state.results = result.response
  },
  doSearchError(state, message) {
    state.error = message;
  },
  setLoadingStatus(state, status) {
    state.loading = status
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
