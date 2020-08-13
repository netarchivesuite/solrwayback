<template>
  <div class="searchBoxContainer">
  <form class="searchForm" @submit.prevent="handleSubmit">
    <input  id="query"
      v-model="futureQuery"
      type="text"
      autofocus
      class=""
      placeholder="Enter search term">
    <button title="Search" id="querySubmit" type="submit">
      <div id="magnifyingGlass"></div>
    </button>
    <button v-if="futureQuery !== ''" title="Clear search and results" id="clearSubmit" type="button" v-on:click="clearResultsAndSearch" />     
  </form>
  <applied-filters />
</div>
</template> 

<script>
import { mapState, mapActions } from 'vuex'
import AppliedFilters from "./AppliedFilters.vue"

export default {
  components: {
    AppliedFilters
  },
  data () {
    return {    
      futureQuery:'' 
    }
  },
  computed: {
    ...mapState({
      query: state => state.searchStore.query,
      filters: state => state.searchStore.filters,
      results: state => state.searchStore.results,
      loading: state => state.searchStore.loading,
    })
  },
  mounted () {
    console.log("NEW MOUNT, CHECK THE PARAMS FROM URL:",this.$router.history.current.query)
    if(this.query === "" && this.$router.history.current.query.q) {
      this.updateQuery(this.$router.history.current.query.q)
      this.futureQuery = this.$router.history.current.query.q;
      if(this.$router.history.current.query.filters) {
        this.updateFilters(this.$router.history.current.query.filters)
      }
      this.search({query:this.query, filters:this.filters})
      this.requestFacets({query:this.query, filters:this.filters})
      }
  },
  
  methods: {
    ...mapActions('searchStore', {
      search: 'search',
      requestFacets: 'requestFacets',
      updateQuery: 'updateQuery',
      clearResults: 'clearResults',
      updateFilters:'updateFilters',
    }),
    handleSubmit() {
      if (this.futureQuery !== this.query) {
        this.updateQuery(this.futureQuery)
        this.search({query:this.futureQuery, filters:this.filters})
        this.requestFacets({query:this.futureQuery, filters:this.filters})
        this.$router.replace({ query: {q:this.query }});
      }
    },
    clearResultsAndSearch() {
      if(this.futureQuery !== "" && this.query !== "") {
        this.$router.replace({ query: {}});
      }
      this.futureQuery = '';
      this.updateQuery("");
      this.clearResults();
      this.updateFilters("");
    }
  }
}

</script>

    