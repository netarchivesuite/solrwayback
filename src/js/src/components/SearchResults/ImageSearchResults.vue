<template>
  <div>
    <span>Showing <span class="highlightText">{{ results.images.length }}</span> images matching <span class="highlightText">{{ query }}. </span> </span>
    <div class="images">
      <div class="column 1">
        <search-masonry-image v-for="(result, index) in getOffsetArray(results.images,0)"
                              :key="index"
                              :number="index"
                              :result="result"
                              :row="1"
                              :row-number="numberOfRows" />
      </div>
      <div class="column 2">
        <search-masonry-image v-for="(result, index) in getOffsetArray(results.images,1)"
                              :key="index"
                              :number="index"
                              :result="result"
                              :row="2"
                              :row-number="numberOfRows" />
      </div>
      <div class="column 3">
        <search-masonry-image v-for="(result, index) in getOffsetArray(results.images,2)"
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

import { mapState, mapActions } from 'vuex'
import HistoryRoutingUtils from './../../mixins/HistoryRoutingUtils'
import SearchMasonryImage from './../SearchSingleItemComponents/SearchMasonryImage'

export default {
  name: 'ImageSearchResults',
  components: {
    SearchMasonryImage
  },
  mixins: [HistoryRoutingUtils],
  data () {
    return {  
      numberOfRows:3,   
    }
  },
  computed: {
    ...mapState({
      query: state => state.Search.query,
      results: state => state.Search.results,
      solrSettings: state => state.Search.solrSettings
    }),
  },
  mounted () {
  },
  
  methods: {
    ...mapActions('Search', {
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