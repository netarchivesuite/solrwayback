<template>
  <div v-if="Object.keys(results).length > 0" class="resultAndFacetContainer">
    <div class="facetContainer">
      <search-facet-options />
    </div>
    <div class="resultContainer">
      <h2>Results</h2><p>Found <span class="highlightText">{{ results.numFound }}</span> entries matching <span class="highlightText">{{ query }}</span></p>
      <div v-if="results && results !== {}" class="results">
        <component :is="SingleEntryComponent(result.type)"
                   v-for="(result, index) in results.docs"
                   :key="index"
                   :result="result" />
      </div>
    </div>
    <div class="marginContainer" />
  </div>
</template>

<script>
import { mapState, mapActions } from 'vuex'
import SearchFacetOptions from './SearchFacetOptions.vue'

export default {
  name: 'SearchResult',
  components: {
    SearchSingleItemDefault: () => import('./SearchSingleItemComponents/SearchSingleItemTypes/SearchSingleItemDefault'),
    SearchSingleItemTweet: () => import('./SearchSingleItemComponents/SearchSingleItemTypes/SearchSingleItemTweet'),
    SearchSingleItemWeb: () => import('./SearchSingleItemComponents/SearchSingleItemTypes/SearchSingleItemWeb'),
    SearchSingleItemImage: () => import('./SearchSingleItemComponents/SearchSingleItemTypes/SearchSingleItemImage'),
    SearchFacetOptions
  },
  data () {
    return {     
    }
  },
  computed: {
    ...mapState({
      query: state => state.Search.query,
      results: state => state.Search.results,
    }),
  },
  mounted () {
  },
  
  methods: {
    ...mapActions('Search', {
      requestSearch: 'requestSearch',
    }),
    SingleEntryComponent(type) {
      switch(type) {   
        case 'Web Page': return 'SearchSingleItemWeb'
        case 'Image': return 'SearchSingleItemImage'
        case 'Twitter Tweet': return 'SearchSingleItemTweet'
        default: return 'SearchSingleItemDefault'
      }
    }
  }
}
</script>

    
