import { Line, mixins } from 'vue-chartjs'
import SearchHelper from '../../searchHelper'
const { reactiveProp } = mixins

export default {
  extends: Line,
  mixins: [reactiveProp, SearchHelper],
  props: ['options'],
  mounted () {
    // this.chartData is created in the mixin.
    // If you want to pass options please create a local options object
    this.renderChart(this.chartData, this.getMergedOptions() )
  },

  methods: {
      
      pointCallBack(evt) {
       const chartInstance = this.$data._chart  
       this.$_handleSearch(chartInstance, evt)
      },

      getMergedOptions(){
         return this.options['onClick'] = this.pointCallBack
      }
  }

}
