<template>
  <div class="linkGraphContainer">
    <h2 class="toolboxHeadline">
      {{ $t('toolBox:ngram_netarchive') }}
    </h2>
    <div class="contentContainer">
      <transition name="loading-overlay">
        <loading-overlay />
      </transition>
      <search-box />
      <netarchive-chart />
    </div>
  </div>
</template>

<script>

import NetarchiveChart from '@/components/ngrams/netarchive/NetarchiveChart.vue'
import SearchBox from '@/components/ngrams/netarchive/SearchBox.vue'
import LoadingOverlay from '@/components/LoadingOverlay'



export default {
  name: 'NgramNetarchive',
  components: {
   NetarchiveChart,
  LoadingOverlay, 
   SearchBox,
  }

  
}
</script>
