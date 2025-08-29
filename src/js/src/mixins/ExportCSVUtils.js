/**
 * Helpers for export to CSV
 * 
 */

import { mapStores } from 'pinia'
import { useNgramStore } from '../store/ngram.store'


 export default {
  computed: {
    // ...mapState({
    //   datasets: state => state.Ngram.datasets,
    //   labels: state => state.Ngram.labels
     
    // })
    ...mapStores(useNgramStore)
  },
  methods: {
  
    $_doCSVExport() {
      //The arrays for constructing the dataset that's needed for the CSV engine
      let dateCount = [], totalCount = []
      let description = ['Date', 'Total_documents']
      
      //Populate the "total count of documents in index" (totalCount) array with a series of 
      //total counts pr. dates. We only need this series once because it is the same for all queries
      Object.keys(this.datasets[0])
      .filter(value => value === 'total')
      .map(e => totalCount.push(this.datasets[0][e]))
  
      this.datasets.forEach((dataEntry, i) => {
     
        //Add queries to descripion array which will serve as our 'first row' containing labels and queries
      Object.keys(dataEntry)
      .filter(value => value === 'query')
      .map(e => description.push(dataEntry[e]))
      
      //Creates array with the count pr. year pr. query
      Object.keys(dataEntry)
      .filter(value => value === 'count')
      .map(e => dateCount.push(dataEntry[e]))
      })
      
      const finalDataset = this.createFinalDataSet(totalCount, dateCount, description)
      const filename = this.getFileName()  
      this.exportToCSV(finalDataset, filename)
    },

    getFileName() {
       let now = new Date()
       let date = now.toDateString().replaceAll(' ', '-')
       let time = now.toTimeString().split(' ')[0].replaceAll(':', '_')
       return  `Netarchive-ngram-${date}-${time}.csv`
    },

    createFinalDataSet(totalCount, dateCount, description){
      let finalDataset = []
       // We need to get the start year so we know how many 'year rows' to generate 
      // Push this first row to the final dataset (holds the queries)
      finalDataset.push(description)
      //Loop the total count pr. year and create all the rows with [year, total_count (for year), count for query, count for query...]
      totalCount[0].forEach((entry, i) => {
        let dataEntrySet = []
        dataEntrySet.push(this.labels[i])
        dataEntrySet.push(entry)
        //We loop year data for every query the user has sumbitted
        dateCount.forEach((countEntry, j) => {
          dataEntrySet.push(countEntry[i])
        })
        finalDataset.push(dataEntrySet)
        //Bump start year with one so we get year progression as loop continues
    })

    return finalDataset
    },

    /**
    * Method to export to CSV. Does all the nice things as handle browser quirks and illegal characters
    * 
    * Kudos:
    * https://stackoverflow.com/questions/14964035/how-to-export-javascript-array-info-to-csv-on-client-side
    * */
    exportToCSV(finalDataset, filename){
      let processRow = function (row) {
        let finalVal = ''
        for (let j = 0; j < row.length; j++) {
            let innerValue = row[j] === null ? '' : row[j].toString()
            if (row[j] instanceof Date) {
                innerValue = row[j].toLocaleString()
            }
            let result = innerValue.replace(/"/g, '""')
            if (result.search(/("|,|\n)/g) >= 0)
                result = '"' + result + '"'
            if (j > 0)
                finalVal += ','
            finalVal += result
        }
        return finalVal + '\n'
    }

    let csvFile = ''
    for (let i = 0; i < finalDataset.length; i++) {
        csvFile += processRow(finalDataset[i])
    }

    let blob = new Blob([csvFile], { type: 'text/csv;charset=utf-8;' })
    if (navigator.msSaveBlob) { // IE 10+
        navigator.msSaveBlob(blob, filename)
    } else {
        let link = document.createElement('a')
        if (link.download !== undefined) { // feature detection
            // Browsers that support HTML5 download attribute
            let url = URL.createObjectURL(blob)
            link.setAttribute('href', url)
            link.setAttribute('download', filename)
            link.style.visibility = 'hidden'
            document.body.appendChild(link)
            link.click()
            document.body.removeChild(link)
        }
    }

    }

  }
}