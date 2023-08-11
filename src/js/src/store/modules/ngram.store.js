import { requestService } from '../../services/RequestService'


// Global ngram state.
const initialState = () => ({
  query: '',
  loading:false,
  countPercent: [],
  countsTotal: [],
  datasetQueries:[],
  datasets:[],
  emptyResult: false,
  searchType:'text',
  labels:[],
  timeScale:''

})

const state = initialState()

const actions = {
  setLoadingStatus( {commit}, param) {
    commit('setLoadingStatus', param)
  },
  setSearchType( {commit}, param) {
    commit('setSearchType', param)
  },
  updateQuery ( {commit}, param) {
    commit('updateQuerySuccess', param)
  },
  setTimeScale( {commit}, param) {
    commit('setTimeScale', param)
  },
  doSearch ({ commit }, params) {
    this.dispatch('Ngram/setLoadingStatus', true)
   
    requestService.getNgramNetarchive(params)
   .then(results => {this.dispatch('Ngram/updateQuery', params.query), this.dispatch('Ngram/setTimeScale', params.timeScale), commit('doSearchSuccess', results)}, error =>
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
      state.countPercent = results.countPercent
      state.countsTotal = results.countsTotal
      state.emptyResult = results.emptyResult
      state.datasetQueries.push(state.query)
      if (state.datasets.length == 0) {
        state.labels = state.countsTotal.map(countTotal => countTotal.date)
      }
      state.datasets.push({
        query: state.query,
        count: state.countsTotal.map(countTotal => countTotal.count),
        total: state.countsTotal.map(countTotal => countTotal.total),
        percent: state.countPercent
      })
    this.dispatch('Ngram/setLoadingStatus', false) 
  },

  doSearchError(state, message) {
    if (message.response.status === 400 && message.response.data.startsWith('Tag syntax not accepted')) {
      this.dispatch('Notifier/setNotification', {
        title: 'We are so sorry!',
        text: 'Please remove all < and > from your query and try again',
        srvMessage: message.response.data,
        type: 'error',
        timeout: false
      })
    } else {
    this.dispatch('Notifier/setNotification', {
        title: 'We are so sorry!',
        text: 'Something went wrong with your search - please try again',
        srvMessage: message.response.data,
        type: 'error',
        timeout: false
      })
    }
    this.dispatch('Search/setLoadingStatus', false)
  },

  setLoadingStatus(state, status) {
    state.loading = status
  },
  
  setSearchType(state, type) {
    state.searchType = type
  },
  setTimeScale(state, scale) {
    state.timeScale = scale
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

