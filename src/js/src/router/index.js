import Vue from 'vue'
import VueRouter from 'vue-router'
import SolrWayback from '../views/SolrWayback.vue'

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
  }
]

const router = new VueRouter({
  mode: 'history',
  base: process.env.BASE_URL,
  routes
})

export default router
