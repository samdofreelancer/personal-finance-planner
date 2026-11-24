import { createRouter, createWebHistory } from 'vue-router'
import PlannerView from './PlannerView.vue'

const routes = [
  { path: '/', redirect: '/dashboard' },
  { path: '/dashboard', component: PlannerView, meta: { section: 'overview' } },
  { path: '/plan/assumptions', component: PlannerView, meta: { section: 'assumptions' } },
  { path: '/loans', component: PlannerView, meta: { section: 'loans' } },
  { path: '/analytics/annual', component: PlannerView, meta: { section: 'annual-report' } },
  { path: '/forecast/monthly', component: PlannerView, meta: { section: 'monthly-forecast' } },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' },
]

export default createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})
