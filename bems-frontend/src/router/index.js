import { createRouter, createWebHistory } from 'vue-router'
import Dashboard from '../views/Dashboard.vue'
import DataQuery from '../views/DataQuery.vue'


const routes = [
  { path: '/', redirect: '/dashboard' }, // 默认打开直接跳转到大屏
  { path: '/dashboard', name: 'Dashboard', component: Dashboard },
  { path: '/query', name: 'DataQuery', component: DataQuery },

]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router