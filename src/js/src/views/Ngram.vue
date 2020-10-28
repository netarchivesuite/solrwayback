<template>
  <div class="contentContainer">
    <transition name="loading-overlay">
      <loading-overlay />
    </transition>
    <notifications />
    <h1>Solr<span>Wayback</span> - ngram</h1>
    <search-box />
    <netarchive-chart />
  </div>
</template>

<script>
// @ is an alias to /src
import NetarchiveChart from '@/components/ngrams/netarchive/NetarchiveChart.vue'
import Spinner from '@/components/ngrams/netarchive/Spinner.vue'
import SearchBox from '@/components/ngrams/netarchive/SearchBox.vue'
import NavHelper from '@/components/ngrams/netarchive/navHelper'
import LoadingOverlay from '@/components/LoadingOverlay'
import Notifications from '@/components/notifications/Notifications.vue'
import {mapActions} from 'vuex'


export default {
  name: 'Ngram',
  components: {
   NetarchiveChart,
  LoadingOverlay, 
   SearchBox,
   Notifications
  },
  mixins: [NavHelper],

  methods: {
    ...mapActions('Ngram', {
      resetSearchState:'resetState'
    })
  }, 

  beforeRouteUpdate (to, from, next) {
    this.$_handleRouteUpdates(to, from)
    next()
  },

  beforeRouteLeave (to, from, next) {
    this.$_handleRouteLeave(to)
    next()
  }
}
</script>
