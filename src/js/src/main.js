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
        console.log(response)
        initializeVue()
    })
    .catch(error => initializeVue())

function initializeVue(){
new Vue({
  router,
  store,
  render: h => h(App)
}).$mount('#app')
}