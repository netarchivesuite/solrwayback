<template>
  <div class="previewImg">
    <a :href="harvestTimesData.pagePreviewUrl">
      <img alt="webpage preview" class="preview" :src="harvestTimesData.pagePreviewUrl">
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
  }
}
</script>
