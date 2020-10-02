<template>
  <div class="aboutContainer">
    {{ aboutText }}
  </div>
</template>

<script>

import { requestService } from '../services/RequestService'

export default {
  name: 'AboutComponent',
  data: () => ({
        aboutText:''
  }),
  mounted () {
    this.getAboutTextFromService()
  },
 
  methods: {
    getAboutTextFromService() {
      requestService.getAboutText().then(result => this.aboutText = result, error => console.log('Error in getting images'))
    }
  }
}

</script>

    
