// Global ngram state.
import { defineStore } from 'pinia';

import { requestService } from '../services/RequestService'

export const useNgramStore = defineStore('ngram', {

  state: () => ({
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

}),

// const state = initialState()

  actions: {
    setLoadingStatus( param ) {
      // commit('setLoadingStatus', param)
      this.loading = param
    },
    setSearchType( param ) {
      // commit('setSearchType', param)
      this.searchType = param
    },
    updateQuery ( param ) {
      // commit('updateQuerySuccess', param)
      this.query = param
    },
    setTimeScale( param ) {
      // commit('setTimeScale', param)
      this.timeScale = param
    },
    doSearch ( params ) {
      // this.dispatch('Ngram/setLoadingStatus', true)
      this.setLoadingStatus(true)
    
      requestService.getNgramNetarchive(params)
        .then(
          results => {
            this.dispatch('Ngram/updateQuery', params.query)
            this.dispatch('Ngram/setTimeScale', params.timeScale)
            // commit('doSearchSuccess', results)
            this.countPercent = results.countPercent
            this.countsTotal = results.countsTotal
            this.emptyResult = results.emptyResult
            this.datasetQueries.push(this.query)
            if (this.datasets.length == 0) {
              this.labels = this.countsTotal.map(countTotal => countTotal.date)
            }
            this.datasets.push({
              query: this.query,
              count: this.countsTotal.map(countTotal => countTotal.count),
              total: this.countsTotal.map(countTotal => countTotal.total),
              percent: this.countPercent
            })
            this.dispatch('Ngram/setLoadingStatus', false) 
          },
          error => {
            if (error.response.status === 400 && error.response.data.startsWith('Tag syntax not accepted')) {
              this.dispatch('Notifier/setNotification', {
                title: 'We are so sorry!',
                text: 'Please remove all < and > from your query and try again',
                srvMessage: error.response.data,
                type: 'error',
                timeout: false
              })
            } else {
            this.dispatch('Notifier/setNotification', {
                title: 'We are so sorry!',
                text: 'Something went wrong with your search - please try again',
                srvMessage: error.response.data,
                type: 'error',
                timeout: false
              })
            }
            this.dispatch('Search/setLoadingStatus', false)
          }
        )
    //  commit('doSearchError', error)
    //   )
    },
    resetState(){
        this.$reset()
    },
    removeDataset() {
      // commit('removeDataset')
      this.datasetQueries.pop()
      this.datasets.pop()
    },
    addDataset() {
      // commit('addDataset')
      this.query = param
    }

  }

})

// const mutations = {
  // updateQuerySuccess(state, param) {
  //   state.query = param
  // },
  // addDataset(state, param) {
  //   state.query = param
  // },
  // removeDataset(state) {
  //   state.datasetQueries.pop()
  //     state.datasets.pop()
  // },
  // doSearchSuccess(state, results) {
  //     state.countPercent = results.countPercent
  //     state.countsTotal = results.countsTotal
  //     state.emptyResult = results.emptyResult
  //     state.datasetQueries.push(state.query)
  //     if (state.datasets.length == 0) {
  //       state.labels = state.countsTotal.map(countTotal => countTotal.date)
  //     }
  //     state.datasets.push({
  //       query: state.query,
  //       count: state.countsTotal.map(countTotal => countTotal.count),
  //       total: state.countsTotal.map(countTotal => countTotal.total),
  //       percent: state.countPercent
  //     })
  //   this.dispatch('Ngram/setLoadingStatus', false) 
  // },

  // doSearchError(state, message) {
  //   if (message.response.status === 400 && message.response.data.startsWith('Tag syntax not accepted')) {
  //     this.dispatch('Notifier/setNotification', {
  //       title: 'We are so sorry!',
  //       text: 'Please remove all < and > from your query and try again',
  //       srvMessage: message.response.data,
  //       type: 'error',
  //       timeout: false
  //     })
  //   } else {
  //   this.dispatch('Notifier/setNotification', {
  //       title: 'We are so sorry!',
  //       text: 'Something went wrong with your search - please try again',
  //       srvMessage: message.response.data,
  //       type: 'error',
  //       timeout: false
  //     })
  //   }
  //   this.dispatch('Search/setLoadingStatus', false)
  // },

  // setLoadingStatus(state, status) {
  //   state.loading = status
  // },
  
  // setSearchType(state, type) {
  //   state.searchType = type
  // },
  // setTimeScale(state, scale) {
  //   state.timeScale = scale
  // },
//   resetState(state) {
//     const newState = initialState()
//     Object.keys(newState).forEach(key => {
//           state[key] = newState[key]
//     })
//   },

// }

// export default {
//   namespaced: true,
//   state,
//   actions,
//   mutations
// }

