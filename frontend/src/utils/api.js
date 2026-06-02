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
  if (isApiPath(url, '/downlink')) {
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
      if (isLatestLocationUrl(response.config.url)) {
        return responseData || {}
      } else if (isApiPath(response.config.url, '/fences')) {
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
  const url = error.config?.url || ''

  if (isLatestLocationUrl(url) && error.response?.status === 404) {
    return null
  }

  if (isApiPath(url, '/downlink')) {
    throw error
  }

  console.error('API请求失败:', error)
  if (error.config && error.config.url) {
    if (isApiPath(error.config.url, '/fences')) {
      return { list: [], total: 0 }
    } else if (isApiPath(error.config.url, '/devices')) {
      return { content: [], total: 0 }
    } else if (isApiPath(error.config.url, '/patients')) {
      return { content: [], total: 0 }
    }
  }
  return {}
}

const isApiPath = (url, path) => {
  const normalized = url.startsWith('/api') ? url.slice(4) : url
  return normalized === path || normalized.startsWith(`${path}/`)
}

const isLatestLocationUrl = (url) => {
  const normalized = url.startsWith('/api') ? url.slice(4) : url
  return normalized.includes('/locations/device/') &&
    (normalized.includes('/latest') ||
      normalized.includes('/latest-with-amap') ||
      normalized.includes('/latest-with-address'))
}
