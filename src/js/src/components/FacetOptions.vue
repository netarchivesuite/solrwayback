<template>
  <div class="facets">
    <h2>Facets</h2>
    <div v-bind:key="index" class="facetCategory" v-for="(facetCategory, index) in Object.entries(facets.facet_fields)"><div class="facetCategoryName">{{ facetCategory[0] }}</div> 
      <div v-on:click="index % 2 === 0 ? filterFromFacet(facetCategory[0], facet) : null" v-bind:key="index" :class="index % 2 === 0 ? 'facetItem' : 'facetCount'" v-for="(facet, index) in facetCategory[1]"> {{ index % 2 === 0 ? facet || "Unknown" : "(" + facet + ")" }}</div>
    </div>
  </div>
</template>

<script>

import { mapState, mapActions } from 'vuex'

export default {
  name: "FacetOptions",
  computed: {
    ...mapState({
      filters: state => state.searchStore.filters,
      facets: state => state.searchStore.facets,
    }),
  },
  mounted () {
  },
  
  methods: {
    ...mapActions('searchStore', {
      search: 'search',
      facets: 'facets',
      updateFilters:'updateFilters'
    }),
    filterFromFacet(facetCategory, facet) {
      console.log(facetCategory, facet)
    }
  }
}

</script>

    
