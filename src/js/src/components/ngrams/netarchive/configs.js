import APP_CONFIGS from '../../../configs'
export default {
  SERVICE_URL : 'services/search/',
  END_YEAR: new Date().getFullYear().toString(),
  BASE_SEARCH_URL: () => {
      let searchPrefix = window.location.pathname.split('/')[1] === 'search' ? '' : 'search'
      return `${APP_CONFIGS.playbackConfig.solrwaybackBaseURL}${searchPrefix}`
  }
}