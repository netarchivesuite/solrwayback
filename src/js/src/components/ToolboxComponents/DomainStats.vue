<template>
  <div class="domainStatsContainer">
    <h2 class="toolboxHeadline">
      Domain stats
    </h2>
    <div class="domainContentContainer">
      <div class="domainContentSettings">
        <input v-model="domain"
               placeholder="Enter domain, like 'kb.dk'"
               :class="$_checkDomain(domain) ? '' : 'urlNotTrue'"
               @keyup.enter="showCurrentChartType(domain)">
        <time-period-refiner ref="refiner"
                             class="refiner"
                             @startdate="(sdate) => startDate = sdate"
                             @enddate="(edate) => endDate = edate" />
        <div class="generateButtonContainer contain">
          <button :disabled="loading" class="domainStatsButton" @click.prevent="showCurrentChartType(domain)">
            Generate
          </button>
        
          <!-- Toggle Button -->
          <button class="toggleViewButton" @click="toggleChartView">
            {{ showCombinedChart ? 'Show Individual Charts' : 'Show Combined Chart' }}
          </button>
        </div>
      </div>
      <div v-if="loading" class="spinner" />
      <!-- Combined Chart -->
      <div v-show="!loading && rawData && showCombinedChart" id="lineContainer">
        <canvas id="line-chart" width="800" height="450"></canvas>
      </div>
      <!-- Individual Charts -->
      <div v-show="!loading && rawData && !showCombinedChart" id="individualChartsContainer">
        <div class="chartWrapper">
          <canvas id="size-chart" width="400" height="300"></canvas>
        </div>
        <div class="chartWrapper">
          <canvas id="pages-chart" width="400" height="300"></canvas>
        </div>
        <div class="chartWrapper">
          <canvas id="links-chart" width="400" height="300"></canvas>
        </div>
        <div class="chartWrapper">
          <canvas id="textsize-chart" width="400" height="300"></canvas>
        </div>
      </div>
      <div v-if="rawData !== null && !loading" id="tableContainer">
        <table id="domainGrowthTable">
          <thead>
            <tr>
              <th />
              <th v-for="(item, index) in rawData" :key="index">
                {{ $_displayDate(item.date, timeScale) }}
              </th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>Size in kilobytes</td>
              <td v-for="(item, index) in rawData" :key="index">
                {{ item.sizeInKb.toLocaleString("en") }}
              </td>
            </tr>
            <tr>
              <td>Pages</td>
              <td v-for="(item, index) in rawData" :key="index">
                {{ item.totalPages.toLocaleString("en") }}
              </td>
            </tr>
            <tr>
              <td>Ingoing links</td>
              <td v-for="(item, index) in rawData" :key="index">
                {{ item.ingoingLinks.toLocaleString("en") }}
              </td>
            </tr>
            <tr>
              <td>Avg. page size in characters</td>
              <td v-for="(item, index) in rawData" :key="index">
                {{ item.contentTextLength.toLocaleString("en") }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script>

import { mapActions } from 'vuex'
import { requestService } from '../../services/RequestService'
import domainScript from './ToolboxResources/domainStats'
import StringManipulationUtils from './../../mixins/StringManipulationUtils'
import TimePeriodRefiner from './../TimePeriodRefiner.vue'

export default {
  name: 'DomainStats',
  components: {
    TimePeriodRefiner
  },
  mixins: [StringManipulationUtils],

  data() {
    return {
      domain:'',
      loading: false,
      rawData:null,
      graphData:{
        chartLabels:[],
        sizeInKb:[],
        ingoingLinks:[],
        numberOfPages:[],
        textSize:[]
      },
      startDate:'',
      endDate:'',
      timeScale:'',
      showCombinedChart: true
    }
  },

  methods: {
    ...mapActions('Notifier', {
      setNotification: 'setNotification'
    }),
    loadGraphData(domain) {
      this.graphData = {
        chartLabels:[],
        sizeInKb:[],
        ingoingLinks:[],
        numberOfPages:[],
        textSize:[]
      }
      // this.rawData = null
      this.loading = true
      this.timeScale = this.$refs.refiner.timeScaleInput
      requestService.getDomainStatistics(this.prepareDomainForGetRequest(),this.startDate, this.endDate, this.timeScale)
        .then(result => this.sanitizeResponseData(result))
        .then(this.loading = false)
        .catch(error => {
              this.loading = false
              this.setNotification({
                title: 'We are so sorry!',
                text: 'Search could not be performed.',
                type: 'error',
                srvMessage: error.response != undefined ? error.response.data : error,
                timeout: false
              })
            })
    },
    prepareDomainForGetRequest() {
      let preparedDomain = this.domain
      preparedDomain = preparedDomain.replace(/http.*:\/\//i,'').trim() //Get domain from URL, using replace and regex to trim domain
      preparedDomain = preparedDomain.replace(/www./i,'') //Get domain from URL, using replace and regex to trim domain
      if( preparedDomain.slice(-1) === '/'){ // if trailing slash on domain it's removed
        preparedDomain = preparedDomain.slice(0, -1)
      }
      return preparedDomain
    },
    sanitizeResponseData(data) {
      this.rawData = data
      for(let i = 0; i < data.length; i++){
        this.graphData.chartLabels.push(this.$_displayDate(data[i].date, this.timeScale))
        this.graphData.sizeInKb.push(data[i].sizeInKb)
        this.graphData.ingoingLinks.push(data[i].ingoingLinks)
        this.graphData.numberOfPages.push(data[i].totalPages)
        
        // Handle missing contentTextLength
        if (data[i].contentTextLength !== undefined && data[i].contentTextLength !== null) {
          this.graphData.textSize.push(data[i].contentTextLength)
        } else {
          this.graphData.textSize.push(0) // Fallback value
        }
      }

      console.log(this.graphData.textSize)
    },
    toggleChartView() {
      this.showCombinedChart = !this.showCombinedChart
      this.renderCurrentlyActiveChart()
    },
    showCurrentChartType(domain) {
      console.log('rawData1', this.rawData)

      this.loadGraphData(domain)
      console.log('rawData2', this.rawData)

      this.renderCurrentlyActiveChart()

      console.log('rawData3', this.rawData)
      this.loading = false
    },
    renderCurrentlyActiveChart() {
      if (this.showCombinedChart) {
        this.renderCombinedChart()
      } else {
        this.renderIndividualCharts()
      }
    },
    renderIndividualCharts() {
      domainScript.drawIndividualCharts(this.graphData.chartLabels, this.graphData.sizeInKb,
                                        this.graphData.numberOfPages, this.graphData.ingoingLinks,
                                        this.graphData.textSize)
    },
    renderCombinedChart() {
      domainScript.drawChart(this.graphData.chartLabels, this.graphData.sizeInKb,
                             this.graphData.numberOfPages, this.graphData.ingoingLinks,
                             this.graphData.textSize)
    }
  }
}
</script>
