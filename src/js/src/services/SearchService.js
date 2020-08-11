import axios from 'axios'

export const searchService = {
  fireSearch,
  fireFacetRequest
}

function fireSearch (query) {
  // Split url and move to config
  const url = '/frontend/solr/search/results/' + `?query=${encodeURIComponent(query)}`
  return axios.get(url).then(response => {
    console.log('response', response)
    return addHighlightDataToSearchResult(response.data)
  }).catch(error => {
    
    return Promise.reject(error)
  })
}

function fireFacetRequest (query) {
  // Split url and move to config
  const url = ' /frontend/solr/search/facets/' + `?query=${encodeURIComponent(query)}`
  return axios.get(url).then(response => {
    console.log('facets', response)
    return response.data
  }).catch(error => {
  
    return Promise.reject(error)
  })
}

function addHighlightDataToSearchResult(data) {
  console.log(data.highlighting)
  for(let i = 0; i < data.response.docs.length; i++) {
    data.response.docs[i].highlight = data.highlighting[data.response.docs[i].id];
  }
  return data
}
