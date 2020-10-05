<template>
  <div class="containerHarvestTimes">
    <h2 class="harvestedResourcesHeader">
      Harvestet resources
    </h2>
    <div v-if="harvestTimesData.resources">
      <ul class="responsive-table">
        <li class="table-header">
          <div class="col col-0" />
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
          <div class="col col-0" :data-title="resource.url">
            <span class="copyToClipboard" :class="urlCopied && index === copiedItem ? 'checkmarkIcon' : 'clipBoardIcon'" @click.prevent="copyUrl(resource.url, index)" />
          </div>
          <div class="col col-1" :data-title="resource.url">
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
              <a :href="resource.downloadUrl" target="_blank">
                <img :src="resource.downloadUrl">
              </a>
            </span>
            <span v-else>
              <a :href="resource.downloadUrl" target="_blank">Download</a>
            </span>
          </div>
        </li>
      </ul>
    </div>
  </div>
</template>

<script>
 import { requestService } from '../../services/RequestService'
 import {copyTextToClipboard} from './harvestUtil'
 
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
  },

  data: () => ({
        urlCopied:false,
        copiedItem: null
  }),

  methods: {
    copyUrl: function(text, index) {
      if (copyTextToClipboard(text)) {
         this.urlCopied = true
         this.copiedItem = index
          setTimeout(() => {
            this.urlCopied = false
            this.copiedItem = null},
            3000)
        }
    }
  }
}
</script>
