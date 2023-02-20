import configs from './index'

export function setServerConfigInApp(configFromServer) {
        configs.playbackConfig.alternativePlaybackBaseURL = configFromServer['openwayback.baseurl']
        configs.playbackConfig.solrwaybackBaseURL = configFromServer['wayback.baseurl']
        configs.playbackConfig.playbackDisabled = configFromServer['playback.disabled']
        configs.search.uploadedFileDisabled = configFromServer['search.uploaded.file.disabled']
        configs.search.pagination = configFromServer['search.pagination']
        configs.exportOptions.warcAllowed = configFromServer['allow.export.warc']
        configs.exportOptions.csvAllowed = configFromServer['allow.export.csv']
        configs.exportOptions.csvFields = configFromServer['export.csv.fields']
        configs.leaflet.attribution = configFromServer['leaflet.attribution']
        configs.leaflet.source = configFromServer['leaflet.source']
        configs.leaflet.map.latitude = configFromServer['maps.latitude']
        configs.leaflet.map.longitude = configFromServer['maps.longitude']
        configs.leaflet.map.radius = configFromServer['maps.radius']
        configs.visualizations.ngram.startYear = configFromServer['archive.start.year']
        configs.logo.url = configFromServer['top.left.logo.image']
        configs.logo.link = configFromServer['top.left.logo.image.link']
                        
        // Collection values starting with PLAYBACK_<collection>. The collectioname is dynamic from solrwaybackweb.properties  
        // Set as attribute configs.collection.key 
        // Example PLAYBACK_coronacollection = http://server1.com/pywbcorona
        Object.keys(configFromServer).forEach(function(key,index) {
           if (key.startsWith('PLAYBACK_')){
              configs.collection.playback.set(key,configFromServer[key])                           
            }                      
        })        
        
}

export function isPlaybackDisabled(){
        return configs.playbackConfig.playbackDisabled === 'true'? true : false
}

export function isUploadFileSearchDisabled(){
      return configs.search.uploadedFileDisabled === 'true'? true : false
    }