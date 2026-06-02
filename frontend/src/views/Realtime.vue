<template>
  <div class="realtime-container">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <h2>实时定位</h2>
        </div>
      </template>
      <div class="realtime-content">
        <el-row :gutter="20">
          <el-col :span="6">
            <div class="device-list-panel">
              <h3>设备列表</h3>
              <el-input
                v-model="searchQuery"
                placeholder="搜索病人姓名或设备IMEI"
                prefix-icon="Search"
                class="search-input"
                clearable
              />
              <el-scrollbar height="600px">
                <el-radio-group v-model="selectedDeviceId" class="device-radio-group">
                  <div v-if="devices.length === 0" class="no-devices">
                    暂无设备数据
                  </div>
                  <el-radio-button
                    v-else
                    v-for="device in devices"
                    :key="device.id"
                    :value="device.id"
                    class="device-radio"
                  >
                    <div class="device-info">
                      <div class="device-patient-name">{{ device.patient?.name || device.imei }}</div>
                      <div class="device-imei-sub" v-if="device.patient">{{ device.imei }}</div>
                      <div class="device-status">
                        <el-tag :type="getDeviceStatus(device.id) ? 'success' : 'danger'" size="small">
                          {{ getDeviceStatus(device.id) ? '在线' : '离线' }}
                        </el-tag>
                      </div>
                    </div>
                  </el-radio-button>
                </el-radio-group>
              </el-scrollbar>
            </div>
          </el-col>
          <el-col :span="18">
            <div class="map-panel">
              <div id="realtime-map" class="realtime-map"></div>
              <div class="map-controls">
                <el-button type="primary" @click="centerToSelectedDevice">定位选中设备</el-button>
                <el-button @click="refreshAllLocations">刷新所有位置</el-button>
                <el-switch v-model="autoRefresh" active-text="自动刷新" inactive-text="手动刷新" />
                <el-switch v-model="showFences" active-text="显示围栏" inactive-text="隐藏围栏" @change="drawAllFences" />
              </div>
            </div>
          </el-col>
        </el-row>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, computed } from 'vue'
import { useRoute } from 'vue-router'
import { deviceApi } from '../api/device'
import { locationApi } from '../api/location'
import { fenceApi } from '../api/fence'
import { ElMessage } from 'element-plus'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

const route = useRoute()
const devices = ref([])
const loading = ref(false)
const searchQuery = ref('')
const selectedDeviceId = ref(null)
const autoRefresh = ref(true)
const deviceLocations = ref(new Map())
const deviceStatus = ref(new Map())
const deviceAlarms = ref(new Map())
const fences = ref([])
const showFences = ref(true)
let map = null
let markers = new Map()
let fenceLayers = new Map()
let refreshTimer = null

// 计算过滤后的设备列表
const filteredDevices = computed(() => {
  if (!searchQuery.value) return devices.value
  const query = searchQuery.value.toLowerCase()
  return devices.value.filter(device => 
    device.imei?.toLowerCase().includes(query) ||
    device.patient?.name?.toLowerCase().includes(query)
  )
})

// 获取设备状态
const getDeviceStatus = (deviceId) => {
  // 直接使用设备对象中的isOnline字段，而不是根据位置获取结果判断
  const device = devices.value.find(d => d.id === deviceId)
  return device ? device.isOnline : false
}

// 获取设备列表
const fetchDevices = async () => {
  loading.value = true
  try {
    const data = await deviceApi.getDevices(100, 0) // 获取所有设备
    console.log('设备列表API返回数据:', data)
    // 提取分页字段作为设备数组
    let deviceList = data
    if (data && Array.isArray(data.content)) {
      deviceList = data.content
    } else if (data && Array.isArray(data.list)) {
      deviceList = data.list
    } else if (data && Array.isArray(data.items)) {
      deviceList = data.items
    } else if (!Array.isArray(data)) {
      deviceList = []
    }
    devices.value = deviceList
    // 修复设备列表显示
    fixDeviceListDisplay()
    if (devices.value.length > 0 && !selectedDeviceId.value) {
      const routeDeviceId = Number(route.query.deviceId)
      const routeDevice = devices.value.find(device => Number(device.id) === routeDeviceId)
      selectedDeviceId.value = routeDevice?.id || devices.value[0].id || 0
    }
    // 初始化设备状态
    devices.value.forEach(device => {
      deviceStatus.value.set(device.id || device.imei, false)
    })
  } catch (error) {
    ElMessage.error('获取设备列表失败')
    console.error('Failed to fetch devices:', error)
    // 出错时重置设备列表
    devices.value = []
  } finally {
    loading.value = false
  }
}

// 获取所有围栏信息
const fetchFences = async () => {
  try {
    const data = await fenceApi.getFences(100, 0)
    console.log('获取到的围栏原始数据:', data)
    
    // 转换围栏数据格式以匹配前端要求
    let fenceList = []
    
    // 确保返回的数据是数组，如果是对象则提取list或data字段
    if (Array.isArray(data)) {
      fenceList = data
    } else if (data && Array.isArray(data.list)) {
      fenceList = data.list
    } else if (data && Array.isArray(data.data)) {
      fenceList = data.data
    }
    
    // 转换围栏数据格式，特别是将JSON字符串转换为坐标数组
    fences.value = fenceList.map(fence => ({
      id: fence.id,
      name: fence.name,
      type: fence.type,
      radius: fence.radius,
      // 将JSON字符串转换为坐标数组，处理可能的异常
      coordinates: typeof fence.coordinates === 'string' ? JSON.parse(fence.coordinates) : [],
      // 将字符串状态转换为布尔值
      status: fence.status === 'active',
      // 添加其他需要的字段
      centerLat: fence.centerLat,
      centerLng: fence.centerLng
    }))
    
    console.log('转换后的围栏数据:', fences.value)
    
    // 绘制围栏，捕获可能的绘制错误
    try {
      drawAllFences()
    } catch (error) {
      console.error('Failed to draw fences:', error)
      // 只记录错误，不显示给用户，因为围栏绘制失败不影响主要功能
    }
  } catch (error) {
    console.error('Failed to fetch fences:', error)
    ElMessage.error('获取围栏信息失败')
  }
}

// 在地图上绘制所有围栏
const drawAllFences = () => {
  if (!map) return

  // 清除现有围栏
  clearAllFences()

  if (!showFences.value) return

  // 过滤掉无效的围栏，只绘制有效的围栏
  const validFences = fences.value.filter(fence => {
    // 验证围栏数据是否有效
    if (!fence || !fence.type || !Array.isArray(fence.coordinates)) {
      console.warn('无效的围栏数据:', fence)
      return false
    }
    
    // 验证坐标格式
    if (fence.type === 'circle') {
      // 圆形围栏：坐标必须是 [lng, lat] 格式的数字数组
      return fence.coordinates.length === 2 && 
             typeof fence.coordinates[0] === 'number' && 
             typeof fence.coordinates[1] === 'number'
    } else if (fence.type === 'polygon') {
      // 多边形围栏：必须至少有3个坐标点，每个坐标点必须是 [lng, lat] 格式的数字数组
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
  
  console.log('有效的围栏数量:', validFences.length)
  
  // 绘制所有有效围栏
  validFences.forEach(fence => {
    drawFence(fence)
  })
}

// 在地图上绘制单个围栏
const drawFence = (fence) => {
  if (!map) return

  // 验证围栏数据是否有效
  if (!fence || !fence.type || !Array.isArray(fence.coordinates)) {
    console.warn('无效的围栏数据:', fence)
    return
  }

  let layer
  if (fence.type === 'circle') {
    // 绘制圆形围栏，验证坐标格式是否为 [lng, lat] 格式
    if (fence.coordinates.length === 2 && typeof fence.coordinates[0] === 'number' && typeof fence.coordinates[1] === 'number') {
      layer = L.circle(
        [fence.coordinates[1], fence.coordinates[0]],
        {
          radius: fence.radius || 50,
          color: '#ff6b6b',
          fillColor: '#ff6b6b',
          fillOpacity: 0.3,
          weight: 2,
          interactive: false
        }
      )
    } else {
      console.warn('圆形围栏坐标格式无效:', fence.coordinates)
      return
    }
  } else if (fence.type === 'polygon') {
    // 绘制多边形围栏，验证坐标数组是否有效
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
          interactive: false
        })
      } catch (error) {
        console.warn('多边形围栏坐标格式无效:', error.message)
        return
      }
    } else {
      console.warn('多边形围栏顶点数量不足:', fence.coordinates.length)
      return
    }
  }

  if (layer) {
    layer.bindPopup(`<b>${fence.name}</b><br>类型: ${fence.type === 'circle' ? '圆形' : '多边形'}`)
    layer.addTo(map)
    // 保存围栏ID到图层
    layer.fenceId = fence.id
    fenceLayers.set(fence.id, layer)
  }
}

// 清除地图上的所有围栏
const clearAllFences = () => {
  if (!map) return

  fenceLayers.forEach((layer, fenceId) => {
    map.removeLayer(layer)
  })
  fenceLayers.clear()
}

// 获取设备最新位置
const fetchDeviceLatestLocation = async (deviceId) => {
  try {
    const location = normalizeLocation(await locationApi.getLatestLocationWithAmap(deviceId))
    const device = findDevice(deviceId)
    const fallbackLocation = location || normalizeLocation({
      latitude: device?.lastLatitude,
      longitude: device?.lastLongitude,
      time: device?.lastLocationTime,
      batteryLevel: device?.batteryLevel,
      source: 'device-status'
    })

    console.log(`设备 ${deviceId} 的最新位置:`, fallbackLocation)
    if (isValidLocation(fallbackLocation)) {
      const normalizedDeviceId = normalizeDeviceId(deviceId)
      deviceLocations.value.set(normalizedDeviceId, fallbackLocation)
      updateMarker(normalizedDeviceId, fallbackLocation)
    } else {
      console.warn(`设备 ${deviceId} 没有有效位置数据`)
    }
  } catch (error) {
    console.error(`Failed to fetch location for device ${deviceId}:`, error)
  }
}

// 修复设备列表显示问题
const fixDeviceListDisplay = () => {
  console.log('Current devices in realtime page:', devices.value)
  // 确保设备列表不会显示不存在的设备
  if (devices.value && Array.isArray(devices.value)) {
    // 过滤掉没有有效IMEI的设备
    devices.value = devices.value.filter(device => device && device.imei)
  } else {
    // 如果设备列表不是数组或为空，重置为空数组
    devices.value = []
  }
  console.log('Fixed devices list:', devices.value)
}

// 刷新所有设备位置
const refreshAllLocations = async () => {
  for (const device of devices.value) {
    // 使用设备ID作为标识符
    const deviceIdentifier = normalizeDeviceId(device.id)
    await fetchDeviceLatestLocation(deviceIdentifier)
  }
  
  // 刷新完所有设备位置后，自动定位到设备位置
  autoLocateToDevices()
}

// 自动定位到设备和围栏位置
const autoLocateToDevices = () => {
  if (!map) return
  
  // 收集所有有效的设备位置和围栏位置
  const allLatLngs = []
  
  // 添加所有设备位置
  deviceLocations.value.forEach((location, deviceId) => {
    if (isValidLocation(location)) {
      allLatLngs.push([location.latitude, location.longitude])
    }
  })
  
  // 添加所有围栏的位置
  fences.value.forEach(fence => {
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
    console.log('没有设备或围栏位置数据，保持默认视图')
    return
  }
  
  // 如果只有一个位置，直接定位到该位置
  if (allLatLngs.length === 1) {
    map.setView(allLatLngs[0], 15)
    console.log('只有一个位置数据，定位到该位置:', allLatLngs[0])
    return
  }
  
  // 如果有多个位置，计算合适的地图边界
  const bounds = L.latLngBounds(allLatLngs)
  map.fitBounds(bounds, { padding: [50, 50], maxZoom: 15 })
  console.log('有多个位置数据，自动调整地图边界')
}

// 初始化地图
const initMap = () => {
  // 创建地图实例
  map = L.map('realtime-map').setView([39.9042, 116.4074], 10) // 默认北京

  // 添加瓦片图层（使用高德地图作为备用）
  L.tileLayer('https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}', {
    subdomains: ['1', '2', '3', '4'],
    attribution: '© 高德地图'
  }).addTo(map)
}

// 更新标记
const updateMarker = (deviceId, location) => {
  if (!map || !isValidLocation(location)) {
    return
  }

  const normalizedDeviceId = normalizeDeviceId(deviceId)
  const device = findDevice(normalizedDeviceId)
  if (!device) return

  const displayName = device.patient?.name || device.imei
  const customIcon = L.divIcon({
    className: 'custom-marker',
    html: `
      <div class="marker-drop">
        <div class="marker-number">${displayName.charAt(0)}</div>
      </div>
    `,
    iconSize: [30, 30],
    iconAnchor: [15, 30]
  });

  const marker = L.marker([location.latitude, location.longitude], { icon: customIcon })
    .bindPopup(`
      <b>${displayName}</b><br>
      ${device.patient?.ward ? '病房: ' + device.patient.ward + '<br>' : ''}
      设备: ${device.imei}<br>
      位置: ${formatLocationText(location)}<br>
      时间: ${formatDate(location.time)}<br>
      来源: ${formatLocationSource(location.source)}<br>
      电池: ${location.batteryLevel ?? '-'}%
    `)

  // 移除旧标记
  if (markers.has(normalizedDeviceId)) {
    map.removeLayer(markers.get(normalizedDeviceId))
  }

  // 添加新标记
  marker.addTo(map)
  markers.set(normalizedDeviceId, marker)

  if (normalizeDeviceId(selectedDeviceId.value) === normalizedDeviceId) {
    marker.openPopup()
  }
}

// 定位到选中设备
const centerToSelectedDevice = () => {
  if (!selectedDeviceId.value || !map) return
  
  const normalizedDeviceId = normalizeDeviceId(selectedDeviceId.value)
  const location = deviceLocations.value.get(normalizedDeviceId)
  if (isValidLocation(location)) {
    map.setView([location.latitude, location.longitude], 15)
    // 打开弹窗
    const marker = markers.get(normalizedDeviceId)
    if (marker) {
      marker.openPopup()
    }
  } else {
    ElMessage.warning('该设备暂无位置数据')
  }
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return ''
  return new Date(dateString).toLocaleString()
}

const normalizeDeviceId = (deviceId) => {
  const numericId = Number(deviceId)
  return Number.isFinite(numericId) ? numericId : deviceId
}

const findDevice = (deviceId) => {
  const normalizedDeviceId = normalizeDeviceId(deviceId)
  return devices.value.find(device => normalizeDeviceId(device.id) === normalizedDeviceId)
}

const normalizeCoordinate = (value) => {
  if (value === null || value === undefined || value === '') return null
  const coordinate = Number(value)
  return Number.isFinite(coordinate) ? coordinate : null
}

const normalizeLocation = (location) => {
  if (!location) return null

  const latitude = normalizeCoordinate(location.latitude ?? location.lat ?? location.lastLatitude)
  const longitude = normalizeCoordinate(location.longitude ?? location.lng ?? location.lon ?? location.lastLongitude)

  return {
    ...location,
    latitude,
    longitude,
    time: location.time || location.createdAt || location.lastLocationTime || location.recvTime,
    address: location.address || location.formattedAddress || location.locationAddress || '',
    source: location.source || location.locationSource || ''
  }
}

const isValidLocation = (location) => {
  return location &&
    Number.isFinite(location.latitude) &&
    Number.isFinite(location.longitude) &&
    location.latitude >= -90 &&
    location.latitude <= 90 &&
    location.longitude >= -180 &&
    location.longitude <= 180
}

const formatLocationText = (location) => {
  if (!location) return '未知'
  if (location.address) return location.address
  if (Number.isFinite(location.latitude) && Number.isFinite(location.longitude)) {
    return `经纬度 ${location.latitude.toFixed(6)}, ${location.longitude.toFixed(6)}`
  }
  return '未知'
}

const formatLocationSource = (source) => {
  const labels = {
    gps: 'GPS',
    wifi: 'Wi-Fi',
    cell: '基站',
    bluetooth: '蓝牙',
    'device-status': '设备状态'
  }
  return labels[source] || source || '-'
}

// 定时刷新
const startAutoRefresh = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
  refreshTimer = setInterval(() => {
    if (autoRefresh.value) {
      refreshAllLocations()
    }
  }, 30000) // 每30秒刷新一次
}

onMounted(() => {
  initMap()
  fetchDevices().then(() => {
    refreshAllLocations()
    startAutoRefresh()
  })
  fetchFences().then(() => {
    // 获取完围栏信息后，自动定位到设备和围栏位置
    autoLocateToDevices()
  })
})

onBeforeUnmount(() => {
  // 清除定时器
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
  // 销毁地图实例
  if (map) {
    map.remove()
  }
})
</script>

<style scoped>
.realtime-container {
  width: 100%;
  padding: 0;
  background: transparent;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-color);
}

.card-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.realtime-content {
  padding: 0;
}

.device-list-panel {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  padding: 16px;
  border-radius: var(--radius-lg);
  height: 100%;
  box-shadow: var(--shadow-md);
}

.device-list-panel h3 {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.search-input {
  margin-bottom: 12px;
}

.device-radio-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.device-radio {
  width: 100%;
  text-align: left;
}

.device-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding: 8px 0;
}

.device-imei {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
  font-family: monospace;
}

.device-patient-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.device-imei-sub {
  font-size: 11px;
  color: var(--text-muted);
  font-family: monospace;
}

.device-status {
  margin-left: 8px;
}

.map-panel {
  height: 600px;
  display: flex;
  flex-direction: column;
}

.realtime-map {
  flex: 1;
  border-radius: var(--radius-lg);
  overflow: hidden;
  border: 1px solid var(--border-color);
}

.map-controls {
  margin-top: 16px;
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

/* 自定义水滴标记样式 */
:deep(.custom-marker) {
  display: flex;
  justify-content: center;
  align-items: center;
}

:deep(.marker-drop) {
  position: relative;
  width: 32px;
  height: 32px;
  background: var(--color-primary);
  border-radius: 50% 50% 50% 0;
  transform: rotate(-45deg);
  display: flex;
  justify-content: center;
  align-items: center;
  box-shadow: var(--elevation-2);
}

:deep(.marker-number) {
  position: absolute;
  bottom: -10px;
  right: -10px;
  background-color: var(--bg-card);
  border: 2px solid var(--primary-color);
  border-radius: 50%;
  width: 20px;
  height: 20px;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 10px;
  font-weight: bold;
  color: var(--primary-color);
  transform: rotate(45deg);
  box-shadow: var(--shadow-sm);
}
</style>
