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
import NavHelper from './navHelper'

export default {
  name: 'SearchBox',
  mixins: [NavHelper],
 
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
  
  mounted () {
     // Gather query params from URL
    const currentURLParams  = this.$router.history.current.query 
    this.$_doSearchFromQueryParams(currentURLParams)
  },
  
  methods: {
    ...mapActions('Ngram', {
      resetSearchState:'resetState',
      doSearch:'doSearch'
    }),

    submitSearch() {
      const newQuery = { ...this.$route.query } 
      newQuery['query' + this.datasetQueries.length] = this.searchQuery
      this.$router.push({ path: 'ngram', query: newQuery})
    },

    resetState() {
      this.$router.push(this.$route.path)
      this.resetSearchState()
    }
  }
}

</script>

    