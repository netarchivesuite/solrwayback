<template>
  <div>
    <div class="harvestCalendarHeading">
      <h1>
        Solr<span>Wayback</span>
      </h1>
    </div>
    
    <div v-if="harvestData" class="tableContainer">
      <div class="harvestUrlHeading">
        Harvests for: <span><a :href="currentHarvestUrl">{{ currentHarvestUrl }}</a></span>
      </div>
      <table class="totalHarvestData">
        <tbody>
          <tr>
            <td>First harvest:</td>
            <td>{{ harvestData.fromDate | human-date }}</td>
          </tr>
          <tr>
            <td>Latest harvest:</td>
            <td>{{ harvestData.toDate | human-date }}</td>
          </tr>
          <tr>
            <td>Total harvests:</td>
            <td>{{ harvestData.numberOfHarvests | formatted-number }}</td>
          </tr>
        </tbody>
      </table>
   
      <p class="detailsMenu">
        Show: 
        <span class="pointer" :class="{active: view === 'year-month'}" @click="showYearMonth">Months</span> - 
        <span class="pointer" :class="{active: view === 'all-years'}" @click="showAllYears">Days</span>
      </p>
      <transition name="fadeIn">
        <year-month-graph v-if="view === 'year-month'" :harvest-data="harvestData" :show-year-details="showYearWeek" />
      </transition>        
      <transition name="fadeIn">
        <week-graph v-if="view === 'year-week'"
                    :year="year"
                    :harvest-data="harvestData"
                    :show-all="showYearMonth"
                    :url="currentHarvestUrl"
                    class="detailsContainer" />
      </transition> 
      <transition name="fadeIn">
        <all-years-graph v-if="view === 'all-years'" :harvest-data="harvestData" class="detailsContainer" />
      </transition>
    </div>
    <div v-if="!harvestData && noResults">
      <p>No results.</p>
    </div> 
    <!-- TODO implement new spinner function when ready -->           
    <div v-if="!harvestData && !noResults">
      <div id="spinner">
        <p class="spinnerText">
          Fetching harvests
        </p>
      </div>
      <div id="overlay" />
    </div>
  </div>     
</template>

<style lang="scss">
  @import '../assets/styles/harvestCalendar.scss'; 
</style>

<script>
import { requestService } from '../services/RequestService'
import {groupHarvestDatesByYearAndMonth} from '../components/harvestCalendar/tranformers/transformerMain'
import {calculateLinearActivityLevel, calculateLogarithmicActivityLevel} from '../components/harvestCalendar/plugins/tranformationHelpers'
import {toHumanDate} from '../components/harvestCalendar/util'
import YearMonthGraph from '../components/harvestCalendar/YearMonthGraph'
import AllYearsGraph from '../components/harvestCalendar/AllYearsGraph'
import WeekGraph from '../components/harvestCalendar/WeekGraph'
import Vue from 'vue'
import VTooltip from 'v-tooltip'

Vue.use(VTooltip)


export default {
  name: 'HarvestCalendar',
  components: {  
    YearMonthGraph,
    AllYearsGraph,
    WeekGraph
  },

  filters: {
  'human-date': function (date, showWeekday = false) {
      return toHumanDate(date, showWeekday)
    },
   'formatted-number': function (value) {
       if (!isNaN(value)) {
        return value.toLocaleString()
    }
    return value
   }
  },
  
  data () {
    return {
            harvestData: null,
            view: 'year-month',
            year: null,
            noResults: false,
            currentHarvestUrl: ''
        }
  },
  
  mounted () {
    if (this.$route.query.url){
       this.currentHarvestUrl = this.$route.query.url
     }
     requestService.getHarvestDates(encodeURIComponent(this.$route.query.url))
        .then(data => {
            if (data.dates === undefined || data.dates.length === 0) {
              console.log(data)
                this.noResults = true
            } else {
           this.harvestData = groupHarvestDatesByYearAndMonth(data.dates, calculateLinearActivityLevel)
            }
        })
  },
  
  methods: {
     showYearWeek(year) {
            this.year = year
            this.view = 'year-week'
        },
        showYearMonth() {
            this.year = null
            this.view = 'year-month'
        },
        showAllYears() {
            this.year = null
            this.view = 'all-years'
        }
  }
}

</script>

    
