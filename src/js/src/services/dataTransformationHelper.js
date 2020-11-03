export default {
  transformSearchResponse(data) {
    data.response.searchType = 'post'
    for(let i = 0; i < data.response.docs.length; i++) {
      data.response.docs[i].highlight = data.highlighting[data.response.docs[i].id]
      }
      return data
  },
  transformGroupedSearchResponse(data) {
    data.response = {}
    data.response.docs = []
    data.response.numFound = data.grouped.url.doclist.numFound
    data.response.maxScore = data.grouped.url.doclist.maxScore
    data.response.start = data.grouped.url.doclist.start
    data.response.cardinality = data.stats.stats_fields.url.cardinality
    data.response.searchType = 'post'
    for(let i = 0; i < data.grouped.url.doclist.docs.length; i++) {
      data.response.docs[i] = data.grouped.url.doclist.docs[i]
      data.response.docs[i].highlight = data.highlighting[data.grouped.url.doclist.docs[i].id]
    }
    return data
  },
  transformImageResponse(data, type) {
    let obj = {}
    obj.response = {
      searchType:type,
      images:data,
      }
    // THIS IS FOR TEST PURPOSES ONLY - IF THE IMAGES DONT HAVE LAT/LNG DATA.
    /*if(type === 'geoImage') {
      for(let y = 0; y < obj.response.images.length; y++) {
        obj.response.images[y].latitude = 56 + Math.random()
        obj.response.images[y].longitude = 10 + Math.random()
      }
    } */
    return obj
  }
}
