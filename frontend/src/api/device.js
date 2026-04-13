import api from './axios'
import { handleApiResponse, handleApiError } from './utils'

export const deviceApi = {
  // 获取设备列表
  getDevices(limit = 20, offset = 0) {
    return api.get('/devices', { params: { limit, offset } })
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 获取可用设备列表（未关联病人的设备）
  getAvailableDevices() {
    return api.get('/devices/available')
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 获取设备详情
  getDevice(id) {
    return api.get(`/devices/${id}`)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 根据IMEI获取设备
  getDeviceByImei(imei) {
    return api.get(`/devices/by-imei/${imei}`)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 创建设备
  createDevice(device) {
    return api.post('/devices', device)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 更新设备
  updateDevice(id, device) {
    return api.put(`/devices/${id}`, device)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 删除设备
  deleteDevice(id) {
    return api.delete(`/devices/${id}`)
      .then(handleApiResponse)
      .catch(handleApiError)
  }
}
