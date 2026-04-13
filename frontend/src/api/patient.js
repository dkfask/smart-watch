import api from './axios'
import { handleApiResponse, handleApiError } from './utils'

export const patientApi = {
  // 获取病人列表
  getPatients(params = {}) {
    const defaultParams = { limit: 20, page: 0 }
    // 将offset转换为page（如果存在）
    if (params.offset) {
      params.page = Math.floor(params.offset / params.limit)
      delete params.offset
    }
    const combinedParams = { ...defaultParams, ...params }
    return api.get('/patients', { params: combinedParams })
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 获取病人详情
  getPatient(id) {
    return api.get(`/patients/${id}`)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 创建病人
  createPatient(patient) {
    return api.post('/patients', patient)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 更新病人
  updatePatient(id, patient) {
    return api.put(`/patients/${id}`, patient)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 删除病人
  deletePatient(id) {
    return api.delete(`/patients/${id}`)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 关联设备和病人
  assignDevice(patientId, deviceId, relationship = 'wearing') {
    return api.post('/patient-devices', { patientId, deviceId, relationship })
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 取消设备关联
  unassignDevice(patientId, deviceId) {
    return api.delete('/patient-devices', { params: { patientId, deviceId } })
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 根据设备ID获取病人
  getPatientByDeviceId(deviceId) {
    return api.get(`/patient-devices/by-device/${deviceId}`)
      .then(handleApiResponse)
      .catch(handleApiError)
  },

  // 获取病人关联设备
  getPatientDevices(patientId) {
    return api.get(`/patient-devices/by-patient/${patientId}`)
      .then(handleApiResponse)
      .catch(handleApiError)
  }
}
