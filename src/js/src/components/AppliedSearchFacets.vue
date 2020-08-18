<template>
  <div v-if="searchAppliedFacets !== ''" class="facets">
    <h2>Applied facets</h2>
    <div class="displayedFacet" v-bind:key="index" v-for="(item, index) in seperateFacets(searchAppliedFacets)"><span>{{ displayFacetName(item) }}</span><span>{{ displayFacetValue(item) }}</span><button v-on:click="removeFacet(item)">✕</button></div>
  </div>
</template>

<script>

import { mapState, mapActions } from 'vuex'

export default {
  name: "AppliedSearchFacets",
  computed: {
    ...mapState({
      searchAppliedFacets: state => state.searchStore.searchAppliedFacets,
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
      updateSearchAppliedFacets:'updateSearchAppliedFacets'
    }),
    seperateFacets(facets) {
      let dividedFacets = facets.split('&fq=')
      dividedFacets.shift()
      return dividedFacets
    },
    displayFacetName(facet) {
      return facet.split(":")[0] + ": "
    },
    displayFacetValue(facet) {
      return facet.split(":")[1].replace(/"/g,'')
    },
    removeFacet(facet) {
      console.log(facet, this.searchAppliedFacets)
      this.updateSearchAppliedFacets(this.searchAppliedFacets.replace("&fq=" + facet,''))
      console.log(this.searchAppliedFacets)
      this.search({query:this.query, facets:this.searchAppliedFacets})
      this.requestFacets({query:this.query, facets:this.searchAppliedFacets})
      this.$router.replace({query: {q:this.query, facets:this.searchAppliedFacets !== "" ?  this.searchAppliedFacets : undefined }});
    }
  }
}

</script>

    
