import Vue from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import Axios from 'axios'
import configs from './configs'

import './assets/styles/main.scss'

Vue.config.productionTip = false

Axios.get('services/frontend/properties/solrwaybackweb/')
    .then(response => {
        configs.playbackConfig.openwaybackBaseURL = response.data['openwayback.baseurl']
        configs.playbackConfig.solrwaybackBaseURL = response.data['wayback.baseurl']
        configs.exportOptions.warcAllowed = response.data['allow.export.warc']
        configs.exportOptions.csvAllowed = response.data['allow.export.csv']
        configs.exportOptions.csvFields = response.data['export.csv.fields']
        configs.leaflet.attribution = response.data['leaflet.attribution']
        configs.leaflet.source = response.data['leaflet.source']
        configs.leaflet.map.latitude = response.data['maps.latitude']
        configs.leaflet.map.longitude = response.data['maps.longitude']
        configs.leaflet.map.radius = response.data['maps.radius']
        configs.visualizations.ngram.startYear = response.data['archive.start.year']
        configs.logo.url = response.data['top.left.logo.image']
        configs.logo.link = response.data['top.left.logo.image.link']  
        router.options.base = configs.playbackConfig.solrwaybackBaseURL
        initializeVue()
    })
    .catch(error => initializeVue())

function initializeVue(){
  console.log('initialize router', router)
  console.log('initialize configs', configs)
  console.log('initialize configs.playbackConfig.solrwaybackBaseURL', configs.playbackConfig.solrwaybackBaseURL)
  console.log('initialize current base', router.options.base)
  console.log('initialize process env base', process.env.BASE_URL)
new Vue({
  router,
  store,
  render: h => h(App)
}).$mount('#app')
console.log('initialize app mounted router', router)
  console.log('initialize app mounted configs', configs)
  console.log('initialize app mounted configs.playbackConfig.solrwaybackBaseURL', configs.playbackConfig.solrwaybackBaseURL)
  console.log('initialize app mounted current base', router.options.base)
  console.log('initialize app mounted process env base', process.env.BASE_URL)
}