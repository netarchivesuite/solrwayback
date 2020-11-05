<template>
  <div class="facets">
    <h2>Facets</h2>
    <div v-if="facetLoading && !loading" class="spinner" />
    <div v-if="!facetLoading && facets.facet_fields" class="allFacets">
      <div v-for="(facetCategory, index) in Object.entries(facets.facet_fields)" :key="index" class="facetCategory">
        <div class="facetCategoryName">
          {{ facetCategory[0] }}
        </div> 
        <div v-for="(facet, facetIndex) in facetCategory[1]"
             :key="facetIndex"
             :class="facetIndex % 2 === 0 ? 'facetItem' : 'facetCount'"
             @click="facetIndex % 2 === 0 ? applyFacet(facetCategory[0], facet) : null">
          {{ facetIndex % 2 === 0 ? facet || "Unknown" : "(" + facet.toLocaleString("en") + ")" }}
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
    applyFacet(facetCategory, facet) {
      let newFacet = '&fq=' + facetCategory + ':"' + facet + '"'
      this.updateSolrSettingOffset(0)
      this.addToSearchAppliedFacets(newFacet)
      this.$_pushSearchHistory('Search', this.query, this.searchAppliedFacets, this.solrSettings)
    }
  }
}

</script>

    
