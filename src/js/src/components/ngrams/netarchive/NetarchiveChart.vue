<template>
  <div>
    <div class="chart-container">
      <line-chart v-if="datasets.length > 0"
                  :chart-data="datacollection"
                  :options="options"
                  :chart-id="'netarchive-chart'"
                  :height="150" />
    </div>
  </div>
</template>

<script>
  
  import LineChart from '../chartsCore/chartEngines/LineChart'
  import ChartHelpers from '../chartsCore/chartHelpers'
  import {mapState} from 'vuex'

  export default {
    name: 'NetarchiveChart',
    components: {
      LineChart
    },

    data() {
      return {
        datacollection: {},
        options: ChartHelpers.getChartOptions(null, this.scale)
       }
    },

    computed: {
    ...mapState({
      query: state => state.Ngram.query,
      datasets: state => state.Ngram.datasets,
      searchType:state => state.Ngram.searchType,
      labels: state => state.Ngram.labels,
      scale: state => state.Ngram.timeScale
    })
    },

    watch: {
      datasets: function (newVal) {
      this.fillData(newVal)
      },
      
      searchType: function (newVal) {
        this.options = ChartHelpers.getChartOptions(newVal, this.scale)
      },
      scale: function (newVal) {
        this.options = ChartHelpers.getChartOptions(this.searchType, this.scale)
      }
    },
    
    methods: {
      fillData () {
        this.datacollection = {
          rawLabels: this.labels,
          labels: ChartHelpers.getChartLabels(this.labels, this.scale),
          datasets: ChartHelpers.getChartDataSet(this.datasets)
        }
      }
    }
  }
</script>

<style lang="scss">
  @import '../../../assets/styles/charts.scss'; 
</style>



