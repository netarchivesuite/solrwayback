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
      path: '/calendar',
      name: 'HarvestCalendar',
      meta: {
        title: 'Harvest Calendar'
      },
      component: () => import('@/views/HarvestCalendar.vue')
    }
  ]
};

const router = createRouter(routes);

export default router;