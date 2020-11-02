export default {
    handleSearch(queryFromClick, yearFromClick) {
      let currentUrl = window.location
      let baseUrl = `${currentUrl.protocol}//${currentUrl.host}/${currentUrl.pathname.split('/')[1]}`
      let searchPrefix = currentUrl.pathname.split('/')[1] === 'search' ? '' : '/search'
      const url = `${baseUrl}${searchPrefix}?query=${encodeURIComponent(queryFromClick)}&grouping=false&imgSearch=false&offset=0&urlSearch=false&facets=%26fq%3Dcrawl_year%3A"${yearFromClick}"` 
      window.open(url, '_blank')
    }
}