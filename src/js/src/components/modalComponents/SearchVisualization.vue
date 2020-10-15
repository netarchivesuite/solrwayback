<template>
  <div class="searchVizualisationContainer">
    <div> THIS STUFF</div>
  </div>
</template>

<script>

import { requestService } from '../../services/RequestService'

export default {
  name: 'SearchGuidelines',
  data: () => ({
    dataVizualisation:'',
  }),
  mounted () {
    this.getSearchVizualisationFromService(query)
  },
  methods: {
    getSearchVizualisationFromService(query) {
      requestService.getSearchVizualisation(query).then(result => this.data = result, error => (this.dataVizualisation = 'No data found', console.log('No data found.')))
    },
  }
}

</script>

    
