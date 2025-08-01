// Global ngram state.
import { defineStore } from 'pinia';
import { useNotifierStore } from '../store/notifier.store'
import { useSearchStore } from '../store/search.store'

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


  actions: {
    setLoadingStatus( param ) {
      this.loading = param
    },
    setSearchType( param ) {
      this.searchType = param
    },
    updateQuery ( param ) {
      this.query = param
    },
    setTimeScale( param ) {
      this.timeScale = param
    },
    doSearch ( params ) {
      this.setLoadingStatus(true)
    
      requestService.getNgramNetarchive(params)
        .then(
          results => {
            this.updateQuery(params.query)
            this.setTimeScale(params.timeScale)
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
            this.setLoadingStatus(false)
          },
          error => {
            const notifier = useNotifierStore();
            const search = useSearchStore();
            if (error.response.status === 400 && error.response.data.startsWith('Tag syntax not accepted')) {
              notifier.setNotification({
                title: 'We are so sorry!',
                text: 'Please remove all < and > from your query and try again',
                srvMessage: error.response.data,
                type: 'error',
                timeout: false
              })
            } else {
            notifier.setNotification({
                title: 'We are so sorry!',
                text: 'Something went wrong with your search - please try again',
                srvMessage: error.response.data,
                type: 'error',
                timeout: false
              })
            }
            search.setLoadingStatus(false)
          }
        )
    },
    resetState(){
        this.$reset()
    },
    removeDataset() {
      this.datasetQueries.pop()
      this.datasets.pop()
    },
    addDataset() {
      this.query = param
    }

  }

})

