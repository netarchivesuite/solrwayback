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
               @keyup.enter="loadGraphData(domain)">
        <time-period-refiner ref="refiner"
                             class="refiner"
                             @startdate="(sdate) => startDate = sdate"
                             @enddate="(edate) => endDate = edate" />
        <div class="generateButtonContainer contain">
          <button :disabled="loading" class="domainStatsButton" @click.prevent="loadGraphData(domain)">
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

// import { mapActions } from 'vuex'
import { mapActions } from 'pinia'
import { useNotifierStore } from '../../store/notifier.store'
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
    ...mapActions(useNotifierStore, {
      setNotification: 'setNotification'
    }),

    /**
     * Loads and processes graph data for the specified domain.
     * This method fetches relevant statistics or metrics associated with the given domain
     * and prepares the data for visualization in the graph component.
     *
     * @param {string} domain - The domain for which to load graph data.
     */
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
        .then(result => {
          this.sanitizeResponseData(result)
          this.loading = false // <-- Move this line up
          this.$nextTick(() => { // <-- Ensure DOM is updated
             this.renderCurrentlyActiveChart()
          })
        })
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

    /**
     * Prepares the domain string for use in a GET request.
     * This method encodes or formats the domain as needed
     * to ensure it is safe and valid for inclusion in a URL.
     *
     * @returns {string} The processed domain string ready for a GET request.
     */
    prepareDomainForGetRequest() {
      let preparedDomain = this.domain
      preparedDomain = preparedDomain.replace(/http.*:\/\//i,'').trim() //Get domain from URL, using replace and regex to trim domain
      preparedDomain = preparedDomain.replace(/www./i,'') //Get domain from URL, using replace and regex to trim domain
      if( preparedDomain.slice(-1) === '/'){ // if trailing slash on domain it's removed
        preparedDomain = preparedDomain.slice(0, -1)
      }
      return preparedDomain
    },

    /**
     * Sanitizes the response data received from the server and saves it in the graphData object of this class.
     * This method processes the input `data` to remove or transform any
     * unwanted or unsafe content before it is used within the component.
     *
     * @param {Object} data - The raw response data to be sanitized.
     */
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
    },

    /**
     * Toggles between the two different chart views in the domain statistics component.
     * It switches the display mode (e.g., between a combined chart and multiple charts).
     * Called when the user interacts with the chart view toggle button.
     */
    toggleChartView() {
      this.showCombinedChart = !this.showCombinedChart
      this.renderCurrentlyActiveChart()
    },

    /**
     * Renders the chart that is currently active based on the user's selection or state.
     * Determines which chart component or visualization to display and returns the corresponding chart.
     * This method is typically used within the component's render logic to dynamically update the displayed chart.
     */
    renderCurrentlyActiveChart() {
      if (this.showCombinedChart) {
        this.renderCombinedChart()
      } else {
        this.renderIndividualCharts()
      }
    },

    /**
     * Renders individual charts for domain statistics.
     * This method is responsible for generating and displaying separate chart components
     * based on the domain statistics data.
     */
    renderIndividualCharts() {
      domainScript.drawIndividualCharts(this.graphData.chartLabels, this.graphData.sizeInKb,
                                        this.graphData.numberOfPages, this.graphData.ingoingLinks,
                                        this.graphData.textSize)
    },

    /**
     * Renders a combined chart displaying domain statistics.
     * This method is responsible for generating and displaying the chart
     * that visualizes the aggregated data for the selected domain.
     */
    renderCombinedChart() {
      domainScript.drawCombinedChart(this.graphData.chartLabels, this.graphData.sizeInKb,
                             this.graphData.numberOfPages, this.graphData.ingoingLinks,
                             this.graphData.textSize)
    }
  }
}
</script>
