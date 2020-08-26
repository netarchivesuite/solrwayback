<template>
  <div class="SingleEntryStandardInfo">
    <p class="scoreInfo">
      score: <span class="highlightText">{{ result.score }}</span>
    </p>
    <p class="entryInfo">
      <span class="highlightText titleInfo">{{ result.title || 'No title' }}</span>
    </p><p class="entryInfo type">
      <span class="attri">type:</span> <span class="val">{{ result.content_type_norm }}, {{ result.type }} @ {{ result.domain }}</span>
    </p>
    <p class="entryInfo date">
      <span class="attri">date:</span> <span class="val">{{ refactoredDate(result.wayback_date) }}</span>
    </p>
    <p class="entryInfo url">
      <span class="attri">url:</span> <span class="val">{{ result.url }}</span>
    </p>
    <div v-if="result.highlight.content">
      <hr class="informationDivider">
      <p class="highlightText entryInfo">
        Highlighted content:
      </p>
      <div class="snippetContainer">
        <span>"</span>
        <p class="snippetHighlight" v-html="result.highlight.content[0]" />
        <span>"</span>
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
    }
  },
  computed: {
  },
  mounted () {
  },
  methods: {
    refactoredDate(date) {
      date = date.toString()
      return date.substring(6,8) + '/' + date.substring(4,6) + '-' + date.substring(0,4)
    }
  }
}

</script>

    
