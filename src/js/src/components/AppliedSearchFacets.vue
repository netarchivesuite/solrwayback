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


export default {
  name: 'AppliedSearchFacets',
  
  mixins: [AppliedSearchFacetsUtils],

  computed: {
    ...mapState({
      searchAppliedFacets: state => state.searchStore.searchAppliedFacets,
      facets: state => state.searchStore.facets,
      query: state => state.searchStore.query
    }),
  },
  mounted () {
  },
 
  methods: {
    ...mapActions('searchStore', {
      requestSearch: 'requestSearch',
      requestFacets: 'requestFacets',
      updateSearchAppliedFacets:'updateSearchAppliedFacets'
    }),
    removeFacet(facet) {
      console.log(facet, this.searchAppliedFacets)
      this.updateSearchAppliedFacets(this.searchAppliedFacets.replace('&fq=' + facet,''))
      console.log(this.searchAppliedFacets)
      this.requestSearch({query:this.query, facets:this.searchAppliedFacets})
      this.requestFacets({query:this.query, facets:this.searchAppliedFacets})
      this.$router.replace({query: {q:this.query, facets:this.searchAppliedFacets !== '' ?  this.searchAppliedFacets : undefined }})
    }
  }
}

</script>

    
