import api from './axios'
import { handleApiResponse, handleApiError } from './utils'

export const alarmApi = {
  // 获取报警列表
  getAlarms(limit = 20, offset = 0, status = null) {
    const params = { page: Math.floor(offset / limit), size: limit }
    if (status !== null) {
      params.status = status
    }
    return api.get('/alarms', { params })
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 获取报警详情
  getAlarm(id) {
    return api.get(`/alarms/${id}`)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 处理报警
  handleAlarm(id, status) {
    return api.put(`/alarms/${id}/handle`, { status })
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 获取设备相关的报警
  getAlarmsByDeviceId(deviceId, limit = 20, offset = 0) {
    return api.get(`/alarms/device/${deviceId}`, { params: { page: Math.floor(offset / limit), size: limit } })
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 获取病人相关的报警
  getAlarmsByPatientId(patientId, limit = 20, offset = 0) {
    return api.get(`/alarms/patient/${patientId}`, { params: { page: Math.floor(offset / limit), size: limit } })
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 获取统计数据
  getAlarmStats() {
    return api.get('/alarms/stats')
      .then(handleApiResponse)
      .catch(handleApiError)
  }
}