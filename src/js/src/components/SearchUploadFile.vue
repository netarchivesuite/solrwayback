<template>
  <div class="fileUploader">
    <div class="fileSelector">
      <input
        ref="file"
        type="file"
        @change="selectFileToUpload">
    </div>
    <button class="fileSelectButton" :disabled="fileToUpload.length === 0" @click="uploadFile">
      Search with file
    </button>
  </div>
</template>

<script>
// import { mapState, mapActions } from 'vuex'
import { mapStores, mapActions } from 'pinia'
import { useSearchStore } from '../store/search.store'
import { useNotifierStore } from '../store/notifier.store'
import { requestService } from '../services/RequestService'
import HistoryRoutingUtils from './../mixins/HistoryRoutingUtils'

export default {
  name: 'SearchFileUpload',
  mixins: [HistoryRoutingUtils],
  data() {
    return {
      fileToUpload: [],
    }
  },
  computed: {
    // ...mapState({
    //     searchAppliedFacets: state => state.Search.searchAppliedFacets,
    //     solrSettings: state => state.Search.solrSettings
    // })
    ...mapStores(useSearchStore)
  },
  methods: {
    ...mapActions(useSearchStore, {
      requestSearch: 'requestSearch',
       updateQuery: 'updateQuery',
    }),

    ...mapActions(useNotifierStore, {
      setNotification: 'setNotification'
     
    }),

  selectFileToUpload() {
      this.fileToUpload = this.$refs.file.files
    },

  uploadFile(){
     //TODO spinner
     requestService.uploadFileRequest(this.fileToUpload[0])
        .then(response => {
         this.updateQuery(this.createRequestQuery(response.data))
         this.$_pushSearchHistory('Search', this.createRequestQuery(response.data), this.searchAppliedFacets, this.solrSettings)
        })
        .catch((error) => {
          this.setNotification({
          	title: 'We are so sorry!',
            text: 'Something went wrong when uploading your image - please try again',
            type: 'error',
            srvMessage: error,
            timeout: false
          })
          this.fileToUpload = []
        })
    
  },

  createRequestQuery(sha1) {
    return `hash:"${sha1}"`
  }
  }
  
}
</script>
