import axios from 'axios'

export const searchService = {
  fireSearch
}

function fireSearch (query) {
  // Split url and move to config
  const url = '/frontend/solr/search/results/' + `?query=${encodeURIComponent(query)}`
  return axios.get(url).then(response => {
    console.log('response', response)
    return response.data
  }).catch(error => {
    
    return Promise.reject(error)
  })
}
