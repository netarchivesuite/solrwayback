<template>
  <div class="searchBoxContainer">
    <form class="searchForm ngram" @submit.prevent="submitSearch">
      <input id="query"
             v-model="searchQuery"
             type="text"
             autofocus
             :placeholder="getPlaceholder()">
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
      <div class="searchChoices">
        <div class="searchTypeContainer contain">
          <label class="linkGraphLabel label">Search in:</label>
          <input id="searchTypeRadioOne"
                 v-model="searchType"
                 type="radio"
                 value="text">
          <label class="label" for="searchTypeRadioOne">Text in HTML-pages</label>
          <input id="searchTypeRadioTwo"
                 v-model="searchType"
                 type="radio"
                 value="tags">
          <label class="label" for="searchTypeRadioTwo">HTML-tags in HTML-pages</label>
        </div>
        <span v-show="datasets.length !== 0" class="exportModalTrigger" @click.prevent="toggleExporter()">
          Export graph data
        </span>
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
        showExporter:false,
        searchType:'text'
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
    
    searchType: function (val){
      this.resetSearchState()
      this.setSearchType(val)
    }
  },

  beforeDestroy() {
        this.resetSearchState()
  },
  
  methods: {
    ...mapActions('Ngram', {
      resetSearchState:'resetState',
      doSearch:'doSearch',
      setSearchType:'setSearchType'
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
          this.doSearch({query:this.searchQuery, searchType:this.searchType})
      }
    },

    getPlaceholder() {
      return  this.searchType === 'tags' ? 'Search for a HTML tag without < >' : 'Enter search term'
    },

    resetState() {
      this.resetSearchState()
    },
  
    toggleExporter() {
       this.showExporter = !this.showExporter
    }
    
  }
  
}

</script>

    