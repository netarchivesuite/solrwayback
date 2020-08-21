<template>
  <div class="searchBoxContainer">
    <form class="searchForm" @submit.prevent="handleSubmit">
      <input id="query"
             v-model="futureQuery"
             type="text"
             autofocus
             class=""
             placeholder="Enter search term">
      <button id="querySubmit" title="Search" type="submit">
        <div id="magnifyingGlass" />
      </button>
      <button v-if="futureQuery !== ''"
              id="clearSubmit"
              title="Clear search and results"
              type="button"
              @click="clearResultsAndSearch" />     
    </form>
    <applied-search-facets />
  </div>
</template> 

<script>
import { mapState, mapActions } from 'vuex'
import AppliedSearchFacets from './AppliedSearchFacets.vue'

export default {
  components: {
    AppliedSearchFacets
  },
  data () {
    return {    
      futureQuery:'' 
    }
  },
  computed: {
    ...mapState({
      query: state => state.Search.query,
      searchAppliedFacets: state => state.Search.searchAppliedFacets,
      results: state => state.Search.results,
      loading: state => state.Search.loading,
    })
  },
  mounted () {
    console.log('NEW MOUNT, CHECK THE PARAMS FROM URL:',this.$router.history.current.query)
    if(this.query === '' && this.$router.history.current.query.q) {
      this.updateQuery(this.$router.history.current.query.q)
      this.futureQuery = this.$router.history.current.query.q
      if(this.$router.history.current.query.facets) {
        this.updateSearchAppliedFacets(this.$router.history.current.query.facets)
      }
      this.search({query:this.query, facets:this.searchAppliedFacets})
      this.requestFacets({query:this.query, facets:this.searchAppliedFacets})
      }
  },
  
  methods: {
    ...mapActions('Search', {
      search: 'search',
      requestFacets: 'requestFacets',
      updateQuery: 'updateQuery',
      clearResults: 'clearResults',
      updateSearchAppliedFacets:'updateSearchAppliedFacets',
      resetSearchState:'resetState'
    }),
    handleSubmit() {
      if (this.futureQuery !== this.query) {
        this.updateQuery(this.futureQuery)
        this.search({query:this.futureQuery, facets:this.searchAppliedFacets})
        this.requestFacets({query:this.futureQuery, facets:this.searchAppliedFacets})
        this.$router.replace({ query: {q:this.query }})
      }
    },
    clearResultsAndSearch() {
      if(this.futureQuery !== '' && this.query !== '') {
        this.$router.replace({ query: {}})
      }
      this.futureQuery = ''
      this.resetSearchState()
     // this.updateQuery('')
      //this.clearResults()
      //this.updateSearchAppliedFacets('')
    }
  }
}

</script>

    