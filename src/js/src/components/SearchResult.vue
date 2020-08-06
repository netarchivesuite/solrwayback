<template>
  <div class="resultContainer" v-if="Object.keys(results).length > 0">
  <p>Found <span class="highlightText">{{ results.numFound }}</span> entries matching <span class="highlightText">{{ query }}</span></p>
    <div class="results">
      <single-entry v-bind:key="index" v-for="(result, index) in results.docs" :result="result" />
    </div>
  </div>
</template>

<script>
import { mapState, mapActions } from 'vuex'
import SingleEntry from './SingleEntry.vue'


export default {
  name: "SearchResult",
  components: {  
    SingleEntry
  },
  data () {
    return {     
    }
  },
  computed: {
    ...mapState({
      query: state => state.searchStore.query,
      results: state => state.searchStore.results,
    })
  },
  mounted () {
  },
  
  methods: {
    ...mapActions('searchStore', {
      search: 'search',
    }),
  }
}

</script>

    
