<template>
  <div>
    <span>Showing <span class="highlightText">{{ imgResults.images.length }}</span> images matching query. </span>
    <div class="images">
      <div class="column 1">
        <search-masonry-image v-for="(result, index) in getOffsetArray(imgResults.images,0)"
                              :key="index"
                              :number="index"
                              :result="result"
                              :row="1"
                              :row-number="numberOfRows" />
      </div>
      <div class="column 2">
        <search-masonry-image v-for="(result, index) in getOffsetArray(imgResults.images,1)"
                              :key="index"
                              :number="index"
                              :result="result"
                              :row="2"
                              :row-number="numberOfRows" />
      </div>
      <div class="column 3">
        <search-masonry-image v-for="(result, index) in getOffsetArray(imgResults.images,2)"
                              :key="index"
                              :number="index"
                              :result="result"
                              :row="3"
                              :row-number="numberOfRows" />
      </div>
    </div>
  </div>
</template>
<script>

// import { mapState, mapActions } from 'vuex'
import { mapStores, mapActions } from 'pinia'
import { useSearchStore } from '../../store/search.store'
import HistoryRoutingUtils from './../../mixins/HistoryRoutingUtils'
import SearchMasonryImage from './../searchSingleItemComponents/SearchMasonryImage.vue'

export default {
  name: 'ImageSearchResults',
  components: {
    SearchMasonryImage
  },
  mixins: [HistoryRoutingUtils],
  props: {
    imgResults: {
      type: Object,
      required: true
    }
  },
  data () {
    return {  
      numberOfRows:3,   
    }
  },
  computed: {
    // ...mapState({
    //   query: state => state.Search.query,
    //   solrSettings: state => state.Search.solrSettings
    // }),
    ...mapStores(useSearchStore)
  },
  mounted () {
  },
  
  methods: {
    ...mapActions(useSearchStore, {
      requestSearch: 'requestSearch',
      requestFacets: 'requestFacets',
      updateSolrSettingOffset:'updateSolrSettingOffset'
    }),
    getOffsetArray(array, number) {
      let newArray = [...array]
      newArray.splice(0,number)
      let returnArray = newArray.filter((item, index) => {
        return index % 3 === 0
      })
      return returnArray
    },
  }
}
</script>