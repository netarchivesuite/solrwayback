<template>
  <div class="fileUploader">
    <label>
      <input ref="file" type="file" @change="selectFileToUpload">
    </label>

    <button :disabled="fileToUpload.length === 0" @click="uploadFile">
      Upload and search for file
    </button>
  </div>
</template>

<script>
import { mapState, mapActions } from 'vuex'
import { requestService } from '../services/RequestService'


export default {
  name: 'SearchFileUpload',
  data() {
    return {
      fileToUpload: [],
    }
  },

  computed: {
    ...mapState({
      searchAppliedFacets: state => state.Search.searchAppliedFacets,
       solrSettings: state => state.Search.solrSettings
    })
  },

  methods: {
    ...mapActions('Search', {
      requestSearch: 'requestSearch',
       updateQuery: 'updateQuery',
    }),

    ...mapActions('Notifier', {
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
         this.requestSearch({query:this.createRequestQuery(response.data), facets:this.searchAppliedFacets, options:this.solrSettings})
        })
        .catch(() => {
          this.setNotification({
          	title: 'We are so sorry!',
            text: 'Something went wrong when uploading your image - please try again',
            type: 'error',
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
