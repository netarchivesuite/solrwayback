<template>
  <div class="resultAndFacetContainer" v-if="Object.keys(results).length > 0">
    <div class="facetContainer">
      <search-facet-options />
    </div>
    <div class="resultContainer">
      <h2>Results</h2><p>Found <span class="highlightText">{{ results.numFound }}</span> entries matching <span class="highlightText">{{ query }}</span></p>
      <div v-if="results && results !== {}" class="results">
        <component :key="index" v-for="(result, index) in results.docs" :is="SingleEntryComponent(result.type)"  :result="result" ></component>
      </div>
    </div>
    <div class="marginContainer">

    </div>
  </div>
</template>

<script>
import { mapState, mapActions } from 'vuex'
import SearchFacetOptions from "./SearchFacetOptions.vue";

export default {
  name: "SearchResult",
  components: {
    SingleEntryDefault: () => import("./SearchSingleItemComponents/SearchSingleItemTypes/SearchSingleItemDefault"),
    SingleEntryTweet: () => import("./SearchSingleItemComponents/SearchSingleItemTypes/SearchSingleItemTweet"),
    SingleEntryWeb: () => import("./SearchSingleItemComponents/SearchSingleItemTypes/SearchSingleItemWeb"),
    SearchFacetOptions
  },
  data () {
    return {     
    }
  },
  computed: {
    ...mapState({
      query: state => state.searchStore.query,
      results: state => state.searchStore.results,
    }),
  },
  mounted () {
  },
  
  methods: {
    ...mapActions('searchStore', {
      search: 'search',
    }),
    SingleEntryComponent(type) {
      switch(type) {   
        case "Web Page": return "SingleEntryWeb";
        case "Twitter Tweet": return "SingleEntryTweet";
        default: return "SingleEntryDefault";
      }
    }
  }
}
</script>

    
