<template>
  <div class="SingleEntryAllData">
    <div class="showAllButtonContainer">
      <button class="allDataButton" @click="toggleAllDataShown">
        {{ allDataButtonText }}
      </button>
    </div>
    <div v-if="allDataShown">
      <hr class="informationDivider">
      <div class="table">
        <div class="tr">
          <span class="td highlightText">Attribute</span><span class="td highlightText">Value</span>
        </div>
        <div v-for="(key, index) in Object.entries(result)" :key="index" class="tr">
          <span class="td">{{ key[0] }}</span> <span class="td">{{ key[1] }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
//import { mapState, mapActions } from 'vuex'

export default {
  name: 'SearchSingleItemStandardInfo',
  components: {  
  },
  props: {
    result: {
      type: Object,
      required: true
    },
  },
  data () {
    return {
      allDataShown:false,
    }
  },
  computed: {
    allDataButtonText: function () {
      return this.allDataShown ? 'Hide data ' : 'See data'
    }
  },
  mounted () {
  },
  methods: {
    toggleAllDataShown() {
      this.allDataShown = !this.allDataShown
    },
    divideString(text) {
     return text[0]
    }
  }
}

</script>

    
