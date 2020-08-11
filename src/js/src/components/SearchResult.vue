<template>
  <div class="resultContainer" v-if="Object.keys(results).length > 0">
  <p>Found <span class="highlightText">{{ results.numFound }}</span> entries matching <span class="highlightText">{{ query }}</span></p>
    <div v-if="results && results !== {}" class="results">
      <component v-bind:key="index" v-for="(result, index) in results.docs" :is="SingleEntryComponent(result.type)"  :result="result" ></component>
    </div>
  </div>
</template>

<script>
import { mapState, mapActions } from 'vuex'

export default {
  name: "SearchResult",
  components: {
    SingleEntryDefault: () => import("./SingleEntryComponents/SingleEntryTypes/SingleEntryDefault"),
    SingleEntryTweet: () => import("./SingleEntryComponents/SingleEntryTypes/SingleEntryTweet"),
    SingleEntryWeb: () => import("./SingleEntryComponents/SingleEntryTypes/SingleEntryWeb"),
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

    
