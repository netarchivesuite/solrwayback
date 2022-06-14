import Vue from 'vue'
import VueRouter from 'vue-router'
import SolrWayback from '../views/SolrWayback.vue'
import configs from '../configs/'

Vue.use(VueRouter)

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
      import(/* webpackChunkName: "about" */ '../views/About.vue')
  },
  {
    path: '/calendar',
    name: 'HarvestCalendar',
    meta: {
      title: 'Harvest Calendar'
    },
    component: () =>
      import(/* webpackChunkName: "harvest-calendar" */ '../views/HarvestCalendar.vue')
  },
  {
    path: '/pageharvestdata',
    name: 'PageHavestData',
    meta: {
      title: 'Page Harvest Data'
    },
    component: () =>
      import(/* webpackChunkName: "page-harvest-data" */ '../views/PageHarvestData.vue')
  },
  {
    path: '/pwid',
    name: 'PWID',
    meta: {
      title: 'PWID'
    },
    component: () =>
      import(/* webpackChunkName: "PWID" */ '../views/PWID.vue')
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
      import(/* webpackChunkName: "Linkgraph" */ '../components/ToolboxComponents/LinkGraph.vue')
  },
]

const router = new VueRouter({
  mode: 'history',
  routes
})
router.options.base = router.options.base=== '' ? configs.playbackConfig.solrwaybackBaseURL : process.env.BASE_URL 
console.log('router router', router)
console.log('router configs', configs)
console.log('router configs.playbackConfig.solrwaybackBaseURL', configs.playbackConfig.solrwaybackBaseURL)
console.log('router current base', router.options.base)
console.log('router process env base', process.env.BASE_URL)

export default router
