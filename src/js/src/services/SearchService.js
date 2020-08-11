import axios from 'axios'

export const searchService = {
  fireSearch,
  fireFacetRequest
}

function fireSearch (query, filters) {
  // Split url and move to config
  const url = '/frontend/solr/search/results/' + `?query=${encodeURIComponent(query) + filters}`
  return axios.get(url).then(response => {
    console.log('results', response)
    return addHighlightDataToSearchResult(response.data)
  }).catch(error => {
    
    return Promise.reject(error)
  })
}

function fireFacetRequest (query, filters) {
  // Split url and move to config
  const url = ' /frontend/solr/search/facets/' + `?query=${encodeURIComponent(query) + filters}`
  return axios.get(url).then(response => {
    console.log('facets', response.data.facet_counts)
    return response.data.facet_counts
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
