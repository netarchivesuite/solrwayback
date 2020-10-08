<template>
  <div class="wordcloudContainer">
    <div class="wordcloudExplanation">
      <input v-model="domain"
             placeholder="Enter domain"
             :class="checkDomain(domain) ? 'goodDomain' : 'badDomain'"
             @keyup.enter="setDomainImage()"><button :disabled="loadingImage" class="wordcloudButton" @click.prevent="setDomainImage()">
               Create wordcloud
             </button>
      <br>
      <p>
        Simply enter the domain you wish to see a wordcloud of, and generate the wordcloud. The image is generated in real time, so it might take some time.
      </p>
    </div>
    <div class="imgContainer">
      <img v-if="imgSrc !== ''" :src="imgSrc" @load="doneLloading()">
      <div v-if="loadingImage">
        LOADING!
      </div>
    </div>
  </div>
</template>

<script>
import { mapState, mapActions } from 'vuex'

export default {
  name: 'Wordcloud',
  data() {
    return {
      domain:'',
      imgSrc:'',
      loadingImage:true
    }
  },
  mounted () {
    this.domain = '',
    this.imgSrc = ''
  },
  methods: {
    checkDomain() {
      return true
    },
    setDomainImage() {
      this.imgSrc = 'services/frontend/wordcloud/domain?domain=' + this.domain
      this.loadingImage = true
      console.log('set source and start loading!')
    },
    doneLloading() {
    console.log('loaded!')
    this.loadingImage = false
    }
  }
}
</script>
