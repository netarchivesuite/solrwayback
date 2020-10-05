<template>
  <div class="previewImg">
    <h2 class="previewHeader">
      Preview
    </h2>
    <div v-if="!isLoaded" class="spinner" />
    <a :href="harvestTimesData.pagePreviewUrl" target="_blank">
      <span v-if="imgLoadError" class="previewFailedHeader">Preview of web page could not be loaded - use direct this link</span>
      <img v-if="!imgLoadError"
           alt="webpage preview"
           class="preview loading"
           :src="harvestTimesData.pagePreviewUrl"
           @load="onImgLoad(false)"
           @error="onImgLoad(true)">
    </a>
  </div>
</template>
<script>

export default {
  name: 'HarvestPagePreview',
  props: {
    harvestTimesData: {
      type: Object,
      required: true
    },
  },

  data: () => ({
        isLoaded:false,
        imgLoadError: false
  }),

  methods: {
    onImgLoad (failedToLoad) {
      this.isLoaded = true
      if (failedToLoad) {
         this.imgLoadError = true   
      }
    }
  }
}
</script>
