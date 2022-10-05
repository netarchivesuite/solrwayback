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
        options: ChartHelpers.getChartOptions()
       }
    },

    computed: {
    ...mapState({
      query: state => state.Ngram.query,
     datasets: state => state.Ngram.datasets,
     searchType:state => state.Ngram.searchType
    })
    },

    watch: {
      datasets: function (newVal) {
      this.fillData(newVal)
      },
      
      searchType: function (newVal) {
        this.options = ChartHelpers.getChartOptions(newVal)
      }
    },
    
    methods: {
      fillData () {
        this.datacollection = {
          labels: ChartHelpers.getChartLabels(),
          datasets: ChartHelpers.getChartDataSet(this.datasets)
        }
      }
    }
  }
</script>

<style lang="scss">
  @import '../../../assets/styles/charts.scss'; 
</style>



