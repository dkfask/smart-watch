/**
 * 缓存服务
 * 用于缓存热点数据，提高前端性能
 */
export class CacheService {
  constructor() {
    this.memoryCache = new Map()
    this.defaultExpiry = 5 * 60 * 1000 // 默认过期时间：5分钟
    this.storageKey = 'smart_location_cache' // 本地存储键
    this.maxStorageSize = 10 * 1024 * 1024 // 本地存储最大容量：10MB
    this.initializeFromStorage()
  }

  /**
   * 从本地存储初始化缓存
   */
  initializeFromStorage() {
    try {
      const storedData = localStorage.getItem(this.storageKey)
      if (storedData) {
        const parsedData = JSON.parse(storedData)
        if (parsedData && typeof parsedData === 'object') {
          // 只加载未过期的缓存
          const now = Date.now()
          for (const [key, item] of Object.entries(parsedData)) {
            if (now <= item.expiry) {
              this.memoryCache.set(key, item)
            }
          }
        }
      }
    } catch (error) {
      console.error('Failed to initialize cache from storage:', error)
    }
  }

  /**
   * 将缓存保存到本地存储
   */
  saveToStorage() {
    try {
      // 清理过期缓存
      this.cleanup()
      
      // 检查存储大小
      const cacheData = Object.fromEntries(this.memoryCache.entries())
      const cacheString = JSON.stringify(cacheData)
      const cacheSize = new Blob([cacheString]).size
      
      if (cacheSize <= this.maxStorageSize) {
        localStorage.setItem(this.storageKey, cacheString)
      } else {
        // 如果超出容量，只保存最近使用的缓存
        this.evictOldestCache(cacheSize - this.maxStorageSize)
        this.saveToStorage()
      }
    } catch (error) {
      console.error('Failed to save cache to storage:', error)
    }
  }

  /**
   * 驱逐最旧的缓存
   * @param {number} targetSize - 目标大小
   */
  evictOldestCache(targetSize) {
    // 按创建时间排序
    const entries = Array.from(this.memoryCache.entries())
      .sort((a, b) => a[1].createdAt - b[1].createdAt)
    
    let removedSize = 0
    for (const [key, item] of entries) {
      if (removedSize >= targetSize) {
        break
      }
      const itemSize = new Blob([JSON.stringify(item)]).size
      this.memoryCache.delete(key)
      removedSize += itemSize
    }
  }

  /**
   * 设置缓存
   * @param {string} key - 缓存键
   * @param {any} value - 缓存值
   * @param {number} expiry - 过期时间（毫秒）
   * @param {boolean} persist - 是否持久化到本地存储
   */
  set(key, value, expiry = this.defaultExpiry, persist = true) {
    const item = {
      value,
      expiry: Date.now() + expiry,
      createdAt: Date.now(),
      persist
    }
    this.memoryCache.set(key, item)
    
    if (persist) {
      this.saveToStorage()
    }
  }

  /**
   * 获取缓存
   * @param {string} key - 缓存键
   * @returns {any} - 缓存值，如果不存在或已过期则返回null
   */
  get(key) {
    // 先从内存缓存获取
    let item = this.memoryCache.get(key)
    if (item) {
      if (Date.now() > item.expiry) {
        this.memoryCache.delete(key)
        this.saveToStorage()
        return null
      }
      return item.value
    }

    // 从本地存储获取
    try {
      const storedData = localStorage.getItem(this.storageKey)
      if (storedData) {
        const parsedData = JSON.parse(storedData)
        item = parsedData[key]
        if (item) {
          if (Date.now() > item.expiry) {
            // 删除过期缓存
            delete parsedData[key]
            localStorage.setItem(this.storageKey, JSON.stringify(parsedData))
            return null
          }
          // 将本地存储的缓存加载到内存
          this.memoryCache.set(key, item)
          return item.value
        }
      }
    } catch (error) {
      console.error('Failed to get cache from storage:', error)
    }

    return null
  }

  /**
   * 删除缓存
   * @param {string} key - 缓存键
   */
  delete(key) {
    this.memoryCache.delete(key)
    this.saveToStorage()
  }

  /**
   * 清除所有缓存
   */
  clear() {
    this.memoryCache.clear()
    try {
      localStorage.removeItem(this.storageKey)
    } catch (error) {
      console.error('Failed to clear cache from storage:', error)
    }
  }

  /**
   * 检查缓存是否存在且未过期
   * @param {string} key - 缓存键
   * @returns {boolean} - 是否存在且未过期
   */
  has(key) {
    return this.get(key) !== null
  }

  /**
   * 获取缓存大小
   * @returns {number} - 缓存大小
   */
  size() {
    return this.memoryCache.size
  }

  /**
   * 清理过期缓存
   */
  cleanup() {
    const now = Date.now()
    let hasChanges = false
    for (const [key, item] of this.memoryCache.entries()) {
      if (now > item.expiry) {
        this.memoryCache.delete(key)
        hasChanges = true
      }
    }
    
    if (hasChanges) {
      this.saveToStorage()
    }
  }

  /**
   * 设置默认过期时间
   * @param {number} expiry - 默认过期时间（毫秒）
   */
  setDefaultExpiry(expiry) {
    this.defaultExpiry = expiry
  }

  /**
   * 获取所有缓存键
   * @returns {Array} - 缓存键数组
   */
  keys() {
    return Array.from(this.memoryCache.keys())
  }

  /**
   * 批量设置缓存
   * @param {Object} items - 缓存项对象
   * @param {number} expiry - 过期时间（毫秒）
   * @param {boolean} persist - 是否持久化到本地存储
   */
  setMultiple(items, expiry = this.defaultExpiry, persist = true) {
    for (const [key, value] of Object.entries(items)) {
      this.set(key, value, expiry, persist)
    }
  }

  /**
   * 批量获取缓存
   * @param {Array} keys - 缓存键数组
   * @returns {Object} - 缓存值对象
   */
  getMultiple(keys) {
    const result = {}
    keys.forEach(key => {
      result[key] = this.get(key)
    })
    return result
  }

  /**
   * 批量删除缓存
   * @param {Array} keys - 缓存键数组
   */
  deleteMultiple(keys) {
    keys.forEach(key => {
      this.delete(key)
    })
  }

  /**
   * 检查是否支持本地存储
   * @returns {boolean} - 是否支持本地存储
   */
  isStorageAvailable() {
    try {
      const testKey = '__test__'
      localStorage.setItem(testKey, testKey)
      localStorage.removeItem(testKey)
      return true
    } catch (error) {
      return false
    }
  }

  /**
   * 获取本地存储使用情况
   * @returns {Object} - 存储使用情况
   */
  getStorageUsage() {
    try {
      const storedData = localStorage.getItem(this.storageKey)
      const used = storedData ? new Blob([storedData]).size : 0
      return {
        used,
        total: this.maxStorageSize,
        percentage: (used / this.maxStorageSize) * 100
      }
    } catch (error) {
      console.error('Failed to get storage usage:', error)
      return { used: 0, total: this.maxStorageSize, percentage: 0 }
    }
  }
}

// 导出单例实例
const cacheService = new CacheService()
export default cacheService