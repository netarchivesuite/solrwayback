<template>
  <div class="columnImageContainer">
    <div class="number">
      #{{ row + number * rowNumber }}
    </div>
    <img 
      loading="lazy"
      :src="result.imageUrl + '&height=200&width=200'"
      @click="toggleFullImage(true)">
    <search-single-item-focus-image v-if="showFullImage === true"
                                    :image="result.downloadUrl"
                                    :index="row + number * rowNumber"
                                    @close-window="closeWindow" />
    <div class="imageButtonContainer">
      <router-link :to="{ path: $_startImageSearchFromImage(result.hash)}">
        <span @click="closeModalIfOpen()">Search for image</span>
      </router-link>
      <router-link :to="{ path: $_startPageSearchFromImage(result.urlNorm)}">
        <span @click="closeModalIfOpen()">Pages linking to image</span>
      </router-link>
    </div>
  </div>
</template>

<script>

import SearchSingleItemFocusImage from './SearchSingleItemFocusImage'
import ImageSearchUtils from './../../mixins/ImageSearchUtils'
import { mapState, mapActions } from 'vuex'

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
      showFullImage:null
    }
  },
  computed: {
    ...mapState({
      showModal: state => state.Modal.showModal,
      currentModal: state => state.Modal.currentModal,
    }),
  },
  methods: {
    ...mapActions('Modal', {
      updateShowModal:'updateShowModal',
      updateCurrentModal:'updateCurrentModal'
    }),
     toggleFullImage(index) {
      this.showFullImage !== null ? this.showFullImage = null : this.showFullImage = index
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