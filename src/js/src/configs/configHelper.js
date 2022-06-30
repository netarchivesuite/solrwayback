import configs from './index'

export function setServerConfigInApp(configFromServer) {
        configs.playbackConfig.openwaybackBaseURL = configFromServer['openwayback.baseurl']
        configs.playbackConfig.solrwaybackBaseURL = configFromServer['wayback.baseurl']
        configs.playbackConfig.playbackDisabled = configFromServer['playback.disabled']
        configs.search.uploadedFileDisabled = configFromServer['search.uploaded.file.disabled']
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
}

export function isPlaybackDisabled(){
    return `${configs.playbackConfig.playbackDisabled}`
}

export function isUploadFileSearchDisabled(){
        return `${configs.search.uploadedFileDisabled}`
    }