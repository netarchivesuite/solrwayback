<template>
  <div v-if="facets.facet_fields" class="facets">
    <h2>Facets</h2>
    <div v-for="(facetCategory, index) in Object.entries(facets.facet_fields)" :key="index" class="facetCategory">
      <div class="facetCategoryName">
        {{ facetCategory[0] }}
      </div> 
      <div v-for="(facet, facetIndex) in facetCategory[1]"
           :key="facetIndex"
           :class="facetIndex % 2 === 0 ? 'facetItem' : 'facetCount'"
           @click="facetIndex % 2 === 0 ? applyFacet(facetCategory[0], facet) : null">
        {{ facetIndex % 2 === 0 ? facet || "Unknown" : "(" + facet + ")" }}
      </div>
    </div>
  </div>
</template>

<script>

import { mapState, mapActions } from 'vuex'

export default {
  name: 'SearchFacetOptions',
  computed: {
    ...mapState({
      searchAppliedFacets: state => state.Search.searchAppliedFacets,
      facets: state => state.Search.facets,
      query: state => state.Search.query
    }),
  },
  mounted () {
  },
  
  methods: {
    ...mapActions('Search', {
      search: 'search',
      requestFacets: 'requestFacets',
      updateSearchAppliedFacets:'updateSearchAppliedFacets'
    }),
    applyFacet(facetCategory, facet) {
      let newFacet = '&fq=' + facetCategory + ':"' + facet + '"'
      this.updateSearchAppliedFacets(this.searchAppliedFacets + newFacet)
      this.search({query:this.query, facets:this.searchAppliedFacets})
      this.requestFacets({query:this.query, facets:this.searchAppliedFacets})
      // test with null!
      this.$router.replace({query: {q:this.query, facets:this.searchAppliedFacets !== '' ?  this.searchAppliedFacets : undefined }})
    }
  }
}

</script>

    
