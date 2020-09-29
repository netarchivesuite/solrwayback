<template>
  <div class="searchBoxContainer">
    <form class="searchForm" @submit.prevent="$_determineNewSearch(futureQuery)">
      <span v-if="preNormalizedQuery !== null" class="orgQuery">Original query: <span class="preQuery">{{ preNormalizedQuery }}</span><span class="preQueryExplanation" title="When you search for an URL, we normalize it for you, so we can search the archive for you."> [ ? ]</span></span>
      <input id="query"
             v-model="futureQuery"
             type="text"
             autofocus
             :class="futureSolrSettings.urlSearch 
               ? decideActiveClassesForQueryBox()
               : ''"
             :placeholder="futureSolrSettings.urlSearch ? 'Enter search url' : 'Enter search term'">
      <transition name="url-search-helper">
        <span v-if="futureSolrSettings.urlSearch && futureQuery.substring(0,8) !== 'url_norm'" class="urlSearchHelper">URL:</span>
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
        <div @click.prevent="updateFutureSolrSettingGrouping(!futureSolrSettings.grouping)">
          <input id="groupedSearch"
                 :checked="futureSolrSettings.grouping"
                 type="checkbox"
                 name="groupedSearch"
                 @click.stop="updateFutureSolrSettingGrouping(!futureSolrSettings.grouping)">
          <label for="groupedSearch">Grouped search <span class="buttonExplanation" title="Grouping results by URL, meaning you only seen an URL as one hit, even though it might have been a hit on several params.">[ ? ]</span></label>
        </div>
        <div class="floatRight" @click.prevent="selectSearchMethod('urlSearch')">
          <input id="urlSearch"
                 :checked="futureSolrSettings.urlSearch"
                 type="checkbox"
                 name="urlSearch"
                 @click.stop="selectSearchMethod('urlSearch')">
          <label for="urlSearch">URL search <span class="buttonExplanation" title="Explanation goes here.">[ ? ]</span></label>
        </div>
        <div class="floatRight marginRight" @click.prevent="selectSearchMethod('imgSearch')">
          <input id="imgSearch"
                 :checked="futureSolrSettings.imgSearch"
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
      futureSolrSettings: state => state.Search.futureSolrSettings,
      loading: state => state.Search.loading,
    })
  },
  watch: {
    query: function (val) {
      this.futureQuery  = val
    },
  },
  mounted () {
    //console.log(this.$router.history.current.query)
    if(this.$router.history.current.query.q) {
      this.updateQuery(this.$router.history.current.query.q)
      this.futureQuery = this.$router.history.current.query.q
      this.$router.history.current.query.facets ? this.updateSearchAppliedFacets(this.$router.history.current.query.facets) : this.updateSearchAppliedFacets('')
      this.$router.history.current.query.grouping === 'true' ? this.updateFutureSolrSettingGrouping(true) : this.updateFutureSolrSettingGrouping(false)
      this.$router.history.current.query.imgSearch === 'true' ? this.updateFutureSolrSettingImgSearch(true) : this.updateFutureSolrSettingImgSearch(false)
      this.$router.history.current.query.urlSearch === 'true' ? this.updateFutureSolrSettingUrlSearch(true) : this.updateFutureSolrSettingUrlSearch(false)
      this.$_determineNewSearch(this.futureQuery)
      }
  },
  
  methods: {
    ...mapActions('Search', {
      requestSearch: 'requestSearch',
      requestImageSearch: 'requestImageSearch',
      requestUrlSearch:'requestUrlSearch',
      requestFacets: 'requestFacets',
      updateQuery: 'updateQuery',
      clearResults: 'clearResults',
      updateSearchAppliedFacets:'updateSearchAppliedFacets',
      resetSearchState:'resetState',
      updateSolrSettingGrouping:'updateSolrSettingGrouping',
      updateSolrSettingImgSearch:'updateSolrSettingImgSearch',
      updateSolrSettingUrlSearch:'updateSolrSettingUrlSearch',
      updateSolrSettingOffset:'updateSolrSettingOffset',
      updateFutureSolrSettingGrouping:'updateFutureSolrSettingGrouping',
      updateFutureSolrSettingImgSearch:'updateFutureSolrSettingImgSearch',
      updateFutureSolrSettingUrlSearch:'updateFutureSolrSettingUrlSearch',
      updatePreNormalizedQuery:'updatePreNormalizedQuery',

    }),
    ...mapActions('Notifier', {
      setNotification: 'setNotification'
     
    }),
    selectSearchMethod(selected) {
      console.log(selected)
      if(selected === 'imgSearch') {
        this.updateFutureSolrSettingImgSearch(!this.futureSolrSettings.imgSearch)
        this.futureSolrSettings.imgSearch ? this.updateFutureSolrSettingUrlSearch(false) : null
      }
      else if(selected === 'urlSearch') {
        this.updateFutureSolrSettingUrlSearch(!this.futureSolrSettings.urlSearch)
        this.futureSolrSettings.urlSearch ? this.updateFutureSolrSettingImgSearch(false) : null
      }
    },
    clearResultsAndSearch() {
      this.updateSolrSettingGrouping(false)
      this.updateSolrSettingImgSearch(false)
      this.updateSolrSettingUrlSearch(false)
      this.updateFutureSolrSettingGrouping(false)
      this.updateFutureSolrSettingImgSearch(false)
      this.updateFutureSolrSettingUrlSearch(false)
      history.pushState({name: 'SolrWayback'}, 'SolrWayback', '/')
      this.futureQuery = ''
      this.preNormalizeQuery = null

      this.resetSearchState()
    },
    decideActiveClassesForQueryBox() {
      return this.$_validateUrlSearchPrefix(this.futureQuery) === false 
                 ? this.futureQuery.substring(0,8) === 'url_norm' ? 'urlNotTrue' : 'urlNotTrue urlSearchActivated'
                 : this.futureQuery.substring(0,8) === 'url_norm' ? '' : 'urlSearchActivated' 
    }
  }
}

</script>

    