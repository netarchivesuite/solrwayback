<template>
  <div class="contentContainerHarvestTimes">
    <notifications />
    <div v-if="harvestTimesData">
      <h2>
        {{ harvestTimesData.pageUrl }}
      </h2>
      <div class="pageHarvestDate">
        Page crawl date: {{ getPrettyDate(harvestTimesData.pageCrawlDate) }}
      </div>
      <harvest-max-time-difference :harvest-times-data="harvestTimesData" />                                                                                                                      
      <harvest-page-preview :harvest-times-data="harvestTimesData" />
      <harvest-resources-missing :harvest-times-data="harvestTimesData" />
      <harvest-resources 
        :harvest-times-data="harvestTimesData"
        :source-file-path="sourceFilePath"
        :offset="offset" />
    </div>
    <div v-else>
      No harvest resource data loaded for {{ sourceFilePath }}
    </div>                
  </div>
</template>

<style lang="scss">
  @import '../assets/styles/harvestTimes.scss'; 
</style>

<script>
import { requestService } from '../services/RequestService'
import HarvestResources from '../components/harvestTimeResources/HarvestResources'
import HarvestResourcesMissing from '../components/harvestTimeResources/HarvestResourcesMissing'
import HarvestPagePreview from '../components/harvestTimeResources/HarvestPagePreview'
import HarvestMaxTimeDifference from '../components/harvestTimeResources/HarvestMaxTimeDifference'

import Notifications from '../components/notifications/Notifications'
import {toHumanDate} from '../components/harvestCalendar/util'
import {mapActions} from 'vuex'

export default {
  name: 'PageHarvestData',
   components: {
     HarvestResources,
     HarvestResourcesMissing,
     HarvestPagePreview,
     HarvestMaxTimeDifference,
     Notifications
  },

  data: () => ({
        harvestTimesData:null,
        sourceFilePath: null,
        offset:null
  }),
 
  mounted() {
    this.sourceFilePath = this.$route.query.source_file_path
    this.offset = this.$route.query.offset
    requestService.getHarvestedPageResources(this.sourceFilePath, this.offset)
        .then(data => {
           this.harvestTimesData = data
           /* Uncomment for local test only */
           //this.harvestTimesData.notHarvested = ['http://foobar.dk/images/bar/lorem/public/images/icon_facebook.png','http://foobar.dk/images/bar/lorem/public/images/icon_instagram.png','http://foobar.dk/images/bar/lorem/public/images/payment_icon.png','http://foobar.dk/images/bar/lorem/public/images/icon_favorite.png']
          //this.harvestTimesData.notHarvested = []
      }).catch(() => {
          this.setNotification({
          	title: 'We are so sorry!',
            text: 'Something went wrong when fetching the harvest data for this page - please try again',
            type: 'error',
            timeout: false
          })
        })
  },

  methods: {
     ...mapActions('Notifier', {
      setNotification: 'setNotification'
    }),

    getPrettyDate(date) {
      return toHumanDate(new Date(date), true, true)
    }
   },
}
</script>
