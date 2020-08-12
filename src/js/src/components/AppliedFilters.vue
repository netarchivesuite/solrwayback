<template>
  <div class="filters">
    <h3>Applied filters:</h3>
    <div class="displayedFilter" v-bind:key="index" v-for="(item, index) in breakFilters(filters)">{{ displayFilter(item) }}<button v-on:click="removeFilter(item)">X</button></div>
  </div>
</template>

<script>

import { mapState, mapActions } from 'vuex'

export default {
  name: "AppliedFilters",
  computed: {
    ...mapState({
      filters: state => state.searchStore.filters,
      facets: state => state.searchStore.facets,
      query: state => state.searchStore.query
    }),
  },
  mounted () {
  },
  
  methods: {
    ...mapActions('searchStore', {
      search: 'search',
      requestFacets: 'requestFacets',
      updateFilters:'updateFilters'
    }),
    breakFilters(filters) {
      let dividedFilters = filters.split('&fq=')
      dividedFilters.shift()
      return dividedFilters
    },
    displayFilter(filter) {
      return filter.replace(/"/g,'')
    },
    removeFilter(filter) {
      console.log(filter, this.filters)
      this.updateFilters(this.filters.replace("&fq=" + filter,''))
      console.log(this.filters)
      this.search({query:this.query, filters:this.filters})
      this.requestFacets({query:this.query, filters:this.filters})
      this.$router.replace({ query: {q:this.query + this.filters }});
    }
  }
}

</script>

    
