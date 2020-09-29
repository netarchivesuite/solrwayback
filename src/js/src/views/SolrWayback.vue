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
 import SearchUtils from './../mixins/SearchUtils'
 import { mapState, mapActions } from 'vuex'

export default {
  name: 'SolrWayback',
   components: {
   SearchBox,
   AllSearchResults,
   Notifications,
   LoadingOverlay
  },
  mixins: [SearchUtils],
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
      requestUrlSearch: 'requestUrlSearch',
      updateSearchAppliedFacets:'updateSearchAppliedFacets',
      updateSolrSettingGrouping:'updateSolrSettingGrouping',
      updateSolrSettingImgSearch:'updateSolrSettingImgSearch',
      updateSolrSettingUrlSearch:'updateSolrSettingUrlSearch',
      updateFutureSolrSettingGrouping:'updateFutureSolrSettingGrouping',
      updateFutureSolrSettingImgSearch:'updateFutureSolrSettingImgSearch',
      updateFutureSolrSettingUrlSearch:'updateFutureSolrSettingUrlSearch'
    }),
    onScroll(e) {
    e.target.documentElement.scrollTop > 0 ? this.scrolledFromTop = true : this.scrolledFromTop = false
    },
    backToTop() {
      window.scrollTo({ top: 0, behavior: 'smooth' })
    }, 
  },
  beforeRouteUpdate (to, from, next) {
    console.log('route changed!',to)
    this.updateQuery(to.query.q)
    to.query.grouping === 'true' ? this.updateFiutureSolrSettingGrouping(true) : this.updateFutureSolrSettingGrouping(false)
    to.query.imgSearch === 'true' ? this.updateFutureSolrSettingImgSearch(true) : this.updateFutureSolrSettingImgSearch(false)
    to.query.urlSearch === 'true' ? this.updateFutureSolrSettingUrlSearch(true) : this.updateFutureSolrSettingUrlSearch(false)
    to.query.facets ? this.updateSearchAppliedFacets(to.query.facets) : this.updateSearchAppliedFacets('')
    this.determineNewSearch(to.query.q)
  },
}
</script>
