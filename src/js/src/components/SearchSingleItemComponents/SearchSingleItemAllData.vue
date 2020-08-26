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
        <div v-for="(key, index) in Object.entries(allData)"
             :key="index"
             class="tr">
          <span class="td">{{ key[0] }}</span>
          <div v-if="key[1].constructor === Array"
               :class="key[0] !== 'content' ? 'td content clickAble' : 'td content'">
            <span v-for="(entry, newIndex) in key[1]"
                  :key="newIndex"
                  :class="index === currentDataShown ? 'singleEntry' : newIndex < arrayShownLimit ? 'singleEntry' : 'singleEntry hidden'"
                  @click="key[0] !== 'content' ? searchFromAllValues(key[0], entry) : null">
              {{ entry }} <br>
            </span>
            <button v-if="key[1].length > arrayShownLimit"
                    :key="index + '-button' "
                    class="attributeButton"
                    @click="toggleShownData(index)">
              {{ specificValueButtonText(index) }}
            </button>
          </div>
          <div v-if="key[1].constructor !== Array" :class="key[0] !== 'content' ? 'td content clickAble' : 'td content'" @click="key[0] !== 'content' ? searchFromAllValues(key[0], key[1]) : null">
            <span :class="key[0] === 'content' ? '' : 'singleEntry'"> {{ key[0] === 'content' ? displayContentValue(key[1]) : key[1] }}</span>
          </div>
          <button v-if="key[0] === 'content' && key[1].length > contentShownLength" class="contentButton" @click="allContentShownToggle()">
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

export default {
  name: 'SearchSingleItemAllData',
  components: {  
  },
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
      results: state => state.Search.results,
      results: state => state.Search.query,
      searchAppliedFacets: state => state.Search.searchAppliedFacets,
    }),
    allDataButtonText: function () {
      return this.allDataShown ? 'Hide raw data' : 'See raw data'
    },
    contentButtonText: function () {
      return this.allContentShown ? 'Show less ↑' : 'Show all ↓'
    },
  },
  mounted () {
  },
  methods: {
    ...mapActions('Search', {
      requestSearch: 'requestSearch',
      requestFacets: 'requestFacets',
      updateQuery: 'updateQuery',
      updateSearchAppliedFacets:'updateSearchAppliedFacets',

    }),
    toggleAllDataShown() {
      this.allDataShown = !this.allDataShown
      if(Object.keys(this.allData).length === 0 && this.allData.constructor === Object) {
        requestService.fireLookupRequest(encodeURIComponent(this.id)).then(result => (this.allData = result.response.docs[0], this.allData === {} ? console.log('request successfull, no data!') : null), error => (console.log('Error in getting full post'), this.allData = {}))
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
      let searchString = attribute + ':"' + value + '"'
      this.updateQuery(searchString)
      this.updateSearchAppliedFacets('')
      this.requestSearch({query:searchString, facets:this.searchAppliedFacets})
      this.requestFacets({query:searchString, facets:this.searchAppliedFacets})
      //this.$router.push({ name:'SolrWayback', params:{query:searchString }})
      history.pushState({name: 'SolrWayback'}, 'SolrWayback', '?q=' + searchString)
      //this.$router.replace({ query: {q:searchString }})
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

    
