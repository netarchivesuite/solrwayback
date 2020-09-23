<template>
  <div class="searchBoxContainer">
    <form class="searchForm" @submit.prevent="handleSubmit">
      <input id="query"
             v-model="futureQuery"
             type="text"
             autofocus
             class=""
             autocomplete="off"
             placeholder="Enter search term">
      <button id="querySubmit" title="Search" type="submit">
        <div id="magnifyingGlass" />
      </button>
      <button v-if="futureQuery !== '' || Object.keys(results).length !== 0"
              id="clearSubmit"
              title="Clear search and results"
              type="button"
              @click="clearResultsAndSearch" />
      <div class="sortOptions">
        <div @click.prevent="futureGrouped = !futureGrouped">
          <input id="groupedSearch"
                 :checked="futureGrouped"
                 type="checkbox"
                 name="groupedSearch">
          <label for="groupedSearch">Grouped search <span class="buttonExplanation" title="Grouping results by URL, meaning you only seen an URL as one hit, even though it might have been a hit on several params.">[ ? ]</span></label>
        </div>
        <div class="floatRight" @click.prevent="selectSearchMethod('urlSearch')">
          <input id="urlSearch"
                 :checked="futureUrlSearch"
                 type="checkbox"
                 name="urlSearch"
                 @click.stop="selectSearchMethod('urlSearch')">
          <label for="urlSearch">URL search <span class="buttonExplanation" title="Explanation goes here.">[ ? ]</span></label>
        </div>
        <div class="floatRight marginRight" @click.prevent="selectSearchMethod('imgSearch')">
          <input id="imgSearch"
                 :checked="futureImgSearch"
                 type="checkbox"
                 name="imgSearch"
                 @click.stop="selectSearchMethod('imgSearch')">
          <label for="imgSearch">Image search <span class="buttonExplanation" title="Explanation goes here">[ ? ]</span></label>
        </div>
      </div>
      <div class="tools">
        <span @click="showUploadFileSearch = !showUploadFileSearch">Search with uploaded file</span> <span>Search for HTML-tags</span> <span>Domain stats</span><span>Link graphs</span>
      </div>
    </form>
    <applied-search-facets />
    <search-upload-file v-if="showUploadFileSearch" />
  </div>
</template> 

<script>
import { mapState, mapActions } from 'vuex'
import AppliedSearchFacets from './AppliedSearchFacets.vue'
import HistoryRoutingUtils from './../mixins/HistoryRoutingUtils'
import SearchUploadFile from './SearchUploadFile.vue'

export default {
  components: {
    AppliedSearchFacets,
    SearchUploadFile
  },
  mixins: [HistoryRoutingUtils],
  data () {
    return {    
      futureQuery:'',
      futureGrouped:false,
      futureUrlSearch:false,
      futureImgSearch:false,
      showUploadFileSearch: false

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
    console.log(this.$router.history.current.query)
    if(this.$router.history.current.query.q) {
      this.updateQuery(this.$router.history.current.query.q)
      this.futureQuery = this.$router.history.current.query.q
      this.$router.history.current.query.facets ? this.updateSearchAppliedFacets(this.$router.history.current.query.facets) : this.updateSearchAppliedFacets('')
      this.$router.history.current.query.grouping === 'true' ? (this.updateSolrSettingGrouping(true), this.futureGrouped = true) : (this.updateSolrSettingGrouping(false), this.futureGrouped = false)
      this.$router.history.current.query.imgSearch === 'true' ? (this.updateSolrSettingImgSearch(true), this.futureImgSearch = true) : (this.updateSolrSettingImgSearch(false), this.futureImgSearch = false)
      this.$router.history.current.query.urlSearch === 'true' ? (this.updateSolrSettingUrlSearch(true), this.futureUrlSearch = true) : (this.updateSolrSettingUrlSearch(false), this.futureUrlSearch = false)
      if(this.solrSettings.imgSearch) {
           this.requestImageSearch({query:this.query})
           this.$_pushSearchHistory('SolrWayback', this.query, this.searchAppliedFacets, this.solrSettings)
        }
        else if(this.solrSettings.urlSearch) {
        }
        else {
          this.requestSearch({query:this.futureQuery, facets:this.searchAppliedFacets, options:this.solrSettings})
          this.requestFacets({query:this.futureQuery, facets:this.searchAppliedFacets, options:this.solrSettings})
          this.$_pushSearchHistory('SolrWayback', this.query, this.searchAppliedFacets, this.solrSettings)
        }
      }
  },
  
  methods: {
    ...mapActions('Search', {
      requestSearch: 'requestSearch',
      requestImageSearch: 'requestImageSearch',
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
      if (this.futureQuery !== this.query ||
          this.futureGrouped !== this.solrSettings.grouping ||
          this.futureUrlSearch !== this.solrSettings.urlSearch ||
          this.futureImgSearch !== this.solrSettings.imgSearch) 
        {
        console.log('search params changed!', this.query)
        this.clearResults()
        this.updateQuery(this.futureQuery)
        this.updateSolrSettingGrouping(this.futureGrouped)
        this.updateSolrSettingUrlSearch(this.futureUrlSearch)
        this.updateSolrSettingImgSearch(this.futureImgSearch)
        if(this.solrSettings.imgSearch) {
           this.requestImageSearch({query:this.query})
           this.$_pushSearchHistory('SolrWayback', this.query, this.searchAppliedFacets, this.solrSettings)
          return
        }
        else if(this.solrSettings.urlSearch) {
          return
        }
        else {
          this.requestSearch({query:this.futureQuery, facets:this.searchAppliedFacets, options:this.solrSettings})
          this.requestFacets({query:this.futureQuery, facets:this.searchAppliedFacets, options:this.solrSettings})
          this.$_pushSearchHistory('SolrWayback', this.query, this.searchAppliedFacets, this.solrSettings)
          return
        }
      }
    },
    selectSearchMethod(selected) {
      console.log(selected)
      if(selected === 'imgSearch') {
        this.futureImgSearch = !this.futureImgSearch
        this.futureImgSearch === true ? this.futureUrlSearch = false : null
        //this.solrSettings.urlSearch ? this.updateSolrSettingUrlSearch(!this.solrSettings.urlSearch) : null
        //this.updateSolrSettingImgSearch(!this.solrSettings.imgSearch)
      }
      else if(selected === 'urlSearch') {
        this.futureUrlSearch = !this.futureUrlSearch
        this.futureUrlSearch === true ? this.futureImgSearch = false : null
        //this.solrSettings.imgSearch ? this.updateSolrSettingImgSearch(!this.solrSettings.imgSearch) : null
        //this.updateSolrSettingUrlSearch(!this.solrSettings.urlSearch)
      }
    },
    clearResultsAndSearch() {
      history.pushState({name: 'SolrWayback'}, 'SolrWayback', '/')
      this.futureQuery = ''
      this.resetSearchState()
    },
  }
}

</script>

    