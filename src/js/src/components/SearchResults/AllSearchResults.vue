<template>
  <div v-if="Object.keys(results).length > 0" class="resultAndFacetContainer">
    <div class="facetContainer">
      <search-facet-options />
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
      numberOfRows:3,   
      configs:configs
    }
  },
  computed: {
    ...mapState({
      query: state => state.Search.query,
      searchAppliedFacets: state => state.Search.searchAppliedFacets,
      results: state => state.Search.results,
      solrSettings: state => state.Search.solrSettings
    }),
  },
  methods: {
    ...mapActions('Search', {
      requestSearch: 'requestSearch',
      requestFacets: 'requestFacets',
      updateSolrSettingOffset:'updateSolrSettingOffset'
    }),
    getNextResults() {
      this.updateSolrSettingOffset(this.solrSettings.offset + 20)
      this.requestSearch({query:this.query, facets:this.searchAppliedFacets, options:this.solrSettings})
      this.requestFacets({query:this.query, facets:this.searchAppliedFacets, options:this.solrSettings})
      this.$_pushSearchHistory('SolrWayback', this.query, this.searchAppliedFacets, this.solrSettings)
    },
    getPreviousResults() {
      this.updateSolrSettingOffset(this.solrSettings.offset - 20)
      this.requestSearch({query:this.query, facets:this.searchAppliedFacets, options:this.solrSettings})
      this.requestFacets({query:this.query, facets:this.searchAppliedFacets, options:this.solrSettings})
      this.$_pushSearchHistory('SolrWayback', this.query, this.searchAppliedFacets, this.solrSettings)
    },
    SingleEntryComponent(type) {
      switch(type) {   
        case 'Web Page': return 'SearchSingleItemWeb'
        case 'Image': return 'SearchSingleItemImage'
        case 'Twitter Tweet': return 'SearchSingleItemTweet'
        default: return 'SearchSingleItemDefault'
      }
    },
  }
}
</script>

    
