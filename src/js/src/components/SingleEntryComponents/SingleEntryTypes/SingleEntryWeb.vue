<template>
  <div class="singleEntryResult">
    <single-entry-standard-info :result="result" />
    <single-entry-all-data :result="result" />
  </div>
</template>

<script>
import SingleEntryStandardInfo from "./../SingleEntryStandardInfo.vue";
import SingleEntryAllData from "./../SingleEntryAllData.vue";

//import { mapState, mapActions } from 'vuex'

export default {
  name: "SingleEntryWeb",
  components: { 
    SingleEntryStandardInfo,
    SingleEntryAllData
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

    
