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
    <button v-if="futureQuery !== ''" title="Clear search and results" id="clearSubmit" type="button" @click="clearResultsAndSearch" />     
  </form>
  <applied-search-facets />
</div>
</template> 

<script>
import { mapState, mapActions } from 'vuex'
import AppliedSearchFacets from "./AppliedSearchFacets.vue"

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
      query: state => state.searchStore.query,
      searchAppliedFacets: state => state.searchStore.searchAppliedFacets,
      results: state => state.searchStore.results,
      loading: state => state.searchStore.loading,
    })
  },
  mounted () {
    console.log("NEW MOUNT, CHECK THE PARAMS FROM URL:",this.$router.history.current.query)
    if(this.query === "" && this.$router.history.current.query.q) {
      this.updateQuery(this.$router.history.current.query.q)
      this.futureQuery = this.$router.history.current.query.q;
      if(this.$router.history.current.query.facets) {
        this.updateSearchAppliedFacets(this.$router.history.current.query.facets)
      }
      this.search({query:this.query, facets:this.searchAppliedFacets})
      this.requestFacets({query:this.query, facets:this.searchAppliedFacets})
      }
  },
  
  methods: {
    ...mapActions('searchStore', {
      search: 'search',
      requestFacets: 'requestFacets',
      updateQuery: 'updateQuery',
      clearResults: 'clearResults',
      updateSearchAppliedFacets:'updateSearchAppliedFacets',
    }),
    handleSubmit() {
      if (this.futureQuery !== this.query) {
        this.updateQuery(this.futureQuery)
        this.search({query:this.futureQuery, facets:this.searchAppliedFacets})
        this.requestFacets({query:this.futureQuery, facets:this.searchAppliedFacets})
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
      this.updateSearchAppliedFacets("");
    }
  }
}

</script>

    