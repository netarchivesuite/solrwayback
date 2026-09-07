<template>
  <div>
    <h2 class="toolboxHeadline">
      Wordcloud
    </h2>
    <div class="wordcloudContainer">
      <div class="wordcloudExplanation">
        <input v-model="domain"
               :placeholder="getPlaceholder()"
               :class="$_checkDomain(domain) ? '' : 'urlNotTrue'"
               @keyup.enter="setWordcloudImage()">
        <div class="domainHostChoiceSettings">
          <div class="domainHostChoiceContainer contain">
            <label class="domainHostChoiceLabel label">Search for:</label>
            <input id="domainHostChoiceRadioOne" v-model="isDomain" type="radio" :value="true">
            <label class="label" for="domainHostChoiceRadioOne">domain</label>
            <input id="domainHostChoiceRadioTwo" v-model="isDomain" type="radio" :value="false">
            <label class="label" for="domainHostChoiceRadioTwo">host</label>
          </div>
        </div>
        <button :disabled="loadingImage" class="wordcloudButton" @click.prevent="setWordcloudImage()">
          Create wordcloud
        </button>
        <br>
        <p>
          Enter the domain or the host you wish to see a wordcloud of, and generate the wordcloud. The image is generated in real time, so it might take some time.
        </p>
        <br>
        <p>
          The domain entered must be without http://www, and only contain the trailing domain, like 'kb.dk'.
        </p>
        <br>
        <p>
          The host entered must also be without http:// or www, and only contain the trailing host, like 'pro.kb.dk'.
        </p>
        <br>
        <p>
          If the image returned is black, it simply means the archive holds no data on the entered domain or host.
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
      isDomain: true,
      imgSrc:'',
      loadingImage:false
    }
  },
  mounted () {
    this.domain = ''
    this.imgSrc = ''
    this.isDomain = true

  },
  methods: {
    setWordcloudImage() {
        this.loadingImage = true
        const paramName = this.isDomain ? 'domain' : 'host'
        this.imgSrc = `services/frontend/wordcloud/url?${paramName}=${encodeURIComponent(this.domain)}&time=${Date.now()}`
    },
    getPlaceholder() {
      return this.isDomain ? "Enter domain, like 'kb.dk'" : "Enter host, like 'pro.kb.dk'"
    },
    doneLoading() {
    this.loadingImage = false
    }
  }
}
</script>
