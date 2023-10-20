<template>
  <div>
    <transition name="url-search-helper">
      <div v-if="searchHints.length > 0" class="searchHints">
        <h2>Search Hints</h2>
        <ul>
          <li v-for="(hint, index) in searchHints"
              :key="index">
            <span class="searchHintHeading">{{ hint.substr(0, hint.indexOf('.')) }}</span><span>{{ hint.substr(hint.indexOf('.')) }}</span>
          </li>
        </ul>
      </div>
    </transition>   
    <div class="searchBoxContainer">
      <form class="searchForm" @submit.prevent="launchNewSearch()">
        <span v-if="preNormalizedQuery !== null" class="orgQuery">Normalized query<span class="preQueryExplanation" title="When you search for an URL, we normalize it for you - this is the normalized query we used for the search">[ ? ]</span>: <span class="preQuery" :title="normalizedQuery"><span class="urlNormPrefix">url_norm:"</span>{{ normalizedQuery }}<span class="urlNormPrefix">"</span></span><span class="copyToClipboard"
                                                                                                                                                                                                                                                                                                                                                                                                                       :class="normalizedQueryCopied ? 'checkmarkIcon' : 'clipboardIcon'"
                                                                                                                                                                                                                                                                                                                                                                                                                       title="Copy normalized query to clipboard"
                                                                                                                                                                                                                                                                                                                                                                                                                       @click.prevent="copyURLSearchQuery()" /></span>
        <transition name="url-search-helper">
          <span v-if="solrSettings.urlSearch" class="urlSearchHelper">URL:</span>
        </transition>
        <textarea id="query"
                  ref="query"
                  v-model="futureQuery"
                  type="text"
                  rows="1"
                  autofocus
                  :class="solrSettings.urlSearch 
                    ? decideActiveClassesForQueryBox()
                    : ''"
                  :placeholder="solrSettings.urlSearch ? 'Enter search url' : 'Enter search term'"
                  @keydown.enter="checkKeyPresses()"
                  @keyup.prevent="solrSettings.urlSearch ? null : checkQuery()"
                  @input="$_getSizeOfTextArea('query')" />
        <button type="button"
                title="See search guidelines"
                class="searchGuidelinesButton"
                @click.prevent="openSelectedModal('guidelines')">
          ?
        </button>
        <button id="querySubmit" title="Search" type="submit">
          <div id="magnifyingGlass" />
        </button>
        <button v-if="futureQuery !== '' || Object.keys(results).length !== 0"
                id="clearSubmit"
                title="Clear search and results"
                type="button"
                @click="clearResultsAndSearch()" />
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
            <label for="urlSearch">URL search <span class="buttonExplanation" title="Search for a specific URL. The search will also puny encode the domain name and normalize the URL so all different representations of the same URL is found.">[ ? ]</span></label>
          </div>
          <div class="floatRight marginRight" @click.prevent="selectSearchMethod('imgSearch')">
            <input id="imgSearch"
                   :checked="solrSettings.imgSearch"
                   type="checkbox"
                   name="imgSearch"
                   @click.stop="selectSearchMethod('imgSearch')">
            <label for="imgSearch">Image search <span class="buttonExplanation" title="Will show images found in  HTML pages near the search text and also include images where search text matches meta-data or resource name in URL.">[ ? ]</span></label>
          </div>
        </div>
        <div class="tools">
          <button :class="getSearchFileUploadClass()"
                  :title="getSearchWithUploadedFileTitle()"
                  :disabled="searchWithUploadedFileDisabled()"
                  @click.prevent="showUploadFileSearch = !showUploadFileSearch">
            Search with uploaded file
          </button>
          <button class="collectionInfo" @click.prevent="openSelectedModal('collectioninfo')">
            <span class="collectionInfoText">About the collection</span>
            <span class="collectionInfoIcon" />
          </button>
          <button class="toolbox" @click.prevent="toggleToolbox()">
            <span class="toolboxText">Toolbox</span>
            <span class="toolboxIcon" />
          </button>
          <button class="gpsSearchButton" @click.prevent="openSelectedModal('gpssearch')">
            <span class="gpsText">GPS Image Search</span>
            <span class="gpsIcon" />
          </button>
        </div>
      </form>
      <applied-search-facets />
      <search-upload-file v-if="showUploadFileSearch" />
      <toolbox v-if="showToolbox" @close-toolbox="toggleToolbox()" />
    </div>
  </div>
</template> 

<script>
import { mapState, mapActions } from 'vuex'
import {copyTextToClipboard} from '../utils/globalUtils'
import AppliedSearchFacets from './AppliedSearchFacets.vue'
import HistoryRoutingUtils from './../mixins/HistoryRoutingUtils'
import SearchboxUtils from './../mixins/SearchboxUtils'
import SearchUtils from './../mixins/SearchUtils'
import SearchUploadFile from './SearchUploadFile.vue'
import Toolbox from './Toolbox.vue'
import {debounce} from './../utils/globalUtils'
import { isUploadFileSearchDisabled } from '../configs/configHelper'

export default {
  components: {
    AppliedSearchFacets,
    SearchUploadFile,
    Toolbox,
    },
  mixins: [HistoryRoutingUtils, SearchUtils, SearchboxUtils],
  data () {
    return {    
      futureQuery:'',
      showUploadFileSearch: false,
      showToolbox: false,
      searchHints:[],
      normalizedQueryCopied: false
    }
  },
  computed: {
    ...mapState({
      query: state => state.Search.query,
      preNormalizedQuery: state => state.Search.preNormalizedQuery,
      normalizedQuery: state => state.Search.normalizedQuery,
      searchAppliedFacets: state => state.Search.searchAppliedFacets,
      results: state => state.Search.results,
      solrSettings: state => state.Search.solrSettings,
      loading: state => state.Search.loading,
      showModal: state => state.Modal.showModal,
      currentModal: state => state.Modal.currentModal
    })
  },
  watch: {
    query: function (val) {
      this.futureQuery  = val
    },
  },
  mounted () {
    const routerQuery = this.$router.history.current.query
    if(routerQuery.query) {
      this.futureQuery =  routerQuery.query
      if(routerQuery.facets) {
      let newFacets = routerQuery.facets.split('&fq=')
      newFacets.shift()
      newFacets.length > 0 ? newFacets.forEach((item) => {
        this.addToSearchAppliedFacets('&fq=' + item)  
      }) : null
      }
      routerQuery.grouping === 'true' || routerQuery.grouping === true ? this.updateSolrSettingGrouping(true) : this.updateSolrSettingGrouping(false)
      routerQuery.imgSearch === 'true' || routerQuery.imgSearch === true ? this.updateSolrSettingImgSearch(true) : this.updateSolrSettingImgSearch(false)
      routerQuery.urlSearch === 'true' || routerQuery.urlSearch === true ? this.updateSolrSettingUrlSearch(true) : this.updateSolrSettingUrlSearch(false)
      routerQuery.offset ? this.updateSolrSettingOffset(Number(routerQuery.offset)) : this.updateSolrSettingOffset(0)
          routerQuery.sort ? this.updateSolrSettingSort(routerQuery.sort) : this.updateSolrSettingSort('score desc')
      this.$_determineNewSearch(this.futureQuery, false)
      }
      else {
        //If we mount and there is no query, we just make sure to empty the state, results and facets for good measure.
        //If this is resulted by a backbutton going to the first time the user landed on the page, we want a clean slate.
        this.resetSearchState()
      }
      this.$refs.query.value = this.futureQuery
      this.$_getSizeOfTextArea('query')
  },

  created() {
    //debouncing dynamically initiated via created hook to prevent weird debounce behaviour across instances of this component
    this.checkQuery = debounce(this.checkQuery, 500)
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
      updateSolrSettingSort:'updateSolrSettingSort',
      emptySearchAppliedFacets:'emptySearchAppliedFacets'
    }),
    ...mapActions('Modal', {
      updateShowModal:'updateShowModal',
      updateCurrentModal:'updateCurrentModal'
    }),
    checkKeyPresses() {
      !event.shiftKey ? (event.preventDefault(),this.launchNewSearch()) : null
    },
    selectSearchMethod(selected) {
      if(selected === 'imgSearch') {
        this.updateSolrSettingImgSearch(!this.solrSettings.imgSearch)
        this.updateSolrSettingUrlSearch(false)
      }
      else if(selected === 'urlSearch') {
        this.updateSolrSettingUrlSearch(!this.solrSettings.urlSearch)
        this.updateSolrSettingImgSearch(false)
        if (!this.$_validateUrlSearchPrefix(this.futureQuery)) {
            this.futureQuery = 'http://' + this.futureQuery
        }
        if(this.solrSettings.urlSearch === false) {
          this.futureQuery = ''
        }
      }
    },
    clearResultsAndSearch() {
      this.updateSolrSettingGrouping(false)
      this.updateSolrSettingImgSearch(false)
      this.updateSolrSettingUrlSearch(false)
      this.futureQuery = ''
      // Check if we gotta push a route (if we're on the frontpage and just deleting query and no results, we dont have to.)
      if(Object.keys(this.results).length !== 0) {
        this.$_pushCleanHistory('SolrWayback')
      }
      this.preNormalizeQuery = null
      this.resetSearchState()
      //Make sure the query textarea is returned to its original size, regardless of what it was before.
      this.$refs.query.style.height = '1px'
    },
    decideActiveClassesForQueryBox() {
      const isPrefixUrlNorm = this.futureQuery.substring(0,8) === 'url_norm'
      return !this.$_validateUrlSearchPrefix(this.futureQuery) 
                 ? isPrefixUrlNorm === 'url_norm' 
                    ? 'urlNotTrue' 
                    : 'urlNotTrue'
                 : isPrefixUrlNorm !== 'url_norm' 
                    ? '' 
                    : '' 
    },
    launchNewSearch() {
      this.emptySearchAppliedFacets()
      this.updateSolrSettingOffset(0)
      this.updateSolrSettingSort('score desc')
      this.$_pushSearchHistory('Search', this.futureQuery, this.searchAppliedFacets, this.solrSettings)
    },
    openSelectedModal(modal) {
      this.updateShowModal(!this.showModal),
      this.updateCurrentModal(modal)
    },
    toggleToolbox() {
      this.showToolbox = !this.showToolbox
    },

    checkQuery() {
      this.searchHints = this.$_checkQueryForBadSyntax(this.futureQuery.trim())
    },

    searchWithUploadedFileDisabled(){
      return isUploadFileSearchDisabled()
    },

    getSearchFileUploadClass() {
      return `searchByFileButton ${this.searchWithUploadedFileDisabled() ? 'fade' : ''}`
    },

    getSearchWithUploadedFileTitle() {
      return this.searchWithUploadedFileDisabled() ? 'Search by uploaded file has been disabled in the configuration' : ''
    },
    copyURLSearchQuery(){
      const normalizedQueryToCopy = `url_norm:"${this.normalizedQuery}"`  
      if (copyTextToClipboard(normalizedQueryToCopy)) {
         this.normalizedQueryCopied = true
          setTimeout(() => {
            this.normalizedQueryCopied = false
            },
            3000)
        }
    }
  }
}

</script>

    