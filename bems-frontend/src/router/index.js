import { createRouter, createWebHistory } from 'vue-router'
import Dashboard from '../views/Dashboard.vue'
import DataQuery from '../views/DataQuery.vue'
import Login from '../views/Login.vue' // 🚀 引入登录页

const routes = [
  { path: '/login', name: 'Login', component: Login },
  { path: '/', redirect: '/dashboard' },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: Dashboard,
    meta: { requiresAuth: true } // 🚀 标记需要登录
  },
  {
    path: '/query',
    name: 'DataQuery',
    component: DataQuery,
    meta: { requiresAuth: true } // 🚀 标记需要登录
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 🚀 核心修复：使用 Vue Router 4 的最新 return 语法，彻底告别卡死！
router.beforeEach((to, from) => {
  const token = localStorage.getItem('bems-token')

  if (to.meta.requiresAuth && !token) {
    // 没登录，想进后台 -> 踹回登录页
    return '/login'
  } else if (to.path === '/login' && token) {
    // 登了录，还想回登录页 -> 送回大屏
    return '/dashboard'
  }
  // 其他情况自动放行，不需要写 return
})

export default router