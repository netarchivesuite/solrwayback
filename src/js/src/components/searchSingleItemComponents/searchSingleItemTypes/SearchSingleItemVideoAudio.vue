<template>
  <div class="singleEntryResult">
    <search-single-item-standard-info :rank="rankNumber" :result="result" />
    <search-single-item-images :source="result.source_file_path"
                               :offset="result.source_file_offset"
                               :hash="result.hash"
                               :url-norm="result.url_norm"
                               input-type="multiple" />
    <inline-player v-if="initVideo"
                   :result="result" />
   
    <div v-if="!initVideo"
         class="vjs-play-button wrap"
         title="Play"
         @click="initVideo = !initVideo">
      <div class="vjs-play-button circle" />
    </div>
   
    <div class="downloadFileLinkBox">
      <!-- we need to pick a path here - wither the download attribute og target _blank. I'm using both for now to acommodate both Firefox and Chrome -->
      <a :href="downloadRawSourceFile()" target="_blank" download>
        Download source file
      </a>
    </div>
    <search-single-item-all-data :id="result.id"
                                 :source="result.source_file_path"
                                 :offset="result.source_file_offset" />
  </div>
</template>

<script>
import SearchSingleItemStandardInfo from './../SearchSingleItemStandardInfo.vue'
import SearchSingleItemAllData from './../SearchSingleItemAllData.vue'
import SearchSingleItemImages from './../SearchSingleItemImages.vue'
import InlinePlayer from '../../inlinePlayer/InlinePlayer'
import Configs from '../../../configs/'

export default {
  name: 'SearchSingleItemVideo',
  components: {  
    SearchSingleItemStandardInfo,
    SearchSingleItemAllData,
    SearchSingleItemImages,
    InlinePlayer
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
      initVideo: false
      }
  },

  
  watch: {
    /* We watch result here and if changed we now that player needs a new source 
     so we toggle the v-if (initVideo) to false hence triggering a destroy()
     component chain in the player component...
     This feels somewhat hackish but if removed the active players will persist
     when paging og using facets. 
     Why this is the case (vue reusing DOM elements??) I do not know */
    result: function() { 
      this.initVideo = false
    }
  },

  methods: {
    downloadRawSourceFile() {
      return `${Configs.playbackConfig.solrwaybackBaseURL}services/downloadRaw?source_file_path=${this.result.source_file_path}&offset=${this.result.source_file_offset}`
    }
  }

}

</script>

    
