<template>
  <div class="yearTables">
    <table class="harvestCalendarTable monthLabels">
      <tr>
        <td class="empty">
&nbsp;
        </td>
      </tr>
      <tr><td>January</td></tr>
      <tr><td>February</td></tr>
      <tr><td>March</td></tr>
      <tr><td>April</td></tr>
      <tr><td>May</td></tr>
      <tr><td>June</td></tr>
      <tr><td>July</td></tr>
      <tr><td>August</td></tr>
      <tr><td>September</td></tr>
      <tr><td>October</td></tr>
      <tr><td>November</td></tr>
      <tr><td>December</td></tr>
    </table>
    <table v-for="(yearData, year, index) in harvestData.dates" :key="index" class="harvestCalendarTable">
      <thead>
        <tr>
          <th>{{ year }}</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(data, month, index_month) in yearData.months" :key="index_month">
          <td v-tooltip.top-center="formatHarvestDate(data)" :class="mapActivityLevel(data)" @click="showYearDetails(year)">
&nbsp;
          </td>
        </tr>
      </tbody>
    </table>
    <color-legend />            
  </div>        
</template>

<script>

import ColorLegend from '../harvestCalendar/ColorLegend.vue'

export default {
  name: 'YearMontGraph',
  components: {  
  ColorLegend
  },
  props: {
    harvestData: {
      type: Object,
      required: true
    },

    showYearDetails: {
      type: Function,
      required: true
    }
  },
  methods: {
     formatHarvestDate(data) {
            return 'Harvests: ' + data.numberOfHarvests.toLocaleString()
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

    
