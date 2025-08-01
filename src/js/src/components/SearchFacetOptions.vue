<template>
  <div class="facets">
    <h2 v-if="checkForFacets(this.searchStore.facets.facet_fields)">
      Facets
    </h2>
    <div v-if="this.searchStore.facetLoading && !this.searchStore.loading" class="spinner" />
    <div v-if="!this.searchStore.facetLoading && checkForFacets(this.searchStore.facets.facet_fields)" class="allFacets">
      <div v-for="(facetCategory, index) in Object.entries(this.searchStore.facets.facet_fields)" :key="index" class="facetCategory">
        <div class="facetCategoryName">
          {{ facetCategory[0] }}
        </div> 
        <div v-for="(facet, facetIndex) in facetCategory[1]"
             :key="facetIndex"
             :class="facetIndex % 2 === 0 ? 'facetItem' : 'facetCount'"
             @click="facetIndex % 2 === 0 ? applyFacet(facetCategory[0], facet, $event) : null">
          {{ facetIndex % 2 === 0 ? facet || "Unknown" : "(" + facet.toLocaleString("en") + ")" }}
        </div>
        <div v-show="this.searchStore.extraFacetLoading === facetCategory[0]" class="extraFacetsloading" />
        <!-- here we're excluding the crawl_year facets, because OP don't want a show more on those -->
        <div v-if="facetCategory[1].length >= 20 && facetCategory[0] !== 'crawl_year'" class="moreFacets">
          <div v-if="facetCategory[1].length > 20" class="facetArrow up">
            ︿
          </div>
          <button :disabled="!!this.searchStore.extraFacetLoading" class="moreFacetText" @click="determineFacetAction(facetCategory[0], facetCategory[1].length)">
            {{ determineText(facetCategory[0], facetCategory[1].length) }} 
          </button>
          <div v-if="facetCategory[1].length === 20" class="facetArrow down">
            ﹀
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>

import { mapStores, mapActions } from 'pinia'
import { useSearchStore } from '../store/search.store'
import HistoryRoutingUtils from './../mixins/HistoryRoutingUtils'
import { requestService } from '../services/RequestService'


export default {
  name: 'SearchFacetOptions',
  mixins: [HistoryRoutingUtils],
  computed: {
    // ...mapState({
    //   searchAppliedFacets: state => state.Search.searchAppliedFacets,
    //   facets: state => state.Search.facets,
    //   query: state => state.Search.query,
    //   solrSettings: state => state.Search.solrSettings,
    //   loading: state => state.Search.loading,
    //   facetLoading: state => state.Search.facetLoading,
    //   extraFacetLoading: state => state.Search.extraFacetLoading
    // })
    ...mapStores(useSearchStore)
  },
  methods: {
    ...mapActions(useSearchStore, {
      updateSolrSettingOffset:'updateSolrSettingOffset',
      addToSearchAppliedFacets:'addToSearchAppliedFacets',
      addSpecificRequestedFacets:'addSpecificRequestedFacets',
      setFacetToInitialAmount:'setFacetToInitialAmount'
    }),
    determineFacetAction(facet, length) {
      if(length === 20) {
        this.requestAdditionalFacets(facet)
      }
      else if(length > 20) {
        this.setFacetToInitialAmount(facet)
      }
    },
    determineText(facet, length) {
      return length <= 20 ? 'more ' + facet + 's' : 'less ' + facet + 's'
    },
    requestAdditionalFacets(facetArea) {
      let structuredQuery = this.searchStore.query
      let appliedFacets = this.searchStore.searchAppliedFacets.join('')
      this.addSpecificRequestedFacets({facet:facetArea, query:structuredQuery, appliedFacets:appliedFacets})
    },
    applyFacet(facetCategory, facet, event) {
      let facetAllreadyApplied = false
      let newFacet = '&fq=' + facetCategory + ':"' + facet + '"'
      this.searchStore.searchAppliedFacets.forEach((facet) => {
        facet === newFacet ? facetAllreadyApplied = true : null
      })
      if(!facetAllreadyApplied) {
        if(event.ctrlKey || event.metaKey) {
            const localSolrSettings = JSON.parse(JSON.stringify(this.searchStore.solrSettings))
            const localFacets = [...this.searchStore.searchAppliedFacets]
            localFacets.push(newFacet)
            localSolrSettings.offset = 0
            window.open(this.$_getResolvedUrl('Search', this.searchStore.query, localFacets, localSolrSettings).href, '_blank')
          }
          else {
            this.updateSolrSettingOffset(0)
            this.addToSearchAppliedFacets(newFacet)
            this.$_pushSearchHistory('Search', this.searchStore.query, this.searchStore.searchAppliedFacets, this.searchStore.solrSettings)
        }
      }
      else if(facetAllreadyApplied && event.ctrlKey ||
              facetAllReadyApplied && event.metaKey) {
        window.open(this.$_getResolvedUrl('Search', this.searchStore.query, this.searchStore.searchAppliedFacets, this.searchStore.solrSettings).href, '_blank')
      }
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

    
