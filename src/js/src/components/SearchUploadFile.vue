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
    })
  },

  methods: {
    ...mapActions('Search', {
      requestSearch: 'requestSearch',
       updateQuery: 'updateQuery',
    }),

  selectFileToUpload() {
      this.fileToUpload = this.$refs.file.files
    },

  uploadFile(){
     //TODO spinner
     requestService.uploadFileRequest(this.fileToUpload[0])
        .then(response => {
          //Search for file
         this.updateQuery(this.createRequestQuery(response.data))
         this.requestSearch({query:this.createRequestQuery(response.data), facets:this.searchAppliedFacets})
        })
        .catch(() => {
           //TODO Call error handler - something went wrong
          this.fileToUpload = []
        })
    
  },

  createRequestQuery(sha1) {
    return `hash:"${sha1}"`
  }
  }
  
}
</script>
