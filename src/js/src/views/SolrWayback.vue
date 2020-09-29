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
watch: {
    '$route.query.q': function (to, from) {
      console.log('wathcing route to-from', to, from)
    }
  },

  mounted() {
    window.addEventListener('scroll', this.onScroll)
  },
  methods: {
    ...mapActions('Search', {
      resetState:'resetState',
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
  

   beforeRouteEnter (to, from, next) {
        console.log('enter!!')
        next()
   },
  beforeRouteUpdate (to, from, next) {
    console.log('route changed!fooooo',next)
    this.updateQuery(to.query.q)
    to.query.grouping === 'true' ? (this.updateSolrSettingGrouping(true), this.updateFiutureSolrSettingGrouping(true)) : (this.updateSolrSettingGrouping(false), this.updateFutureSolrSettingGrouping(false))
    to.query.imgSearch === 'true' ? (this.updateSolrSettingImgSearch(true), this.updateFutureSolrSettingImgSearch(true)) : (this.updateSolrSettingImgSearch(false), this.updateFutureSolrSettingImgSearch(false))
    to.query.urlSearch === 'true' ? (this.updateSolrSettingUrlSearch(true), this.updateFutureSolrSettingUrlSearch(true)) :  (this.updateSolrSettingUrlSearch(false), this.updateFutureSolrSettingUrlSearch(false))
    to.query.facets ? this.updateSearchAppliedFacets(to.query.facets) : this.updateSearchAppliedFacets('')
    if(this.solrSettings.imgSearch) {
      this.requestImageSearch({query:this.query})
    }
    else if(this.solrSettings.urlSearch) {
      let queryString = ''
      if(this.query.substring(0,10) === 'url_norm:"') {
        queryString = this.query.replace('url_norm:"', '')
        queryString.substring(queryString.length-1, queryString.length) === '"' ? queryString.slice(0,-1) : null
        this.updateQuery(queryString)
      }
      this.requestUrlSearch({query:this.query, facets:this.searchAppliedFacets, options:this.solrSettings})
      this.requestFacets({query:this.query, facets:this.searchAppliedFacets, options:this.solrSettings})
    } 
    else {
      this.requestSearch({query:this.query, facets:this.searchAppliedFacets, options:this.solrSettings})
      this.requestFacets({query:this.query, facets:this.searchAppliedFacets, options:this.solrSettings})
    }

   
  },
}
</script>
