<template>
  <div class="contentContainer">
    <notifications />
    <h1>Solr<span>Wayback</span> - PWID</h1>
   
    <div v-if="PWIDData" class="pwidContainer">
      <div class="copyContainer">
        <span class="copyToClipboardText" @click.prevent="copyPWID(PWIDData)">Copy PWID to clip board</span>
        <span class="copyToClipboard" :class="PWIDCopied ? 'checkmarkIcon' : 'clipBoardIcon'" @click.prevent="copyPWID(PWIDData)" />
      </div>
      {{ PWIDData }}
    </div>
  </div>     
</template>

<style lang="scss">
  @import '../assets/styles/pwid.scss'; 
</style>

<script>
import { requestService } from '../services/RequestService'
import Notifications from '../components/notifications/Notifications'
 import {copyTextToClipboard} from '../utils/globalUtils'
import { mapActions } from 'vuex'

export default {
  name: 'PWID',
  components: {  
    Notifications
  },

  data () {
    return {
            PWIDData: null,
            sourceFilePath:'',
            offset:'',
            PWIDCopied:false,
            copiedItem: null
        }
  },
  
  mounted () {
    if (this.$route.query.source_file_path){
       this.sourcefilepath = this.$route.query.source_file_path
        this.offset = this.$route.query.offset
     }
     requestService.getPWID(this.sourcefilepath, this.offset)
        .then(response => {
            if (response === '') {
              this.noResults = true
            } else {
           this.PWIDData = response
          }
        }).catch((e) => {
          this.setNotification({
          	title: 'We are so sorry!',
            text: 'Something went wrong when fetching the PWID data - please try again',
            type: 'error',
            timeout: false
          })
          
        })
  },
  
  methods: {
    ...mapActions('Notifier', {
      setNotification: 'setNotification'
    }),
    
    copyPWID: function(text, index) {
      if (copyTextToClipboard(text)) {
         this.PWIDCopied = true
          setTimeout(() => {
            this.PWIDCopied = false
            },
            3000)
        }
    }
  }
}

</script>

    
