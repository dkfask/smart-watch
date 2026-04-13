/**
 * API响应处理工具函数
 */

/**
 * 处理后端返回的{code, message, data}格式响应
 * 统一解包ApiResponse外层，保留分页元数据
 * @param {Object} response - axios返回的响应对象
 * @returns {Object|Array} - 提取后的数据（分页响应返回完整对象含content/total等）
 */
export const handleApiResponse = (response) => {
  let responseData = response;
  if (response && response.data) {
    responseData = response.data;
  }

  if (responseData && responseData.code === 200 && responseData.data) {
    responseData = responseData.data;
  }

  const url = response.config?.url || '';
  if (url.includes('/api/downlink/')) {
    return responseData || {}
  }

  if (Array.isArray(responseData)) {
    return responseData
  } else if (responseData && Array.isArray(responseData.content)) {
    return responseData
  } else if (responseData && Array.isArray(responseData.list)) {
    return responseData
  } else if (responseData && Array.isArray(responseData.items)) {
    return responseData
  } else if (responseData && Array.isArray(responseData.data)) {
    return responseData
  } else {
    if (response.config && response.config.url) {
      if (response.config.url.includes('/api/locations/device/') &&
          (response.config.url.includes('/latest') || response.config.url.includes('/latest-with-amap') || response.config.url.includes('/latest-with-address'))) {
        return responseData || {}
      } else if (response.config.url.includes('/api/fences')) {
        return []
      }
    }
    return responseData || {}
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
  if (error.config && error.config.url) {
    if (error.config.url.includes('/api/fences')) {
      return { list: [], total: 0 }
    } else if (error.config.url.includes('/api/devices')) {
      return { content: [], total: 0 }
    } else if (error.config.url.includes('/api/patients')) {
      return { content: [], total: 0 }
    }
  }
  return {}
}
