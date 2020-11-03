import { requestService } from '../../services/RequestService'


// Global ngram state.
const initialState = () => ({
  query: '',
  loading:false,
  yearCountPercent: [],
  yearCountsTotal: [],
  datasetQueries:[],
  datasets:[],
  emptyResult: false,

})

const state = initialState()

const actions = {
  setLoadingStatus( {commit}, param) {
    commit('setLoadingStatus', param)
  },
  updateQuery ( {commit}, param) {
    commit('updateQuerySuccess', param)
  },
  doSearch ({ commit }, params) {
    this.dispatch('Search/setLoadingStatus', true)
   
    requestService.getNgramNetarchive(params)
   .then(results => {this.dispatch('Ngram/updateQuery', params), commit('doSearchSuccess', results)}, error =>
   commit('doSearchError', error))
  },
  resetState({ commit }) {
    commit('resetState')
  },
  removeDataset({ commit }) {
    commit('removeDataset')
  },
  addDataset({ commit }) {
    commit('addDataset')
  },

  

}

const mutations = {
  updateQuerySuccess(state, param) {
    state.query = param
  },
  addDataset(state, param) {
    state.query = param
  },
  removeDataset(state) {
    state.datasetQueries.pop()
      state.datasets.pop()
  },
  doSearchSuccess(state, results) {
      state.yearCountPercent = results.yearCountPercent
      state.yearCountsTotal = results.yearCountsTotal
      state.emptyResult = results.emptyResult
      state.datasetQueries.push(state.query)
      state.datasets.push({
        query: state.query,
        count: state.yearCountsTotal.map(yearCountTotal => yearCountTotal.count),
        total: state.yearCountsTotal.map(yearCountTotal => yearCountTotal.total),
        percent: state.yearCountPercent
      })
    this.dispatch('Search/setLoadingStatus', false)
   
  },

  doSearchError(state, message) {
    this.dispatch('Notifier/setNotification', {
        title: 'We are so sorry!',
        text: 'Something went wrong with your search - please try again',
        srvMessage: message,
        type: 'error',
        timeout: false
      })
      this.dispatch('Search/setLoadingStatus', false)
  
  },

  setLoadingStatus(state, status) {
    state.loading = status
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

