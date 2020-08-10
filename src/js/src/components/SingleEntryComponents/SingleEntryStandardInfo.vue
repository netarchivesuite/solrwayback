<template>
  <div class="SingleEntryStandardInfo">
    <p class="entryInfo"><span class="highlightText titleInfo">{{ result.title }}</span>
    <p class="domainInfo entryInfo">{{ result.content_type_norm }} {{ result.type }} @ {{ result.domain }}</p>
    <p class="highlightText entryInfo">Content</p>
    <div class="snippetContainer">
      <span>"</span>
      <p class="snippetHighlight" v-html="result.highlight.content[0]"></p>
      <span>"</span>
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
    }
  },
  computed: {
  },
  mounted () {
  },
  methods: {
  }
}

</script>

    
