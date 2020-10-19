<template>
  <div>
    <span v-if="!results.cardinality">
      <span>Showing <span class="highlightText">{{ solrSettings.offset }}</span>  - <span class="highlightText">{{ solrSettings.offset + 20 > results.numFound ? results.numFound : solrSettings.offset + 20 }}</span> of </span>
      <span class="highlightText">{{ results.numFound.toLocaleString("en") }}</span> entries matching query.
    </span>
    <span v-if="results.cardinality">
      <span>Showing <span class="highlightText">{{ solrSettings.offset }}</span> - <span class="highlightText">{{ solrSettings.offset + 20 > results.cardinality ? results.cardinality : solrSettings.offset + 20 }}</span> of </span>
      <span class="highlightText">{{ results.cardinality.toLocaleString("en") }}</span> unique entries matching query 
      <span class="tonedDownText">(total hits: {{ results.numFound.toLocaleString("en") }})</span>.
    </span>
    <div class="pagingContainer">
      <button :disabled="solrSettings.offset < 20" @click="getPreviousResults()">
        Previous 20
      </button>
      <button :disabled="results.cardinality ? solrSettings.offset + 20 >= results.cardinality : solrSettings.offset + 20 >= results.numFound" @click="getNextResults()">
        Next 20
      </button>
    </div>
    <div v-if="results && results !== {}" class="results">
      <component :is="SingleEntryComponent(result.type)"
                 v-for="(result, index) in results.docs"
                 :key="index"
                 :result="result"
                 :rank-number="index" />
    </div>
    <div class="pagingContainer">
      <button :disabled="solrSettings.offset < 20" @click="getPreviousResults()">
        Previous 20
      </button>
      <button :disabled="results.cardinality ? solrSettings.offset + 20 >= results.cardinality : solrSettings.offset + 20 >= results.numFound" @click="getNextResults()">
        Next 20
      </button>
    </div>
  </div>
</template>

<script>
import { mapState, mapActions } from 'vuex'
import SearchFacetOptions from './../SearchFacetOptions.vue'
import HistoryRoutingUtils from './../../mixins/HistoryRoutingUtils'
import ImageSearchResults from './ImageSearchResults'


export default {
  name: 'PostSearchResults',
  components: {
    SearchSingleItemDefault: () => import('./../searchSingleItemComponents/searchSingleItemTypes/SearchSingleItemDefault'),
    SearchSingleItemTweet: () => import('./../searchSingleItemComponents/searchSingleItemTypes/SearchSingleItemTweet'),
    SearchSingleItemWeb: () => import('./../searchSingleItemComponents/searchSingleItemTypes/SearchSingleItemWeb'),
    SearchSingleItemImage: () => import('./../searchSingleItemComponents/searchSingleItemTypes/SearchSingleItemImage'),
    SearchSingleItemVideoAudio: () => import('./../searchSingleItemComponents/searchSingleItemTypes/SearchSingleItemVideoAudio'),
   
   SearchFacetOptions,
    ImageSearchResults
  },
  mixins: [HistoryRoutingUtils],
  data () {
    return {
            hitsPerPage: 20
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
      updateSolrSettingOffset:'updateSolrSettingOffset'
    }),
    getNextResults() {
      this.updateSolrSettingOffset(this.solrSettings.offset + this.hitsPerPage)
      this.$_pushSearchHistory('Search', this.query, this.searchAppliedFacets, this.solrSettings)
    },
    getPreviousResults() {
      this.updateSolrSettingOffset(this.solrSettings.offset - this.hitsPerPage)
      this.$_pushSearchHistory('Search', this.query, this.searchAppliedFacets, this.solrSettings)
    },
    SingleEntryComponent(type) {
      //console.log('search result type', type)
      switch(type) {   
        case 'Web Page': return 'SearchSingleItemWeb'
        case 'Image': return 'SearchSingleItemImage'
        case 'Twitter Tweet': return 'SearchSingleItemTweet'
        case 'Video' : 
        case 'Audio' : return 'SearchSingleItemVideoAudio'
        default: return 'SearchSingleItemDefault'
      }
    }
  }
}
</script>