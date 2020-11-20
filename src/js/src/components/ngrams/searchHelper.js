import Configs from './netarchive/configs'
export default {
    handleSearch(queryFromClick, yearFromClick, searchType) {  
      let url
      searchType === 'tags' ?    
      url = `${Configs.BASE_SEARCH_URL()}?query=${encodeURIComponent('elements_used:"'+ queryFromClick +'"')}&fq=${encodeURIComponent()}&grouping=false&imgSearch=false&offset=0&urlSearch=false&facets=%26fq%3Dcrawl_year%3A"${yearFromClick}"%26fq%3Dcontent_type_norm%3A"html"` 
      :
      url = `${Configs.BASE_SEARCH_URL()}?query=${encodeURIComponent(queryFromClick)}&fq=${encodeURIComponent()}&grouping=false&imgSearch=false&offset=0&urlSearch=false&facets=%26fq%3Dcrawl_year%3A"${yearFromClick}"%26fq%3Dcontent_type_norm%3A"html"` 
      window.open(url, '_blank')
    }
}                                                                                                                                                                                    