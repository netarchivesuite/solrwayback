<template>
  <div id="details">  
    <div v-for="(_, year, index) in harvestData.dates" :key="index">
      <p class="yearHeader">
        {{ year }}
      </p>
      <table v-for="(week, weekNumber, index_week_number) in harvestData.dates[year]['weeks']" :key="index_week_number" class="harvestCalendarTable">
        <tbody>
          <tr v-for="(data, dayNumber, index_day_number) in week" :key="index_day_number">
            <td v-if="data !== null"
                v-tooltip.top-center="formatHarvestDate(data)"
                class="weekday"
                :class="mapActivityLevel(data)" />
            <td v-if="data === null" class="weekday filler">
              <!-- non-existing day -->
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script>

import {toHumanDate} from './util'

export default {
  name: 'AllYearsGraph',
  components: {  
   
  },
  props: {
    harvestData: {
      type: Object,
      required: true
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

    
