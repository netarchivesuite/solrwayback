// import { config } from '../config'
import axios from 'axios'


export const SearchService = {
  search
}

function search (query) {
  //let query = `?q=${encodeURIComponent(params.searchTerm)}`
  

  //const url = `${config.apiUrl}` + '/searchsolr' + callParams
  // Split url and move to config
  const url = '/frontend/solr/search/results/' + `?query=${encodeURIComponent(query)}`
  return axios.get(url).then(response => {
    console.log('response', response)
    return response.data
  }).catch(error => {
    
    return Promise.reject(error)
  })
}
