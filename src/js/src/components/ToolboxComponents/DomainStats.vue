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
      <button :disabled="loadingGraphs" class="domainStatsButton" @click.prevent="loadGraphData(domain)">
        Generate
      </button>
    </div>
  </div>
</template>

<script>

import { requestService } from '../../services/RequestService'

export default {
  name: 'DomainStats',
  data() {
    return {
      domain:'',
      loadingGraphs:false
    }
  },
  mounted () {
    requestService.getDomainStatistics('ekot.dk').then(result => console.log(result), error => console.log('No information found about this archive.'))
  },
  methods: {
    loadGraphData(domain) {

    },
    checkDomain(domain) {
      return true
    },
    prepareDomainForGetRequest() {
      let preparedDomain = this.domain
      preparedDomain = preparedDomain.replace(/http.*:\/\//i,'').trim() //Get domain from URL, using replace and regex to trim domain
      preparedDomain = preparedDomain.replace(/www./i,'') //Get domain from URL, using replace and regex to trim domain
      if( preparedDomain.slice(-1) === '/'){ // if trailing slash on domain it's removed
        preparedDomain = preparedDomain.slice(0, -1)
      }
      return preparedDomain
    }
  }
}
</script>
