import api from './axios'

export const deviceApi = {
  // 获取设备列表
  getDevices(limit = 20, offset = 0) {
    return api.get('/devices', { params: { limit, offset } })
      .then(response => {
        console.log('getDevices API返回结果:', response)
        // 确保返回数组，处理不同的数据格式
        let responseData = response;
        if (response && response.data) {
          responseData = response.data;
        }
        
        // 处理后端返回的{code, message, data: {list, total}}格式
        if (responseData && responseData.code === 200 && responseData.data) {
          responseData = responseData.data;
        }
        
        if (Array.isArray(responseData)) {
          return responseData
        } else if (responseData && Array.isArray(responseData.list)) {
          return responseData.list
        } else if (responseData && Array.isArray(responseData.items)) {
          return responseData.items
        } else if (responseData && Array.isArray(responseData.content)) {
          return responseData.content
        } else {
          console.warn('getDevices API返回的不是预期格式:', responseData)
          return []
        }
      })
      .catch(error => {
        console.error('Failed to get devices:', error)
        return []
      })
  },
  
  // 获取可用设备列表（未关联病人的设备）
  getAvailableDevices() {
    return api.get('/devices/available')
      .then(response => {
        console.log('getAvailableDevices API返回结果:', response)
        // 确保返回数组
        if (Array.isArray(response)) {
          return response
        } else if (response && Array.isArray(response.data)) {
          return response.data
        } else {
          console.warn('getAvailableDevices API返回的不是数组:', response)
          return []
        }
      })
      .catch(error => {
        console.error('Failed to get available devices:', error)
        return []
      })
  },

  // 获取设备详情
  getDevice(id) {
    return api.get(`/devices/${id}`)
      .catch(error => {
        console.error(`Failed to get device ${id}:`, error)
        throw error
      })
  },

  // 根据IMEI获取设备
  getDeviceByImei(imei) {
    return api.get(`/devices/by-imei/${imei}`)
      .catch(error => {
        console.error(`Failed to get device by imei ${imei}:`, error)
        throw error
      })
  },

  // 创建设备
  createDevice(device) {
    return api.post('/devices', device)
      .catch(error => {
        console.error('Failed to create device:', error)
        throw error
      })
  },

  // 更新设备
  updateDevice(id, device) {
    return api.put(`/devices/${id}`, device)
      .catch(error => {
        console.error(`Failed to update device ${id}:`, error)
        throw error
      })
  },

  // 删除设备
  deleteDevice(id) {
    return api.delete(`/devices/${id}`)
      .catch(error => {
        console.error(`Failed to delete device ${id}:`, error)
        throw error
      })
  }
}
