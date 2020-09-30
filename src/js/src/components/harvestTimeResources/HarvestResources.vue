<template>
  <div class="containerHarvestTimes">
    <h2 class="harvestedResourcesHeader">
      Harvestet resources
    </h2>
    <div v-if="harvestTimesData.resources">
      <ul class="responsive-table">
        <li class="table-header">
          <div class="col col-1">
            Resource URL
          </div>
          <div class="col col-2">
            Content Type
          </div>
          <div class="col col-3">
            Time Difference
          </div>
          <div class="col col-4">
            See/Download
          </div>
        </li>
        <li v-for="(resource, index) in harvestTimesData.resources" :key="index" class="table-row">
          <div class="col col-1">
            {{ resource.url }}
          </div>
          <div class="col col-2">
            {{ resource.contentType }}
          </div>
          <div class="col col-3">
            {{ resource.timeDifference }}
          </div>
          <div class="col col-4">
            <span v-if="resource.contentType === 'image'">
              <a :href="resource.downloadUrl">
                <img :src="resource.downloadUrl">
              </a>
            </span>
            <span v-else>
              <a :href="resource.downloadUrl">Download</a>
            </span>
          </div>
        </li>
      </ul>
    </div>
  </div>
</template>

<script>
 import { requestService } from '../../services/RequestService'
 
export default {
  name: 'HarvestResources',

  props: {
    harvestTimesData: {
      type: Object,
      required: true
    },
    sourceFilePath: {
      type: String,
      required: true
    },
    offset: {
      type: String,
      required: true
    },
  }
}
</script>
