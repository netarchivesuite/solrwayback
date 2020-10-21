<template>
  <div>
    <h2 class="toolboxHeadline">
      Wordcloud
    </h2>
    <div class="wordcloudContainer">
      <div class="wordcloudExplanation">
        <input v-model="domain"
               placeholder="Enter domain, like 'kb.dk'"
               :class="checkDomain(domain) ? '' : 'urlNotTrue'"
               @keyup.enter="setDomainImage()">
        <button :disabled="loadingImage" class="wordcloudButton" @click.prevent="setDomainImage()">
          Create wordcloud
        </button>
        <br>
        <p>
          Simply enter the domain you wish to see a wordcloud of, and generate the wordcloud. The image is generated in real time, so it might take some time.
        </p>
        <br>
        <p>
          The domain entered must be without http://www, and only contain the trailing domain, like 'kb.dk' or 'statsbiblioteket.dk'.
        </p>
        <br>
        <p>
          If the image returned is black, it simply means the archive holds no data on the entered domain.
        </p>
      </div>
      <div class="imgContainer">
        <img v-if="imgSrc !== ''"
             :class="loadingImage ? 'imageNotLoaded' : 'imageLoaded'"
             :src="imgSrc"
             @load="doneLoading()">
        <div v-if="loadingImage" class="spinner" />
      </div>
    </div>
  </div>
</template>

<script>

import StringManipulationUtils from './../../mixins/StringManipulationUtils'

export default {
  name: 'Wordcloud',

  mixins: [StringManipulationUtils],

  data() {
    return {
      domain:'',
      imgSrc:'',
      loadingImage:false
    }
  },
  mounted () {
    this.domain = '',
    this.imgSrc = ''
  },
  methods: {
    setDomainImage() {
        this.loadingImage = true
        this.imgSrc = 'services/frontend/wordcloud/domain?domain=' + this.domain + '&time=' + new Date().getTime()
    },
    doneLoading() {
    this.loadingImage = false
    }
  }
}
</script>
