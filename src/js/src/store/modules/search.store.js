// Global search state.

import { requestService } from '../../services/RequestService'

const initialState = () => ({
  query: '',
  searchAppliedFacets:'',
  results: {},
  facets: {},
  solrSettings:{
    grouping:false,
    offset:0,
    imgSearch:false,
    urlSearch:false
  },
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
    commit('setLoadingStatus',true)
    requestService
      .fireSearchRequest(params.query, params.facets, params.options)
      .then(result => commit('doSearchSuccess', result), error =>
        commit('doSearchError', error))
  },
  requestFacets({commit}, params) {
    commit('setFacetLoadingStatus')
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
