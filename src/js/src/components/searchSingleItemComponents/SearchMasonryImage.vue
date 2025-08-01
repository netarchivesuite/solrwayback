<template>
  <div :class="getColumnClass()">
    <div class="number">
      #{{ row + number * rowNumber }}
    </div>
    <div v-if="!playbackDisabled() && !imageLoaded" class="loader" />
    <img
      loading="lazy" 
      :src="result.imageUrl + '&height=200&width=200'"
      @load="setImageLoaded(true)"
      @click="toggleFullImage(true)">
    <search-single-item-focus-image v-if="showFullImage === true"
                                    :image="result.downloadUrl"
                                    :index="row + number * rowNumber"
                                    @close-window="closeWindow" />
    <div class="imageButtonContainer">
      <router-link :to="$_startImageSearchFromImage(result.hash)">
        <span @click="closeModalIfOpen()">Search for image</span>
      </router-link>
      <router-link :to="$_startPageSearchFromImage(result.urlNorm)">
        <span @click="closeModalIfOpen()">Pages linking to image</span>
      </router-link>
    </div>
  </div>
</template>

<script>

import SearchSingleItemFocusImage from './SearchSingleItemFocusImage.vue'
import ImageSearchUtils from './../../mixins/ImageSearchUtils'
import configs from '../../configs'
import { isPlaybackDisabled } from '../../configs/configHelper'
import { mapStores, mapActions } from 'pinia'
import { useModalStore } from '../../store/modal.store'

export default {
  name: 'SearchMasonryImage',
  components: {
    SearchSingleItemFocusImage
  },
  mixins: [ImageSearchUtils],
  props: {
    result: {
      type: Object,
      required: true
    },
    number: {
      type: Number,
      required: true
    },
    row: {
      type:Number,
      required: true
    },
    rowNumber: {
      type: Number,
      required: true
    }
  },
  data () {
    return {  
      showFullImage:null,
      imageLoaded:false,
    }
  },
  computed: {
    // ...mapState({
    //   showModal: state => state.Modal.showModal,
    //   currentModal: state => state.Modal.currentModal,
    // }),
    ...mapStores(useModalStore)
  },
  methods: {
    ...mapActions(useModalStore, {
      updateShowModal:'updateShowModal',
      updateCurrentModal:'updateCurrentModal'
    }),
    setImageLoaded(value) {
      this.imageLoaded = value
    },
     toggleFullImage(index) {
      if (!this.playbackDisabled()){
      this.showFullImage !== null ? this.showFullImage = null : this.showFullImage = index
      }
    },
    playbackDisabled(){
      return isPlaybackDisabled()
    },
    getColumnClass() {
      return `columnImageContainer ${this.playbackDisabled() ? 'noPointer' : ''}`
    },
    closeWindow(index) {
      this.showFullImage = null
    },
    closeModalIfOpen() {
      this.updateShowModal(false)
      this.updateCurrentModal('')
    } 
  }
}
</script>