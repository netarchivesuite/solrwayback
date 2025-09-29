import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import Axios from 'axios'
import VTooltip from 'v-tooltip'
import { setServerConfigInApp } from './configs/configHelper'

import './assets/styles/main.scss'

const baseURL = import.meta.env.DEV
  ? '/' 
  : import.meta.env.BASE_URL;

  if (import.meta.env.DEV) {
      Axios.defaults.baseURL = '/';
}

Axios.get(baseURL + 'services/frontend/properties/solrwaybackweb/')
    .then(response => {
        setServerConfigInApp(response.data)

        const pinia = createPinia()
        const app = createApp(App)

        app.use(pinia)
        app.use(router)
        app.use(VTooltip)

        app.mount('#app')
    })
    .catch(error => {
        // TODO - unsure what best to do here (Ben)
        console.error("Failed to load server config", error)
    })
  
