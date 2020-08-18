<template>
  <div v-if="facets.length !== 0" class="facets">
    <h2>Facets</h2>
    <div :key="index" class="facetCategory" v-for="(facetCategory, index) in Object.entries(facets.facet_fields)"><div class="facetCategoryName">{{ facetCategory[0] }}</div> 
      <div @click="index % 2 === 0 ? applyFacet(facetCategory[0], facet) : null" :key="index" :class="index % 2 === 0 ? 'facetItem' : 'facetCount'" v-for="(facet, index) in facetCategory[1]"> {{ index % 2 === 0 ? facet || "Unknown" : "(" + facet + ")" }}</div>
    </div>
  </div>
</template>

<script>

import { mapState, mapActions } from 'vuex'

export default {
  name: "SearchFacetOptions",
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
    applyFacet(facetCategory, facet) {
      let newFacet = '&fq=' + facetCategory + ':"' + facet + '"';
      this.updateSearchAppliedFacets(this.searchAppliedFacets + newFacet)
      this.search({query:this.query, facets:this.searchAppliedFacets})
      this.requestFacets({query:this.query, facets:this.searchAppliedFacets})
      // test with null!
      this.$router.replace({query: {q:this.query, facets:this.searchAppliedFacets !== "" ?  this.searchAppliedFacets : undefined }});
    }
  }
}

</script>

    
