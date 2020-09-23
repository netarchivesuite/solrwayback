<template>
  <div class="searchBoxContainer">
    <form class="searchForm" @submit.prevent="handleSubmit">
      <span v-if="preNormalizeQuery !== null" class="orgQuery">Original query: <span class="preQuery">{{ preNormalizeQuery }}</span><span class="preQueryExplanation" title="When you search for an URL, we normalize it for you, so we can search the archive for you."> [ ? ]</span></span>
      <input id="query"
             v-model="futureQuery"
             type="text"
             autofocus
             :class="futureUrlSearch 
               ? decideActiveClassesForQueryBox()
               : ''"
             :placeholder="futureUrlSearch ? 'Enter search url' : 'Enter search term'">
      <transition name="url-search-helper">
        <span v-if="futureUrlSearch && futureQuery.substring(0,8) !== 'url_norm'" class="urlSearchHelper">URL:</span>
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
      preNormalizeQuery:null,
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
    //console.log(this.$router.history.current.query)
    if(this.$router.history.current.query.q) {
      this.updateQuery(this.$router.history.current.query.q)
      this.futureQuery = this.$router.history.current.query.q
      this.$router.history.current.query.facets ? this.updateSearchAppliedFacets(this.$router.history.current.query.facets) : null
      this.$router.history.current.query.grouping === 'true' ? (this.updateSolrSettingGrouping(true), this.futureGrouped = true) : null
      this.$router.history.current.query.imgSearch === 'true' ? (this.updateSolrSettingImgSearch(true), this.futureImgSearch = true) : null
      this.$router.history.current.query.urlSearch === 'true' ? (this.updateSolrSettingUrlSearch(true), this.futureUrlSearch = true) : null
      if(this.solrSettings.imgSearch) {
           this.requestImageSearch({query:this.query})
           this.$_pushSearchHistory('SolrWayback', this.query, this.searchAppliedFacets, this.solrSettings)
        }
        else if(this.solrSettings.urlSearch) {
          this.preNormalizeQuery = this.futureQuery
          let queryString = ''
          if(this.futureQuery.substring(0,10) === 'url_norm:"') {
            queryString = this.futureQuery.replace('url_norm:"', '')
            queryString.substring(queryString.length-1, queryString.length) === '"' ? queryString = queryString.slice(0,-1) : null
          }
          else {
            queryString = this.futureQuery
          }
          if(this.validateUrl(queryString)) {
            this.updateQuery('url_norm:"' + queryString + '"')
            this.requestUrlSearch({query:queryString, facets:this.searchAppliedFacets, options:this.solrSettings})
            this.requestFacets({query:'url_norm:"' + queryString + '"', facets:this.searchAppliedFacets, options:this.solrSettings})
            this.$_pushSearchHistory('SolrWayback', this.query, this.searchAppliedFacets, this.solrSettings)
          }
          else {
            this.setNotification({
          	title: 'We are so sorry!',
            text: 'This query is not valid. the url must start with \'http://\' or \'https://\'',
            type: 'error',
            timeout: false
          })
          }
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
      requestUrlSearch:'requestUrlSearch',
      requestFacets: 'requestFacets',
      updateQuery: 'updateQuery',
      clearResults: 'clearResults',
      updateSearchAppliedFacets:'updateSearchAppliedFacets',
      resetSearchState:'resetState',
      updateSolrSettingGrouping:'updateSolrSettingGrouping',
      updateSolrSettingImgSearch:'updateSolrSettingImgSearch',
      updateSolrSettingUrlSearch:'updateSolrSettingUrlSearch',
      updateSolrSettingOffset:'updateSolrSettingOffset'
    }),
    ...mapActions('Notifier', {
      setNotification: 'setNotification'
     
    }),
    handleSubmit() {
      if (this.futureQuery !== this.query ||
          this.futureGrouped !== this.solrSettings.grouping ||
          this.futureUrlSearch !== this.solrSettings.urlSearch ||
          this.futureImgSearch !== this.solrSettings.imgSearch) 
        {
        console.log('search params changed!')
        this.preNormalizeQuery = null
        this.clearResults()
        this.updateQuery(this.futureQuery)
        this.updateSolrSettingOffset(0)
        this.updateSolrSettingGrouping(this.futureGrouped)
        this.updateSolrSettingUrlSearch(this.futureUrlSearch)
        this.updateSolrSettingImgSearch(this.futureImgSearch)
        if(this.solrSettings.imgSearch) {
           this.requestImageSearch({query:this.query})
           this.$_pushSearchHistory('SolrWayback', this.query, this.searchAppliedFacets, this.solrSettings)
        }
        else if(this.solrSettings.urlSearch) {
          this.preNormalizeQuery = this.futureQuery
          let queryString = ''
          if(this.futureQuery.substring(0,10) === 'url_norm:"') {
            queryString = this.futureQuery.replace('url_norm:"', '')
            queryString.substring(queryString.length-1, queryString.length) === '"' ? queryString = queryString.slice(0,-1) : null
          }
          else {
            queryString = this.futureQuery
          }
          if(this.validateUrl(queryString)) {
            this.requestUrlSearch({query:queryString, facets:this.searchAppliedFacets, options:this.solrSettings})
            this.requestFacets({query:'url_norm:"' + queryString + '"', facets:this.searchAppliedFacets, options:this.solrSettings})
            this.$_pushSearchHistory('SolrWayback', queryString, this.searchAppliedFacets, this.solrSettings)
          }
          else {
            this.setNotification({
          	title: 'We are so sorry!',
            text: 'This query is not valid. the url must start with \'http://\' or \'https://\'',
            type: 'error',
            timeout: false
          })
          }
        }
        else {
          this.requestSearch({query:this.futureQuery, facets:this.searchAppliedFacets, options:this.solrSettings})
          this.requestFacets({query:this.futureQuery, facets:this.searchAppliedFacets, options:this.solrSettings})
          this.$_pushSearchHistory('SolrWayback', this.query, this.searchAppliedFacets, this.solrSettings)
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
      this.updateSolrSettingGrouping(false)
      this.futureGrouped = false
      this.updateSolrSettingImgSearch(false)
      this.futureImgSearch = false
      this.updateSolrSettingUrlSearch(false)
      this.futureUrlSearch = false
      history.pushState({name: 'SolrWayback'}, 'SolrWayback', '/')
      this.futureQuery = ''
      this.preNormalizeQuery = null
      this.futureGrouped = false
      this.futureUrlSearch = false
      this.futureImgSearch = false
      this.resetSearchState()
    },
    validateUrl(testString) {
      return testString.substring(0,7) === 'http://' || 
             testString.substring(0,8) === 'https://' || 
             testString.substring(0,10) === 'url_norm:"'
    },
    decideActiveClassesForQueryBox() {
      return this.validateUrl(this.futureQuery) === false 
                 ? this.futureQuery.substring(0,8) === 'url_norm' ? 'urlNotTrue' : 'urlNotTrue urlSearchActivated'
                 : this.futureQuery.substring(0,8) === 'url_norm' ? '' : 'urlSearchActivated' 
    }
  }
}

</script>

    