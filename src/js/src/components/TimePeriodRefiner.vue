<template>
  <div class="timePeriodRefinerSettings">
    <div class="timePeriodRefinerPeriodContainer contain">
      <label class="timePeriodRefinerLabel">Refine:</label>
      <label class="timePeriodRefinerLabel">Time frame:</label>
      between
      <input v-model="startDateInput"
             placeholder="YYYY-MM-DD"
             :class="$_checkDate(startDateInput) ? '' : 'urlNotTrue'"
             @input="updateStartDate()">
      and
      <input v-model="endDateInput" 
             placeholder="YYYY-MM-DD"
             :class="$_checkDate(endDateInput) ? '' : 'urlNotTrue'"
             @input="updateEndDate()">
    </div>
    <div id="timeScaleDropdown" class="timePeriodRefinerTimeScaleContainer">
      <label class="timePeriodRefinerLabel">Time scale:</label>
      <select id="timeScaleSelect"
              v-model="timeScaleInput"
              @change="updateTimeScale()">
        <option value="YEAR">
          Year
        </option>
        <option value="MONTH">
          Month
        </option>
        <option value="WEEK">
          Week
        </option>
        <option value="DAY">
          Day
        </option>
      </select>
    </div>
  </div>
</template>

<script>

import StringManipulationUtils from './../mixins/StringManipulationUtils'
import configs from './../configs'

export default {
  mixins: [StringManipulationUtils],
  emits: ['startdate', 'enddate', 'timescale'],
  data: () => ({
    startDateInput:configs.visualizations.ngram.startYear + '-01-01',
    endDateInput:(new Date().getUTCFullYear() + 1) + '-01-01',
    timeScaleInput:'YEAR'
  }),
  mounted() {
    this.updateStartDate()
    this.updateEndDate()
    this.updateTimeScale()
  },
  methods: {
    updateStartDate(){
      this.$emit('startdate', this.startDateInput)
    },
    updateEndDate(){
      this.$emit('enddate', this.endDateInput)
    },
    updateTimeScale(){
      this.$emit('timescale', this.timeScaleInput)
    }
  }
}

</script>