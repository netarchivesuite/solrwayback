<template>
  <div v-if="Object.keys(results).length > 0" class="resultAndFacetContainer">
    <div class="facetContainer">
      <search-facet-options />
    </div>
    <div class="resultContainer">
      <h2>Results</h2>
      <!-- HERE COMES RESULTS -->
      <div v-if="results.searchType === 'post'">
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
      <!-- HERE COMES PICTURES -->
      <div v-if="results.searchType === 'image'">
        <span>Showing <span class="highlightText">{{ results.images.length }}</span> images matching <span class="highlightText">{{ query }}. </span> </span>
        <div class="images">
          <div class="column 1">
            <search-masonry-image v-for="(result, index) in getOffsetArray(results.images,0)"
                                  :key="index"
                                  :number="index"
                                  :result="result"
                                  :row="1" />
          </div>
          <div class="column 2">
            <search-masonry-image v-for="(result, index) in getOffsetArray(results.images,1)"
                                  :key="index"
                                  :number="index"
                                  :result="result"
                                  :row="2" />
          </div>
          <div class="column 3">
            <search-masonry-image v-for="(result, index) in getOffsetArray(results.images,2)"
                                  :key="index"
                                  :number="index"
                                  :result="result"
                                  :row="3" />
          </div>
        </div>
      </div>
    </div>
    <div class="marginContainer" />
  </div>
</template>

<script>
import { mapState, mapActions } from 'vuex'
import SearchFacetOptions from './SearchFacetOptions.vue'
import HistoryRoutingUtils from './../mixins/HistoryRoutingUtils'
import SearchMasonryImage from './SearchSingleItemComponents/SearchMasonryImage'

export default {
  name: 'SearchResult',
  components: {
    SearchSingleItemDefault: () => import('./SearchSingleItemComponents/SearchSingleItemTypes/SearchSingleItemDefault'),
    SearchSingleItemTweet: () => import('./SearchSingleItemComponents/SearchSingleItemTypes/SearchSingleItemTweet'),
    SearchSingleItemWeb: () => import('./SearchSingleItemComponents/SearchSingleItemTypes/SearchSingleItemWeb'),
    SearchFacetOptions,
    SearchMasonryImage
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
      this.$_pushSearchHistory('SolrWayback', this.query, this.searchAppliedFacets, this.solrSettings)
    },
    getPreviousResults() {
      this.updateSolrSettingOffset(this.solrSettings.offset - 20)
      this.requestSearch({query:this.query, facets:this.searchAppliedFacets, options:this.solrSettings})
      this.requestFacets({query:this.query, facets:this.searchAppliedFacets, options:this.solrSettings})
      this.$_pushSearchHistory('SolrWayback', this.query, this.searchAppliedFacets, this.solrSettings)
    },
    getOffsetArray(array, number) {
      let newArray = [...array]
      newArray.splice(0,number)
      let returnArray = newArray.filter((item, index) => {
        return index % 3 === 0
      })
      console.log(returnArray)
      return returnArray
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

    
