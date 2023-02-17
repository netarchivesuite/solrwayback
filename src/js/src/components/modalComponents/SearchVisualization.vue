<template>
  <div class="searchVisualizationContainer">
    <div class="searchVisualizationHeadline">
      <h2>Visualization of search result by domain</h2>
      <p>
        query: <span>{{ query }}</span>
      </p>
      <p v-if="searchAppliedFacets.length > 0">
        filters: <span>{{ searchAppliedFacets.join('') }}</span>
      </p>
      <div class="searchVisualizationSettings">
        <time-period-refiner @startdate="(sdate) => startDate = sdate"
                             @enddate="(edate) => endDate = edate" 
                             @timescale="(ts) => timeScale = ts" />
        <div class="generateButtonContainer contain">
          <button class="searchVisualizationButton" @click.prevent="loadVisualisation()">
            Generate
          </button>
        </div>
      </div>
    </div>
    <div v-show="!loading" key="d3-viz" class="visualized" />
    <div v-if="loading" key="spinner-viz" class="spinner" />
  </div>
</template>

<script>

import { mapState } from 'vuex'
import Visualization from './Visualization'
import TimePeriodRefiner from './../TimePeriodRefiner.vue'

export default {
  name: 'SearchVisualization',
  components: {
    TimePeriodRefiner
  },
  data: () => ({
    loading:false,
    startDate:'',
    endDate:'',
    timeScale:''
  }),
  computed: {
    ...mapState({
      query: state => state.Search.query,
      searchAppliedFacets: state => state.Search.searchAppliedFacets,
      solrSettings: state => state.Search.solrSettings,
    })
  },
// For test purposes
 /* watch: {
    loading: function(val) {
      console.log('LOADING CHANGED!')
    }
  }, */
  mounted () {
    this.loading = true
    Visualization.createVisualization(this.query, this.searchAppliedFacets, this.solrSettings, this.startDate, this.endDate, this.timeScale).then(() =>{
    this.loading = false
    })
  },
  methods: {
    loadVisualisation() {
      this.loading = true
      Visualization.createVisualization(this.query, this.searchAppliedFacets, this.solrSettings, this.startDate, this.endDate, this.timeScale) .then(() =>{
      this.loading = false
      })
    }
  }
}

</script>

    
