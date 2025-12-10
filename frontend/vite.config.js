import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

// Vite 配置：主要配置了一个代理，将 /api 请求代理到后端 Spring Boot（本地默认 http://localhost:8080）
// 这样在开发模式下可以避免跨域与 CSRF 的跨域问题（后端需对 /api/** 放行 CSRF，项目 README 已说明）。
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src')
    }
  },
  server: {
    port: 5173,
    proxy: {
      // 所有 /api 的请求会被代理到后端应用，保留路径
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false
      }
    }
  }
})
