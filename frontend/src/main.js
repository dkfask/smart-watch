// 应用入口：创建 Vue 应用并挂载路由
import { createApp } from 'vue'
import App from './App.vue'
import router from './router'

// 引入全局样式（用于登录/注册表单的基础样式）
import './assets/styles.css'

// Leaflet styles
import 'leaflet/dist/leaflet.css'
import 'leaflet-draw/dist/leaflet.draw.css'

const app = createApp(App)
app.use(router)
app.mount('#app')
