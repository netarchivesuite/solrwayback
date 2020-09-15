<template>
  <div id="harvests-for-day">
    <h3>Harvests for {{ date | human-date }}</h3>
    <ol>
      <li v-for="(harvest, index) in harvests" :key="index">
        <a :href="generateLink(harvest)" target="_blank">{{ harvest | human-date-and-time }}</a>
      </li>
    </ol>
  </div>
</template>

<script>
import {toHumanDate} from './util'
import format from 'date-fns/format'
import configs from '../../configs'

export default {
  name: 'HarvestDay',
  components: {  
   
  },
  filters: {
  'human-date': function (date, showWeekday = false) {
      return toHumanDate(date, showWeekday)
    },

  'human-date-and-time': function (date) {
       if (date instanceof Date) {
        return toHumanDate(date) + ` ${date.getHours() < 10 ? '0' + date.getHours() : date.getHours()}:${date.getMinutes() < 10 ? '0' + date.getMinutes() : date.getMinutes()}`
    }
    return date
    }
   },

  props: {
    harvests: {
      type: Array,
      required: true
    },

    date: {
      type: Date,
      required: true
    },

    url:{
      type: String,
      required:true
    }
  },
  
  methods: {
    generateLink(harvest) {
      const solrWaybackUrl = configs.playbackConfig.solrwaybackBaseURL 
      return `${solrWaybackUrl}services/web/${format(harvest, 'YYYYMMDDHHmmss')}/${this.url}`
    }
  }
}

</script>

    
