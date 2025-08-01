//import Vue, { createApp } from 'vue'
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router';
//import store from './store'
import Axios from 'axios'
import VTooltip from 'v-tooltip'
import { setServerConfigInApp } from './configs/configHelper'

import './assets/styles/main.scss'

// Vue.config.productionTip = false

Axios.get('services/frontend/properties/solrwaybackweb/')
    .then(response => {
        setServerConfigInApp(response.data)
    })
    .catch(error => {
        // TODO - unsure what best to do here (Ben)
        console.error("Failed to load server config", error)
    })


const pinia = createPinia()
const app = createApp(App);

app.use(pinia)
app.use(router)
app.use(VTooltip)

app.mount('#app')
  
