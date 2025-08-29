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

async function fireSearchRequest (query, facets, options) {
  let optionString = '&start=' + options.offset + '&grouping=' + options.grouping + '&sort=' + options.sort
  // Split url and move to config
  let facetsStr = ''
    for (const f of facets){
      facetsStr = facetsStr + '&fq=' + encodeURIComponent(f.substring(4))
    }

  const url = 'services/frontend/solr/search/results/' + `?query=${encodeURIComponent(query) + facetsStr + optionString}`

  try{
    const response = await axios.get(url)
    let returnObj = response.data

    if(options.grouping === false) {
      returnObj = dataTransformationHelper.transformSearchResponse(returnObj)
    }
    else {
      returnObj = dataTransformationHelper.transformGroupedSearchResponse(returnObj)
    }

    return returnObj

  } catch (error){
    return Promise.reject(error)
  }

}

async function fireImageSearchRequest(query) {
  // Split url and move to config
  const url = 'services/frontend/images/search/' + `?query=${encodeURIComponent(query)}`

  try{
    const response = await axios.get(url)
    let returnObj = response.data

    returnObj = dataTransformationHelper.transformImageResponse(returnObj,'image')

    return returnObj

  } catch (error){
    return Promise.reject(error)
  }

}

async function fireFacetRequest (query, facets, options) {
  let optionString = '&start=' + options.offset + '&grouping=' + options.grouping
  // Split url and move to config
  let facetsStr = ''
  for (const f of facets){
    // encoding the part after the =
    facetsStr = facetsStr + '&fq=' + encodeURIComponent(f.substring(4))
  }
  // Split url and move to config
  const url = 'services/frontend/solr/search/facets/' + `?query=${encodeURIComponent(query) + facetsStr + optionString}`

  try{
    const response = await axios.get(url)
    
    return response.data.facet_counts

  } catch (error){
    return Promise.reject(error)
  }

}

async function fireImagesRequest (source_file_path, offset) {
  const url = 'services/frontend/images/htmlpage/' + `?source_file_path=${encodeURIComponent(source_file_path)}&offset=${offset}`

  try{
    const response = await axios.get(url)

    return response.data

  } catch (error){
    return Promise.reject(error)
  }
  
}

async function fireLookupRequest(id) {
  const url = 'services/frontend/solr/idlookup/' + `?id=${id}`

  try{
    const response = await axios.get(url)

    return response.data

  } catch (error){
    return Promise.reject(error)
  }
  
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

async function getNormalizedURL(query) {
  const url =  `services/frontend/util/normalizeurl/?url=${encodeURIComponent(query)}`

  try{
    const response = await axios.get(url)

    return response.data.url

  } catch (error){
    return Promise.reject(error)
  }

}

async function getNormalizedUrlSearch(url, facets, options) {

  try{
    const response = await fireSearchRequest('url_norm:"' + url + '"', facets, options)

    return response

  } catch (error){
    return Promise.reject(error)
  }

}

async function getNormalizedUrlFacets(url, facets, options) {

  try{
    const response = await fireFacetRequest('url_norm:"' + url + '"', facets, options)

    return response

  } catch (error){
    return Promise.reject(error)
  }

}

async function getHarvestDates(harvestUrl) {
  const url = 'services/frontend/harvestDates/' + `?url=${harvestUrl}`

  try{
    const response = await axios.get(url)

    return response.data

  } catch (error){
    return Promise.reject(error)
  }

}

async function getAboutText() {
  const url = 'services/frontend/help/about/'

  try{
    const response = await axios.get(url)

    return response.data

  } catch (error){
    return Promise.reject(error)
  }

}

async function getSearchGuidelines() {
  const url = 'services/frontend/help/search/'

  try{
    const response = await axios.get(url)

    return response.data

  } catch (error){
    return Promise.reject(error)
  }

}

async function getCollectionInfo() {
  const url = 'services/frontend/help/collection/'

  try{
    const response = await axios.get(url)

    return response.data

  } catch (error){
    return Promise.reject(error)
  }

}
  
async function getHarvestedPageResources(source_file_path, offset) {
  const url = `services/timestampsforpage/?source_file_path=${encodeURIComponent(source_file_path)}&offset=${offset}`

  try{
    const response = await axios.get(url)

    return response.data

  } catch (error){
    return Promise.reject(error)
  }

}

async function getDomainStatistics(domain, startDate, endDate, timeScale) {
  let settings = ''
  if (timeScale != null && timeScale != '') {
      settings = '&startdate=' + startDate +'&enddate='+ endDate + '&scale=' + timeScale
  }
  const url = `services/statistics/domain/?domain=${domain + settings}`

  try{
    const response = await axios.get(url)

    return response.data

  } catch (error){
    return Promise.reject(error)
  }

}

async function getNgramNetarchive(params){
  let url
  let settings = ''
    if (params.timeScale != null && params.timeScale != '') {
      settings = '&startdate=' + params.startDate +'&enddate=' + params.endDate + '&scale=' + params.timeScale
  }
  params.searchType ==='tags' ? 
  url = `services/frontend/smurf/tags/?tag=${encodeURIComponent(params.query) + settings}`
  :
  url = `services/frontend/smurf/text/?q=${encodeURIComponent(params.query) + settings}`

  try{
    const response = await axios.get(url)

    return response.data

  } catch (error){
    return Promise.reject(error)
  }

}
    
async function fireGeoImageSearchRequest(query,latitude,longitude,radius) {
  const url = 'services/frontend/images/search/location/' + `?query=${query}&latitude=${latitude}&longitude=${longitude}&d=${radius}`

  try{
    const response = await axios.get(url)
    let returnObj = response.data

    returnObj = dataTransformationHelper.transformImageResponse(returnObj,'geoImage')

    return returnObj

  } catch (error){
    return Promise.reject(error)
  }

}

 
async function getPWID(sourceFilePath, offset) {
  const url = `services/generatepwid/?source_file_path=${encodeURIComponent(sourceFilePath)}&offset=${offset}`

  try{
    const response = await axios.get(url)

    return response.data

  } catch (error){
    return Promise.reject(error)
  }

}

async function getWarcHeader(sourceFilePath, offset) {
  const url = `services/warc/header/?source_file_path=${encodeURIComponent(sourceFilePath)}&offset=${offset}`

  try{
    const response = await axios.get(url)

    return response.data

  } catch (error){
    return Promise.reject(error)
  }

}

async function getLinkGraph(domain, facetLimit, ingoing, dateStart, dateEnd) {
  const url = `services/frontend/tools/linkgraph/?domain=${domain}&facetLimit=${facetLimit}&ingoing=${ingoing}&dateStart=${dateStart}&dateEnd=${dateEnd}`

  try{
    const response = await axios.get(url)

    return response.data

  } catch (error){
    return Promise.reject(error)
  }

}

async function getMoreFacets(domain, query, appliedFacets) {
  const url = `services/frontend/solr/search/facets/loadmore/?facetfield=${domain}&grouping=false&query=${encodeURIComponent(query) + appliedFacets}`

  try{
    const response = await axios.get(url)

    return response.data

  } catch (error){
    return Promise.reject(error)
  }

}
