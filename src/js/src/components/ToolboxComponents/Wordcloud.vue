<template>
  <div>
    <h2 class="toolboxHeadline">
      Wordcloud
    </h2>
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

export default {
  name: 'Wordcloud',
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
    checkDomain() {
      return true
    },
    setDomainImage() {
      this.loadingImage = true
      this.imgSrc = 'services/frontend/wordcloud/domain?domain=' + this.domain
    },
    doneLoading() {
    this.loadingImage = false
    }
  }
}
</script>
