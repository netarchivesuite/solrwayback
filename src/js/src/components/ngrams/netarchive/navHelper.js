/**
 * Helpers for app navigation
 * 
 */

import {mapActions, mapState} from 'vuex'

 export default {
  computed: {
    ...mapState({
      datasetQueries: state => state.Ngram.datasetQueries,
    })
  },
  methods: {
    ...mapActions('Ngram', {
      doSearch:'doSearch',
      resetSearchState:'resetState',
      removeDataset:'removeDataset'
    }),
    
    $_doSearchFromQueryParams(currentURLParams) {
      //Get all the params that start with query
      const queryParams = Object.keys(currentURLParams)
         .filter(value => /^query/i.test(value))
         .map(e => currentURLParams[e])
     //Loop through params and trigger new search if param is not empty string and update display query 
      queryParams.forEach((val) => {
        if (val !== ''){
        this.doSearch(val)
        }
      })
    },

    $_handleRouteUpdates(to){
      const queryParamCount = Object.keys(to.query).length
      //If no params but we are in path search - just reset the whole shabang
      if (to.name === 'Search' && queryParamCount === 0){
        this.resetSearchState()
      }
      // If we have more params tha datasets some action may be warrented
      if (queryParamCount > this.datasetQueries.length){
         /**If the change in params is just 1 relative to the current datasets it is 
         /* a new search on top of the current ones so we grab the new query and trigger a search
         */ 
        //TODO refactor!!!! cannot rely on order when working with objects --> rookie mistake...
         if (queryParamCount-1 === this.datasetQueries.length){
                const ents = Object.entries(to.query)
                const last = ents[ents.length-1]
                let obj = {}
                obj[last[0]] = last[1]
                //console.log(obj)
                      this.$_doSearchFromQueryParams(obj)
                }
        }

      if (this.datasetQueries.length > queryParamCount){
        this.removeDataset()
      }
    },

    $_handleRouteLeave(to){
      if (to.name === 'Home') {
        this.resetSearchState()
      } else if (to.name === 'Search') {
        this.$_doSearchFromQueryParams(to.query)
      }
    }
  }
}