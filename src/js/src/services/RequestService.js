import axios from 'axios'
import dataTransformationHelper from './dataTransformationHelper'

export const requestService = {
  fireSearchRequest,
  fireFacetRequest,
  fireLookupRequest,
  fireImagesRequest,
  uploadFileRequest,
  fireImageSearchRequest,
  getHarvestDates,
  getNormalizedUrlSearch,
  getNormalizedUrlFacets,
  getHarvestedPageResources,
}

function fireSearchRequest (query, facets, options) {
  let optionString = '&start=' + options.offset + '&grouping=' + options.grouping
  // Split url and move to config
  const url = 'services/frontend/solr/search/results/' + `?query=${query + facets.join('') + optionString}`
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
  const url = 'services/frontend/images/search/' + `?query=${query}`
  return axios.get(
    url, {
      transformResponse: [
        function(response) {
          let returnObj = JSON.parse(response)
          returnObj = dataTransformationHelper.transformImageResponse(returnObj)
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
  const url = 'services/frontend/solr/search/facets/' + `?query=${query + facets.join('') + optionString}`
  return axios.get(
    url).then(response => {
    //console.log('facets', response.data.facet_counts)
    return response.data.facet_counts
  }).catch(error => {
    return Promise.reject(error)
  })
}

function fireImagesRequest (source_file_path, offset) {
  const url = 'services/frontend/images/htmlpage/' + `?source_file_path=${source_file_path}&offset=${offset}`
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

function getNormalizedUrlSearch(query, facets, options) {
  const url = 'services/frontend/util/normalizeurl/' + '?url=' + query
  return axios.get(
    url).then(response => {
    // Split url and move to config
    return fireSearchRequest('url_norm:"' + response.data.url + '"', facets, options).then(returnObj => {
        return returnObj
      }).catch(error => {
        return Promise.reject(error)
      })
  }).catch(error => {
    return Promise.reject(error)
  })
}

function getNormalizedUrlFacets(query, facets, options) {
  const url = 'services/frontend/util/normalizeurl/' + '?url=' + query
  return axios.get(
    url).then(response => {
    // Split url and move to config
    return fireFacetRequest('url_norm:"' + response.data.url + '"', facets, options).then(returnObj => {
        return returnObj
      }).catch(error => {
        return Promise.reject(error)
      })
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

function getHarvestedPageResources(source_file_path, offset) {
  const url = `services/timestampsforpage/?source_file_path=${encodeURIComponent(source_file_path)}&offset=${offset}`
  return axios.get(
    url).then(response => {
    return response.data
  }).catch(error => {
    return Promise.reject(error)
  })
}
