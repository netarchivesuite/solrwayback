<template>
  <div class="SingleResultEntry">
    TWEET
    <single-entry-standard-info :result="result" />
  </div>
</template>

<script>
import SingleEntryStandardInfo from "./../SingleEntryStandardInfo.vue";
//import { mapState, mapActions } from 'vuex'

export default {
  name: "SingleEntryTweet",
  components: {  
    SingleEntryStandardInfo
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

    
