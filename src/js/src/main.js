import Vue from 'vue'
import App from './App.vue'
import VueRouter from 'vue-router'
import { routes } from './router/routes'
import store from './store'
import Axios from 'axios'
import { setServerConfigInApp } from './configs/configHelper'

import './assets/styles/main.scss'

Vue.config.productionTip = false

Axios.get('services/frontend/properties/solrwaybackweb/')
    .then(response => {
        setServerConfigInApp(response.data)
        initializeVue(response.data['webapp.baseurl'])
    })
    .catch(error => initializeVue('/'))

function initializeVue(appBaseURL){
  const router = new VueRouter({
    mode: 'history',
    base: appBaseURL,
    routes
  })
 
  Vue.use(VueRouter)

  new Vue({
    router,
    store,
    render: h => h(App)
  }).$mount('#app')
  
}