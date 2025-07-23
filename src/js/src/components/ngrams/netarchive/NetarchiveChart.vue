<template>
  <div>
    <div class="chart-container">
      <button v-if="this.ngramStore.datasets.length > 0" class="download" @click="downloadOpen = !downloadOpen">
        DOWNLOAD
      </button>
      <line-chart v-if="this.ngramStore.datasets.length > 0"
                  :chart-data="datacollection"
                  :options="options"
                  :chart-id="'netarchive-chart'"
                  :height="150" />
    </div>
    <div v-if="downloadOpen">
      <export-data @close-exporter="downloadOpen = false" />
    </div>
  </div>
</template>

<script>
  
  import LineChart from '../chartsCore/chartEngines/LineChart'
  import ChartHelpers from '../chartsCore/chartHelpers'
  import ExportData from '../exporterCSV/ExportData.vue'
  // import {mapState} from 'vuex'
  import { mapStores } from 'pinia'
import { useNgramStore } from '../../../store/ngram.store'

  export default {
    name: 'NetarchiveChart',
    components: {
      LineChart,
      ExportData
    },

    data() {
      return {
        datacollection: {},
        downloadOpen: false,
        options: ChartHelpers.getChartOptions(null, this.scale)
       }
    },

    computed: {
    // ...mapState({
    //   query: state => state.Ngram.query,
    //   datasets: state => state.Ngram.datasets,
    //   searchType:state => state.Ngram.searchType,
    //   labels: state => state.Ngram.labels,
    //   scale: state => state.Ngram.timeScale
    // })
    ...mapStores(useNgramStore)
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
  @use '../../../assets/styles/charts.scss'; 

  .download {
    border: 2px solid var(--secondary-bg-color);
    border-radius: 0;
    background-color: white;
    color: var(--secondary-bg-color);
    padding: 5px 10px 5px 10px;
    box-shadow: 0px 3px 6px 0px rgba(0, 0, 0, 0.25);
    cursor: pointer;
    box-sizing: border-box;
    transition: all 0.2s linear 0s;
    text-align:center;
    width:fit-content;
    position: relative;
    left: 50%;
    transform: translateX(-50%);
  }

  .download:hover {
    border: 2px solid white;
    background-color: var(--secondary-bg-color);
    color: white;



  }

</style>



