<template>
  <div>
    <div v-if="configs.exportOptions.warcAllowed === 'true' || configs.exportOptions.csvAllowed === 'true'" class="downloadSearchResultDropdown">
      <div class="downloadSearchResultButton">
        See export options
      </div>
      <a v-if="configs.exportOptions.warcAllowed === 'true'" class="exportButton" :href="exportToWARC()">
        WARC export
      </a>
      <a v-if="configs.exportOptions.warcAllowed === 'true'" class="exportButton" :href="exportToExtendedWARC()">
        expanded WARC export
      </a>
      <button v-if="configs.exportOptions.csvAllowed === 'true'" class="exportButton" @click="toggleCsvExportOptions()">
        CSV export
      </button>
    </div>
    <div v-if="csvExportOpen" class="csvExportOptions">
      <div class="csvExportContent">
        <button class="closeButton" @click="toggleCsvExportOptions()">
          ✕ 
        </button>
        <div class="exportContent" />
        <div class="exportContent">
          <h2>CSV EXPORT</h2>
          <p>Select the fields of the posts in your search result, that you wish to have in the exported csv file.</p><br>
          <p>
            You can also rank them in the order that you wish to have them in, should you desire so. Simply hover over the item you wish to move 
            and press the desired direction.
          </p><br>
          <p>When you're done, simply press the download button.</p><p>
            <br>
            <a :href="exportToCSV()" class="downloadButton">
              Download
            </a>
          </p>
        </div>
        <div class="exportContent">
          <h2>Selected</h2>
          <div v-for="(item, index) in selectedArray" :key="'selected' + index" class="fieldItem">
            {{ item }} <div class="fieldItem actions">
              <button class="up" @click="moveItemInArray(selectedArray, 'up', index, item)">
                ⌃
              </button><button class="down" @click="moveItemInArray(selectedArray, 'down', index, item)">
                ⌄
              </button><button class="select" @click="toggleItemInArrays(nonSelectedArray, selectedArray, item, index, 'selectedArray')">
                ✕
              </button>
            </div>
          </div>
        </div>
        <div class="exportContent">
          <h2 class="nonSelectedExportHeadline">
            Non selected
          </h2> <button class="exportSelectAll" @click="selectAllFields()">
            Select all
          </button>
          <div v-for="(item, index) in nonSelectedArray" :key="'nonSelected' + index" class="fieldItem">
            {{ item }}  <div class="fieldItem actions">
              <button class="select" @click="toggleItemInArrays(selectedArray, nonSelectedArray, item, index,'nonSelectedArray')">
                ✔
              </button>
            </div>
          </div>
        </div>
        <div class="exportContent" />
      </div>
    </div>
  </div>
</template>
<script>
import { mapState } from 'vuex'

export default {
  name: 'SearchResultExport',
  props: {
    configs: {
      type: Object,
      required: true
    },
  },
  data () {
    return {  
      csvExportOpen:false,
      selectedArray:[],
      nonSelectedArray:[]
    }
  },
  computed: {
    ...mapState({
      query: state => state.Search.query,
      searchAppliedFacets: state => state.Search.searchAppliedFacets,
    }),
  },
  mounted () {
    this.selectedArray = this.getSplitFieldsSelected(this.configs.exportOptions.csvFields)
    this.nonSelectedArray = this.getSplitFieldsNotSelected(this.configs.exportOptions.csvFields)
    //console.log(configs.exportOptions.csvFields)
  },
  methods: {
    exportToWARC() {
     return this.searchAppliedFacets ? 
       this.returnExportUrl() + 'warc?query=' + this.query + this.searchAppliedFacets :
       this.returnExportUrl() + 'warc?query=' + this.query
    },
    exportToExtendedWARC() {
      return this.searchAppliedFacets ? 
      this.returnExportUrl() + 'warcExpanded?query=' + this.query + this.searchAppliedFacets :
      this.returnExportUrl() + 'warcExpanded?query=' + this.query
    },
    exportToCSV() {
      let fields = this.selectedArray.join(',')
      return this.searchAppliedFacets ? 
      this.returnExportUrl() + 'csv?query=' + this.query + this.searchAppliedFacets + '&fields=' + fields :
      this.returnExportUrl() + 'csv?query=' + this.query + '&fields=' + fields
    },
    returnExportUrl() {
      return this.configs.playbackConfig.solrwaybackBaseURL + 'services/export/'
    },
    toggleCsvExportOptions() {
      this.csvExportOpen = !this.csvExportOpen
      if(this.csvExportOpen === false) {
        this.selectedArray = this.getSplitFieldsSelected(this.configs.exportOptions.csvFields)
        this.nonSelectedArray = this.getSplitFieldsNotSelected(this.configs.exportOptions.csvFields)
      }
    },
    selectAllFields() {
      this.selectedArray = this.configs.exportOptions.csvFields.replace(/ /g, '').split(',')
      this.nonSelectedArray = []
    },
    getSplitFieldsSelected(fields) {
      return fields.replace(/ /g, '').split(',').slice(0,9)
    },
    getSplitFieldsNotSelected(fields) {
      let newArray = fields.replace(/ /g, '').split(',')
      return newArray.slice(9,newArray.length)
    },
    moveItemInArray(array, direction, itemNumber, item) {
      if(itemNumber >= 0 && itemNumber < array.length) {
      direction === 'up'
        // Small check to see if it's the first element. If so, it can't move up. 
        ? itemNumber !== 0 ? (array.splice(itemNumber, 1), array.splice(itemNumber - 1, 0, item)) : null
        // If its down, we don't have those kinda problems.
        : (array.splice(itemNumber, 1), array.splice(itemNumber + 1, 0, item))
      }
    },
    toggleItemInArrays(toArray, fromArray, item, itemNumber, recipient) {
      recipient === 'nonSelectedArray' ? toArray.push(item) : toArray.unshift(item)
      fromArray.splice(itemNumber,1)

    }
  },
}
</script>