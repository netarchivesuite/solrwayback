<template>
  <div class="singleEntryResult">
    <search-single-item-standard-info :result="result" :rank="rankNumber" />
    <search-single-item-images :source="result.source_file_path" :offset="result.source_file_offset" input-type="singluar" />
    <search-single-item-all-data :id="result.id" />
  </div>
</template>

<script>
import SearchSingleItemStandardInfo from './../SearchSingleItemStandardInfo.vue'
import SearchSingleItemAllData from './../SearchSingleItemAllData.vue'
import SearchSingleItemImages from './../SearchSingleItemImages.vue'
import configs from '../../../configs'

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
    rankNumber: {
      type:Number,
      required:true
    }
  }
}

</script>

    
