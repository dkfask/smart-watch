import { formatBeijingTime } from '../utils/time'
/**
 * 地图服务
 * 优化地图加载和渲染性能
 */
export class MapService {
  constructor() {
    this.map = null
    this.markers = new Map()
    this.fenceLayers = new Map()
    this.tileLayer = null
    this.isInitialized = false
    this.tileCache = new Map() // 瓦片缓存
    this.markerCluster = null // 标记点聚合
    this.L = null // Leaflet实例
  }

  /**
   * 加载Leaflet库
   * @returns {Promise<void>}
   */
  async loadLeaflet() {
    if (!this.L) {
      // 动态导入Leaflet库
      const leaflet = await import('leaflet')
      this.L = leaflet.default
      // 导入Leaflet样式
      import('leaflet/dist/leaflet.css')
    }
    return this.L
  }

  /**
   * 初始化地图
   * @param {string} containerId - 容器ID
   * @param {Object} options - 初始化选项
   * @returns {Promise<L.Map>} - 地图实例
   */
  async initMap(containerId, options = {}) {
    if (this.map) {
      return this.map
    }

    // 加载Leaflet库
    const L = await this.loadLeaflet()

    const defaultOptions = {
      center: [39.9042, 116.4074], // 默认北京
      zoom: 10,
      minZoom: 3,
      maxZoom: 18,
      preferCanvas: true, // 启用Canvas渲染，提高性能
      renderer: L.canvas({ padding: 0.5 }) // 使用Canvas渲染器
    }

    const finalOptions = { ...defaultOptions, ...options }

    // 创建地图实例
    this.map = L.map(containerId, finalOptions)

    // 添加瓦片图层（使用天地图标准矢量底图 + 注记）
    const tiandituKey = import.meta.env.VITE_TIANDITU_KEY || ''
    const subdomains = ['0', '1', '2', '3', '4', '5', '6', '7']

    this.tileLayer = L.tileLayer(
      `https://t{s}.tianditu.gov.cn/vec_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=vec&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=${tiandituKey}`,
      {
        subdomains,
        attribution: '© 天地图 GS(2024)0082号',
        maxZoom: 18,
        minZoom: 3,
        tileSize: 256,
        zoomOffset: 0,
        reuseTiles: true,
        updateWhenIdle: true,
        tileCacheSize: 200
      }
    ).addTo(this.map)

    // 添加文字注记图层
    L.tileLayer(
      `https://t{s}.tianditu.gov.cn/cva_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=cva&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=${tiandituKey}`,
      {
        subdomains,
        maxZoom: 18,
        minZoom: 3,
        tileSize: 256
      }
    ).addTo(this.map)

    // 实现瓦片缓存
    this.tileLayer.on('tileload', (e) => {
      const url = e.tile.src
      this.tileCache.set(url, e.tile)
    })

    // 尝试使用Leaflet.markercluster进行标记点聚合
    if (window.L && window.L.MarkerClusterGroup) {
      this.markerCluster = L.markerClusterGroup({
        maxClusterRadius: 50,
        showCoverageOnHover: false,
        zoomToBoundsOnClick: true,
        spiderfyOnMaxZoom: true,
        removeOutsideVisibleBounds: true // 移除视野外的标记，提高性能
      })
      this.map.addLayer(this.markerCluster)
    }

    this.isInitialized = true
    return this.map
  }

  /**
   * 获取地图实例
   * @returns {L.Map} - 地图实例
   */
  getMap() {
    return this.map
  }

  /**
   * 更新设备标记
   * @param {string} deviceId - 设备ID
   * @param {Object} location - 位置信息
   * @param {Object} device - 设备信息
   */
  async updateMarker(deviceId, location, device) {
    if (!this.map || !location || !location.latitude || !location.longitude) {
      return
    }

    // 确保Leaflet已加载
    const L = await this.loadLeaflet()

    // 检查是否已有标记
    if (this.markers.has(deviceId)) {
      // 更新现有标记位置
      const marker = this.markers.get(deviceId)
      marker.setLatLng([location.latitude, location.longitude])
      marker.bindPopup(`
        <b>设备: ${device.imei}</b><br>
        位置: ${location.address || '未知'}<br>
        时间: ${this.formatDate(location.time)}<br>
        电池: ${location.batteryLevel || '-'}%
      `)
    } else {
      // 创建自定义水滴图标
      const customIcon = L.divIcon({
        className: 'custom-marker',
        html: `
          <div class="marker-drop">
            <div class="marker-number">${device.id}</div>
          </div>
        `,
        iconSize: [30, 30],
        iconAnchor: [15, 30]
      })

      // 创建新标记
      const marker = L.marker([location.latitude, location.longitude], { icon: customIcon })
        .bindPopup(`
          <b>设备: ${device.imei}</b><br>
          位置: ${location.address || '未知'}<br>
          时间: ${this.formatDate(location.time)}<br>
          电池: ${location.batteryLevel || '-'}%
        `)

      // 添加到地图或聚合组
      if (this.markerCluster) {
        this.markerCluster.addLayer(marker)
      } else {
        marker.addTo(this.map)
      }
      this.markers.set(deviceId, marker)
    }
  }

  /**
   * 移除设备标记
   * @param {string} deviceId - 设备ID
   */
  removeMarker(deviceId) {
    if (this.map && this.markers.has(deviceId)) {
      const marker = this.markers.get(deviceId)
      if (this.markerCluster) {
        this.markerCluster.removeLayer(marker)
      } else {
        this.map.removeLayer(marker)
      }
      this.markers.delete(deviceId)
    }
  }

  /**
   * 清除所有标记
   */
  clearAllMarkers() {
    if (this.map) {
      if (this.markerCluster) {
        this.markerCluster.clearLayers()
      } else {
        this.markers.forEach((marker, deviceId) => {
          this.map.removeLayer(marker)
        })
      }
      this.markers.clear()
    }
  }

  /**
   * 绘制围栏
   * @param {Object} fence - 围栏信息
   */
  async drawFence(fence) {
    if (!this.map || !fence || !fence.type || !Array.isArray(fence.coordinates)) {
      return
    }

    // 确保Leaflet已加载
    const L = await this.loadLeaflet()

    // 清除旧围栏
    this.removeFence(fence.id)

    let layer
    if (fence.type === 'circle') {
      // 绘制圆形围栏
      if (fence.coordinates.length === 2 && typeof fence.coordinates[0] === 'number' && typeof fence.coordinates[1] === 'number') {
        layer = L.circle(
          [fence.coordinates[1], fence.coordinates[0]],
          {
            radius: fence.radius || 50,
            color: '#ff6b6b',
            fillColor: '#ff6b6b',
            fillOpacity: 0.3,
            weight: 2,
            interactive: false,
            className: 'fence-layer' // 添加CSS类，便于样式控制
          }
        )
      }
    } else if (fence.type === 'polygon') {
      // 绘制多边形围栏
      if (fence.coordinates.length >= 3) {
        try {
          const latLngs = fence.coordinates.map(coord => {
            if (Array.isArray(coord) && coord.length === 2 && typeof coord[0] === 'number' && typeof coord[1] === 'number') {
              return [coord[1], coord[0]]
            } else {
              throw new Error('无效的坐标点: ' + coord)
            }
          })

          layer = L.polygon(latLngs, {
            color: '#4ecdc4',
            fillColor: '#4ecdc4',
            fillOpacity: 0.3,
            weight: 2,
            interactive: false,
            className: 'fence-layer' // 添加CSS类，便于样式控制
          })
        } catch (error) {
          console.warn('多边形围栏坐标格式无效:', error.message)
          return
        }
      }
    }

    if (layer) {
      layer.bindPopup(`<b>${fence.name}</b><br>类型: ${fence.type === 'circle' ? '圆形' : '多边形'}`)
      layer.addTo(this.map)
      layer.fenceId = fence.id
      this.fenceLayers.set(fence.id, layer)
    }
  }

  /**
   * 移除围栏
   * @param {string} fenceId - 围栏ID
   */
  removeFence(fenceId) {
    if (this.map && this.fenceLayers.has(fenceId)) {
      const layer = this.fenceLayers.get(fenceId)
      this.map.removeLayer(layer)
      this.fenceLayers.delete(fenceId)
    }
  }

  /**
   * 清除所有围栏
   */
  clearAllFences() {
    if (this.map) {
      this.fenceLayers.forEach((layer, fenceId) => {
        this.map.removeLayer(layer)
      })
      this.fenceLayers.clear()
    }
  }

  /**
   * 绘制所有围栏
   * @param {Array} fences - 围栏列表
   */
  async drawAllFences(fences) {
    if (!this.map) return

    // 确保Leaflet已加载
    const L = await this.loadLeaflet()

    // 清除现有围栏
    this.clearAllFences()

    // 过滤掉无效的围栏
    const validFences = fences.filter(fence => {
      if (!fence || !fence.type || !Array.isArray(fence.coordinates)) {
        return false
      }

      if (fence.type === 'circle') {
        return fence.coordinates.length === 2 && 
               typeof fence.coordinates[0] === 'number' && 
               typeof fence.coordinates[1] === 'number'
      } else if (fence.type === 'polygon') {
        return fence.coordinates.length >= 3 && 
               fence.coordinates.every(coord => 
                 Array.isArray(coord) && 
                 coord.length === 2 && 
                 typeof coord[0] === 'number' && 
                 typeof coord[1] === 'number'
               )
      }

      return false
    })

    // 批量添加围栏，减少DOM操作
    const fenceGroup = L.layerGroup()
    validFences.forEach(fence => {
      let layer
      if (fence.type === 'circle') {
        layer = L.circle(
          [fence.coordinates[1], fence.coordinates[0]],
          {
            radius: fence.radius || 50,
            color: '#ff6b6b',
            fillColor: '#ff6b6b',
            fillOpacity: 0.3,
            weight: 2,
            interactive: false,
            className: 'fence-layer'
          }
        )
      } else if (fence.type === 'polygon') {
        const latLngs = fence.coordinates.map(coord => [coord[1], coord[0]])
        layer = L.polygon(latLngs, {
          color: '#4ecdc4',
          fillColor: '#4ecdc4',
          fillOpacity: 0.3,
          weight: 2,
          interactive: false,
          className: 'fence-layer'
        })
      }

      if (layer) {
        layer.bindPopup(`<b>${fence.name}</b><br>类型: ${fence.type === 'circle' ? '圆形' : '多边形'}`)
        layer.fenceId = fence.id
        fenceGroup.addLayer(layer)
        this.fenceLayers.set(fence.id, layer)
      }
    })

    fenceGroup.addTo(this.map)
  }

  /**
   * 定位到设备
   * @param {Object} location - 位置信息
   * @param {number} zoom - 缩放级别
   */
  centerToLocation(location, zoom = 15) {
    if (!this.map || !location || !location.latitude || !location.longitude) {
      return
    }

    // 使用animate选项，使地图平滑过渡
    this.map.setView([location.latitude, location.longitude], zoom, {
      animate: true,
      duration: 0.5
    })
  }

  /**
   * 自动调整地图视图以包含所有设备和围栏
   * @param {Map} deviceLocations - 设备位置映射
   * @param {Array} fences - 围栏列表
   */
  async autoAdjustView(deviceLocations, fences) {
    if (!this.map) return

    // 确保Leaflet已加载
    const L = await this.loadLeaflet()

    // 收集所有有效的位置
    const allLatLngs = []

    // 添加所有设备位置
    deviceLocations.forEach((location, deviceId) => {
      if (location && location.latitude && location.longitude) {
        allLatLngs.push([location.latitude, location.longitude])
      }
    })

    // 添加所有围栏的位置
    fences.forEach(fence => {
      if (fence && fence.type && Array.isArray(fence.coordinates)) {
        if (fence.type === 'circle') {
          // 圆形围栏，添加中心点
          if (fence.coordinates.length === 2 && typeof fence.coordinates[0] === 'number' && typeof fence.coordinates[1] === 'number') {
            allLatLngs.push([fence.coordinates[1], fence.coordinates[0]])
          }
        } else if (fence.type === 'polygon') {
          // 多边形围栏，添加所有顶点
          fence.coordinates.forEach(coord => {
            if (Array.isArray(coord) && coord.length === 2 && typeof coord[0] === 'number' && typeof coord[1] === 'number') {
              allLatLngs.push([coord[1], coord[0]])
            }
          })
        }
      }
    })

    // 如果没有任何位置数据，保持默认视图
    if (allLatLngs.length === 0) {
      return
    }

    // 如果只有一个位置，直接定位到该位置
    if (allLatLngs.length === 1) {
      this.map.setView(allLatLngs[0], 15, {
        animate: true,
        duration: 0.5
      })
      return
    }

    // 如果有多个位置，计算合适的地图边界
    const bounds = L.latLngBounds(allLatLngs)
    this.map.fitBounds(bounds, { 
      padding: [50, 50], 
      maxZoom: 15,
      animate: true,
      duration: 0.5
    })
  }

  /**
   * 预加载地图瓦片
   * @param {Array} locations - 位置列表
   * @param {number} zoom - 缩放级别
   */
  async preloadTiles(locations, zoom = 15) {
    if (!this.map) return

    locations.forEach(location => {
      if (location && location.latitude && location.longitude) {
        // 计算该位置在指定缩放级别的瓦片坐标
        const point = this.map.project([location.latitude, location.longitude], zoom)
        const tileSize = this.tileLayer.options.tileSize || 256
        const tilePoint = point.divideBy(tileSize).floor()

        // 预加载周围的瓦片
        for (let x = tilePoint.x - 1; x <= tilePoint.x + 1; x++) {
          for (let y = tilePoint.y - 1; y <= tilePoint.y + 1; y++) {
            const url = this.tileLayer.getTileUrl({ x, y, z: zoom })
            if (!this.tileCache.has(url)) {
              // 预加载瓦片
              const img = new Image()
              img.src = url
              this.tileCache.set(url, img)
            }
          }
        }
      }
    })
  }

  /**
   * 销毁地图
   */
  destroy() {
    if (this.map) {
      this.clearAllMarkers()
      this.clearAllFences()
      if (this.markerCluster) {
        this.map.removeLayer(this.markerCluster)
        this.markerCluster = null
      }
      this.map.remove()
      this.map = null
      this.tileCache.clear()
      this.isInitialized = false
    }
  }

  /**
   * 格式化日期
   * @param {string} dateString - 日期字符串
   * @returns {string} - 格式化后的日期
   */
  formatDate(dateString) {
    if (!dateString) return ''
    return formatBeijingTime(dateString)
  }
}

// 导出单例实例
const mapService = new MapService()
export default mapService