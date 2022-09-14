<template>
  <div class="aboutContainer">
    <div v-if="showAbout" class="aboutTextContainer" v-html="aboutText" />
    <button :class="showAbout ? 'activated' : ''" @click="toggleAboutText()">
      {{ aboutButtonText }}
    </button>
  </div>
</template>

<script>

import { requestService } from '../services/RequestService'

export default {
  name: 'AboutComponent',
  data: () => ({
        aboutText:'',
        showAbout:false
  }),
  computed: {
    aboutButtonText: function() {
      return this.showAbout ? 'Show less' : 'About Us'
    }
  },
  mounted () {
    this.getAboutTextFromService()
  },
  methods: {
    getAboutTextFromService() {
      requestService.getAboutText().then(result => this.aboutText = result, error => console.log('No information found about this archival institution.'))
    },
    toggleAboutText() {
      this.showAbout = !this.showAbout
    }
  }
}

</script>

    
