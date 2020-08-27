<template>
  <div class="searchBoxContainer">
    <form class="searchForm" @submit.prevent="handleSubmit">
      <input id="query"
             v-model="futureQuery"
             type="text"
             autofocus
             class=""
             placeholder="Enter search term">
      <button id="querySubmit" title="Search" type="submit">
        <div id="magnifyingGlass" />
      </button>
      <button v-if="futureQuery !== ''"
              id="clearSubmit"
              title="Clear search and results"
              type="button"
              @click="clearResultsAndSearch" />
      <div class="sortOptions">
        <div @click="updateSolrSettingGrouping(!solrSettings.grouping)">
          <input id="groupedSearch"
                 :checked="solrSettings.grouping"
                 type="checkbox"
                 name="groupedSearch">
          <label for="male">Grouped search</label>
        </div>
        <div class="floatRight" @click="selectSearchMethod('urlSearch')">
          <input id="urlSearch"
                 :checked="solrSettings.urlSearch"
                 type="checkbox"
                 name="urlSearch"
                 @click.stop="selectSearchMethod('urlSearch')">
          <label for="male">URL search</label>
        </div>
        <div class="floatRight marginRight" @click="selectSearchMethod('imgSearch')">
          <input id="imgSearch"
                 :checked="solrSettings.imgSearch"
                 type="checkbox"
                 name="imgSearch"
                 @click.stop="selectSearchMethod('imgSearch')">
          <label for="male">Image search</label>
        </div>
      </div>
      <div class="tools">
        <span>Search with uploaded file</span> <span>Search for HTML-tags</span> <span>Domain stats</span><span>Link graphs</span>
      </div>
    </form>
    <applied-search-facets />
  </div>
</template> 

<script>
import { mapState, mapActions } from 'vuex'
import AppliedSearchFacets from './AppliedSearchFacets.vue'

export default {
  components: {
    AppliedSearchFacets
  },
  data () {
    return {    
      futureQuery:'',
    }
  },
  computed: {
    ...mapState({
      query: state => state.Search.query,
      searchAppliedFacets: state => state.Search.searchAppliedFacets,
      results: state => state.Search.results,
      solrSettings: state => state.Search.solrSettings,
      loading: state => state.Search.loading,
    })
  },
  watch: {
    query: function (val) {
      this.futureQuery  = val
    },
  },
  mounted () {
    if(this.$router.history.current.query.q) {
      this.updateQuery(this.$router.history.current.query.q)
      this.futureQuery = this.$router.history.current.query.q
      if(this.$router.history.current.query.facets) {
        this.updateSearchAppliedFacets(this.$router.history.current.query.facets)
      }
      this.requestSearch({query:this.query, facets:this.searchAppliedFacets, options:this.solrSettings})
      this.requestFacets({query:this.query, facets:this.searchAppliedFacets, options:this.solrSettings})
      }
  },
  
  methods: {
    ...mapActions('Search', {
      requestSearch: 'requestSearch',
      requestFacets: 'requestFacets',
      updateQuery: 'updateQuery',
      clearResults: 'clearResults',
      updateSearchAppliedFacets:'updateSearchAppliedFacets',
      resetSearchState:'resetState',
      updateSolrSettingGrouping:'updateSolrSettingGrouping',
      updateSolrSettingImgSearch:'updateSolrSettingImgSearch',
      updateSolrSettingUrlSearch:'updateSolrSettingUrlSearch'

    }),
    handleSubmit() {
      if (this.futureQuery !== this.query) {
        this.updateQuery(this.futureQuery)
        this.requestSearch({query:this.futureQuery, facets:this.searchAppliedFacets, options:this.solrSettings})
        this.requestFacets({query:this.futureQuery, facets:this.searchAppliedFacets, options:this.solrSettings})
        let newFacetUrl = this.searchAppliedFacets !== '' ? '&facets=' + encodeURIComponent(this.searchAppliedFacets) : ''
        history.pushState({name: 'SolrWayback'}, 'SolrWayback', '?q=' + this.query + newFacetUrl)
      }
    },
    selectSearchMethod(selected) {
      if(selected === 'imgSearch') {
        this.solrSettings.urlSearch ? this.updateSolrSettingUrlSearch(!this.solrSettings.urlSearch) : null
        this.updateSolrSettingImgSearch(!this.solrSettings.imageSearch)
      }
      if(selected === 'urlSearch') {
        this.solrSettings.imgSearch ? this.updateSolrSettingImgSearch(!this.solrSettings.imgSearch) : null
        this.updateSolrSettingUrlSearch(!this.solrSettings.urlSearch)
      }
    },
    clearResultsAndSearch() {
      history.pushState({name: 'SolrWayback'}, 'SolrWayback', '/')
      this.futureQuery = ''
      this.resetSearchState()
      //this.updateQuery('')
      //this.clearResults()
      //this.updateSearchAppliedFacets('')
    },
  }
}

</script>

    