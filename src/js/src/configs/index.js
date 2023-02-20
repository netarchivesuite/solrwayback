export default {
  playbackConfig: { 
      alternativePlaybackBaseURL: '',
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

  collection:{
   playback: new Map()  
  },

  search:{
    uploadedFileDisabled:false,
    pagination:20
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