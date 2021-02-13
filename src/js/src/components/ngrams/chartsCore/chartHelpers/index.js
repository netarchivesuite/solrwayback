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
  getChartLabels: () => {
   return  ChartOptionsCore.getChartLabels()
  },

  /**
  * Generate options for chart.
  * - Override with own options config as you see fit -
  */
  getChartOptions: (searchType) => {
   return ChartOptionsCore.getChartOptions(searchType)
  },

  /**
  * Generate dataset for chart.
  * - Override with own data set config as you see fit -
  */
  getChartDataSet(rawData) {
   return ChartOptionsCore.getChartDataSet(rawData)
  }

}



