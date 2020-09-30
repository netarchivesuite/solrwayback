<template>
  <div class="missingResourcesContainer">
    <span class="missingResourcesHeader">
      <h2>
        Missing resources
      </h2>
    </span>
    <p v-if="harvestTimesData.notHarvested.length === 0">
      No missing resources registered for this harvest
    </p>
    <div v-if="harvestTimesData.notHarvested.length > 0">
      <ol>
        <li v-for="(resource, index) in harvestTimesData.notHarvested" :key="index">
          {{ resource }}
        </li>
      </ol>
    </div>
  </div>
</template>

<script>
import { requestService } from '../../services/RequestService'

export default {
  name: 'HarvestResourcesMissing',
  
  props: {
    harvestTimesData: {
      type: Object,
      required: true
    }
  }

}
</script>
