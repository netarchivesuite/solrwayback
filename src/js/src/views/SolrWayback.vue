<template>
  <div class="contentContainer">
    <notifications />
    <h1>Solr<span>Wayback</span></h1>
    <search-box />
    <search-result />
    <!--<router-link class="aboutLink" to="/about">Om Solrwayback search</router-link> -->
  </div>
</template>

<script>
 import SearchBox from '../components/SearchBox'
 import SearchResult from '../components/SearchResult'
 import Notifications from '../components/notifications/Notifications'
 import { mapState, mapActions } from 'vuex'


export default {
  name: 'SolrWayback',
   components: {
   SearchBox,
   SearchResult,
   Notifications
  },
  computed: {
    ...mapState({
      searchAppliedFacets: state => state.Search.searchAppliedFacets,
      query: state => state.Search.query
    }),
  },
  methods: {
    ...mapActions('Search', {
      requestSearch: 'requestSearch',
      requestFacets: 'requestFacets',
      updateQuery: 'updateQuery',
      updateSearchAppliedFacets:'updateSearchAppliedFacets',
    }),
  },
  beforeRouteUpdate (to, from, next) {
    console.log('we updated route')
    this.updateQuery(to.query.q)
    to.query.facets ? this.updateSearchAppliedFacets(to.query.facets) : this.updateSearchAppliedFacets('')
    this.requestSearch({query:this.query, facets: this.searchAppliedFacets})
    this.requestFacets({query:this.query, facets: this.searchAppliedFacets})
  } 
}
</script>
