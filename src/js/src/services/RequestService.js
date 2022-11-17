import axios from 'axios'
import dataTransformationHelper from './dataTransformationHelper'
import APP_CONFIGS from '../configs'

export const requestService = {
  fireSearchRequest,
  fireFacetRequest,
  fireLookupRequest,
  fireImagesRequest,
  fireImageSearchRequest,
  uploadFileRequest,
  getHarvestDates,
  getNormalizedUrlSearch,
  getAboutText,
  getNormalizedURL,
  getNormalizedUrlFacets,
  getHarvestedPageResources,
  getDomainStatistics,
  getSearchGuidelines,
  getCollectionInfo,
  getNgramNetarchive,
  fireGeoImageSearchRequest,
  getPWID,
  getWarcHeader,
  getLinkGraph,
  getMoreFacets
}

function fireSearchRequest (query, facets, options) {
  let optionString = '&start=' + options.offset + '&grouping=' + options.grouping
  // Split url and move to config
  const url = 'services/frontend/solr/search/results/' + `?query=${encodeURIComponent(query) + facets.join('') + optionString}`
  return axios.get(
    url, {
      transformResponse: [
        function(response) {
          let returnObj = JSON.parse(response)
          if(options.grouping === false) {
            returnObj = dataTransformationHelper.transformSearchResponse(returnObj)
          }
          else {
            returnObj = dataTransformationHelper.transformGroupedSearchResponse(returnObj)
          }
          return returnObj
        }
      ]}).then(returnObj => {
    return returnObj.data
  }).catch(error => {
    return Promise.reject(error)
  })
}

function fireImageSearchRequest(query) {
  // Split url and move to config
  const url = 'services/frontend/images/search/' + `?query=${encodeURIComponent(query)}`
  return axios.get(
    url, {
      transformResponse: [
        function(response) {
          let returnObj = JSON.parse(response)
          returnObj = dataTransformationHelper.transformImageResponse(returnObj,'image')
          return returnObj
        }
      ]}).then(returnObj => {
    return returnObj.data
  }).catch(error => {
    return Promise.reject(error)
  })
}

function fireFacetRequest (query, facets, options) {
  let optionString = '&start=' + options.offset + '&grouping=' + options.grouping
  // Split url and move to config
  const url = 'services/frontend/solr/search/facets/' + `?query=${encodeURIComponent(query) + facets.join('') + optionString}`
  return axios.get(
    url).then(response => {
    //console.log('facets', response.data.facet_counts)
    return response.data.facet_counts
  }).catch(error => {
    return Promise.reject(error)
  })
}

function fireImagesRequest (source_file_path, offset) {
  const url = 'services/frontend/images/htmlpage/' + `?source_file_path=${encodeURIComponent(source_file_path)}&offset=${offset}`
  return axios.get(
    url).then(response => {
    return response.data
  }).catch(error => {
    return Promise.reject(error)
  })
}

function fireLookupRequest(id) {
  const url = 'services/frontend/solr/idlookup/' + `?id=${id}`
  return axios.get(
    url).then(response => {
    return response.data
  }).catch(error => {
    return Promise.reject(error)
  })
}

function uploadFileRequest(fileData) {
 const url = 'services/frontend/upload/gethash/'
 let formData = new FormData()
 formData.append('file', fileData)
  return axios.post(url, formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }}).catch(error => {
  return Promise.reject(error)
  })
}

function getNormalizedURL(query) {
  const url =  `services/frontend/util/normalizeurl/?url=${encodeURIComponent(query)}` 
  return axios.get(
    url).then(response => {
    return response.data.url 
      }).catch(error => {
        return Promise.reject(error)
      })
}

function getNormalizedUrlSearch(url, facets, options) {
    return fireSearchRequest('url_norm:"' + url + '"', facets, options).then(returnObj => {
        return returnObj
      }).catch(error => {
        return Promise.reject(error)
      })
}

function getNormalizedUrlFacets(url, facets, options) {
    return fireFacetRequest('url_norm:"' + url + '"', facets, options).then(returnObj => {
        return returnObj
      }).catch(error => {
        return Promise.reject(error)
      })
}

function getHarvestDates(harvestUrl) {
  const url = 'services/frontend/harvestDates/' + `?url=${harvestUrl}`
  return axios.get(
    url).then(response => {
    return response.data
  }).catch(error => {
    return Promise.reject(error)
  })
}

function getAboutText() {
  const url = 'services/frontend/help/about/'
  return axios.get(
    url).then(response => {
    return response.data
  }).catch(error => {
    return Promise.reject(error)
  })
}

function getSearchGuidelines() {
  const url = 'services/frontend/help/search/'
  return axios.get(
    url).then(response => {
    return response.data
  }).catch(error => {
    return Promise.reject(error)
  })
}

function getCollectionInfo() {
  const url = 'services/frontend/help/collection/'
  return axios.get(
    url).then(response => {
    return response.data
  }).catch(error => {
    return Promise.reject(error)
  })
}
  
function getHarvestedPageResources(source_file_path, offset) {
  const url = `services/timestampsforpage/?source_file_path=${encodeURIComponent(source_file_path)}&offset=${offset}`
  return axios.get(
    url).then(response => {
    return response.data
  }).catch(error => {
    return Promise.reject(error)
  })
}

function getDomainStatistics(domain, startDate, endDate, timeScale) {
  let settings = ''
  if (timeScale != null && timeScale != '') {
      settings = '&startdate=' + startDate +'&enddate='+ endDate + '&scale=' + timeScale
  }
  const url = `services/statistics/domain/?domain=${domain + settings}`
  return axios.get(
    url).then(response => {
    return response.data
  }).catch((error) => {
    return Promise.reject(error)
  })
}

function getNgramNetarchive(params){
  let url
  params.searchType ==='tags' ? 
  url = `services/frontend/smurf/tags/?tag=${encodeURIComponent(params.query)}&startyear=${APP_CONFIGS.visualizations.ngram.startYear}`
  :
  url = `services/frontend/smurf/text/?q=${encodeURIComponent(params.query)}&startyear=${APP_CONFIGS.visualizations.ngram.startYear}`
  return axios.get(url).then(response => {
    return response.data
  }).catch(error => {
    return Promise.reject(error)
  })
}
    
function fireGeoImageSearchRequest(query,latitude,longitude,radius) {
  const url = 'services/frontend/images/search/location/' + `?query=${query}&latitude=${latitude}&longitude=${longitude}&d=${radius}`
  return axios.get(
    url, {
      transformResponse: [
        function(response) {
          let returnObj = JSON.parse(response)
          returnObj = dataTransformationHelper.transformImageResponse(returnObj,'geoImage')
          return returnObj
        }
      ]}).then(returnObj => {
    return returnObj.data
  }).catch(error => {
    return Promise.reject(error)
  })
}

 
function getPWID(sourceFilePath, offset) {
  const url = `services/generatepwid/?source_file_path=${encodeURIComponent(sourceFilePath)}&offset=${offset}`
  return axios.get(
    url).then(response => {
    return response.data
  }).catch(error => {
    return Promise.reject(error)
  })
}

function getWarcHeader(sourceFilePath, offset) {
  const url = `services/warc/header/?source_file_path=${encodeURIComponent(sourceFilePath)}&offset=${offset}`
  return axios.get(
    url).then(response => {
    return response.data
  }).catch(error => {
    return Promise.reject(error)
  })
}

function getLinkGraph(domain, facetLimit, ingoing, dateStart, dateEnd) {
  const url = `services/frontend/tools/linkgraph/?domain=${domain}&facetLimit=${facetLimit}&ingoing=${ingoing}&dateStart=${dateStart}&dateEnd=${dateEnd}`
  return axios.get(
    url).then(response => {
    return response.data
  }).catch(error => {
    return Promise.reject(error)
  })
}

function getMoreFacets(domain, query, appliedFacets) {
  const url = `services/frontend/solr/search/facets/loadmore/?facetfield=${domain}&grouping=false&query=${encodeURIComponent(query) + appliedFacets}`
  return axios.get(
    url).then(response => {
    return response.data
  }).catch(error => {
    return Promise.reject(error)
  })
}
