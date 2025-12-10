import api from './axios'
import { handleApiResponse, handleApiError } from './utils'

export const fenceApi = {
  // 获取围栏列表
  getFences(limit = 20, offset = 0) {
    return api.get('/fences', { params: { limit, offset } })
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 获取围栏详情
  getFence(id) {
    return api.get(`/fences/${id}`)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 创建围栏
  createFence(fence) {
    return api.post('/fences', fence)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 更新围栏
  updateFence(id, fence) {
    return api.put(`/fences/${id}`, fence)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 删除围栏
  deleteFence(id) {
    return api.delete(`/fences/${id}`)
      .then(handleApiResponse)
      .catch(handleApiError)
  }
}