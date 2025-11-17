import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import Home from '../views/Home.vue'
import Menu from '../views/Menu.vue'
import Dashboard from '../views/Dashboard.vue'
import Devices from '../views/Devices.vue'
import DeviceDetail from '../views/DeviceDetail.vue'
import Realtime from '../views/Realtime.vue'
import Settings from '../views/Settings.vue'

// 路由说明：
// /login - 登录页面（与后端表单登录配合）
// /register - 注册页面
// /dashboard - 系统仪表盘（设备统计、最近日志等）
// /devices - 设备列表
// /devices/:id - 设备详情
// /realtime - 实时监控（WebSocket/SSE 可集成）
// /settings - 系统设置

// 路由：/login /register /home /menu
const routes = [
  { path: '/', redirect: '/dashboard' },
  { path: '/login', component: Login },
  { path: '/register', component: Register },
  { path: '/menu', component: Menu },
  { path: '/home', component: Home },
  { path: '/dashboard', component: Dashboard },
  { path: '/devices', component: Devices },
  { path: '/devices/:id', component: DeviceDetail },
  { path: '/realtime', component: Realtime },
  { path: '/settings', component: Settings }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
