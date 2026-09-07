import api from './axios'
import { handleApiResponse, handleApiError } from './utils'

export const locationApi = {
  // 获取设备最近位置
  getRecentLocations(deviceId, limit = 50, offset = 0) {
    return api.get(`/locations/device/${deviceId}`, { params: { limit, offset } })
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 获取设备最新位置
  getLatestLocation(deviceId) {
    return api.get(`/locations/device/${deviceId}/latest`)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 获取设备历史位置范围
  getLocationsByRange(deviceId, start, end, limit = 200, offset = 0) {
    return api.get(`/locations/device/${deviceId}/range`, {
      params: {
        start,
        end,
        limit,
        offset
      }
    })
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 获取设备最新位置（带详细地址）
  getLatestLocationWithAddress(deviceId) {
    return api.get(`/locations/device/${deviceId}/latest-with-address`)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 上报位置
  reportLocation(location) {
    return api.post('/locations/report', location)
      .then(handleApiResponse)
      .catch(handleApiError)
  }
}
