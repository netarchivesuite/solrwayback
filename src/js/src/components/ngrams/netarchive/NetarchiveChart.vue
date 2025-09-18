<template>
  <div>
    <div class="chart-container">
      <button
        v-if="ngramStore.datasets.length > 0"
        class="download"
        @click="downloadOpen = !downloadOpen"
      >
        DOWNLOAD
      </button>
      <canvas
        id="ngramChart"
        width="600"
        height="300"
      />
    </div>
    <div v-if="downloadOpen">
      <export-data @close-exporter="downloadOpen = false" />
    </div>
  </div>
</template>

<script>
import { drawSingleLineChart } from '../chartsCore/chartEngines/LineChart';
import ChartHelpers from '../chartsCore/chartHelpers';
import ExportData from '../exporterCSV/ExportData.vue';
import { mapStores } from 'pinia';
import { useNgramStore } from '../../../store/ngram.store';

export default {
  name: 'NetarchiveChart',
  components: { ExportData },
  data() {
    return {
      datacollection: {},
      downloadOpen: false,
      chartInstance: null,
      options: ChartHelpers.getChartOptions(null, this.scale),
    };
  },
  computed: {
    ...mapStores(useNgramStore),
  },
watch: {
  'ngramStore.datasets': {
    handler() {
      this.fillData();
    },
    deep: true
  },
  'ngramStore.searchType'() {
    this.options = ChartHelpers.getChartOptions(this.ngramStore.searchType, this.ngramStore.scale);
    this.redrawChart();
  },
  'ngramStore.scale'() {
    this.options = ChartHelpers.getChartOptions(this.ngramStore.searchType, this.ngramStore.scale);
    this.redrawChart();
  }
},


  mounted() {
    this.fillData();
  },
  methods: {
    fillData() {
      if (!this.ngramStore.datasets.length) return;
      // Prepare datacollection
      this.datacollection = {
        labels: ChartHelpers.getChartLabels(this.ngramStore.labels, this.ngramStore.scale),
        datasets: ChartHelpers.getChartDataSet(this.ngramStore.datasets),
      };
      // Redraw chart whenever data updates
      this.redrawChart();
    },

    redrawChart() {
  if (!this.datacollection || !this.datacollection.datasets.length) return;

  this.chartInstance = drawSingleLineChart(
    'ngramChart',
    this.datacollection.labels,
    this.datacollection.datasets, // <-- pass all datasets now
    'Count'
  );
}

  },
};
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
  text-align: center;
  width: fit-content;
  position: relative;
  left: 50%;
  transform: translateX(-50%);
}

#ngramChart {
  max-height:300px !important;

}
.download:hover {
  border: 2px solid white;
  background-color: var(--secondary-bg-color);
  color: white;
}
</style>
