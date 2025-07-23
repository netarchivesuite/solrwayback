<template>
  <div>
    <video
      ref="inlinePlayerObj"
      class="video-js" />
  </div>
</template>

<script>
import videojs from 'video.js'
// import {mapActions} from 'vuex'
import { mapActions } from 'pinia'
import { useNotifierStore } from '../../store/notifier.store'
import Configs from '../../configs'

export default {
    name: 'InlinePlayer',
    props: {
        result: {
            type: Object,
            default() {
                return {}
            }
        }
    },

    data() {
        return {
            playerInstance: null
        }
    },

    mounted() {
        this.playerInstance = videojs(this.$refs.inlinePlayerObj, this.getPlayerOptions())
        this.playerInstance.on('error', () => {
        const errorObj = this.playerInstance.error()
        this.dispatchError(errorObj.code, errorObj.message)
      })
    },

    beforeDestroy() {
        if (this.playerInstance) {
            this.playerInstance.dispose()
        }
    },

    methods: {
      ...mapActions(useNotifierStore, {
      setNotification: 'setNotification'
     
        }),
        
        dispatchError(code, playerErrorMessage) {
        if (code === 4 ){
          this.setNotification({
          	title: 'Unsupported format',
            text:`The media format (${this.getFileExtension()}) is not supported in the web-player. Use the Download link to download the file and use video-player that supports the format`,
            type: 'error',
            srvMessage: playerErrorMessage,
            timeout: true
          })
 
        } else {
          this.setNotification({
          	title: 'We are so sorry!',
            text: 'Something went wrong when trying to stream the source',
            type: 'error',
            srvMessage: playerErrorMessage,
            timeout: true
          })
        }
      },

      //Move to seperate conf file if this balloons
      getPlayerOptions() {
        let playerConf = {
				  autoplay: true,
          controls: true,
          width: 400, 
          height: this.result.type === 'Video' ? 200 : 30,
				  sources: this.getSource() 
        }
        return playerConf    
      },
      
      getSource() {
          const contentType = this.result.content_type
          return  {type:  contentType === 'application/mp4' ? 'video/mp4' : contentType, 
                   src: `${Configs.playbackConfig.solrwaybackBaseURL}services/downloadRaw?source_file_path=${this.result.source_file_path}&offset=${this.result.source_file_offset}`
                  } 
      },

      // Regex matches all characters from the last '.' to the end of the string  
      getFileExtension() {
        return this.result.url_norm.match(/\.[0-9a-z]+$/i)
      } 
    }
}
</script>

<style lang="scss">
  @import '../../../node_modules/video.js/dist/video-js.min.css';
  </style>