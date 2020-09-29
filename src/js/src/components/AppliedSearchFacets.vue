<template>
  <div v-if="searchAppliedFacets !== ''" class="selectedFacets">
    <h2>Applied facets</h2>
    <div v-for="(item, index) in seperateFacets(searchAppliedFacets)" :key="index" class="displayedFacet">
      <span>{{ displayFacetName(item) }}</span><span>{{ displayFacetValue(item) }}</span><button @click="removeFacet(item)">
        ✕
      </button>
    </div>
  </div>
</template>

<script>

import { mapState, mapActions } from 'vuex'
import AppliedSearchFacetsUtils from './../mixins/AppliedSearchFacetsUtils'
import HistoryRoutingUtils from './../mixins/HistoryRoutingUtils'

export default {
  name: 'AppliedSearchFacets',
  
  mixins: [AppliedSearchFacetsUtils, HistoryRoutingUtils],

  computed: {
    ...mapState({
      searchAppliedFacets: state => state.Search.searchAppliedFacets,
      facets: state => state.Search.facets,
      query: state => state.Search.query,
      solrSettings: state => state.Search.solrSettings,
    }),
  },
  mounted () {
  },
 
  methods: {
    ...mapActions('Search', {
      requestSearch: 'requestSearch',
      requestFacets: 'requestFacets',
      updateSearchAppliedFacets:'updateSearchAppliedFacets',
      updateSolrSettingOffset:'updateSolrSettingOffset'
    }),
    removeFacet(facet) {
      this.updateSolrSettingOffset(0)
      this.updateSearchAppliedFacets(this.searchAppliedFacets.replace('&fq=' + facet,''))
      this.requestSearch({query:this.query, facets:this.searchAppliedFacets, options:this.solrSettings})
      this.requestFacets({query:this.query, facets:this.searchAppliedFacets, options:this.solrSettings})
      this.$_pushSearchHistory('SolrWayback', this.query, this.searchAppliedFacets, this.solrSettings)
      //let newFacetUrl = this.searchAppliedFacets !== '' ? '&facets=' + encodeURIComponent(this.searchAppliedFacets) : ''
      //history.pushState({name: 'SolrWayback'}, 'SolrWayback', '?q=' + this.query + newFacetUrl)
      //this.$router.replace({query: {q:this.query, facets:this.searchAppliedFacets !== '' ?  this.searchAppliedFacets : undefined }})
    }
  }
}

</script>

    
