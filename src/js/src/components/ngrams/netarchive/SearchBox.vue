<template>
  <div class="searchBoxContainer">
    <form class="searchForm" @submit.prevent="submitSearch">
      <input id="query"
             v-model="searchQuery"
             type="text"
             :class="searcBoxClass()"
             autofocus
             placeholder="Enter search term">
      <button id="querySubmit"
              title="Search"
              type="submit">
        <div id="magnifyingGlass" />
      </button>
      <button v-if="searchQuery !== '' || datasets.length !== 0"
              id="clearSubmit"
              title="Clear search and results"
              type="button"
              @click.prevent="resetState()" />
      <div v-show="datasets.length !== 0" class="exportModalTrigger" @click.prevent="toggleExporter()">
        Export graph data
      </div>
    </form>
    <exporter v-if="showExporter" @close-exporter="toggleExporter()" />
    <div v-if="searchQuery === '' || datasets.length === 0">
      <h1><span class="ngramAboutHeaderStart">Visualization</span> of search query by year</h1>

      <p class="ngramAbout">
        The graph shows how frequently the query appears in webpages in the corpus relative for each year. Mouse over on the graph will show number of hits and total number of documents for that year.
      </p>
      <p class="ngramAbout">
        Clicking on a year will open a search and show how the results found for that year.
      </p> <p>The graph can show multiple queries at the same time for comparison.</p> 
      <p class="ngramAbout">
        The data for the graph can be exported as a CSV file.
      </p>
    </div>
  </div>
</template> 

<script>
import { mapState, mapActions } from 'vuex'
import Exporter from '../exporterCSV/ExportData'

export default {
  name: 'SearchBox',
   components: {
      Exporter
  },
 
  data () {
    return {    
        searchQuery:'',
        showExporter:false
    }
  },
  
  computed: {
    ...mapState({
     query: state => state.Ngram.query,
     datasets: state => state.Ngram.datasets,
     loading: state => state.Ngram.loading,
     datasetQueries: state => state.Ngram.datasetQueries
    })
  },
  
  watch: {
    query: function (val) {
      this.searchQuery  = val
    },
  },

  beforeDestroy() {
        this.resetSearchState()
  },
  
  methods: {
    ...mapActions('Ngram', {
      resetSearchState:'resetState',
      doSearch:'doSearch'
    }),

    ...mapActions('Notifier', {
      setNotification: 'setNotification'
     
    }),

    submitSearch() {
      if (this.datasetQueries.includes(this.searchQuery.toLowerCase())) {
         this.setNotification({
          	title: `Sorry - you have already searched for ${this.searchQuery}`,
            text: this.searchQuery.toLowerCase() === 'tenebrous horse' ? 'Try a new and exciting one - so many queries out there' : 'Try a new and exciting one like "tenebrous horse"',
            type: 'error',
            timeout: false
          })
      } else {
          this.doSearch(this.searchQuery)
      }
    },

    resetState() {
      this.resetSearchState()
    },
    toggleExporter() {
       this.showExporter = !this.showExporter
    },
    searcBoxClass() {
      return this.searchQuery.includes('<') || this.searchQuery.includes('>') ?  'urlNotTrue' : ''
    },
    
  }
  
}

</script>

    