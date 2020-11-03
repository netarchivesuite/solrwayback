export default {
  SERVICE_URL : 'services/search/',
  START_YEAR : '1998',
  END_YEAR: '2021',
  BASE_SEARCH_URL: () => {
      let currentUrl = window.location
      let baseUrl = `${currentUrl.protocol}//${currentUrl.host}/${currentUrl.pathname.split('/')[1]}`
      let searchPrefix = currentUrl.pathname.split('/')[1] === 'search' ? '' : '/search'
      return `${baseUrl}${searchPrefix}`
  }
}