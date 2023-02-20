<template>
  <div class="searchBoxContainer">
    <div v-if="searcBoxClass() === 'urlNotTrue' && searchType === 'tags'" class="badTagQueryNotice">
      You don't need <span v-if="searchQuery.includes('<')" class="queryErrorColor">&lt;</span><span v-if="searchQuery.includes('>')" class="queryErrorColor">&gt;</span> when searching for tags
    </div>
    <form class="searchForm ngram" @submit.prevent="submitSearch">
      <input id="query"
             v-model="searchQuery"
             class="ngramQuery"
             type="text"
             :class="searchType === 'tags' ? searcBoxClass() : ''"
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
          <label class="linkGraphLabel label">Search for:</label>
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
      </div>
      <time-period-refiner ref="refiner"
                           class="searchBoxRefiner"
                           @startdate="(sdate) => startDate = sdate"
                           @enddate="(edate) => endDate = edate" 
                           @timescale="(ts) => timeScale = ts" />
    </form>
    <div v-if="searchQuery === '' || datasets.length === 0">
      <h1><span class="ngramAboutHeaderStart">Visualization</span> of search query overtime</h1>

      <p class="ngramAbout">
        The graph shows how frequently the query appears in webpages in the corpus relative for the defined period and at a defined time scale.
        Mouse over on the graph will show number of hits and total number of documents for a year, month, week or day.
      </p>
      <p class="ngramAbout">
        Clicking on a point of the graph will open a search and show the matching results.
      </p> 
      <p class="ngramAbout">
        It is possible to run multiple queries, one after the other, to compare results.
        Changing between Text and HTML-tags in webpages or changing the time frame and time scale will reset the previous results.
      </p> 
      <p class="ngramAbout">
        The data for the graph can be exported as a CSV file.
      </p>
    </div>
  </div>
</template> 

<script>
import { mapState, mapActions } from 'vuex'
import TimePeriodRefiner from './../../TimePeriodRefiner.vue'
export default {
  name: 'SearchBox',
   components: {
      TimePeriodRefiner
  },
 
  data () {
    return {    
        searchQuery:'',
        searchType:'text',
        startDate:'',
        endDate:'',
        timeScale:'',
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
      this.resetState()
      this.searchType  = val
      this.setSearchType(val)
    },
    startDate : function (){this.resetResults()},
    endDate : function (){this.resetResults()},
    timeScale : function (){this.resetResults()}
  },
  beforeDestroy() {
        this.resetSearchState()
  },
  
  methods: {
    ...mapActions('Ngram', {
      resetSearchState:'resetState',
      doSearch:'doSearch',
      setSearchType:'setSearchType',
      updateQuery:'updateQuery'
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
          if (this.searchType === 'tags') {
            this.rinseQuery()
          }
          this.doSearch({query:this.searchQuery, searchType:this.searchType, startDate:this.startDate, endDate:this.endDate, timeScale:this.timeScale})
          
      }
    },
    getPlaceholder() {
      return  this.searchType === 'tags' ? 'Search for a HTML tag without < >' : 'Enter search term'
    },
    resetState() {
      this.resetSearchState()
      this.$refs.refiner.resetAll()
    },
    resetResults(){
      var oldQuery = this.query
      if (this.datasets.length != 0) {
        this.resetSearchState()
      }
      this.updateQuery(oldQuery)
    },
    searcBoxClass() {
      return this.searchQuery.includes('<') || this.searchQuery.includes('>') ?  'urlNotTrue' : ''
    },
    rinseQuery() {
      this.searchQuery = this.searchQuery.replace(/[<>]/g, '')

    }
    
  }
  
}
</script>