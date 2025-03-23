import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'

const routes = [
  {
    path: '/',
    redirect: '/home'
  },
  {
    path: '/home',
    name: 'home',
    component: HomeView,
    meta: { requiresAuth: true }
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue'),
    meta: { guest: true }
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('../views/RegisterView.vue'),
    meta: { guest: true }
  },
  {
    path: '/about',
    name: 'about',
    component: () => import('../views/AboutView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/news/:id',
    name: 'newsDetail',
    component: () => import('../views/NewsDetailView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'profile',
    component: () => import('../views/UserProfileView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/data',
    name: 'dataVisualization',
    component: () => import('../views/DataVisualizationView.vue'),
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  
  // 如果token无效或格式错误，清除token
  if (token && (!token.startsWith('Bearer ') && !token.includes('.'))) {
    localStorage.removeItem('token')
  }
  
  // 需要登录的页面，但没有token
  if (to.matched.some(record => record.meta.requiresAuth) && !token) {
    next('/login')
  }
  // 游客页面（如登录、注册），但已经有token
  else if (to.matched.some(record => record.meta.guest) && token) {
    next('/home')
  } else {
    next()
  }
})

export default router
