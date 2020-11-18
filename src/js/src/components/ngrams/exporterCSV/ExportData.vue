<template>
  <div class="exportData">
    <div class="exportboxContainer">
      <button class="exportboxCloseButton" @click="closeExporter()">
        ✕
      </button>
      <div class="exportboxContent">
        <h1>Download Graph data</h1>
        <div class="exportDataBox">
          <div>Format: <span class="dataFormat">CSV</span></div>  
          <div> Graphs in the data set:</div>
          <ul>
            <li v-for="(query, index) in datasetQueries"
                :key="index">
              {{ query }}
            </li>
          </ul>                    
          <button type="button" class="" @click.prevent="exportGraphData()">
            Download
          </button>
          <div />
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { mapState, mapActions } from 'vuex'
import ExportHelper from '../../../mixins/ExportCSVUtils'

export default {
  name: 'ExportData',
  
  mixins: [ExportHelper],
  data: function() {
    return {
          submittedQueries: this.datasetQueries
      }
},

  computed: {
    ...mapState({
      notifications: state => state.Notifier.notifications,
      datasets: state => state.Ngram.datasets,
      datasetQueries: state => state.Ngram.datasetQueries
    }),
  },

  

  methods: {
  ...mapActions('Ngram', {
      dismissNotification: 'dismissNotification'
      
  }),

  closeExporter() {
      this.$emit('close-exporter')
    },

   exportGraphData(filename) {
         this.$_doCSVExport()
          this.$emit('close-exporter')
     }
  }

}

</script>

    
