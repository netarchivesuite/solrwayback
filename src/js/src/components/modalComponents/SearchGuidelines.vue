<template>
  <div class="searchGuidelinesContainer">
    <div v-html="searchGuidelines" />
  </div>
</template>

<script>

import { requestService } from '../../services/RequestService'

export default {
  name: 'SearchGuidelines',
  data: () => ({
        searchGuidelines:'',
  }),
  mounted () {
    this.getSearchGuidelinesFromService()
  },
  methods: {
    getSearchGuidelinesFromService() {
      requestService.getSearchGuidelines().then(result => this.searchGuidelines = result, error => (this.searchGuidelines, console.log('No guidelines found.')))
    },
  }
}

</script>

    
