<template>
  <div v-if="Object.keys(this.searchStore.results).length > 0" class="resultAndFacetContainer">
    <div class="facetContainer">
      <search-facet-options v-if="this.searchStore.results.searchType === 'post'" />
    </div>
    <div class="resultContainer">
      <search-result-export v-if="configs" :configs="configs" />
      <h2>Results</h2><button title="Visualize result"
                              type="button"
                              class="visualizeButton"
                              @click="showVisualizedResult('visualization')" />
      <!-- HERE COMES RESULTS // Figure out if this should be splitted out into a new component -->
      <post-search-results v-if="this.searchStore.results.searchType === 'post'" />
      <!-- HERE COMES PICTURES -->
      <image-search-results v-if="this.searchStore.results.searchType === 'image'" :img-results="this.searchStore.results" />
    </div>
    <div class="marginContainer" />
  </div>
</template>

<script>

import { mapStores, mapActions } from 'pinia'
import { useModalStore } from '../../store/modal.store'
import { useSearchStore } from '../../store/search.store'
import SearchFacetOptions from './../SearchFacetOptions.vue'
import HistoryRoutingUtils from './../../mixins/HistoryRoutingUtils'
import ImageSearchResults from './ImageSearchResults.vue'
import PostSearchResults from './PostSearchResults.vue'
import SearchResultExport from './SearchResultExport.vue'
import configs from '../../configs'

export default {
  name: 'AllSearchResults',
  components: {
    SearchFacetOptions,
    ImageSearchResults,
    PostSearchResults,
    SearchResultExport
  },
  mixins: [HistoryRoutingUtils],
  data () {
    return {  
      configs:configs
    }
  },
  computed: {
    // ...mapState({
    //   results: state => state.Search.results,
    //   showModal: state => state.Modal.showModal,
    //   currentModal: state => state.Modal.currentModal
    // }),
    ...mapStores(useModalStore, useSearchStore)
  },
  methods: {
    ...mapActions(useModalStore, {
      updateShowModal:'updateShowModal',
      updateCurrentModal:'updateCurrentModal'
    }),
    showVisualizedResult(modal) {
      this.updateCurrentModal(modal)
      this.updateShowModal(true)
    }
  }
}
</script>

    
