<template>
  <div class="previewImg">
    <div v-if="!isLoaded" class="spinner" />
    <a :href="harvestTimesData.pagePreviewUrl" target="_blank">
      <img alt="webpage preview"
           class="preview loading"
           :src="harvestTimesData.pagePreviewUrl"
           @load="onImgLoad">
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
  }),

  methods: {
    onImgLoad () {
      this.isLoaded = true
    }
  }
}
</script>
