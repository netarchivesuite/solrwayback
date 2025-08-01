<template>
  <div class="SingleEntryAllData">
    <div class="showAllButtonContainer">
      <button title="View warc header" :class="warcHeaderShown ? 'warcHeaderDataButton active' : 'warcHeaderDataButton'" @click="toggleWarcHeader" />
      <button :class="allDataShown ? 'allDataButton active' : 'allDataButton'" @click="toggleAllDataShown">
        {{ allDataButtonText }}
      </button>
    </div>
    <div v-if="warcHeaderShown && warcHeaderData !== ''" class="warcHeaderInfo">
      <hr class="informationDivider">
      <h2>Warc header</h2>
      <br>
      {{ warcHeaderData }}
    </div>
    <div v-if="allDataShown && Object.keys(allData).length !== 0">
      <hr class="informationDivider">
      <div class="table">
        <div class="tr">
          <span class="td highlightText">Attribute</span><span class="td highlightText">Value</span>
        </div>
        <div v-for="(singleObject, index) in Object.entries(allData)"
             :key="index"
             class="tr">
          <span class="td">{{ singleObject[0] }}</span>
          <div v-if="singleObject[1].constructor === Array"
               class="td content clickAble">
            <span v-for="(singleLine, newIndex) in singleObject[1]"
                  :key="newIndex"
                  :class="determinePlaceAndVisiblity(index, newIndex)"
                  @click="singleObject[0] !== 'content' ? searchFromAllValues(singleObject[0], singleLine) : null">
              {{ singleLine }} <br>
            </span>
            <button v-if="singleObject[1].length > arrayShownLimit"
                    :key="index + '-button' "
                    class="attributeButton"
                    @click="toggleShownData(index)">
              {{ specificValueButtonText(index) }}
            </button>
          </div>
          <div v-if="singleObject[1].constructor !== Array" 
               :class="singleObject[0] === 'content' ? 'td content' : 'td content clickAble'" 
               @click="singleObject[0] === 'content' ? null : searchFromAllValues(singleObject[0], singleObject[1])">
            <span :class="singleObject[0] === 'content' ? '' : 'singleEntry'">
              {{ singleObject[0] === 'content' ? displayContentValue(singleObject[1]) : singleObject[1] }}
            </span>
          </div>
          <button v-if="singleObject[0] === 'content' && singleObject[1].length > contentShownLength" 
                  class="contentButton" 
                  @click="allContentShownToggle()">
            {{ contentButtonText }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>

import { mapStores, mapActions } from 'pinia'
import { useSearchStore } from '../../store/search.store'
import { requestService } from '../../services/RequestService'
import HistoryRoutingUtils from './../../mixins/HistoryRoutingUtils'

export default {
  name: 'SearchSingleItemAllData',
  components: {  
  },
  mixins: [HistoryRoutingUtils],
  props: {
    id: {
      type: String,
      required: true
    },
    source: {
      type: String,
      required: true
    },
    offset: {
      type: Number,
      required: true
    },
  },
  data () {
    return {
      allDataShown:false,
      allData:{},
      allContentShown:false,
      currentDataShown:null,
      contentShownLength:300,
      arrayShownLimit:4,
      warcHeaderShown:false,
      warcHeaderData:''
    }
  },
  computed: {
    // ...mapState({
    //   solrSettings: state => state.Search.solrSettings,
    //   searchAppliedFacets: state => state.Search.searchAppliedFacets,
    // }),
    ...mapStores(useSearchStore),
    allDataButtonText: function () {
      return this.allDataShown ? 'Hide data fields' : 'View data fields'
    },
    contentButtonText: function () {
      return this.allContentShown ? 'Show less ↑' : 'Show all ↓'
    },
    warcHeaderButtonText: function () {
      return this.warcHeaderShown ? 'Hide warc header' : 'View warc header'
    },
  },
  methods: {
    ...mapActions(useSearchStore, {
      updateQuery: 'updateQuery',
      emptySearchAppliedFacets:'emptySearchAppliedFacets',
      updateSolrSettingUrlSearch:'updateSolrSettingUrlSearch'
    }),
    determinePlaceAndVisiblity(place, lineNumber) {
      return place === this.currentDataShown ? 'singleEntry' : lineNumber < this.arrayShownLimit ? 'singleEntry' : 'singleEntry hidden'
    },
    toggleAllDataShown() {
      this.allDataShown = !this.allDataShown
      this.allDataShown ? this.warcHeaderShown = false : null
      if(Object.keys(this.allData).length === 0) {
        requestService.fireLookupRequest(encodeURIComponent(this.id))
          .then(result => 
            (this.allData = this.orderResult(result.response.docs[0]), this.allData === {} ? console.log('request successfull, no data!') : null),
            error => (console.log('Error in getting full post'), this.allData = {}))
      }
    },
    orderResult(result) {

      let orderedResult = {}
      Object.keys(result).sort().forEach(function(key) {
        orderedResult[key] = result[key]
      })
      delete orderedResult['score']
      return orderedResult
    },
    specificValueButtonText(index) {
      return index === this.currentDataShown ? 'Show less ↑' : 'Show all ↓'
    },
    divideString(text) {
     return text[0]
    },
    searchFromAllValues(attribute, value) {
      this.allDataShown = !this.allDataShown
      const searchString = attribute + ':"' + value + '"'
      this.updateQuery(searchString)
      this.updateSolrSettingUrlSearch(false)
      this.emptySearchAppliedFacets()
      this.$_pushSearchHistory('Search', searchString, this.searchStore.searchAppliedFacets, this.searchStore.solrSettings)
    },
    toggleShownData(index) {
      index === this.currentDataShown ? this.currentDataShown = null : this.currentDataShown = index
    },
    allContentShownToggle() {
      return this.allContentShown  = !this.allContentShown
    },
    displayContentValue(content) {
      return this.allContentShown ? content : content.substring(0,this.contentShownLength)
    },
    toggleWarcHeader() {
      this.warcHeaderShown = !this.warcHeaderShown
      this.warcHeaderShown ? this.allDataShown = false : null
      if(this.warcHeaderData === '' && this.warcHeaderShown) {
        requestService.getWarcHeader(this.source, this.offset)
          .then(result => 
            (this.warcHeaderData = result, this.warcHeaderData === '' ? console.log('request successfull, no data!') : null),
            error => (console.log('Error in getting warc header'), this.allData = ''))
      }
    }
  }
}

</script>

    
