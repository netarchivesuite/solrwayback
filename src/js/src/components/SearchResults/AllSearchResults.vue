<template>
  <div v-if="Object.keys(results).length > 0" class="resultAndFacetContainer">
    <div class="facetContainer">
      <search-facet-options v-if="results.searchType === 'post'" />
    </div>
    <div class="resultContainer">
      <search-result-export v-if="configs" :configs="configs" />
      <h2>Results</h2>
      <!-- HERE COMES RESULTS // Figure out if this should be splitted out into a new component -->
      <post-search-results v-if="results.searchType === 'post'" />
      <!-- HERE COMES PICTURES -->
      <image-search-results v-if="results.searchType === 'image'" />
    </div>
    <div class="marginContainer" />
  </div>
</template>

<script>
import { mapState, mapActions } from 'vuex'
import SearchFacetOptions from './../SearchFacetOptions.vue'
import HistoryRoutingUtils from './../../mixins/HistoryRoutingUtils'
import ImageSearchResults from './ImageSearchResults'
import PostSearchResults from './PostSearchResults'
import SearchResultExport from './SearchResultExport'
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
    ...mapState({
      results: state => state.Search.results,
    }),
  },
}
</script>

    
