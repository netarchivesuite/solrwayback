<template>
  <div class="domainStatsContainer">
    <h2 class="toolboxHeadline">
      Domain stats
    </h2>
    <div class="domainContentContainer">
      <input v-model="domain"
             placeholder="Enter domain, like 'kb.dk'"
             :class="checkDomain(domain) ? '' : 'urlNotTrue'"
             @keyup.enter="loadGraphData(domain)">
      <button :disabled="loading" class="domainStatsButton" @click.prevent="loadGraphData(domain)">
        Generate
      </button>
      <div v-if="loading" class="spinner" />
      <div v-show="!loading && rawData" id="lineContainer">
        <canvas
          id="line-chart"
          width="800"
          height="450" />
      </div>
      <div v-if="rawData !== null && !loading" id="tableContainer">
        <table id="domainGrowthTable">
          <thead>
            <tr>
              <th />
              <th v-for="(item, index) in rawData" :key="index">
                {{ item.year }}
              </th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>Size in KB</td>
              <td v-for="(item, index) in rawData" :key="index">
                {{ item.sizeInKb.toLocaleString("en") }}
              </td>
            </tr>
            <tr>
              <td>Total pages</td>
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
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script>

import { requestService } from '../../services/RequestService'
import domainScript from './ToolboxResources/domainStats'
import StringManipulationUtils from './../../mixins/StringManipulationUtils'

export default {
  name: 'DomainStats',

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
        numberOfPages:[]
      }
    }
  },
  methods: {
    loadGraphData(domain) {
      this.graphData = {
        chartLabels:[],
        sizeInKb:[],
        ingoingLinks:[],
        numberOfPages:[]
      }
      this.rawData = null
      this.loading = true
      requestService.getDomainStatistics(this.prepareDomainForGetRequest()).then(result => (this.sanitizeResponseDataAndDrawChart(result), this.rawData = result), error => console.log('No information found about this archive.'))
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
    sanitizeResponseDataAndDrawChart(data) {
      for(let i = 0; i < data.length; i++){
        this.graphData.chartLabels.push(data[i].year)
        this.graphData.sizeInKb.push(data[i].sizeInKb)
        this.graphData.ingoingLinks.push(data[i].ingoingLinks)
        this.graphData.numberOfPages.push(data[i].totalPages)
      }
      domainScript.drawChart(this.graphData.chartLabels, this.graphData.sizeInKb, this.graphData.numberOfPages, this.graphData.ingoingLinks)
      this.loading = false
    }
    
  }
}
</script>
