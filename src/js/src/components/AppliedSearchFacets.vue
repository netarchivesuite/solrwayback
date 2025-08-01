<template>
  <div v-if="this.searchStore.searchAppliedFacets.length > 0" class="selectedFacets">
    <h2>Applied facets</h2>
    <div v-for="(item, index) in this.searchStore.searchAppliedFacets" :key="index" class="displayedFacet">
      <span>{{ $_displayFacetName(item) }}</span><span>{{ $_displayFacetValue(item) }}</span><button @click="removeFacet(index)">
        ✕
      </button>
    </div>
  </div>
</template>

<script>

import { mapStores, mapActions } from 'pinia'
import { useSearchStore } from '../store/search.store'
import StringManipulationUtils from './../mixins/StringManipulationUtils'
import HistoryRoutingUtils from './../mixins/HistoryRoutingUtils'

export default {
  name: 'AppliedSearchFacets',
  
  mixins: [StringManipulationUtils, HistoryRoutingUtils],

  computed: {
    // ...mapState({
    //   searchAppliedFacets: state => state.Search.searchAppliedFacets,
    //   facets: state => state.Search.facets,
    //   query: state => state.Search.query,
    //   solrSettings: state => state.Search.solrSettings,
    // }),
    ...mapStores(useSearchStore)
  },
  methods: {
    ...mapActions(useSearchStore, {
      requestSearch: 'requestSearch',
      requestFacets: 'requestFacets',
      removeFromSearchAppliedFacets:'removeFromSearchAppliedFacets',
      updateSolrSettingOffset:'updateSolrSettingOffset'
    }),
    removeFacet(index) {
      this.updateSolrSettingOffset(0)
      this.removeFromSearchAppliedFacets(index)
      this.$_pushSearchHistory('Search', this.searchStore.query, this.searchStore.searchAppliedFacets, this.searchStore.solrSettings)
    }
  }
}

</script>

    
