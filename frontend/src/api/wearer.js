import api from './axios'
import { handleApiResponse, handleApiError } from './utils'

export const wearerApi = {
  // 获取佩戴者列表
  getWearers(limit = 20, offset = 0) {
    return api.get('/wearers', { params: { limit, offset } })
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 获取佩戴者详情
  getWearer(id) {
    return api.get(`/wearers/${id}`)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 创建佩戴者
  createWearer(wearer) {
    return api.post('/wearers', wearer)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 更新佩戴者
  updateWearer(id, wearer) {
    return api.put(`/wearers/${id}`, wearer)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 删除佩戴者
  deleteWearer(id) {
    return api.delete(`/wearers/${id}`)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 关联设备和佩戴者
  assignDevice(wearerId, deviceId) {
    return api.post(`/wearers/${wearerId}/assign-device`, { deviceId })
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 取消设备关联
  unassignDevice(wearerId) {
    return api.post(`/wearers/${wearerId}/unassign-device`)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 根据设备ID获取佩戴者
  getWearerByDeviceId(deviceId) {
    return api.get(`/wearers/by-device/${deviceId}`)
      .then(handleApiResponse)
      .catch(handleApiError)
  }
}