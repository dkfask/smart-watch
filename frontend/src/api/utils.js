// API响应处理工具函数

/**
 * 处理后端返回的{code, message, data}格式响应
 * @param {Object} response - axios返回的响应对象
 * @returns {Object|Array} - 提取后的数据
 */
export const handleApiResponse = (response) => {
  console.log('API返回结果:', response)
  // 确保返回数组，处理不同的数据格式
  let responseData = response;
  if (response && response.data) {
    responseData = response.data;
  }
  
  // 处理后端返回的{code, message, data}格式
  if (responseData && responseData.code === 200 && responseData.data) {
    responseData = responseData.data;
  }
  
  // 检查请求URL，处理非数组类型的响应
  const url = response.config?.url || '';
  // 下行指令API返回的是非数组类型，直接返回响应数据
  if (url.includes('/api/downlink/')) {
    return responseData || {}
  }
  
  // 处理各种可能的数据格式
  if (Array.isArray(responseData)) {
    return responseData
  } else if (responseData && Array.isArray(responseData.list)) {
    return responseData.list
  } else if (responseData && Array.isArray(responseData.items)) {
    return responseData.items
  } else if (responseData && Array.isArray(responseData.content)) {
    return responseData.content
  } else if (responseData && Array.isArray(responseData.data)) {
    return responseData.data
  } else {
    console.warn('API返回的不是预期格式:', responseData)
    // 根据不同API返回默认值
    if (response.config && response.config.url) {
      if (response.config.url.includes('/api/fences')) {
        return []
      } else if (response.config.url.includes('/api/locations/device/')) {
        return []
      }
    }
    return []
  }
}

/**
 * 处理API请求错误
 * @param {Error} error - axios返回的错误对象
 * @param {string} defaultError - 默认错误信息
 * @returns {Object|Array} - 错误情况下的默认返回值
 */
export const handleApiError = (error, defaultError = '请求失败') => {
  console.error('API请求失败:', error)
  // 根据不同API返回不同的默认值
  if (error.config && error.config.url) {
    if (error.config.url.includes('/api/fences')) {
      return { list: [], total: 0 }
    } else if (error.config.url.includes('/api/devices')) {
      return { list: [], total: 0 }
    } else if (error.config.url.includes('/api/patients')) {
      return { data: [], total: 0 }
    }
  }
  return {}
}
