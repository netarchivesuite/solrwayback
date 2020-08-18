import axios from 'axios'

export const searchService = {
  fireSearchRequest,
  fireFacetRequest
}

function fireSearchRequest (query, facets) {
  // Split url and move to config
  const url = '/frontend/solr/search/results/' + `?query=${query + facets}`
  return axios.get(
    url, {
      transformResponse: [
        function(response) {
          let returnObj = JSON.parse(response);
          for(let i = 0; i < returnObj.response.docs.length; i++) {
            returnObj.response.docs[i].highlight = returnObj.highlighting[returnObj.response.docs[i].id];
          }
          return returnObj;
        }
      ]}).then(returnObj => {
    return returnObj.data
  }).catch(error => {
    return Promise.reject(error)
  })
}

function fireFacetRequest (query, facets) {
  // Split url and move to config
  const url = ' /frontend/solr/search/facets/' + `?query=${query + facets}`
  return axios.get(
    url).then(response => {
    console.log('facets', response.data.facet_counts)
    return response.data.facet_counts
  }).catch(error => {
    return Promise.reject(error)
  })
}
