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
import HistoryRoutingUtils from './../mixins/HistoryRoutingUtils'

export default {
  name: 'SearchFacetOptions',
  mixins: [HistoryRoutingUtils],
  computed: {
    ...mapState({
      searchAppliedFacets: state => state.Search.searchAppliedFacets,
      facets: state => state.Search.facets,
      query: state => state.Search.query,
      solrSettings: state => state.Search.solrSettings
    }),
  },
  mounted () {
  },
  methods: {
    ...mapActions('Search', {
      requestSearch: 'requestSearch',
      requestFacets: 'requestFacets',
      updateSearchAppliedFacets:'updateSearchAppliedFacets'
    }),
    applyFacet(facetCategory, facet) {
      let newFacet = '&fq=' + facetCategory + ':"' + facet + '"'
      this.updateSearchAppliedFacets(this.searchAppliedFacets + newFacet)
      this.requestSearch({query:this.query, facets:this.searchAppliedFacets, options: this.solrSettings})
      this.requestFacets({query:this.query, facets:this.searchAppliedFacets, options: this.solrSettings})
      this.pushHistory('SolrWayback', this.query, this.searchAppliedFacets, this.solrSettings)
      //history.pushState({name: 'SolrWayback'}, 'SolrWayback', '?q=' + this.query + '&facets=' + encodeURIComponent(this.searchAppliedFacets))
      //this.$router.replace({query: {q:this.query, facets:this.searchAppliedFacets !== '' ?  this.searchAppliedFacets : undefined }})
    }
  }
}

</script>

    
