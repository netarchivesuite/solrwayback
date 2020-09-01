<template>
  <div class="singleEntryResult">
    <search-single-item-standard-info :result="result" />
    <search-single-item-images :source="result.source_file_path" :offset="result.source_file_offset" />
    <search-single-item-all-data :id="result.id" />
    <img :src="'/services/downloadRaw?source_file_path=' + result.source_file_path + '&offset=' + result.source_file_offset">
  </div>
</template>

<script>
import SearchSingleItemStandardInfo from './../SearchSingleItemStandardInfo.vue'
import SearchSingleItemAllData from './../SearchSingleItemAllData.vue'
import SearchSingleItemImages from './../SearchSingleItemImages.vue'


//import { mapState, mapActions } from 'vuex'

export default {
  name: 'SearchSingleItemImage',
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

    
