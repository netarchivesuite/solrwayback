<template>
  <div class="searchBoxContainer">
    <form class="searchForm" @submit.prevent="submitSearch">
      <input id="query"
             v-model="searchQuery"
             type="text"
             autofocus
             placeholder="Enter search term">
      <button id="querySubmit" title="Search" type="submit">
        <div id="magnifyingGlass" />
      </button>
      <button v-if="searchQuery !== '' || Object.keys(results).length !== 0"
              id="clearSubmit"
              title="Clear search and results"
              type="button"
              @click.prevent="resetState()">
        X
      </button>
    </form>
  </div>
</template> 

<script>
import { mapState, mapActions } from 'vuex'

export default {
  name: 'SearchBox',
 
  data () {
    return {    
        searchQuery:''
    }
  },
  
  computed: {
    ...mapState({
     query: state => state.Ngram.query,
     results: state => state.Ngram.results,
     loading: state => state.Ngram.loading,
     datasetQueries: state => state.Ngram.datasetQueries
    })
  },
  
  watch: {
    query: function (val) {
      this.searchQuery  = val
    },
  },

  beforeDestroy() {
        this.resetSearchState()
  },
  
  methods: {
    ...mapActions('Ngram', {
      resetSearchState:'resetState',
      doSearch:'doSearch'
    }),

    submitSearch() {
      this.doSearch(this.searchQuery)

    },

    resetState() {
      this.resetSearchState()
    }
    
  }
  
}

</script>

    