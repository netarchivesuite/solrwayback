<template>
  <div class="previewImg">
    <div v-if="!isLoaded" class="spinner" />
    <a :href="harvestTimesData.pagePreviewUrl" target="_blank">
      <h2 v-if="imgLoadError" class="previewFailedHeader">Preview of web page could not be loaded - use direct link</h2>
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
