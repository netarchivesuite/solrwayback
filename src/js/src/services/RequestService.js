import axios from 'axios'
import dataTransformationHelper from './dataTransformationHelper'

export const requestService = {
  fireSearchRequest,
  fireFacetRequest,
  fireLookupRequest,
  fireImagesRequest
}

function fireSearchRequest (query, facets, options) {
  let optionString = '&start=' + options.offset + '&grouping=' + options.grouping
  // Split url and move to config
  const url = 'services/frontend/solr/search/results/' + `?query=${query + facets + optionString}`
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

function fireFacetRequest (query, facets, options) {
  let optionString = '&start=' + options.offset + '&grouping=' + options.grouping
  // Split url and move to config
  const url = 'services/frontend/solr/search/facets/' + `?query=${query + facets + optionString}`
  return axios.get(
    url).then(response => {
    console.log('facets', response.data.facet_counts)
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