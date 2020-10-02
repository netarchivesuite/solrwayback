<template>
  <div class="SingleEntryAllData">
    <div class="showAllButtonContainer">
      <button class="allDataButton" @click="toggleAllDataShown">
        {{ allDataButtonText }}
      </button>
    </div>
    <div v-if="allDataShown && allData !== {}">
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
import { mapState, mapActions } from 'vuex'
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
  },
  data () {
    return {
      allDataShown:false,
      allData:{},
      allContentShown:false,
      currentDataShown:null,
      contentShownLength:300,
      arrayShownLimit:4
    }
  },
  computed: {
    ...mapState({
      solrSettings: state => state.Search.solrSettings,
      searchAppliedFacets: state => state.Search.searchAppliedFacets,
    }),
    allDataButtonText: function () {
      return this.allDataShown ? 'Hide raw data' : 'See raw data'
    },
    contentButtonText: function () {
      return this.allContentShown ? 'Show less ↑' : 'Show all ↓'
    },
  },
  methods: {
    ...mapActions('Search', {
      updateQuery: 'updateQuery',
      emptySearchAppliedFacets:'emptySearchAppliedFacets',

    }),
    determinePlaceAndVisiblity(place, lineNumber) {
      return place === this.currentDataShown ? 'singleEntry' : lineNumber < this.arrayShownLimit ? 'singleEntry' : 'singleEntry hidden'
    },
    toggleAllDataShown() {
      this.allDataShown = !this.allDataShown
      if(Object.keys(this.allData).length === 0) {
        requestService.fireLookupRequest(encodeURIComponent(this.id))
          .then(result => 
            (this.allData = result.response.docs[0], this.allData === {} ? console.log('request successfull, no data!') : null),
            error => (console.log('Error in getting full post'), this.allData = {}))
      }
    },
    specificValueButtonText(index) {
      return index === this.currentDataShown ? 'Show less ↑' : 'Show all ↓'
    },
    divideString(text) {
     return text[0]
    },
    searchFromAllValues(attribute, value) {
      this.allDataShown = !this.allDataShown
      const searchString = attribute + ':"' + encodeURIComponent(value) + '"'
      this.updateQuery(searchString)
      this.emptySearchAppliedFacets()
      this.$_pushSearchHistory('SolrWayback', searchString, this.searchAppliedFacets, this.solrSettings)
    },
    toggleShownData(index) {
      index === this.currentDataShown ? this.currentDataShown = null : this.currentDataShown = index
    },
    allContentShownToggle() {
      return this.allContentShown  = !this.allContentShown
    },
    displayContentValue(content) {
      return this.allContentShown ? content : content.substring(0,this.contentShownLength)
    }
  }
}

</script>

    
