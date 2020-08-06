<template>
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
</template> 

<script>
import { mapState, mapActions } from 'vuex'

export default {
  components: {
    
  },
  data () {
    return {    
      futureQuery:'' 
    }
  },
  computed: {
    ...mapState({
      query: state => state.searchStore.query,
      results: state => state.searchStore.results,
      loading: state => state.searchStore.loading,
    })
  },
  mounted () {
  },
  
  methods: {
    ...mapActions('searchStore', {
      search: 'search',
      updateQuery: 'updateQuery',
      clearResults: 'clearResults'
    }),
    handleSubmit() {
      if (this.futureQuery !== this.query) {
        this.updateQuery(this.futureQuery)
        this.search(this.futureQuery)
      }
    },
    clearResultsAndSearch() {
      this.futureQuery = ''
      this.clearResults()
    }
  }
}

</script>

    