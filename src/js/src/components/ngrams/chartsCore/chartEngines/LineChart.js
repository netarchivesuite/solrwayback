import { Line, mixins } from 'vue-chartjs'
import SearchHelper from '../../searchHelper'
const { reactiveProp } = mixins

export default {
  extends: Line,
  mixins: [reactiveProp, SearchHelper],
  props: {
    chartData: {
      type: Object,
      default: null
    },
    options: {
      type: Object,
      default: null
    }
  },
  mounted () {
    // this.chartData is created in the mixin.
    // If you want to pass options please create a local options object
    this.renderChart(this.chartData,  this.options)
  },

  watch: {
    options () {
       this.renderChart(this.chartData, this.options)
    }
  },

  beforeDestroy() {
    this._data._chart.destroy()
},

}
