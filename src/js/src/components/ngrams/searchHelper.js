import Configs from './netarchive/configs'
export default {
    handleSearch(queryFromClick, dateFromClick, searchType, scale) {
      let url
      let gap
      let filter
      //  search on crawl_year if scale is YEAR
      if (scale == 'YEAR') {
        filter = '&fq=crawl_year:' + dateFromClick.slice(0,4)
      } else {
        // search on crawl_date otherwise
        if (scale == 'WEEK') {
          gap = '+7DAYS'
        } else {
          gap = '+1' + scale
        }
        filter = '&fq=crawl_date:[' + dateFromClick + 'T00:00:00Z TO ' + dateFromClick + 'T00:00:00Z'+ gap +']'
      }
      let param = '&grouping=false&imgSearch=false&offset=0&urlSearch=false'
      let facets = filter + '&fq=content_type_norm:"html"'
      if (searchType === 'tags'){
        url = `${Configs.BASE_SEARCH_URL()}?query=${encodeURIComponent('elements_used:"'+ queryFromClick +'"')}${param}&facets=${encodeURIComponent(facets)}`
      } else {
        url = `${Configs.BASE_SEARCH_URL()}?query=${encodeURIComponent(queryFromClick)}${param}&facets=${encodeURIComponent(facets)}`
      }
      window.open(url, '_blank')
    }
}