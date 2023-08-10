<template>
  <div class="linkGraphContainer">
    <h2 class="toolboxHeadline">
      Ngram Netarchive
    </h2>
    <div class="contentContainer">
      <search-box />
      <netarchive-chart />
    </div>
  </div>
</template>

<script>

import NetarchiveChart from '@/components/ngrams/netarchive/NetarchiveChart.vue'
import SearchBox from '@/components/ngrams/netarchive/SearchBox.vue'


export default {
  name: 'NgramNetarchive',
  components: {
   NetarchiveChart,
   SearchBox,
  }

  
}
</script>
