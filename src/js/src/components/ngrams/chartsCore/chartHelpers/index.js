/**
 * Helpers for charting.
 * 
 */

import ChartOptionsCore from '../../chartsCore/chartConfigs/chartOptionsCore'

export default {
  
  /**
  * Generate labels for chart.
  *  - Override with own label config as you see fit -
  */
  getChartLabels: (labels, scale) => {
   return  ChartOptionsCore.getChartLabels(labels, scale)
  },

  /**
  * Generate options for chart.
  * - Override with own options config as you see fit -
  */
  getChartOptions: (searchType, scale) => {
   return ChartOptionsCore.getChartOptions(searchType, scale)
  },

  /**
  * Generate dataset for chart.
  * - Override with own data set config as you see fit -
  */
  getChartDataSet(rawData) {
   return ChartOptionsCore.getChartDataSet(rawData)
  }

}



