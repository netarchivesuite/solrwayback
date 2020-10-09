<template>
  <div class="searchBoxContainer">
    <form class="searchForm" @submit.prevent="launchNewSearch()">
      <span v-if="preNormalizedQuery !== null" class="orgQuery">Original query: <span class="preQuery">{{ preNormalizedQuery }}</span><span class="preQueryExplanation" title="When you search for an URL, we normalize it for you, so we can search the archive for you."> [ ? ]</span></span>
      <input id="query"
             v-model="futureQuery"
             type="text"
             autofocus
             :class="solrSettings.urlSearch 
               ? decideActiveClassesForQueryBox()
               : ''"
             :placeholder="solrSettings.urlSearch ? 'Enter search url' : 'Enter search term'">
      <transition name="url-search-helper">
        <span v-if="solrSettings.urlSearch && futureQuery.substring(0,8) !== 'url_norm'" class="urlSearchHelper">URL:</span>
      </transition>
      <button id="querySubmit" title="Search" type="submit">
        <div id="magnifyingGlass" />
      </button>
      <button v-if="futureQuery !== '' || Object.keys(results).length !== 0"
              id="clearSubmit"
              title="Clear search and results"
              type="button"
              @click="clearResultsAndSearch" />
      <div class="sortOptions">
        <div @click.prevent="updateSolrSettingGrouping(!solrSettings.grouping)">
          <input id="groupedSearch"
                 :checked="solrSettings.grouping"
                 type="checkbox"
                 name="groupedSearch"
                 @click.stop="updateSolrSettingGrouping(!solrSettings.grouping)">
          <label for="groupedSearch">Grouped search <span class="buttonExplanation" title="Grouping results by URL, meaning you only seen an URL as one hit, even though it might have been a hit on several params.">[ ? ]</span></label>
        </div>
        <div class="floatRight" @click.prevent="selectSearchMethod('urlSearch')">
          <input id="urlSearch"
                 :checked="solrSettings.urlSearch"
                 type="checkbox"
                 name="urlSearch"
                 @click.stop="selectSearchMethod('urlSearch')">
          <label for="urlSearch">URL search <span class="buttonExplanation" title="Explanation goes here.">[ ? ]</span></label>
        </div>
        <div class="floatRight marginRight" @click.prevent="selectSearchMethod('imgSearch')">
          <input id="imgSearch"
                 :checked="solrSettings.imgSearch"
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
import SearchUtils from './../mixins/SearchUtils'
import SearchUploadFile from './SearchUploadFile.vue'

export default {
  components: {
    AppliedSearchFacets,
    SearchUploadFile
  },
  mixins: [HistoryRoutingUtils, SearchUtils],
  data () {
    return {    
      futureQuery:'',
      showUploadFileSearch: false

    }
  },
  computed: {
    ...mapState({
      query: state => state.Search.query,
      preNormalizedQuery: state => state.Search.preNormalizedQuery,
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
    const routerQuery = this.$router.history.current.query
    if(routerQuery.query) {
      this.futureQuery = decodeURIComponent(routerQuery.query)
      if(routerQuery.facets) {
      let newFacets = routerQuery.facets.split('&fq=')
      newFacets.shift()
      newFacets.length > 0 ? newFacets.forEach((item) => {
        this.addToSearchAppliedFacets('&fq=' + item)  
      }) : null
      }
      routerQuery.grouping === 'true' ? this.updateSolrSettingGrouping(true) : this.updateSolrSettingGrouping(false)
      routerQuery.imgSearch === 'true' ? this.updateSolrSettingImgSearch(true) : this.updateSolrSettingImgSearch(false)
      routerQuery.urlSearch === 'true' ? this.updateSolrSettingUrlSearch(true) : this.updateSolrSettingUrlSearch(false)
      routerQuery.offset ? this.updateSolrSettingOffset(Number(routerQuery.offset)) : this.updateSolrSettingOffset(0)
      this.$_determineNewSearch(this.futureQuery, false)
      }
  },
  
  methods: {
    ...mapActions('Search', {
      clearResults: 'clearResults',
      addToSearchAppliedFacets:'addToSearchAppliedFacets',
      resetSearchState:'resetState',
      updateSolrSettingGrouping:'updateSolrSettingGrouping',
      updateSolrSettingImgSearch:'updateSolrSettingImgSearch',
      updateSolrSettingUrlSearch:'updateSolrSettingUrlSearch',
      updateSolrSettingOffset:'updateSolrSettingOffset',
      emptySearchAppliedFacets:'emptySearchAppliedFacets'
    }),
    selectSearchMethod(selected) {
      console.log(selected)
      if(selected === 'imgSearch') {
        this.updateSolrSettingImgSearch(!this.solrSettings.imgSearch)
        this.updateSolrSettingUrlSearch(false)
      }
      else if(selected === 'urlSearch') {
        this.updateSolrSettingUrlSearch(!this.solrSettings.urlSearch)
        this.updateSolrSettingImgSearch(false)
      }
    },
    clearResultsAndSearch() {
      this.updateSolrSettingGrouping(false)
      this.updateSolrSettingImgSearch(false)
      this.updateSolrSettingUrlSearch(false)
      this.futureQuery = ''
      this.$_pushCleanHistory('SolrWayback')
      this.preNormalizeQuery = null
      this.resetSearchState()
    },
    decideActiveClassesForQueryBox() {
      const isPrefixUrlNorm = this.futureQuery.substring(0,8) === 'url_norm'
      return this.$_validateUrlSearchPrefix(this.futureQuery) === false 
                 ? isPrefixUrlNorm === 'url_norm' 
                    ? 'urlNotTrue' 
                    : 'urlNotTrue urlSearchActivated'
                 : isPrefixUrlNorm !== 'url_norm' 
                    ? '' 
                    : 'urlSearchActivated' 
    },
    launchNewSearch() {
      this.emptySearchAppliedFacets()
      this.updateSolrSettingOffset(0)
      this.$_determineNewSearch(this.futureQuery, true)
      //this.$_pushSearchHistory('Search', this.futureQuery, this.searchAppliedFacets, this.solrSettings)
    }
  }
}

</script>

    