<template>
  <div>
    <span v-if="!results.cardinality">
      <span>Showing <span class="highlightText">{{ results.numFound !== 0 ? solrSettings.offset + 1 : 0 }}</span>  - <span class="highlightText">{{ solrSettings.offset + hitsPerPage > results.numFound ? results.numFound : solrSettings.offset + hitsPerPage }}</span> of </span>
      <span class="highlightText">{{ results.numFound.toLocaleString("en") }}</span> entries matching query.
    </span>
    <span v-if="results.cardinality">
      <span>Showing <span class="highlightText">{{ results.numFound !== 0 ? solrSettings.offset + 1 : 0 }}</span> - <span class="highlightText">{{ solrSettings.offset + hitsPerPage > results.cardinality ? results.cardinality : solrSettings.offset + hitsPerPage }}</span> of </span>
      <span class="highlightText">{{ results.cardinality.toLocaleString("en") }}</span> unique entries matching query 
      <span class="tonedDownText">(total hits approximated: {{ results.numFound.toLocaleString("en") }})</span>.
    </span>
    <div class="postSearchContainer">
      <div v-if="results.cardinality !== 0 && results.numFound !== 0" class="pagingContainer">
        <button :disabled="solrSettings.offset < hitsPerPage" @click="getPreviousResults()">
          Previous {{ hitsPerPage }}
        </button>
        <button :disabled="results.cardinality ? solrSettings.offset + hitsPerPage >= results.cardinality : solrSettings.offset + hitsPerPage >= results.numFound" @click="getNextResults()">
          Next {{ hitsPerPage }}
        </button>
      </div>
      <div class="sortContainer">
        <span>Sort by: </span>
        <select id="sortSelect" v-model="sortInput" @change="getResultsWithSort($event)">
          <option value="score desc">
            score desc
          </option>
          <option value="crawl_date desc">
            crawl_date desc
          </option>
          <option value="crawl_date asc">
            crawl_date asc
          </option>
        </select>
      </div>
    </div>
    <div v-if="results && results !== {}" class="results">
      <component :is="SingleEntryComponent(result.type)"
                 v-for="(result, index) in results.docs"
                 :key="index"
                 :result="result"
                 :rank-number="index" />
    </div>
    <div v-if="results.cardinality !== 0 && results.numFound !== 0" class="pagingContainer">
      <button :disabled="solrSettings.offset < hitsPerPage" @click="getPreviousResults()">
        Previous {{ hitsPerPage }}
      </button>
      <button :disabled="results.cardinality ? solrSettings.offset + hitsPerPage >= results.cardinality : solrSettings.offset + hitsPerPage >= results.numFound" @click="getNextResults()">
        Next {{ hitsPerPage }}
      </button>
    </div>
  </div>
</template>

<script>
import { mapState, mapActions } from 'vuex'
import SearchFacetOptions from './../SearchFacetOptions.vue'
import HistoryRoutingUtils from './../../mixins/HistoryRoutingUtils'
import ImageSearchResults from './ImageSearchResults'
import configs from '../../configs'


export default {
  name: 'PostSearchResults',
  components: {
    SearchSingleItemDefault: () => import('./../searchSingleItemComponents/searchSingleItemTypes/SearchSingleItemDefault'),
    SearchSingleItemTweet: () => import('./../searchSingleItemComponents/searchSingleItemTypes/SearchSingleItemTweet'),
    SearchSingleItemWeb: () => import('./../searchSingleItemComponents/searchSingleItemTypes/SearchSingleItemWeb'),
    SearchSingleItemImage: () => import('./../searchSingleItemComponents/searchSingleItemTypes/SearchSingleItemImage'),
    SearchSingleItemVideoAudio: () => import('./../searchSingleItemComponents/searchSingleItemTypes/SearchSingleItemVideoAudio'),
   
   SearchFacetOptions,
    ImageSearchResults
  },
  mixins: [HistoryRoutingUtils],
  data () {
    return {
            hitsPerPage: 20
        }
  },
  computed: {
    ...mapState({
      query: state => state.Search.query,
      searchAppliedFacets: state => state.Search.searchAppliedFacets,
      results: state => state.Search.results,
      solrSettings: state => state.Search.solrSettings
    }),
    sortInput: {
      get () {
        return this.$store.state.Search.solrSettings.sort
      },
      set (value) {
        this.updateSolrSettingSort(value)
      }
    }
  },
  mounted () {
    this.hitsPerPage = parseInt(configs.search.pagination)
  },
  methods: {
    ...mapActions('Search', {
      updateSolrSettingOffset:'updateSolrSettingOffset',
      updateSolrSettingSort: 'updateSolrSettingSort'
    }),
    getNextResults() {
      this.updateSolrSettingOffset(this.solrSettings.offset + this.hitsPerPage)
      this.$_pushSearchHistory('Search', this.query, this.searchAppliedFacets, this.solrSettings)
    },
    getPreviousResults() {
      this.updateSolrSettingOffset(this.solrSettings.offset - this.hitsPerPage)
      this.$_pushSearchHistory('Search', this.query, this.searchAppliedFacets, this.solrSettings)
    },
    getResultsWithSort(event) {
      this.updateSolrSettingSort(event.target.value)
      this.$_pushSearchHistory('Search', this.query, this.searchAppliedFacets, this.solrSettings)
    },
    SingleEntryComponent(type) {
      //console.log('search result type', type)
      switch(type) {   
        case 'Web Page': return 'SearchSingleItemWeb'
        case 'Image': return 'SearchSingleItemImage'
        case 'Twitter Tweet': return 'SearchSingleItemTweet'
        case 'Video' : 
        case 'Audio' : return 'SearchSingleItemVideoAudio'
        default: return 'SearchSingleItemDefault'
      }
    }
  }
}
</script>