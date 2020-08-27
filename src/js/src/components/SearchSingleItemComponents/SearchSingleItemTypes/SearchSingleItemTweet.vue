<template>
  <div class="singleEntryResult">
    <search-single-item-standard-info :rank="rankNumber" :result="result" />
    <search-single-item-images :source="result.source_file_path" :offset="result.source_file_offset" />
    <search-single-item-all-data :result="result" />
  </div>
</template>

<script>
import SearchSingleItemStandardInfo from './../SearchSingleItemStandardInfo.vue'
import SearchSingleItemAllData from './../SearchSingleItemAllData.vue'
import SearchSingleItemImages from './../SearchSingleItemImages.vue'


//import { mapState, mapActions } from 'vuex'

export default {
  name: 'SearchSingleItemTweet',
  components: {  
    SearchSingleItemStandardInfo,
    SearchSingleItemAllData,
    SearchSingleItemImages
  },
  props: {
    result: {
      type: Object,
      required: true
    },
    rankNumber: {
      type:Number,
      required:true
    }
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

    
