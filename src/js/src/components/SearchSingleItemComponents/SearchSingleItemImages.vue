<template>
  <div v-if="imageSrcs.length > 0" class="singleEntryImages">
    <div class="singleEntryImagesHeadline">
      <p class="highlightText entryInfo imageHeadline">
        {{ inputType === 'multiple' ? 'Images:' : 'Image:' }}
      </p>
      <p v-if="inputType === 'multiple'"> 
        showing {{ showNumberOfPictures(imageSrcs.length) }} out of {{ imageSrcs.length }}
      </p>
      <button v-if="imageSrcs.length > 4" class="allImagesButton" @click="toggleAllImagesShown">
        {{ shownImagesButtonText }}
      </button> 
    </div>
    <div v-for="(item, index) in shownImages(imageSrcs)"
         :key="index"
         class="previewImageContainer">
      <img
        loading="lazy"
        :class="inputType === 'multiple' ? 'previewImage' : 'imageEntry'"
        :src="inputType === 'multiple' ? item.imageUrl + '&height=200&width=200' : item"
        @click="toggleFullImage(index)">
      <search-single-item-focus-image v-if="showFullImage === index"
                                      :image="inputType === 'multiple' ? item.downloadUrl + '&height=200&width=200' : item"
                                      :index="index"
                                      @close-window="closeWindow" />
    </div>
  </div>
</template>

<script>
//import { mapState, mapActions } from 'vuex'
import { requestService } from '../../services/RequestService'
import SearchSingleItemFocusImage from './SearchSingleItemFocusImage.vue'
import configs from '../../configs'

export default {
  name: 'SearchSingleItemImages',
  components: {  
    SearchSingleItemFocusImage
  },
  props: {
    offset: {
      type: Number,
      required: true
    },
    source: {
      type: String,
      required: true
    },
    inputType: {
      type:String,
      required:true
    }
  },
  data () {
    return {     
      imageSrcs:[],
      allImagesShown:false,
      showFullImage:null
    }
  },
  computed: {
    shownImagesButtonText: function () {
      return this.allImagesShown ? 'See fewer images ' : 'See all images'
    }
  },
  watch: { 
    source: function() { // watch it
      if(this.inputType === 'multiple') {
        requestService.fireImagesRequest(this.source, this.offset).then(result => (this.imageSrcs = result, this.imageSrcs === [] ? console.log('request successfull, no images!') : null), error => (console.log('Error in getting images'), this.imageSrc = []))
      }
      else {
        this.imageSrcs.length = 0
        this.imageSrcs.push(`${configs.playbackConfig.solrwaybackBaseURL}services/downloadRaw?source_file_path=${this.source}&offset=${this.offset}`)
      }
    }
  },
  mounted() {
    if(this.inputType === 'multiple') {
      requestService.fireImagesRequest(this.source, this.offset).then(result => (this.imageSrcs = result, this.imageSrcs === [] ? console.log('request successfull, no images!') : null), error => (console.log('Error in getting images'), this.imageSrc = []))
    }
    else {
      this.imageSrcs.push(`${configs.playbackConfig.solrwaybackBaseURL}services/downloadRaw?source_file_path=${this.source}&offset=${this.offset}`)
    }
  },
  methods: {
    refactoredDate(date) {
      date = date.toString()
      return date.substring(6,8) + '/' + date.substring(4,6) + '-' + date.substring(0,4)
    },
    shownImages(imageArray) {
      if(this.allImagesShown) {
        return imageArray
      }
      else {
        return imageArray.slice(0,4)
      }
    },
    toggleAllImagesShown() {
      this.allImagesShown = !this.allImagesShown
    },
    showNumberOfPictures(number) {
      if(this.allImagesShown) {
        return number
      }
      else {
        return number > 4 ? '4' : number
      }
    },
    toggleFullImage(index) {
      this.showFullImage !== null ? this.showFullImage = null : this.showFullImage = index
    },
    closeWindow(index) {
      this.showFullImage = null
    } 
  }
}

</script>

    
