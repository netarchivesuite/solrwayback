<template>
  <div class="SingleEntryStandardInfo">
    <div class="scoreInfo">
      <p class="textAlignRight">
        #<span class="highlightText">{{ solrSettings.offset + rank + 1 }}</span>
      </p>
      <p>score: <span class="highlightText"> {{ result.score }}</span></p>
    </div>
   
    <p class="entryInfo">
      <span class="highlightText titleInfo">
        <div :title="result.type.toLowerCase()" :class="'typePreview ' + getIconForType(result.type)" />
        <span v-if="playbackDisabled()">
          <span title="Playback has been disabled in the configuration">{{ result.title || `${result.content_type_norm} - no title` }}</span>
        </span>
        <span v-else>
          <a :href="getPlaybackURL(result.source_file_path, result.source_file_offset)" target="_blank">
            <span>{{ result.title || `${result.content_type_norm} - no title` }}</span>
          </a>
        </span>
      </span>

      <a v-if="result.content_type_norm === 'html' && !playbackDisabled() && getAlternativePlaybackEngineLink(result.wayback_date, result.url, result.collection) !== null"
         :href="getAlternativePlaybackEngineLink(result.wayback_date, result.url, result.collection)"
         title="Alternative playback engine"
         class="alternativePlaybackLink"
         target="_blank" />
    </p>
    <p class="entryInfo type">
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
import configs from '../../configs'
import { isPlaybackDisabled } from '../../configs/configHelper'
import { mapState } from 'vuex'


export default {
  name: 'SearchSingleItemStandardInfo',
  props: {
    result: {
      type: Object,
      required: true
    },
    rank: {
      type: Number,
      required: true
    }
  },
  computed: {
    ...mapState({
      solrSettings: state => state.Search.solrSettings
    }),
  },
  methods: {
    refactoredDate(date) {
      date = date.toString()
      return date.substring(6,8) + '/' + date.substring(4,6) + '-' + date.substring(0,4)
    },

    getPlaybackURL(source_file_path, source_file_offset) {
      return `${configs.playbackConfig.solrwaybackBaseURL}services/viewForward?source_file_path=${source_file_path}&offset=${source_file_offset}`    
    },

    getAlternativePlaybackEngineLink(wayback_date, url, collection) {    
        //Test if collection specific playback is enabled. 
        var collectionPlayback=configs.collection.playback.get('PLAYBACK_'+collection)
        if (collectionPlayback != null){ //Use default playback engine
         //console.log('collection playback')
         var collectionPlaybackLink = collectionPlayback+wayback_date+'/'+url                                                                    
         return collectionPlaybackLink            
        }
        else if(configs.playbackConfig.alternativePlaybackBaseURL != null){ //Alternative playback
          //console.log('alternative playback')
          return `${configs.playbackConfig.alternativePlaybackBaseURL}${wayback_date}/${url}`
                
        }
        else{ //No alternative playback defined
          //console.log('no alternative playback')
          return null
        }    
    },


    playbackDisabled(){
      return isPlaybackDisabled()
    },
    
    getIconForType(type) {
      switch(type) {   
        case 'Web Page': return 'web'
        case 'Image': return 'image'
        case 'Twitter Tweet': return 'twitter'
        case 'Audio': return 'audio'
        case 'Video': return 'video'
        default: return 'default'
      }
    }
  }
}

</script>

    
