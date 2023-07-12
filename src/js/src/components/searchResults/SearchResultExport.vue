<template>
  <div>
    <div v-if="configs.exportOptions.warcAllowed === 'true' || configs.exportOptions.csvAllowed === 'true'" class="downloadSearchResultDropdown">
      <div class="downloadSearchResultButton">
        See available export options
      </div>
      <a v-if="configs.exportOptions.warcAllowed === 'true'" class="exportButton" :href="exportToWARC()">
        WARC export
      </a>
      <a v-if="configs.exportOptions.warcAllowed === 'true'" class="exportButton" :href="exportToWARCGZ()">
        WARC.GZ export
      </a>
      <a v-if="configs.exportOptions.warcAllowed === 'true'" class="exportButton" :href="exportToExtendedWARC()">
        expanded WARC export
      </a>
      <a v-if="configs.exportOptions.warcAllowed === 'true'" class="exportButton" :href="exportToExtendedWARCGZ()">
        expanded WARC.GZ export
      </a>
      <button v-if="configs.exportOptions.csvAllowed === 'true'" class="exportButton" @click="toggleCsvExportOptions()">
        CSV export
      </button>
      <button class="exportButton">
        Batch content export
      </button>
      
    </div>
    <div v-if="csvExportOpen" class="csvExportOptions">
      <div class="csvExportContent">
        <button class="closeButton" @click="toggleCsvExportOptions()">
          ✕ 
        </button>
       
       
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
        <div class="exportContent">
          <h2>Options</h2>
          <div class="csvExportOptionHeader">
            <h4>Format</h4>
          </div>
          <div>
            <div>
              <input id="export-csv"
                     v-model="exportOptions.format"
                     type="radio"
                     value="csv">
              <label for="export-csv">CSV</label>
            </div>
            <div>
              <input id="export-json"
                     v-model="exportOptions.format"
                     type="radio"
                     value="json">
              <label for="export-json">JSON</label>
            </div>
          
            <input id="export-jsonl"
                   v-model="exportOptions.format"
                   type="radio"
                   value="jsonl">
            <label for="export-jsonl">JSONL</label>
          </div>
          <div class="csvExportOptionHeader">
            <h4>Other</h4>
          </div>
          <div>
            <div>
              <input id="export-gzip"
                     v-model="exportOptions.gzip"
                     type="checkbox"
                     true-value="true"
                     false-value="false">
              <label for="export-gzip">Gzip</label>
            </div>
            <div>
              <input id="export-flatten"
                     v-model="exportOptions.flatten" 
                     type="checkbox"
                     true-value="true"
                     false-value="false">
              <label for="export-flatten">Flatten <span class="buttonExplanation" title="Flatten will split multivalue fields into single values and add as many new lines to the export. Using with 'content' field selected can cause very large exports.">[ ? ]</span></label>
            </div>
          </div>
        </div>
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
      nonSelectedArray:[],
      exportOptions: {
        grouping:'',
        flatten:false,
        format:'csv',
        gzip:false
      }
    }
  },
  computed: {
    ...mapState({
      query: state => state.Search.query,
      searchAppliedFacets: state => state.Search.searchAppliedFacets,
      solrSettings: state => state.Search.solrSettings
    })
  },
  mounted () {
    this.selectedArray = this.getSplitFieldsSelected(this.configs.exportOptions.csvFields)
    this.nonSelectedArray = this.getSplitFieldsNotSelected(this.configs.exportOptions.csvFields)
    this.exportOptions.grouping = this.solrSettings.grouping ? 'url_norm' : this.solrSettings.grouping
  },
  methods: {
    exportToWARC() {
     return this.searchAppliedFacets ? 
      `${this.returnExportUrl()}warc?query=${encodeURIComponent(this.query)}${this.getEncodedAppliedFacets(this.searchAppliedFacets).join('')}`:
      `${this.returnExportUrl()}warc?query=${encodeURIComponent(this.query)}`
    },
    exportToExtendedWARC() {
      return this.searchAppliedFacets ? 
      `${this.returnExportUrl()}warcExpanded?query=${encodeURIComponent(this.query)}${this.getEncodedAppliedFacets(this.searchAppliedFacets).join('')}`:
      `${this.returnExportUrl()}warcExpanded?query=${encodeURIComponent(this.query)}`
    },
    exportToWARCGZ() {
      return this.searchAppliedFacets ? 
      `${this.returnExportUrl()}warc?gzip=true&query=${encodeURIComponent(this.query)}${this.getEncodedAppliedFacets(this.searchAppliedFacets).join('')}` :
      `${this.returnExportUrl()}warc?gzip=true&query=${encodeURIComponent(this.query)}`
    },
    exportToExtendedWARCGZ() {
      return this.searchAppliedFacets ? 
      `${this.returnExportUrl()}warcExpanded?gzip=true&query=${encodeURIComponent(this.query)}${this.getEncodedAppliedFacets(this.searchAppliedFacets).join('')}`:
      `${this.returnExportUrl()}warcExpanded?gzip=true&query=${encodeURIComponent(this.query)}`
    },
    exportToCSV() {
      let fields = this.selectedArray.join(',')
      const groupFieldParam =  this.exportOptions.grouping ? `&groupfield=${this.exportOptions.grouping}` : ''
      return this.searchAppliedFacets ? 
      `${this.returnExportUrl()}fields?query=${encodeURIComponent(this.query)}${this.getEncodedAppliedFacets(this.searchAppliedFacets).join('')}&fields=${encodeURIComponent(fields)}${groupFieldParam}&flatten=${this.exportOptions.flatten}&format=${this.exportOptions.format}` :
      `${this.returnExportUrl()}fields?query=${encodeURIComponent(this.query)}&fields=${encodeURIComponent(fields)}${groupFieldParam}&flatten=${this.exportOptions.flatten}&format=${this.exportOptions.format}`
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

    },
    getEncodedAppliedFacets(appliedFacets) {
      return appliedFacets.map(facet => 
     `&fq=${encodeURIComponent(facet.slice(facet.indexOf('=') + 1))}`
     )
    }
  },
}
</script>