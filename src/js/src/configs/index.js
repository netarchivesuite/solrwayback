export default {
  playbackConfig: { 
      openwaybackBaseURL: '',
      solrwaybackBaseURL:'',
      playbackDisabled:false
  },
  exportOptions: {
    warcAllowed:false,
    csvAllowed:false,
    csvFields:''
  },

  visualizations:{
    ngram:{
      startYear:''
    }
  },

  logo:{
    url: ''
  },

  search:{
    uploadedFileDisabled:false
  },

  leaflet: {
    attribution:'',
    source:'',
    map: {
      latitude:'',
      longitude:'',
      radius:''
    }
  },
}