<template>
  <div class="history-container">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <h2>历史轨迹</h2>
          <el-button type="primary" @click="goBack">返回列表</el-button>
        </div>
      </template>
      <div class="history-content">
        <el-row :gutter="20">
          <el-col :span="24">
            <div class="filter-panel">
              <el-form :inline="true" :model="filterForm" class="filter-form">
                <el-form-item label="设备 IMEI">
                  <el-input v-model="device.imei" disabled placeholder="设备 IMEI" />
                </el-form-item>
                <el-form-item label="开始时间">
                  <el-date-picker
                    v-model="filterForm.startTime"
                    type="datetime"
                    placeholder="选择开始时间"
                    value-format="YYYY-MM-DD HH:mm:ss"
                    default-time="00:00:00"
                  />
                </el-form-item>
                <el-form-item label="结束时间">
                  <el-date-picker
                    v-model="filterForm.endTime"
                    type="datetime"
                    placeholder="选择结束时间"
                    value-format="YYYY-MM-DD HH:mm:ss"
                    default-time="23:59:59"
                  />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="fetchHistoryLocations">查询</el-button>
                </el-form-item>
                <el-form-item>
                  <el-button @click="resetFilter">重置</el-button>
                </el-form-item>
              </el-form>
            </div>
          </el-col>
        </el-row>
        <el-row :gutter="20" style="margin-top: 20px;">
          <el-col :span="16">
            <div class="map-panel">
              <div id="history-map" class="history-map"></div>
              <div class="map-controls">
                <el-button type="primary" @click="centerToFirstLocation">定位到起点</el-button>
                <el-button @click="centerToLastLocation">定位到终点</el-button>
                <el-button @click="fitAllLocations">显示全部轨迹</el-button>
                <el-switch v-model="showFences" active-text="显示围栏" inactive-text="隐藏围栏" @change="drawAllFences" />
              </div>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="location-list-panel">
              <h3>轨迹点列表</h3>
              <el-scrollbar height="500px">
                <el-timeline>
                  <el-timeline-item
                    v-for="(location, index) in historyLocations"
                    :key="index"
                    :timestamp="formatDate(location.time)"
                    placement="top"
                  >
                    <el-card shadow="hover" @click="highlightLocation(index)">
                      <div class="location-item">
                        <div class="location-coord">
                          {{ location.longitude }}, {{ location.latitude }}
                        </div>
                        <div class="location-address">{{ location.address || '未知地址' }}</div>
                        <div class="location-info">
                          <span>精度: {{ location.accuracy || '-' }}m</span>
                          <span>海拔: {{ location.altitude || '-' }}m</span>
                          <span>电量: {{ location.batteryLevel || '-' }}%</span>
                          <span>来源: {{ location.source || '-' }}</span>
                        </div>
                      </div>
                    </el-card>
                  </el-timeline-item>
                </el-timeline>
              </el-scrollbar>
              <div class="pagination">
                <el-pagination
                  v-model:current-page="currentPage"
                  v-model:page-size="pageSize"
                  :page-sizes="[10, 20, 50, 100]"
                  layout="total, sizes, prev, pager, next, jumper"
                  :total="totalLocations"
                  @size-change="handleSizeChange"
                  @current-change="handleCurrentChange"
                />
              </div>
            </div>
          </el-col>
        </el-row>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { deviceApi } from '../api/device'
import { locationApi } from '../api/location'
import { fenceApi } from '../api/fence'
import { ElMessage } from 'element-plus'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

const route = useRoute()
const router = useRouter()
const deviceId = ref(route.params.deviceId)
const device = ref({})
const historyLocations = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const totalLocations = ref(0)
const fences = ref([])
const showFences = ref(true)
let map = null
let markers = []
let polyline = null
let fenceLayers = new Map()

// 过滤条件
const filterForm = reactive({
  startTime: '',
  endTime: ''
})

// 获取设备详情
const fetchDeviceDetail = async () => {
  try {
    const data = await deviceApi.getDevice(deviceId.value)
    device.value = data
  } catch (error) {
    ElMessage.error('获取设备详情失败')
    console.error('Failed to fetch device detail:', error)
  }
}

// 获取所有围栏信息
const fetchFences = async () => {
  try {
    const data = await fenceApi.getFences(100, 0)
    fences.value = data
    drawAllFences()
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

  // 绘制所有围栏
  fences.value.forEach(fence => {
    drawFence(fence)
  })
}

// 在地图上绘制单个围栏
const drawFence = (fence) => {
  if (!map) return

  // 验证围栏数据有效性
  if (!fence || !fence.coordinates || !Array.isArray(fence.coordinates)) {
    console.error('Invalid fence data:', fence)
    return
  }

  let layer
  if (fence.type === 'circle') {
    // 绘制圆形围栏 - 验证坐标是有效的数字
    const lon = parseFloat(fence.coordinates[0])
    const lat = parseFloat(fence.coordinates[1])
    const radius = parseFloat(fence.radius)
    
    // 验证坐标和半径是否有效
    if (!isNaN(lat) && !isNaN(lon) && !isNaN(radius) && radius > 0) {
      layer = L.circle(
        [lat, lon],
        {
          radius: radius,
          color: '#ff6b6b',
          fillColor: '#ff6b6b',
          fillOpacity: 0.3,
          weight: 2,
          interactive: false
        }
      )
    } else {
      console.error('Invalid circle fence coordinates or radius:', fence)
      return
    }
  } else if (fence.type === 'polygon') {
    // 绘制多边形围栏 - 验证所有坐标都是有效的数字
    const latLngs = fence.coordinates.map(coord => {
      const lon = parseFloat(coord[0])
      const lat = parseFloat(coord[1])
      return [lat, lon]
    })
    
    // 检查所有坐标是否有效
    const allCoordsValid = latLngs.every(coord => !isNaN(coord[0]) && !isNaN(coord[1]))
    
    if (allCoordsValid && latLngs.length >= 3) {
      layer = L.polygon(latLngs, {
        color: '#4ecdc4',
        fillColor: '#4ecdc4',
        fillOpacity: 0.3,
        weight: 2,
        interactive: false
      })
    } else {
      console.error('Invalid polygon fence coordinates:', fence)
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

// 判断位置是否在围栏内
const isLocationInFence = (location, fence) => {
  if (!location || !location.latitude || !location.longitude) return false
  if (!fence || !fence.coordinates || !Array.isArray(fence.coordinates)) return false

  const latLng = L.latLng(parseFloat(location.latitude), parseFloat(location.longitude))

  if (fence.type === 'circle') {
    // 验证围栏中心坐标和半径
    const lon = parseFloat(fence.coordinates[0])
    const lat = parseFloat(fence.coordinates[1])
    const radius = parseFloat(fence.radius)
    
    if (!isNaN(lat) && !isNaN(lon) && !isNaN(radius) && radius > 0) {
      const fenceCenter = L.latLng(lat, lon)
      return latLng.distanceTo(fenceCenter) <= radius
    }
  } else if (fence.type === 'polygon') {
    // 验证多边形坐标
    const latLngs = fence.coordinates.map(coord => {
      const lon = parseFloat(coord[0])
      const lat = parseFloat(coord[1])
      return [lat, lon]
    })
    
    const allCoordsValid = latLngs.every(coord => !isNaN(coord[0]) && !isNaN(coord[1]))
    
    if (allCoordsValid && latLngs.length >= 3) {
      const polygon = L.polygon(latLngs)
      return polygon.contains(latLng)
    }
  }

  return false
}

// 判断位置是否越界（不在任何围栏内）
const isLocationBreached = (location) => {
  if (!location || !location.latitude || !location.longitude) return false

  // 如果没有围栏，默认不越界
  if (fences.value.length === 0) return false

  // 检查位置是否在任何一个围栏内
  for (const fence of fences.value) {
    if (isLocationInFence(location, fence)) {
      return false
    }
  }

  return true
}

// 获取历史位置
const fetchHistoryLocations = async () => {
  loading.value = true
  try {
    let data
    if (filterForm.startTime && filterForm.endTime) {
      // 按时间范围查询
      data = await locationApi.getLocationsByRange(
        deviceId.value,
        filterForm.startTime,
        filterForm.endTime,
        pageSize.value,
        (currentPage.value - 1) * pageSize.value
      )
    } else {
      // 查询最近位置
      data = await locationApi.getRecentLocations(
        deviceId.value,
        pageSize.value,
        (currentPage.value - 1) * pageSize.value
      )
    }
    historyLocations.value = data
    totalLocations.value = data.length // 实际应用中应该从后端获取总数
    updateMap()
  } catch (error) {
    ElMessage.error('获取历史位置失败')
    console.error('Failed to fetch history locations:', error)
  } finally {
    loading.value = false
  }
}

// 初始化地图
const initMap = () => {
  map = L.map('history-map').setView([39.9042, 116.4074], 10) // 默认北京

  // 添加瓦片图层（使用高德地图作为备用）
  L.tileLayer('https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}', {
    subdomains: ['1', '2', '3', '4'],
    attribution: '© 高德地图'
  }).addTo(map)
}

// 更新地图
const updateMap = () => {
  if (!map) return

  // 清除现有标记和轨迹
  clearMap()

  if (historyLocations.value.length === 0) {
    ElMessage.info('暂无历史轨迹数据')
    return
  }

  // 创建轨迹点数组
  const latLngs = []

  // 添加标记
  historyLocations.value.forEach((location, index) => {
    if (location.latitude && location.longitude) {
      const latLng = [location.latitude, location.longitude]
      latLngs.push(latLng)

      // 检查是否越界
      const isBreached = isLocationBreached(location)

      // 创建自定义水滴图标
      const customIcon = L.divIcon({
        className: 'custom-marker',
        html: `
          <div class="marker-drop ${isBreached ? 'breached' : ''}">
            <div class="marker-icon">${isBreached ? '🚨' : '📍'}</div>
            <div class="marker-number">${index + 1}</div>
          </div>
        `,
        iconSize: [40, 40],
        iconAnchor: [20, 40]
      });

      // 创建标记
      const marker = L.marker(latLng, { icon: customIcon })
        .bindPopup(`
          <b>时间: ${formatDate(location.time)}</b><br>
          位置: ${location.address || '未知'}<br>
          经度: ${location.longitude}<br>
          纬度: ${location.latitude}<br>
          电池: ${location.batteryLevel || '-'}%<br>
          状态: ${isBreached ? '<span style="color: red;">越界</span>' : '正常'}
        `)

      marker.addTo(map)
      markers.push(marker)

      // 为第一个和最后一个点添加特殊标记
      if (index === 0) {
        // 起点标记
        L.circleMarker(latLng, {
          radius: 8,
          color: '#20a0ff',
          fillColor: '#20a0ff',
          fillOpacity: 0.8
        }).addTo(map)
      } else if (index === historyLocations.value.length - 1) {
        // 终点标记
        L.circleMarker(latLng, {
          radius: 8,
          color: '#f56c6c',
          fillColor: '#f56c6c',
          fillOpacity: 0.8
        }).addTo(map)
      }

      // 越界点添加特殊标记
      if (isBreached) {
        L.circleMarker(latLng, {
          radius: 10,
          color: '#ff4d4f',
          fillColor: '#ff4d4f',
          fillOpacity: 0.5,
          weight: 2
        }).addTo(map)
      }
    }
  })

  // 添加轨迹线
  if (latLngs.length > 1) {
    polyline = L.polyline(latLngs, {
      color: '#409eff',
      weight: 3,
      opacity: 0.8,
      smoothFactor: 1
    }).addTo(map)
  }
}

// 清除地图
const clearMap = () => {
  // 清除标记
  markers.forEach(marker => {
    map.removeLayer(marker)
  })
  markers = []

  // 清除轨迹线
  if (polyline) {
    map.removeLayer(polyline)
    polyline = null
  }
}

// 高亮显示指定位置
const highlightLocation = (index) => {
  if (!map || !historyLocations.value[index]) return
  
  const location = historyLocations.value[index]
  if (location.latitude && location.longitude) {
    map.setView([location.latitude, location.longitude], 15)
    // 打开弹窗
    if (markers[index]) {
      markers[index].openPopup()
    }
  }
}

// 定位到起点
const centerToFirstLocation = () => {
  if (!historyLocations.value[0] || !map) return
  
  const location = historyLocations.value[0]
  if (location.latitude && location.longitude) {
    map.setView([location.latitude, location.longitude], 15)
    if (markers[0]) {
      markers[0].openPopup()
    }
  }
}

// 定位到终点
const centerToLastLocation = () => {
  if (!historyLocations.value.length || !map) return
  
  const location = historyLocations.value[historyLocations.value.length - 1]
  if (location.latitude && location.longitude) {
    map.setView([location.latitude, location.longitude], 15)
    if (markers[markers.length - 1]) {
      markers[markers.length - 1].openPopup()
    }
  }
}

// 显示全部轨迹
const fitAllLocations = () => {
  if (!historyLocations.value.length || !map) return
  
  const latLngs = historyLocations.value
    .filter(location => location.latitude && location.longitude)
    .map(location => [location.latitude, location.longitude])
  
  if (latLngs.length > 0) {
    map.fitBounds(latLngs)
  }
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return ''
  return new Date(dateString).toLocaleString()
}

// 返回列表
const goBack = () => {
  router.push('/devices')
}

// 重置过滤条件
const resetFilter = () => {
  filterForm.startTime = ''
  filterForm.endTime = ''
  currentPage.value = 1
  fetchHistoryLocations()
}

// 分页大小变化
const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  fetchHistoryLocations()
}

// 当前页码变化
const handleCurrentChange = (page) => {
  currentPage.value = page
  fetchHistoryLocations()
}

onMounted(() => {
  initMap()
  fetchDeviceDetail().then(() => {
    fetchHistoryLocations()
  })
  fetchFences()
})

onBeforeUnmount(() => {
  // 销毁地图实例
  if (map) {
    map.remove()
  }
})
</script>

<style scoped>
.history-container {
  width: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.history-content {
  padding: 20px 0;
}

.filter-panel {
  background-color: #f5f7fa;
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.filter-form {
  display: flex;
  align-items: center;
}

.map-panel {
  height: 600px;
  display: flex;
  flex-direction: column;
}

.history-map {
  flex: 1;
  border-radius: 8px;
  overflow: hidden;
}

.map-controls {
  margin-top: 15px;
  display: flex;
  gap: 10px;
  align-items: center;
}

.location-list-panel {
  background-color: #f5f7fa;
  padding: 15px;
  border-radius: 8px;
  height: 600px;
  display: flex;
  flex-direction: column;
}

.location-list-panel h3 {
  margin: 0 0 15px 0;
  font-size: 16px;
  font-weight: bold;
}

.location-item {
  cursor: pointer;
}

.location-coord {
  font-weight: bold;
  margin-bottom: 5px;
}

.location-address {
  color: #606266;
  margin-bottom: 5px;
  font-size: 14px;
}

.location-info {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 12px;
  color: #909399;
}

.pagination {
  margin-top: 15px;
  display: flex;
  justify-content: flex-end;
}

/* 自定义水滴标记样式 */
:deep(.custom-marker) {
  display: flex;
  justify-content: center;
  align-items: center;
}

:deep(.marker-drop) {
  position: relative;
  width: 40px;
  height: 40px;
  background-color: #1890ff;
  border-radius: 50% 50% 50% 0;
  transform: rotate(-45deg);
  display: flex;
  justify-content: center;
  align-items: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  transition: all 0.3s ease;
}

/* 越界标记样式 */
:deep(.marker-drop.breached) {
  background-color: #ff4d4f;
  animation: pulse 1s infinite;
}

@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(255, 77, 79, 0.4);
  }
  70% {
    box-shadow: 0 0 0 10px rgba(255, 77, 79, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(255, 77, 79, 0);
  }
}

:deep(.marker-icon) {
  transform: rotate(45deg);
  font-size: 20px;
  position: absolute;
  top: 8px;
  left: 8px;
}

:deep(.marker-number) {
  position: absolute;
  bottom: -15px;
  right: -15px;
  background-color: #fff;
  border: 2px solid #1890ff;
  border-radius: 50%;
  width: 25px;
  height: 25px;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 12px;
  font-weight: bold;
  color: #1890ff;
  transform: rotate(45deg);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

:deep(.marker-drop.breached) .marker-number {
  border-color: #ff4d4f;
  color: #ff4d4f;
}
</style>
