<template>
  <div class="contentContainer">
    <transition name="loading-overlay">
      <loading-overlay />
    </transition>
    <notifications />
    <h1>Solr<span>Wayback</span></h1>
    <search-box />
    <all-search-results />
    <transition name="loading-overlay">
      <div v-if="scrolledFromTop" class="topTopArrow" @click="backToTop">
        ↑
      </div>
    </transition>
    <!--<router-link class="aboutLink" to="/about">Om Solrwayback search</router-link> -->
  </div>
</template>

<script>
 import SearchBox from '../components/SearchBox'
 import AllSearchResults from '../components/SearchResults/AllSearchResults'
 import Notifications from '../components/notifications/Notifications'
 import LoadingOverlay from '../components/LoadingOverlay'
 import { mapState, mapActions } from 'vuex'

export default {
  name: 'SolrWayback',
   components: {
   SearchBox,
   AllSearchResults,
   Notifications,
   LoadingOverlay
  },
  data: () => ({
        scrolledFromTop:false
  }),
  computed: {
    ...mapState({
      searchAppliedFacets: state => state.Search.searchAppliedFacets,
      query: state => state.Search.query,
      solrSettings: state => state.Search.solrSettings
    }),
  },
  mounted() {
    window.addEventListener('scroll', this.onScroll)
  },
  methods: {
    ...mapActions('Search', {
      requestSearch: 'requestSearch',
      requestFacets: 'requestFacets',
      updateQuery: 'updateQuery',
      requestImageSearch: 'requestImageSearch',
      updateSearchAppliedFacets:'updateSearchAppliedFacets',
      updateSolrSettingGrouping:'updateSolrSettingGrouping',
      updateSolrSettingImgSearch:'updateSolrSettingImgSearch',
      updateSolrSettingUrlSearch:'updateSolrSettingUrlSearch'
    }),
    onScroll(e) {
    e.target.documentElement.scrollTop > 0 ? this.scrolledFromTop = true : this.scrolledFromTop = false
    },
    backToTop() {
      window.scrollTo({ top: 0, behavior: 'smooth' })
    }, 
  },
  beforeRouteUpdate (to, from, next) {
    this.updateQuery(to.query.q)
    to.query.grouping === 'true' ? this.updateSolrSettingGrouping(true) : this.updateSolrSettingGrouping(false)
    to.query.imgSearch === 'true' ? this.updateSolrSettingImgSearch(true) : this.updateSolrSettingImgSearch(false)
    to.query.urlSearch === 'true' ? this.updateSolrSettingUrlSearch(true) :  this.updateSolrSettingUrlSearch(false)
    to.query.facets ? this.updateSearchAppliedFacets(to.query.facets) : this.updateSearchAppliedFacets('')
    if(this.solrSettings.imgSearch) {
      this.requestImageSearch({query:this.query})
      return
    }
    else if(this.solrSettings.urlSearch) {
      return
    } 
    else {
      this.requestSearch({query:this.query, facets:this.searchAppliedFacets, options:this.solrSettings})
      this.requestFacets({query:this.query, facets:this.searchAppliedFacets, options:this.solrSettings})
      return
    }
  },
}
</script>
