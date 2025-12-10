import api from './axios'

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
      .then(response => {
        console.log('getPatients API返回结果:', response)
        // 处理后端返回的{code, message, data}格式
        let responseData = response;
        if (response && response.data) {
          responseData = response.data;
        }
        
        // 处理后端返回的{code, message, data: {data, total}}格式
        if (responseData && responseData.code === 200 && responseData.data) {
          responseData = responseData.data;
        }
        
        return responseData
      })
      .catch(error => {
        console.error('Failed to get patients:', error)
        return { data: [], total: 0 }
      })
  },

  // 获取病人详情
  getPatient(id) {
    return api.get(`/patients/${id}`)
  },

  // 创建病人
  createPatient(patient) {
    return api.post('/patients', patient)
  },

  // 更新病人
  updatePatient(id, patient) {
    return api.put(`/patients/${id}`, patient)
  },

  // 删除病人
  deletePatient(id) {
    return api.delete(`/patients/${id}`)
  },

  // 关联设备和病人
  assignDevice(patientId, deviceId, relationship = 'wearing') {
    return api.post('/patient-devices', { patientId, deviceId, relationship })
  },

  // 取消设备关联
  unassignDevice(patientId, deviceId) {
    return api.delete('/patient-devices', { params: { patientId, deviceId } })
  },

  // 根据设备ID获取病人
  getPatientByDeviceId(deviceId) {
    return api.get(`/patient-devices/by-device/${deviceId}`)
  },

  // 获取病人关联设备
  getPatientDevices(patientId) {
    return api.get(`/patient-devices/by-patient/${patientId}`)
  }
}
