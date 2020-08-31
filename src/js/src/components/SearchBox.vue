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
    <div @click="showUploadFileSearch = !showUploadFileSearch">
      <span class="searchBoxActionLink">Search with uploaded file</span>
    </div>
    <applied-search-facets />
    <search-upload-file v-if="showUploadFileSearch" />
  </div>
</template> 

<script>
import { mapState, mapActions } from 'vuex'
import AppliedSearchFacets from './AppliedSearchFacets.vue'
import SearchUploadFile from './SearchUploadFile.vue'

export default {
  components: {
    AppliedSearchFacets,
    SearchUploadFile
  },
  data () {
    return {    
      futureQuery:'',
      showUploadFileSearch: false

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
  watch: {
    query: function (val) {
      this.futureQuery  = val
    },
  },
  mounted () {
    if(this.$router.history.current.query.q) {
      this.updateQuery(this.$router.history.current.query.q)
      this.futureQuery = this.$router.history.current.query.q
      if(this.$router.history.current.query.facets) {
        this.updateSearchAppliedFacets(this.$router.history.current.query.facets)
      }
      this.requestSearch({query:this.query, facets:this.searchAppliedFacets})
      this.requestFacets({query:this.query, facets:this.searchAppliedFacets})
      }
  },
  
  methods: {
    ...mapActions('Search', {
      requestSearch: 'requestSearch',
      requestFacets: 'requestFacets',
      updateQuery: 'updateQuery',
      clearResults: 'clearResults',
      updateSearchAppliedFacets:'updateSearchAppliedFacets',
      resetSearchState:'resetState'
    }),
    handleSubmit() {
      if (this.futureQuery !== this.query) {
        this.updateQuery(this.futureQuery)
        this.requestSearch({query:this.futureQuery, facets:this.searchAppliedFacets})
        this.requestFacets({query:this.futureQuery, facets:this.searchAppliedFacets})
        let newFacetUrl = this.searchAppliedFacets !== '' ? '&facets=' + encodeURIComponent(this.searchAppliedFacets) : ''
        history.pushState({name: 'SolrWayback'}, 'SolrWayback', '?q=' + this.query + newFacetUrl)

      }
    },
    clearResultsAndSearch() {
      history.pushState({name: 'SolrWayback'}, 'SolrWayback', '/')
      this.futureQuery = ''
      this.resetSearchState()
    },
  }
}

</script>

    