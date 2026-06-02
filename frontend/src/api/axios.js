import axios from 'axios'
import { ElMessage } from 'element-plus'

// 创建axios实例
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 10000,
  withCredentials: true
})

// 请求拦截器
api.interceptors.request.use(
  config => {
    // 可以在这里添加认证token
    return config
  },
  error => {
    ElMessage.error('请求发送失败，请检查网络连接')
    return Promise.reject(error)
  }
)

// 响应拦截器
api.interceptors.response.use(
  response => {
    return response
  },
  error => {
    // 处理401未授权错误
    if (error.response && error.response.status === 401) {
      const requestUrl = error.config?.url || ''
      const isAuthProbe = requestUrl.includes('/auth/me')
      const isLoginRequest = requestUrl.includes('/auth/login')
      const isAlreadyOnLogin = window.location.pathname === '/login'

      if (isAuthProbe || isLoginRequest || isAlreadyOnLogin) {
        return Promise.reject(error)
      }

      // 清除本地存储的认证信息
      localStorage.removeItem('user')
      // 跳转到登录页面
      window.location.href = '/login'
      ElMessage.error('登录已过期，请重新登录')
      return Promise.reject(error)
    }
    
    // 处理其他错误
    if (error.response) {
      // 服务器返回了错误状态码
      const status = error.response.status
      const data = error.response.data
      let message = data.message || data.error || `服务器错误 (${status})`
      
      // 处理ApiResponse格式的错误
      if (data.code && data.message) {
        message = data.message
      }
      
      // 根据不同状态码显示不同的错误信息
      switch (status) {
        case 400:
          console.error('请求参数错误:', data)
          ElMessage.error(`请求参数错误: ${message}`)
          break
        case 403:
          console.error('权限不足:', data)
          ElMessage.error(`权限不足: ${message}`)
          break
        case 404:
          console.error('资源不存在:', data)
          ElMessage.error(`资源不存在: ${message}`)
          break
        case 500:
          console.error('服务器内部错误:', data)
          ElMessage.error(`服务器错误: ${message}`)
          break
        default:
          console.error(`未知错误 (${status}):`, data)
          ElMessage.error(`未知错误: ${message}`)
      }
      
      // 将错误信息传递给调用者
      error.message = message
    } else if (error.request) {
      // 请求已发送但没有收到响应
      console.error('网络错误，服务器未响应:', error.request)
      error.message = '网络错误，服务器未响应，请检查网络连接'
      ElMessage.error(error.message)
    } else {
      // 请求配置有误
      console.error('请求配置错误:', error.message)
      error.message = '请求配置错误'
      ElMessage.error(error.message)
    }
    
    return Promise.reject(error)
  }
)

export default api
