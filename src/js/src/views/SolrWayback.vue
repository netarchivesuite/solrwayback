<template>
  <div>
    <div class="topMenu">
      <a :href="logoLink">
        <img :src="logoUrl"
             width="100"
             height="40"
             alt="logo">
      </a>
    </div>
    <div class="contentContainer">
      <transition name="loading-overlay">
        <loading-overlay />
      </transition>
      <notifications />
      <h1>Solr<span>Wayback</span></h1>
      <search-box />
      <all-search-results />
      <about-component />
      <primary-modal v-if="showModal" />
      <transition name="loading-overlay">
        <div v-if="scrolledFromTop" class="topTopArrow" @click="backToTop">
          ↑
        </div>
      </transition>
    <!--<router-link class="aboutLink" to="/about">Om Solrwayback search</router-link> -->
    </div>
    <div />
  </div>
</template>

<script>
 import SearchBox from '../components/SearchBox'
 import AboutComponent from '../components/AboutComponent'
 import AllSearchResults from '../components/searchResults/AllSearchResults'
 import Notifications from '../components/notifications/Notifications'
 import LoadingOverlay from '../components/LoadingOverlay'
 import SearchUtils from './../mixins/SearchUtils'
 import { mapState, mapActions } from 'vuex'
 import PrimaryModal from './../components/modalComponents/PrimaryModal'
 import Configs from '../configs'

export default {
  name: 'SolrWayback',
   components: {
   SearchBox,
   AllSearchResults,
   Notifications,
   LoadingOverlay,
   AboutComponent,
   PrimaryModal
  },
  mixins: [SearchUtils],
  data: () => ({
        scrolledFromTop:false,
        logoUrl: Configs.logo.url,
        logoLink: Configs.logo.link
  }),
  computed: {
    ...mapState({
      searchAppliedFacets: state => state.Search.searchAppliedFacets,
      query: state => state.Search.query,
      solrSettings: state => state.Search.solrSettings,
      showModal: state => state.Modal.showModal
    }),
  },

  mounted() {
    window.addEventListener('scroll', this.onScroll)
  },
  methods: {
    ...mapActions('Search', {
      resetState:'resetState',
      addToSearchAppliedFacets:'addToSearchAppliedFacets',
      emptySearchAppliedFacets:'emptySearchAppliedFacets',
      updateSolrSettingGrouping:'updateSolrSettingGrouping',
      updateSolrSettingImgSearch:'updateSolrSettingImgSearch',
      updateSolrSettingUrlSearch:'updateSolrSettingUrlSearch',
      updateSolrSettingOffset:'updateSolrSettingOffset',
    }),
    onScroll(e) {
    e.target.documentElement.scrollTop > 0 ? this.scrolledFromTop = true : this.scrolledFromTop = false
    },
    backToTop() {
      window.scrollTo({ top: 0, behavior: 'smooth' })
    },
    checkForChangesBetweenRouteQueries(to, from) {
      return to.query.query !== undefined &&
       (to.query.query !== from.query.query ||
       to.query.offset !== from.query.offset ||
       to.query.imgSearch !== from.query.imgSerach ||
       to.query.urlSearch !== from.query.urlSearch ||
       to.query.grouping !== from.query.grouping ||
       to.query.facets !== from.query.facets)
    },
    checkIfRoutingIsPageTurn(to, from) {
      return to.query.query === from.query.query &&
                  to.query.imgSearch === from.query.imgSearch && 
                  to.query.urlSearch === from.query.urlSearch && 
                  to.query.grouping === from.query.grouping &&
                  to.query.facets === from.query.facets &&
                  to.query.offset !== from.query.offset
    }
  },
  beforeRouteUpdate (to, from, next) {
    console.log('route changed!',to.query, from.query)
    // Check if any of our params have changed. Could be refactored into a nice functon.
    if(this.checkForChangesBetweenRouteQueries(to, from)) {
      //console.log('we doing a route search')
      // update our variables from the query.
      to.query.grouping === 'true' || to.query.grouping === true ? this.updateSolrSettingGrouping(true) : this.updateSolrSettingGrouping(false)
      to.query.imgSearch === 'true' || to.query.imgSearch === true ? this.updateSolrSettingImgSearch(true) : this.updateSolrSettingImgSearch(false)
      to.query.urlSearch === 'true' || to.query.urlSearch === true ? this.updateSolrSettingUrlSearch(true) : this.updateSolrSettingUrlSearch(false)
      to.query.offset ? this.updateSolrSettingOffset(Number(to.query.offset)) : this.updateSolrSettingOffset(0)
      // Update our filers set from facets, if there are any. To avoid dublicated, we empty it first, then refill it.
      this.emptySearchAppliedFacets()
      if(to.query.facets) {
        let newFacets = to.query.facets.split('&fq=')
        newFacets.shift()
        newFacets.length > 0 ? newFacets.forEach((item) => {
          this.addToSearchAppliedFacets('&fq=' + item)  
        }) : null 
      }
      // Fire off a new search based on the updated variables.
      this.$_determineNewSearch(to.query.query, false, this.checkIfRoutingIsPageTurn(to, from))
    }
    else {
      // If the route was changed and the query is undefined, we reset everything.
      if(to.query.query === undefined) {
        this.resetState()
      }
    }
    next()
  },
}
</script>
