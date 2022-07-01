<template>
  <div class="previewImageContainer">
    <div v-show="!imageLoaded" class="loader" />
    <img
      loading="lazy"
      :class="getImgClass()"
      :src="inputType === 'multiple' ? item.imageUrl + '&height=200&width=200' : item"
      @load="setImageLoaded(true)"
      @click="toggleFullImage(index)">
    <search-single-item-focus-image v-if="showFullImage === index"
                                    :image="inputType === 'multiple' ? item.downloadUrl : item"
                                    :index="index"
                                    @close-window="closeWindow" />
    <div class="imageButtonContainer">
      <router-link :to="{ path: $_startImageSearchFromImage(item.hash ? item.hash : hash )}">
        <span>Search for image</span>
      </router-link>
      <router-link :to="{ path: $_startPageSearchFromImage(item.urlNorm ? item.urlNorm : urlNorm)}">
        <span>Pages linking to image</span>
      </router-link>
    </div>
  </div>
</template>

<script>
import SearchSingleItemFocusImage from './SearchSingleItemFocusImage.vue'
import ImageSearchUtils from './../../mixins/ImageSearchUtils'
import configs from '../../configs'
import { isPlaybackDisabled } from '../../configs/configHelper'


export default {
  name: 'SearchSingleItemPreviewImage',
  components: {  
    SearchSingleItemFocusImage
  },
  mixins: [ImageSearchUtils],
  props: {
    showFullImage: {
      type:Number,
      default:null
    },
    index: {
      type: Number,
      required: true
    },
    inputType: {
      type:String,
      required:true
    },
    hash: {
      type:String,
      required:true
    },
    urlNorm: {
      type:String,
      required: true
    },
    item: {
      type:[Object, String],
      required: true
    }
  },
  data () {
    return {  
      imageLoaded:false
    }
  },
  methods: {
    toggleFullImage(index) {
      if (!isPlaybackDisabled()){
      this.$emit('toggle-fullimage', this.index)
      }
    },

    getImgClass() {
      return `${this.inputType === 'multiple' ? 'previewImage' : 'imageEntry'} ${isPlaybackDisabled() ? 'noPointer' : ''}`
    },

    closeWindow() {
      this.$emit('toggle-fullimage', null)
    },
    setImageLoaded(value) {
      this.imageLoaded = value
    },
  }
}

</script>

    
