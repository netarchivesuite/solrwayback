<template>
  <div v-if="searchAppliedFacets.length > 0" class="selectedFacets">
    <h2>Applied facets</h2>
    <div v-for="(item, index) in searchAppliedFacets" :key="index" class="displayedFacet">
      <span>{{ displayFacetName(item) }}</span><span>{{ displayFacetValue(item) }}</span><button @click="removeFacet(index)">
        ✕
      </button>
    </div>
  </div>
</template>

<script>

import { mapState, mapActions } from 'vuex'
import StringManipulationUtils from './../mixins/StringManipulationUtils'
import HistoryRoutingUtils from './../mixins/HistoryRoutingUtils'

export default {
  name: 'AppliedSearchFacets',
  
  mixins: [StringManipulationUtils, HistoryRoutingUtils],

  computed: {
    ...mapState({
      searchAppliedFacets: state => state.Search.searchAppliedFacets,
      facets: state => state.Search.facets,
      query: state => state.Search.query,
      solrSettings: state => state.Search.solrSettings,
    }),
  },
  methods: {
    ...mapActions('Search', {
      requestSearch: 'requestSearch',
      requestFacets: 'requestFacets',
      removeFromSearchAppliedFacets:'removeFromSearchAppliedFacets',
      updateSolrSettingOffset:'updateSolrSettingOffset'
    }),
    removeFacet(index) {
      this.updateSolrSettingOffset(0)
      this.removeFromSearchAppliedFacets(index)
      this.$_pushSearchHistory('Search', this.query, this.searchAppliedFacets, this.solrSettings)
    }
  }
}

</script>

    
