import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import Axios from 'axios'
import VTooltip from 'v-tooltip'
import { setServerConfigInApp } from './configs/configHelper'

import './assets/styles/main.scss'

Axios.get(import.meta.env.BASE_URL + 'services/frontend/properties/solrwaybackweb/')
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
  
