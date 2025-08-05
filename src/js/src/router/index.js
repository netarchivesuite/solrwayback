import { createRouter, createWebHistory } from 'vue-router';
import SolrWayback from '../views/SolrWayback.vue'

export const routes = {
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
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
      component: () => import(/* webpackChunkName: "about" */ '@/views/About.vue')
    },
    {
      path: '/calendar',
      name: 'HarvestCalendar',
      meta: {
        title: 'Harvest Calendar'
      },
      component: () => import('@/views/HarvestCalendar.vue')
    },
    {
      path: '/pageharvestdata',
      name: 'PageHavestData',
      meta: {
        title: 'Page Harvest Data'
      },
      component: () => import(/* webpackChunkName: "page-harvest-data" */ '@/views/PageHarvestData.vue')
    },
    {
      path: '/pwid',
      name: 'PWID',
      meta: {
        title: 'PWID'
      },
      component: () => import(/* webpackChunkName: "PWID" */ '@/views/PWID.vue')
    },
    {
      path: '/linkgraph',
      name: 'Linkgraph',
      meta: {
        title: 'Link graph'
      },
      component: () => import(/* webpackChunkName: "Linkgraph" */ '@/components/ToolboxComponents/LinkGraph.vue')
    }
  ]
};

const router = createRouter(routes);

export default router;