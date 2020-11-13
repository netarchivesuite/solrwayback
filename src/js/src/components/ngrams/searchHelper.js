import Configs from './netarchive/configs'
export default {
    handleSearch(queryFromClick, yearFromClick) {
      const url = `${Configs.BASE_SEARCH_URL()}?query=${encodeURIComponent(queryFromClick)}&grouping=false&imgSearch=false&offset=0&urlSearch=false&facets=%26fq%3Dcrawl_year%3A"${yearFromClick}"` 
      window.open(url, '_blank')
    }
}