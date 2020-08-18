<template>
  <div class="SingleEntryAllData">
    <div class="showAllButtonContainer">
    <button v-on:click="toggleAllDataShown" class="allDataButton">See all data</button>
  </div>
    <div v-if="allDataShown">
      <hr class="informationDivider">
        <div class="table">
          <div class="tr"><span class="td highlightText">Attribute</span><span class="td highlightText">Value</span></div>
          <div v-bind:key="index" class="tr" v-for="(key, index) in Object.entries(result)"><span class="td">{{ key[0] }}</span> <span class="td">{{ key[1] }}</span></div>
        </div>
    </div>
  </div>
</template>

<script>
//import { mapState, mapActions } from 'vuex'

export default {
  name: "SingleEntryStandardInfo",
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
  },
  mounted () {
  },
  methods: {
    toggleAllDataShown() {
      this.allDataShown = !this.allDataShown;
    },
    divideString(text) {
     return text[0]
    }
  }
}

</script>

    
