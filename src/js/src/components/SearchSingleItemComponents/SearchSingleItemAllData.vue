<template>
  <div class="SingleEntryAllData">
    <div class="showAllButtonContainer">
      <button class="allDataButton" @click="toggleAllDataShown">
        See all data
      </button>
    </div>
    <div v-if="allDataShown && result !== {}">
      <hr class="informationDivider">
      <div class="table">
        <div class="tr">
          <span class="td highlightText">Attribute</span><span class="td highlightText">Value</span>
        </div>
        <div v-for="(key, index) in Object.entries(result)"
             :key="index"
             :class="key[0] !== 'content' ? 'tr clickAble' : 'tr'"
             @click="key[0] !== 'content' ? searchFromAllValues(key[0], key[1]) : null">
          <span class="td">{{ key[0] }}</span> <span class="td">{{ key[1] }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { mapState } from 'vuex'
import { searchService } from '../../services/SearchService'

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
      result: {}
    }
  },
  computed: {
    ...mapState({
      results: state => state.Search.results,
    }),
  },
  computed: {
  },
  mounted () {
  },
  methods: {
    toggleAllDataShown() {
      this.allDataShown = !this.allDataShown
      if(Object.keys(this.result).length === 0 && this.result.constructor === Object) {
        searchService.fireLookupRequest(this.id).then(result => (this.result = result.response.docs[0], this.result === [] ? console.log('request successfull, no data!') : null), error => (console.log('Error in getting full post'), this.result = []))
      }
    },
    divideString(text) {
     return text[0]
    },
    searchFromAllValues(attribute, value) {
        console.log('yay')
        let searchString = attribute + ':"' + value + '"'
        console.log(searchString)
        //searchService.fireSearchRequest(searchString).then(result => (this.result = result.response.docs[0], this.result === [] ? console.log('request successfull, no data!') : null), error => (console.log('Error in getting full post'), this.result = []))
    } 
  }
}

</script>

    
