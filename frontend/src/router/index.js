import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    name: 'Dashboard',
    component: () => import('../views/Dashboard.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/alarm',
    redirect: '/alarms'
  },
  {
    path: '/devices',
    name: 'Devices',
    component: () => import('../views/Devices.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/devices/:id',
    name: 'DeviceDetail',
    component: () => import('../views/DeviceDetail.vue'),
    meta: { requiresAuth: true }
  },
  {    path: '/wearers',    redirect: '/patients'  },  {    path: '/patients',    name: 'Patients',    component: () => import('../views/Patient.vue'),    meta: { requiresAuth: true }  },
  {
    path: '/patients/:id',
    name: 'PatientDetail',
    component: () => import('../views/PatientDetail.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/fences',
    name: 'Fences',
    component: () => import('../views/Fence.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/alarms',
    name: 'Alarms',
    component: () => import('../views/Alarm.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/realtime',
    name: 'Realtime',
    component: () => import('../views/Realtime.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/history/:deviceId',
    name: 'History',
    component: () => import('../views/History.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/ai',
    name: 'AiAssistant',
    component: () => import('../views/AiAssistant.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
let isAuthChecked = false

router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()
  const requiresAuth = to.meta.requiresAuth
  
  // ?????????????????????? 401 ???????
  if (requiresAuth && !isAuthChecked) {
    isAuthChecked = true
    const isValid = await authStore.checkAuth()
    if (!isValid) {
      next('/login')
      return
    }
  }
  
  const isAuthenticated = authStore.isAuthenticated

  if (requiresAuth && !isAuthenticated) {
    next('/login')
  } else {
    next()
  }
})

export default router
