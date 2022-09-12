<template>
  <div class="searchGuidelinesContainer">
    <div v-html="collectionInfo" />
  </div>
</template>

<script>

import { requestService } from '../../services/RequestService'

export default {
  name: 'CollectionInfo',
  data: () => ({
        collectionInfo:''
  }),
  mounted () {
    this.getCollectionInfoFromService()
  },
  methods: {
    getCollectionInfoFromService() {
      requestService.getCollectionInfo().then(result => this.collectionInfo = result, error => (this.collectinoInfo, console.log('No collection info found.')))
    },
  }
}

</script>