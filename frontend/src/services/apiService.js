import api from '../api/axios'
import { ElMessage } from 'element-plus'
import cacheService from './cacheService'

/**
 * 统一API服务
 * 处理所有API调用，提供统一的错误处理和响应处理
 */
export class ApiService {
  /**
   * 发送GET请求
   * @param {string} url - 请求URL
   * @param {Object} params - 请求参数
   * @param {Object} options - 额外选项
   * @returns {Promise} - 返回Promise
   */
  static async get(url, params = {}, options = {}) {
    // 生成缓存键
    const cacheKey = `${url}_${JSON.stringify(params)}`
    
    // 检查缓存
    if (options.cache !== false) {
      const cachedData = cacheService.get(cacheKey)
      if (cachedData) {
        console.log('从缓存获取数据:', cacheKey)
        return cachedData
      }
    }
    
    try {
      const response = await api.get(url, { params, ...options })
      const data = this.handleResponse(response)
      
      // 缓存数据
      if (options.cache !== false) {
        const expiry = options.cacheExpiry || 5 * 60 * 1000 // 默认5分钟
        cacheService.set(cacheKey, data, expiry)
        console.log('缓存数据:', cacheKey)
      }
      
      return data
    } catch (error) {
      return this.handleError(error, options)
    }
  }

  /**
   * 发送POST请求
   * @param {string} url - 请求URL
   * @param {Object} data - 请求数据
   * @param {Object} options - 额外选项
   * @returns {Promise} - 返回Promise
   */
  static async post(url, data = {}, options = {}) {
    try {
      const response = await api.post(url, data, options)
      const result = this.handleResponse(response)
      
      // 清除相关缓存
      this.clearRelatedCache(url)
      
      return result
    } catch (error) {
      return this.handleError(error, options)
    }
  }

  /**
   * 发送PUT请求
   * @param {string} url - 请求URL
   * @param {Object} data - 请求数据
   * @param {Object} options - 额外选项
   * @returns {Promise} - 返回Promise
   */
  static async put(url, data = {}, options = {}) {
    try {
      const response = await api.put(url, data, options)
      const result = this.handleResponse(response)
      
      // 清除相关缓存
      this.clearRelatedCache(url)
      
      return result
    } catch (error) {
      return this.handleError(error, options)
    }
  }

  /**
   * 发送DELETE请求
   * @param {string} url - 请求URL
   * @param {Object} options - 额外选项
   * @returns {Promise} - 返回Promise
   */
  static async delete(url, options = {}) {
    try {
      const response = await api.delete(url, options)
      const result = this.handleResponse(response)
      
      // 清除相关缓存
      this.clearRelatedCache(url)
      
      return result
    } catch (error) {
      return this.handleError(error, options)
    }
  }

  /**
   * 清除相关缓存
   * @param {string} url - 请求URL
   */
  static clearRelatedCache(url) {
    // 根据URL清除相关缓存
    const keys = cacheService.keys()
    for (const key of keys) {
      if (key.startsWith(url.split('/').slice(0, 3).join('/'))) {
        cacheService.delete(key)
        console.log('清除缓存:', key)
      }
    }
  }

  /**
   * 处理API响应
   * @param {Object} response - API响应
   * @returns {Object|Array} - 处理后的数据
   */
  static handleResponse(response) {
    console.log('API响应:', response)
    let responseData = response

    // 处理嵌套的响应结构
    if (response && response.data) {
      responseData = response.data
    }

    // 处理后端返回的{code, message, data}格式
    if (responseData && responseData.code === 200 && responseData.data) {
      responseData = responseData.data
    }

    // 处理不同的数据格式
    if (Array.isArray(responseData)) {
      return responseData
    } else if (responseData && Array.isArray(responseData.list)) {
      return responseData
    } else if (responseData && Array.isArray(responseData.items)) {
      return responseData
    } else if (responseData && Array.isArray(responseData.content)) {
      return responseData
    } else if (responseData && Array.isArray(responseData.data)) {
      return responseData
    } else {
      return responseData
    }
  }

  /**
   * 处理API错误
   * @param {Error} error - 错误对象
   * @param {Object} options - 选项
   * @returns {Object|Array} - 错误情况下的默认返回值
   */
  static handleError(error, options = {}) {
    console.error('API请求失败:', error)

    // 显示错误消息
    const errorMessage = error.response?.data?.message || error.message || '请求失败'
    if (!options.silent) {
      ElMessage.error(errorMessage)
    }

    // 根据不同API返回不同的默认值
    const url = error.config?.url || ''
    
    if (url.includes('/api/fences')) {
      return { list: [], total: 0 }
    } else if (url.includes('/api/devices')) {
      return { list: [], total: 0 }
    } else if (url.includes('/api/patients')) {
      return { data: [], total: 0 }
    } else if (url.includes('/api/alarms')) {
      return { content: [], totalElements: 0 }
    } else if (url.includes('/api/locations')) {
      return {}
    } else {
      return {}
    }
  }
}

// 导出设备相关API
export const deviceApi = {
  // 获取设备列表
  getDevices(limit = 20, offset = 0) {
    return ApiService.get('/devices', { limit, offset })
  },
  
  // 获取可用设备列表（未关联病人的设备）
  getAvailableDevices() {
    return ApiService.get('/devices/available')
  },

  // 获取设备详情
  getDevice(id) {
    return ApiService.get(`/devices/${id}`)
  },

  // 根据IMEI获取设备
  getDeviceByImei(imei) {
    return ApiService.get(`/devices/by-imei/${imei}`)
  },

  // 创建设备
  createDevice(device) {
    return ApiService.post('/devices', device)
  },

  // 更新设备
  updateDevice(id, device) {
    return ApiService.put(`/devices/${id}`, device)
  },

  // 删除设备
  deleteDevice(id) {
    return ApiService.delete(`/devices/${id}`)
  }
}

// 导出病人相关API
export const patientApi = {
  // 获取病人列表
  getPatients(limit = 20, offset = 0) {
    return ApiService.get('/patients', { limit, offset })
  },

  // 获取病人详情
  getPatient(id) {
    return ApiService.get(`/patients/${id}`)
  },

  // 创建病人
  createPatient(patient) {
    return ApiService.post('/patients', patient)
  },

  // 更新病人
  updatePatient(id, patient) {
    return ApiService.put(`/patients/${id}`, patient)
  },

  // 删除病人
  deletePatient(id) {
    return ApiService.delete(`/patients/${id}`)
  }
}

// 导出围栏相关API
export const fenceApi = {
  // 获取围栏列表
  getFences(limit = 20, offset = 0) {
    return ApiService.get('/fences', { limit, offset })
  },

  // 获取围栏详情
  getFence(id) {
    return ApiService.get(`/fences/${id}`)
  },

  // 创建围栏
  createFence(fence) {
    return ApiService.post('/fences', fence)
  },

  // 更新围栏
  updateFence(id, fence) {
    return ApiService.put(`/fences/${id}`, fence)
  },

  // 删除围栏
  deleteFence(id) {
    return ApiService.delete(`/fences/${id}`)
  }
}

// 导出报警相关API
export const alarmApi = {
  // 获取报警列表
  getAlarms(params = {}) {
    return ApiService.get('/alarms', params)
  },

  // 获取报警详情
  getAlarm(id) {
    return ApiService.get(`/alarms/${id}`)
  },

  // 处理报警
  handleAlarm(id, data) {
    return ApiService.put(`/alarms/${id}/handle`, data)
  },

  // 标记报警为已读
  markAlarmAsRead(id) {
    return ApiService.put(`/alarms/${id}/read`)
  }
}

// 导出位置相关API
export const locationApi = {
  // 获取设备最新位置
  getLatestLocation(deviceId) {
    return ApiService.get(`/locations/device/${deviceId}/latest`)
  },

  // 获取设备位置历史
  getLocationHistory(deviceId, params = {}) {
    return ApiService.get(`/locations/device/${deviceId}/history`, params)
  }
}

// 导出下行指令相关API
export const downlinkApi = {
  // 发送下行指令
  sendCommand(deviceId, command) {
    return ApiService.post(`/downlink/send`, { deviceId, command })
  },

  // 获取下行指令历史
  getCommandHistory(deviceId, params = {}) {
    return ApiService.get(`/downlink/history/${deviceId}`, params)
  }
}

export default ApiService