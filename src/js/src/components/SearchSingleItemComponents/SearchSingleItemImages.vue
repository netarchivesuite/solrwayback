<template>
  <div v-if="imageSrcs.length > 0" class="singleEntryImages">
    <div class="singleEntryImagesHeadline">
      <p class="highlightText entryInfo imageHeadline">
        Images:
      </p>
      <p> 
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
        class="previewImage"
        :src="item.imageUrl + '&height=200&width=200'"
        @click="toggleFullImage(index)">
      <search-single-item-focus-image v-if="showFullImage === index"
                                      :image="item.downloadUrl"
                                      :index="index"
                                      @close-window="closeWindow" />
    </div>
  </div>
</template>

<script>
//import { mapState, mapActions } from 'vuex'
import { requestService } from '../../services/RequestService'
import SearchSingleItemFocusImage from './SearchSingleItemFocusImage.vue'

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
      requestService.fireImagesRequest(this.source, this.offset).then(result => (this.imageSrcs = result, this.imageSrcs === [] ? console.log('request successfull, no images!') : null), error => (console.log('Error in getting images'), this.imageSrc = []))
    }
  },
  mounted() {
    requestService.fireImagesRequest(this.source, this.offset).then(result => (this.imageSrcs = result, this.imageSrcs === [] ? console.log('request successfull, no images!') : null), error => (console.log('Error in getting images'), this.imageSrc = []))
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

    
