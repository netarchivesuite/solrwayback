<template>
  <div class="searchBoxContainer">
    <form class="searchForm" @submit.prevent="handleSubmit">
      <input id="query"
             v-model="futureQuery"
             type="text"
             autofocus
             :class="solrSettings.urlSearch 
               ? validateUrl(futureQuery) === false 
                 ? query.substring(0,8) === 'url_norm' ? 'urlNotTrue' : 'urlNotTrue urlSearchActivated'
                 : query.substring(0,8) === 'url_norm' ? '' : 'urlSearchActivated' 
               : ''"
             :placeholder="solrSettings.urlSearch ? 'Enter search url' : 'Enter search term'">
      <transition name="url-search-helper">
        <span v-if="solrSettings.urlSearch && query.substring(0,8) !== 'url_norm'" class="urlSearchHelper">url_norm:</span>
      </transition>
      <button id="querySubmit" title="Search" type="submit">
        <div id="magnifyingGlass" />
      </button>
      <button v-if="futureQuery !== ''"
              id="clearSubmit"
              title="Clear search and results"
              type="button"
              @click="clearResultsAndSearch" />
      <div class="sortOptions">
        <div @click.prevent="updateSolrSettingGrouping(!solrSettings.grouping)">
          <input id="groupedSearch"
                 :checked="solrSettings.grouping"
                 type="checkbox"
                 name="groupedSearch">
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
    //console.log(this.$router.history.current.query)
    if(this.$router.history.current.query.q) {
      this.updateQuery(this.$router.history.current.query.q)
      this.futureQuery = this.$router.history.current.query.q
      this.$router.history.current.query.facets ? this.updateSearchAppliedFacets(this.$router.history.current.query.facets) : null
      this.$router.history.current.query.grouping === 'true' ? this.updateSolrSettingGrouping(true) : null
      this.$router.history.current.query.imgSearch === 'true' ? this.updateSolrSettingImgSearch(true) : null
      this.$router.history.current.query.urlSearch === 'true' ? this.updateSolrSettingUrlSearch(true) : null
      if(this.solrSettings.imgSearch) {
          console.log('firing image search')
           this.requestImageSearch({query:this.query})
           this.$_pushSearchHistory('SolrWayback', this.query, this.searchAppliedFacets, this.solrSettings)
        }
        else if(this.solrSettings.urlSearch) {
          let queryString = ''
          if(this.futureQuery.substring(0,10) === 'url_norm:"') {
            queryString = this.futureQuery.replace('url_norm:"', '').slice(0,-1)
          }
          else {
            queryString = this.futureQuery
          }
          if(this.validateUrl(queryString)) {
            this.updateQuery('url_norm:"' + queryString + '"')
            this.requestUrlSearch({query:queryString, facets:this.searchAppliedFacets, options:this.solrSettings})
            this.requestFacets({query:queryString, facets:this.searchAppliedFacets, options:this.solrSettings})
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
          console.log('firing normal search')
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
        //console.log('search params changed!')
        this.clearResults()
        this.updateQuery(this.futureQuery)
        this.updateSolrSettingOffset(0)
        this.futureGrouped = this.solrSettings.grouping
        this.futureUrlSearch = this.solrSettings.urlSearch
        this.futureImgSearch = this.solrSettings.imgSearch
        if(this.solrSettings.imgSearch) {
           this.requestImageSearch({query:this.query})
           this.$_pushSearchHistory('SolrWayback', this.query, this.searchAppliedFacets, this.solrSettings)
        }
        else if(this.solrSettings.urlSearch) {
          let queryString = ''
          if(this.futureQuery.substring(0,10) === 'url_norm:"') {
            queryString = this.futureQuery.replace('url_norm:"', '').slice(0,-1)
          }
          else {
            queryString = this.futureQuery
          }
          if(this.validateUrl(queryString)) {
            this.updateQuery('url_norm:"' + queryString + '"')
            this.requestUrlSearch({query:queryString, facets:this.searchAppliedFacets, options:this.solrSettings})
            this.requestFacets({query:queryString, facets:this.searchAppliedFacets, options:this.solrSettings})
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
          return
        }
      }
    },
    selectSearchMethod(selected) {
      if(selected === 'imgSearch') {
        this.solrSettings.urlSearch ? this.updateSolrSettingUrlSearch(!this.solrSettings.urlSearch) : null
        this.updateSolrSettingImgSearch(!this.solrSettings.imgSearch)
        return
      }
      if(selected === 'urlSearch') {
        this.solrSettings.imgSearch ? this.updateSolrSettingImgSearch(!this.solrSettings.imgSearch) : null
        this.updateSolrSettingUrlSearch(!this.solrSettings.urlSearch)
        return
      }
    },
    clearResultsAndSearch() {
      history.pushState({name: 'SolrWayback'}, 'SolrWayback', '/')
      this.futureQuery = ''
      this.resetSearchState()
    },
    validateUrl(testString) {
      return testString.substring(0,7) === 'http://' || 
             testString.substring(0,8) === 'https://' || 
             testString.substring(0,10) === 'url_norm:"'
      ? true
      : false
    }
  }
}

</script>

    