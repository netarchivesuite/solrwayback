<template>
  <div class="columnImageContainer">
    <div class="number">
      #{{ row + number * rowNumber }}
    </div>
    <img 
      loading="lazy"
      :src="result.downloadUrl"
      @click="toggleFullImage(true)">
    <search-single-item-focus-image v-if="showFullImage === true"
                                    :image="result.downloadUrl"
                                    :index="row + number * rowNumber"
                                    @close-window="closeWindow" />
    <div class="imageButtonContainer">
      <router-link :to="{ path: $_startImageSearchFromImage(result.hash)}">
        <span @click="$_addHistory('hash',result.hash)">Search for image</span>
      </router-link>
      <router-link :to="{ path: $_startPageSearchFromImage(result.urlNorm)}">
        <span @click="$_addHistory('links_images',result.urlNorm)">Pages linking to image</span>
      </router-link>
    </div>
  </div>
</template>

<script>

import { mapActions, mapState } from 'vuex'
import SearchSingleItemFocusImage from './SearchSingleItemFocusImage'
import HistoryRoutingUtils from './../../mixins/HistoryRoutingUtils'
import ImageSearchUtils from './../../mixins/ImageSearchUtils'

export default {
  name: 'SearchMasonryImage',
  components: {
    SearchSingleItemFocusImage
  },
  mixins: [HistoryRoutingUtils, ImageSearchUtils],
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
      query: state => state.Search.query,
      searchAppliedFacets: state => state.Search.searchAppliedFacets,
      solrSettings: state => state.Search.solrSettings,
    })
  },
  methods: {
    ...mapActions('Search', {
      requestSearch: 'requestSearch',
      requestFacets: 'requestFacets',
      updateQuery: 'updateQuery',
      updateSearchAppliedFacets:'updateSearchAppliedFacets',
      updateSolrSettingImgSearch:'updateSolrSettingImgSearch',
    }),
     toggleFullImage(index) {
      this.showFullImage !== null ? this.showFullImage = null : this.showFullImage = index
    },
    closeWindow(index) {
      this.showFullImage = null
    } 
  }
}
</script>