import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import './assets/styles.css'
import { setTiandituKey } from './utils/tianditu'

// 启动时尝试从后端获取地图配置
fetch('/api/config/map')
  .then(res => res.json())
  .then(data => {
    const payload = data.data || data
    if (payload.tiandituKey) {
      setTiandituKey(payload.tiandituKey)
    }
  })
  .catch(() => {})

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)
app.use(ElementPlus)

app.mount('#app')
