<template>
  <div class="columnImageContainer">
    <div class="number">
      #{{ row + number * 3 }}
    </div>
    <img 
      loading="lazy"
      :src="result.downloadUrl"
      @click="toggleFullImage(row + number * 3)">
    <search-single-item-focus-image v-if="showFullImage === row + number * 3"
                                    :image="result.downloadUrl"
                                    :index="row + number * 3"
                                    @close-window="closeWindow" />
    <div class="imageButtonContainer">
      <button @click="startImageSearch()">
        <span>Search for image</span>
      </button>
      <button @click="startPageSearch()">
        <span>Pages linking to image</span>
      </button>
    </div>
  </div>
</template>

<script>

import { mapActions, mapState } from 'vuex'
import SearchSingleItemFocusImage from './SearchSingleItemFocusImage'
import HistoryRoutingUtils from './../../mixins/HistoryRoutingUtils'

export default {
  name: 'SearchMasonryImage',
  components: {
    SearchSingleItemFocusImage
  },
  mixins: [HistoryRoutingUtils],
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
      required:true
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
    startPageSearch() {
      this.updateQuery('links_images:"' + this.result.urlNorm + '"')
      this.updateSearchAppliedFacets('')
      this.updateSolrSettingImgSearch(false)
      this.requestSearch({query:this.query, facets:this.searchAppliedFacets, options:this.solrSettings})
      this.requestFacets({query:this.query, facets:this.searchAppliedFacets, options:this.solrSettings})
      this.$_pushSearchHistory('SolrWayback', this.query, this.searchAppliedFacets, this.solrSettings)
    },
    startImageSearch() {
       this.updateQuery('hash:"' + this.result.hash + '"')
      this.updateSearchAppliedFacets('')
      this.updateSolrSettingImgSearch(false)
      this.requestSearch({query:this.query, facets:this.searchAppliedFacets, options:this.solrSettings})
      this.requestFacets({query:this.query, facets:this.searchAppliedFacets, options:this.solrSettings})
      this.$_pushSearchHistory('SolrWayback', this.query, this.searchAppliedFacets, this.solrSettings)
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