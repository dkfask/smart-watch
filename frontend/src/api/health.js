import api from './axios'
import { handleApiResponse, handleApiError } from './utils'

export const healthApi = {
  // 获取健康记录列表
  getHealthRecords(patientId, limit = 20, offset = 0) {
    return api.get('/health-records', { params: { patientId, limit, offset } })
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 获取最新健康记录
  getLatestHealthRecords(patientId, dataType = null) {
    const params = { patientId }
    if (dataType) {
      params.dataType = dataType
    }
    return api.get('/health-records/latest', { params })
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 获取特定设备的健康记录
  getHealthRecordsByDeviceId(deviceId, limit = 20, offset = 0) {
    return api.get(`/health-records/device/${deviceId}`, { params: { limit, offset } })
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 获取健康数据统计
  getHealthStats(patientId, startDate, endDate) {
    return api.get('/health-records/stats', { params: { patientId, startDate, endDate } })
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 获取健康告警阈值配置
  getHealthAlertThresholds() {
    return api.get('/health-alert-thresholds')
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 更新健康告警阈值配置
  updateHealthAlertThresholds(thresholds) {
    return api.put('/health-alert-thresholds', thresholds)
      .then(handleApiResponse)
      .catch(handleApiError)
  }
}