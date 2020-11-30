<template>
  <div class="facets">
    <h2 v-if="checkForFacets(facets.facet_fields)">
      Facets
    </h2>
    <div v-if="facetLoading && !loading" class="spinner" />
    <div v-if="!facetLoading && checkForFacets(facets.facet_fields)" class="allFacets">
      <div v-for="(facetCategory, index) in Object.entries(facets.facet_fields)" :key="index" class="facetCategory">
        <div class="facetCategoryName">
          {{ facetCategory[0] }}
        </div> 
        <div v-for="(facet, facetIndex) in facetCategory[1]"
             :key="facetIndex"
             :class="facetIndex % 2 === 0 ? 'facetItem' : 'facetCount'">
          <a v-if="facetIndex % 2 === 0" :href="getFacetSelectionLink(facetCategory, facet)"> {{ facet || 'Unknown' }}</a>
          <span v-else>{{ "(" + facet.toLocaleString("en") + ")" }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>

import { mapState, mapActions } from 'vuex'
import HistoryRoutingUtils from './../mixins/HistoryRoutingUtils'

export default {
  name: 'SearchFacetOptions',
  mixins: [HistoryRoutingUtils],
  computed: {
    ...mapState({
      searchAppliedFacets: state => state.Search.searchAppliedFacets,
      facets: state => state.Search.facets,
      query: state => state.Search.query,
      solrSettings: state => state.Search.solrSettings,
      facetLoading: state => state.Search.facetLoading,
      loading: state => state.Search.loading
    }),
  },
  methods: {
    ...mapActions('Search', {
      updateSolrSettingOffset:'updateSolrSettingOffset',
      addToSearchAppliedFacets:'addToSearchAppliedFacets',
    }),
    getFacetSelectionLink(facetCategory, facet) {
      const newFacet = encodeURIComponent('&fq=' + facetCategory[0] + ':"' + facet + '"')
      console.log(newFacet)
      return `/search?query=${encodeURIComponent(this.query)}&offset=0&grouping=${this.solrSettings.grouping}&imgSearch=${this.solrSettings.imgSearch}&urlSearch=${this.solrSettings.urlSearch}&facets=${encodeURIComponent(this.searchAppliedFacets.join(''))}${newFacet}`
    },
    checkForFacets(facets) {
    //we test if the variable exists first - can cause problems if it's not set yet.
      let fate = false
      if(facets) { 
        Object.keys(facets).forEach(function(item) {
          facets[item].length !== 0 ? fate = true : null 
        }) 
      } return fate
    }
  }
}

</script>

    
