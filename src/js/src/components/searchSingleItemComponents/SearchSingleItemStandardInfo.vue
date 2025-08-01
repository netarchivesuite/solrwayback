<template>
  <div class="SingleEntryStandardInfo">
    <div class="scoreInfo">
      <p class="textAlignRight">
        #<span class="highlightText">{{ this.searchStore.solrSettings.offset + rank + 1 }}</span>
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
          <a :href="getPlaybackURL(result.source_file_path, result.source_file_offset,result.wayback_date, result.url)" target="_blank">
            <span>{{ result.title || `${result.content_type_norm} - no title` }}</span>
          </a>
        </span>
      </span>

      <a v-if="result.content_type_norm === 'html' && !playbackDisabled() && getAlternativePlaybackEngineLink(result.wayback_date, result.url, result.collection, result.collection_id) !== null"
         :href="getAlternativePlaybackEngineLink(result.wayback_date, result.url, result.collection, result.collection_id)"
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
import { mapStores, mapActions } from 'pinia'
import { useSearchStore } from '../../store/search.store'

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
    // ...mapState({
    //   solrSettings: state => state.Search.solrSettings
    // }),
    ...mapStores(useSearchStore)
  },
  methods: {
    refactoredDate(date) {
      date = date.toString()
      return date.substring(6,8) + '/' + date.substring(4,6) + '-' + date.substring(0,4)
    },

    
    //Check if primary playback engine has been set. If no primary playback is defined use the SolrWayback
    getPlaybackURL(source_file_path, source_file_offset, wayback_date,url) {                      
     const primaryPlaybackEngine = configs.playbackConfig.playbackPrimary
     if (primaryPlaybackEngine  != null){
       console.log('Primary playback engine:' + primaryPlaybackEngine)
       const primaryPlaybackUrl =primaryPlaybackEngine+wayback_date+'/'+url                 
       return primaryPlaybackUrl
     }     
      //Default SolrWayback playback. For performance it uses warc-file and offset instead of the /web/date/url syntax.
      return `${configs.playbackConfig.solrwaybackBaseURL}services/viewForward?source_file_path=${source_file_path}&offset=${source_file_offset}`    
    },


    //Return alternative playback link if configured.
    //The alternative playback link can be one of the following
    //1) Hardcoded  
    //2) Defined for each collection. Example PLAYBACK_collection1 
    //3) Template using {$collection}. Of form PLAYBACK_{$collection)
    //4) Template using {$collection_id}. Of form PLAYBACK_{$collection_id)
    //Test if collection specific playback is enabled.                                 
    //First we see if template mapping is used. Look for  {$collection} or {$collection_id}
    
    getAlternativePlaybackEngineLink(wayback_date, url, collection, collection_id) {               
        
        //See if {$collection} template is used.
        const collectionTemplate = configs.collection.playback.get('PLAYBACK_{$collection}')
        if (collectionTemplate != null & collection != null){        
          const link = collectionTemplate.replace('{$collection}',collection)
          const  collectionPlaybackLink=link+wayback_date+'/'+url
          return collectionPlaybackLink 
        }
                
        //See if {$collection_id} template is used.
        const collectionIdTemplate = configs.collection.playback.get('PLAYBACK_{$collection_id}')
        if (collectionIdTemplate != null && collection_id != null){                 
          const link = collectionIdTemplate.replace('{$collection_id}',collection_id)          
          const collectionPlaybackLink=link+wayback_date+'/'+url
          return collectionPlaybackLink          
        }
                
        //See if collection specific playback is defined                
        const collectionPlayback=configs.collection.playback.get('PLAYBACK_'+collection)
        if (collectionPlayback != null && collection != null){ //Use default playback engine
          const collectionPlaybackLink = collectionPlayback+wayback_date+'/'+url                                                                    
          return collectionPlaybackLink            
        }
       
       //Hardcoded value independant of collection or collection_id
        if(configs.playbackConfig.alternativePlaybackBaseURL != null){ //Alternative playback          
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

    
