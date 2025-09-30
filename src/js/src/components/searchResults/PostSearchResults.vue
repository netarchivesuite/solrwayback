<template>
  <div>
    <span v-if="!this.searchStore.results.cardinality">
      <span>Showing <span class="highlightText">{{ this.searchStore.results.numFound !== 0 ? this.searchStore.solrSettings.offset + 1 : 0 }}</span>  - <span class="highlightText">{{ this.searchStore.solrSettings.offset + hitsPerPage > this.searchStore.results.numFound ? this.searchStore.results.numFound : this.searchStore.solrSettings.offset + hitsPerPage }}</span> of </span>
      <span class="highlightText">{{ this.searchStore.results.numFound.toLocaleString("en") }}</span> entries matching query.
    </span>
    <span v-if="this.searchStore.results.cardinality">
      <span>Showing <span class="highlightText">{{ this.searchStore.results.numFound !== 0 ? this.searchStore.solrSettings.offset + 1 : 0 }}</span> - <span class="highlightText">{{ this.searchStore.solrSettings.offset + hitsPerPage > this.searchStore.results.cardinality ? this.searchStore.results.cardinality : this.searchStore.solrSettings.offset + hitsPerPage }}</span> of </span>
      <span class="highlightText">{{ this.searchStore.results.cardinality.toLocaleString("en") }}</span> unique entries matching query 
      <span class="tonedDownText">(total hits approximated: {{ this.searchStore.results.numFound.toLocaleString("en") }})</span>.
    </span>
    <div class="postSearchContainer">
      <div v-if="this.searchStore.results.cardinality !== 0 && this.searchStore.results.numFound !== 0" class="pagingContainer">
        <button :disabled="this.searchStore.solrSettings.offset < hitsPerPage" @click="getPreviousResults()">
          Previous {{ hitsPerPage }}
        </button>
        <button :disabled="this.searchStore.results.cardinality ? this.searchStore.solrSettings.offset + hitsPerPage >= this.searchStore.results.cardinality : this.searchStore.solrSettings.offset + hitsPerPage >= this.searchStore.results.numFound" @click="getNextResults()">
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
    <div v-if="this.searchStore.results && Object.keys(this.searchStore.results).length !== 0" class="results">
      <component :is="SingleEntryComponent(result.type)"
                 v-for="(result, index) in this.searchStore.results.docs"
                 :key="index"
                 :result="result"
                 :rank-number="index" />
    </div>
    <div v-if="this.searchStore.results.cardinality !== 0 && this.searchStore.results.numFound !== 0" class="pagingContainer">
      <button :disabled="this.searchStore.solrSettings.offset < hitsPerPage" @click="getPreviousResults()">
        Previous {{ hitsPerPage }}
      </button>
      <button :disabled="this.searchStore.results.cardinality ? this.searchStore.solrSettings.offset + hitsPerPage >= this.searchStore.results.cardinality : this.searchStore.solrSettings.offset + hitsPerPage >= this.searchStore.results.numFound" @click="getNextResults()">
        Next {{ hitsPerPage }}
      </button>
    </div>
  </div>
</template>

<script>

import { mapStores, mapActions } from 'pinia'
import { useSearchStore } from '../../store/search.store'
import SearchFacetOptions from './../SearchFacetOptions.vue'
import HistoryRoutingUtils from './../../mixins/HistoryRoutingUtils'
import ImageSearchResults from './ImageSearchResults.vue'
import configs from '../../configs'
import { defineAsyncComponent } from 'vue'


export default {
  name: 'PostSearchResults',
  components: {
    SearchSingleItemDefault: defineAsyncComponent(() => import('./../searchSingleItemComponents/searchSingleItemTypes/SearchSingleItemDefault.vue')),
    SearchSingleItemTweet: defineAsyncComponent(() => import('./../searchSingleItemComponents/searchSingleItemTypes/SearchSingleItemTweet.vue')),
    SearchSingleItemWeb: defineAsyncComponent(() => import('./../searchSingleItemComponents/searchSingleItemTypes/SearchSingleItemWeb.vue')),
    SearchSingleItemImage: defineAsyncComponent(() => import('./../searchSingleItemComponents/searchSingleItemTypes/SearchSingleItemImage.vue')),
    SearchSingleItemVideoAudio: defineAsyncComponent(() => import('./../searchSingleItemComponents/searchSingleItemTypes/SearchSingleItemVideoAudio.vue')),
   
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
    // ...mapState({
    //   query: state => state.Search.query,
    //   searchAppliedFacets: state => state.Search.searchAppliedFacets,
    //   results: state => state.Search.results,
    //   solrSettings: state => state.Search.solrSettings
    // }),
    ...mapStores(useSearchStore),
    sortInput: {
      get () {
        // return this.$store.state.Search.solrSettings.sort
        return this.searchStore.solrSettings.sort
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
    ...mapActions(useSearchStore, {
      updateSolrSettingOffset:'updateSolrSettingOffset',
      updateSolrSettingSort: 'updateSolrSettingSort'
    }),
    getNextResults() {
      this.updateSolrSettingOffset(this.searchStore.solrSettings.offset + this.hitsPerPage)
      this.$_pushSearchHistory('Search', this.searchStore.query, this.searchStore.searchAppliedFacets, this.searchStore.solrSettings)
    },
    getPreviousResults() {
      this.updateSolrSettingOffset(this.searchStore.solrSettings.offset - this.hitsPerPage)
      this.$_pushSearchHistory('Search', this.searchStore.query, this.searchStore.searchAppliedFacets, this.searchStore.solrSettings)
    },
    getResultsWithSort(event) {
      this.updateSolrSettingSort(event.target.value)
      this.$_pushSearchHistory('Search', this.searchStore.query, this.searchStore.searchAppliedFacets, this.searchStore.solrSettings)
    },
    SingleEntryComponent(type) {
      //console.log('search result type', type)
      switch(type) {
      case 'Web Page': return this.$options.components.SearchSingleItemWeb
      case 'Image': return this.$options.components.SearchSingleItemImage
      case 'Twitter Tweet': return this.$options.components.SearchSingleItemTweet
      case 'Video':
      case 'Audio': return this.$options.components.SearchSingleItemVideoAudio
      default: return this.$options.components.SearchSingleItemDefault
    }
    }
  }
}
</script>