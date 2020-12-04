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
             :class="facetIndex % 2 === 0 ? 'facetItem' : 'facetCount'"
             @click="facetIndex % 2 === 0 ? applyFacet(facetCategory[0], facet) : null">
          {{ facetIndex % 2 === 0 ? facet || "Unknown" : "(" + facet.toLocaleString("en") + ")" }}
        </div>
        <div v-if="facetCategory[1].length >= 20">
          <div @click="determineFacetAction(facetCategory[0], facetCategory[1].length)">
            {{ determineText(facetCategory[0], facetCategory[1].length) }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>

import { mapState, mapActions } from 'vuex'
import HistoryRoutingUtils from './../mixins/HistoryRoutingUtils'
import { requestService } from '../services/RequestService'


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
    shownEntities(facet) {
      return facet
    },
  },
  methods: {
    ...mapActions('Search', {
      updateSolrSettingOffset:'updateSolrSettingOffset',
      addToSearchAppliedFacets:'addToSearchAppliedFacets',
      addSpecificRequestedFacets:'addSpecificRequestedFacets'
    }),
    determineFacetAction(facet, length) {
      console.log(length)
      if(length === 20) {
        this.requestAdditionalFacets(facet)
      }
      else if(length > 20) {

      }
      else {

      }
    },
    determineText(facet, length) {
      return length <= 20 ? 'more ' + facet + 's' : 'less ' + facet + 's'
    },
    requestAdditionalFacets(facetArea) {
      console.log(this.query, this.facets)
      let structuredQuery = this.query + this.searchAppliedFacets.join('')
      let obj = {...this.facets}
      requestService.getAddonFacets(facetArea, this.query).then(result => this.constructNewFacets(result, facetArea), error => console.log('No additional facets found.'))
      this.addSpecificRequestedFacets(obj)
    },
    constructNewFacets(result, facetArea) {
      console.log(result)
      let facets = JSON.parse(JSON.stringify(this.facets))
      facets['facet_fields'][facetArea] = result.facet_counts.facet_fields[facetArea]
      this.addSpecificRequestedFacets(facets)
    },
    applyFacet(facetCategory, facet) {
      let newFacet = '&fq=' + facetCategory + ':"' + facet + '"'
      this.updateSolrSettingOffset(0)
      this.addToSearchAppliedFacets(newFacet)
      this.$_pushSearchHistory('Search', this.query, this.searchAppliedFacets, this.solrSettings)
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

    
