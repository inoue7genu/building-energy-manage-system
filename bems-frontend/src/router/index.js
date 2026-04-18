import { createRouter, createWebHistory } from 'vue-router'
import Dashboard from '../views/Dashboard.vue'
import DataQuery from '../views/DataQuery.vue'
import Login from '../views/Login.vue' // 🚀 引入登录页
import AdminHub from '../views/AdminHub.vue'

const routes = [
  { path: '/login', name: 'Login', component: Login },
  { path: '/', redirect: '/dashboard' },
  { path: '/admin-hub', name: 'AdminHub', component: AdminHub, meta: { requiresAuth: true } },
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

// 🚀 升级版路由守卫：实现智能分流与安全拦截
router.beforeEach((to, from) => {
  const token = localStorage.getItem('bems-token')
  const role = localStorage.getItem('bems-role') // 🚀 获取当前用户角色

  if (to.meta.requiresAuth && !token) {
    return '/login'
  } else if (to.path === '/login' && token) {
    // 🚀 根据角色决定已登录后的“家”在哪
    return role === 'ADMIN' ? '/admin-hub' : '/dashboard'
  }

  // 🚀 如果管理员访问根路径 /，也自动送往中枢
  if (to.path === '/' && token && role === 'ADMIN') {
    return '/admin-hub'
  }
})

export default router