<template>
  <div v-if="Object.keys(results).length > 0" class="resultAndFacetContainer">
    <div class="facetContainer">
      <search-facet-options />
    </div>
    <div class="resultContainer">
      <h2>Results</h2>
      <span v-if="!results.cardinality">
        <span>Showing <span class="highlightText">{{ solrSettings.offset }}</span>  - <span class="highlightText">{{ solrSettings.offset + 20 > results.numFound ? results.numFound : solrSettings.offset + 20 }}</span> of </span>
        <span class="highlightText">{{ results.numFound.toLocaleString("en") }}</span> entries matching <span class="highlightText">{{ query }}. </span>
      </span>
      <span v-if="results.cardinality">
        <span>Showing <span class="highlightText">{{ solrSettings.offset }}</span> - <span class="highlightText">{{ solrSettings.offset + 20 > results.cardinality ? results.cardinality : solrSettings.offset + 20 }}</span> of </span>
        <span class="highlightText">{{ results.cardinality.toLocaleString("en") }}</span> unique entries matching <span class="highlightText">{{ query }} </span>
        <span class="tonedDownText">(total hits: {{ results.numFound.toLocaleString("en") }})</span>.
      </span>
      <div class="pagingContainer">
        <button :disabled="solrSettings.offset < 20" @click="getPreviousResults()">
          Previous 20
        </button>
        <button :disabled="results.cardinality ? solrSettings.offset + 20 > results.cardinality : solrSettings.offset + 20 > results.numFound" @click="getNextResults()">
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
    </div>
    <div class="marginContainer" />
  </div>
</template>

<script>
import { mapState, mapActions } from 'vuex'
import SearchFacetOptions from './SearchFacetOptions.vue'
import HistoryRoutingUtils from './../mixins/HistoryRoutingUtils'

export default {
  name: 'SearchResult',
  components: {
    SearchSingleItemDefault: () => import('./SearchSingleItemComponents/SearchSingleItemTypes/SearchSingleItemDefault'),
    SearchSingleItemTweet: () => import('./SearchSingleItemComponents/SearchSingleItemTypes/SearchSingleItemTweet'),
    SearchSingleItemWeb: () => import('./SearchSingleItemComponents/SearchSingleItemTypes/SearchSingleItemWeb'),
    SearchFacetOptions
  },
  mixins: [HistoryRoutingUtils],
  data () {
    return {     
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
  mounted () {
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
      this.pushHistory('SolrWayback', this.query, this.searchAppliedFacets, this.solrSettings)
    },
    getPreviousResults() {
      this.updateSolrSettingOffset(this.solrSettings.offset - 20)
      this.requestSearch({query:this.query, facets:this.searchAppliedFacets, options:this.solrSettings})
      this.requestFacets({query:this.query, facets:this.searchAppliedFacets, options:this.solrSettings})
      this.pushHistory('SolrWayback', this.query, this.searchAppliedFacets, this.solrSettings)
    },
    SingleEntryComponent(type) {
      switch(type) {   
        case 'Web Page': return 'SearchSingleItemWeb'
        case 'Twitter Tweet': return 'SearchSingleItemTweet'
        default: return 'SearchSingleItemDefault'
      }
    }
  }
}
</script>

    
