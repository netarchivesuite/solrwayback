<template>
  <div id="details">
    <p class="yearHeader">
      {{ year }} - <span class="hideDetails" @click="showAll()">Hide details</span>
    </p>
    <table v-for="(week, weekNumber, index) in harvestData.dates[year]['weeks']" :key="index" class="harvestCalendarTable">
      <tbody>
        <tr v-for="(data, dayNumber, index_day) in week" :key="index_day">
          <td v-if="data !== null"
              v-tooltip.top-center="formatHarvestDate(data)"
              class="weekday"
              :class="mapActivityLevel(data)"
              @click="harvestsForDay = data.harvests; showDate = data.date;" />
          <td v-if="data === null" class="weekday filler">
            <!-- non-existing day -->
          </td>
        </tr>
      </tbody>
    </table>
    <color-legend />        
    <harvests-day v-if="harvestsForDay !== null"
                  :harvests="harvestsForDay"
                  :date="showDate"
                  :url="url" />
  </div>
</template>

<script>


import {toHumanDate} from './util'
import ColorLegend from './ColorLegend.vue'
import HarvestsDay from './HarvestsDay.vue'

export default {
  name: 'WeekGraph',
  components: { 
    ColorLegend,
    HarvestsDay
  },
  props: {
    harvestData: {
      type: Object,
      required: true
    },

    year: {
      type: String,
      required: true
    },

    showAll:{
      type: Function,
      required:true
    },

    url:{
      type:String,
      required: true
    }


  },
  
  data () {
    return {
            harvestsForDay: null,
            showDate: null
        }
  },

  methods: {
    formatHarvestDate(data) {
            return toHumanDate(data.date, true) + '<br>' +
                'Harvests: ' + data.numberOfHarvests.toLocaleString()
        },
    mapActivityLevel(data) {
            return {
                activityLevel4: data.activityLevel === 4, 
                activityLevel3: data.activityLevel === 3, 
                activityLevel2: data.activityLevel === 2, 
                activityLevel1: data.activityLevel === 1
            }
        }
  }
}

</script>

    
