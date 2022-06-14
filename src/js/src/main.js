import Vue from 'vue'
import App from './App.vue'
import store from './store'
import Axios from 'axios'
import configs from './configs'
import VueRouter from 'vue-router'
import SolrWayback from './views/SolrWayback.vue'



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
        
        initializeVue('/test/solrwayback')
    })
    .catch(error => initializeVue())

function initializeVue(base){
  console.log('initialize router')
  console.log('initialize configs', configs)
  console.log('initialize configs.playbackConfig.solrwaybackBaseURL', configs.playbackConfig.solrwaybackBaseURL)
  console.log('initialize current base')
  console.log('initialize process env base', process.env.BASE_URL)
   
  const routes = [
    {
      path: '/',
      name: 'SolrWayback',
      component: SolrWayback,
    },
    {
      path: '/search/:query?',
      name: 'Search',
      component: SolrWayback,
    },
    {
      path: '/about',
      name: 'About',
      meta: {
        title: 'About SolrWayback'
      },
      // route level code-splitting
      // this generates a separate chunk (about.[hash].js) for this route
      // which is lazy-loaded when the route is visited.
      component: () =>
        import(/* webpackChunkName: "about" */ './views/About.vue')
    },
    {
      path: '/calendar',
      name: 'HarvestCalendar',
      meta: {
        title: 'Harvest Calendar'
      },
      component: () =>
        import(/* webpackChunkName: "harvest-calendar" */ './views/HarvestCalendar.vue')
    },
    {
      path: '/pageharvestdata',
      name: 'PageHavestData',
      meta: {
        title: 'Page Harvest Data'
      },
      component: () =>
        import(/* webpackChunkName: "page-harvest-data" */ './views/PageHarvestData.vue')
    },
    {
      path: '/pwid',
      name: 'PWID',
      meta: {
        title: 'PWID'
      },
      component: () =>
        import(/* webpackChunkName: "PWID" */ './views/PWID.vue')
    },
    {
      path: '/linkgraph',
      name: 'Linkgraph',
      meta: {
        title: 'Link graph'
      },
      // route level code-splitting
      // this generates a separate chunk (about.[hash].js) for this route
      // which is lazy-loaded when the route is visited.
      component: () =>
        import(/* webpackChunkName: "Linkgraph" */ './components/ToolboxComponents/LinkGraph.vue')
    },
  ]
  
  const router = new VueRouter({
    mode: 'history',
    base:base,
    routes
  })
  console.log('router router', router)
  console.log('router configs', configs)
  console.log('router configs.playbackConfig.solrwaybackBaseURL', configs.playbackConfig.solrwaybackBaseURL)
  console.log('router current base', router.options.base)
  console.log('router process env base', process.env.BASE_URL)
  
  Vue.use(VueRouter)
  
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