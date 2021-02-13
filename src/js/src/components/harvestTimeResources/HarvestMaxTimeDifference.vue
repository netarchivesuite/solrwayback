<template>
  <div class="maxTimeDiffContainer">
    <span class="maxTimeDiffHeader">
      <h2>
        Maximum time difference for ressources
      </h2>
    </span>
    <div class="maximumDIfferenceBox">
      <div class="timeDifference">
        <span class="backward">Backward</span> <span class="timeDifferenceArrow">&larr;</span> {{ harvestTimesData.maximumTimeDifferenceBackward }} 
      </div>
      <div class="timeDifference">
        <span class="forward">Forward</span> <span class="timeDifferenceArrow">&rarr;</span> {{ harvestTimesData.maximumTimeDifferenceForward }} 
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'HarvestMaxTimeDifference',
  
  props: {
    harvestTimesData: {
      type: Object,
      required: true
    }
  }

}
</script>
