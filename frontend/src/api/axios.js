import axios from 'axios'

// 创建axios实例
const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器
api.interceptors.request.use(
  config => {
    // 可以在这里添加认证token
    return config
  },
  error => {
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
      // 清除本地存储的认证信息
      localStorage.removeItem('user')
      // 跳转到登录页面
      window.location.href = '/login'
    }
    
    // 处理其他错误
    if (error.response) {
      // 服务器返回了错误状态码
      const status = error.response.status
      const data = error.response.data
      const message = data.message || data.error || `服务器错误 (${status})`
      
      // 根据不同状态码显示不同的错误信息
      switch (status) {
        case 400:
          console.error('请求参数错误:', data)
          break
        case 403:
          console.error('权限不足:', data)
          break
        case 404:
          console.error('资源不存在:', data)
          break
        case 500:
          console.error('服务器内部错误:', data)
          break
        default:
          console.error(`未知错误 (${status}):`, data)
      }
      
      // 将错误信息传递给调用者
      error.message = message
    } else if (error.request) {
      // 请求已发送但没有收到响应
      console.error('网络错误，服务器未响应:', error.request)
      error.message = '网络错误，服务器未响应，请检查网络连接'
    } else {
      // 请求配置有误
      console.error('请求配置错误:', error.message)
      error.message = '请求配置错误'
    }
    
    return Promise.reject(error)
  }
)

export default api
