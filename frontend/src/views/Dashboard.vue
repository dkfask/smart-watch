<template>
  <div class="dashboard-container">
    <!-- 欢迎信息区域 -->
    <div class="welcome-section">
      <h1 class="welcome-title">你好 东软, 你的本地时间是 {{ currentTime }}</h1>
    </div>

    <!-- 统计卡片区域 -->
    <div class="stats-section">
      <!-- 设备状态卡片 -->
      <div class="stats-card">
        <div class="stats-icon device-icon"></div>
        <div class="stats-content">
          <h3 class="stats-title">设备状态</h3>
          <div class="stats-values">
            <div class="stats-item">
              <span class="stats-number">{{ deviceStats.total }}</span>
              <span class="stats-label">总设备</span>
            </div>
            <div class="stats-item">
              <span class="stats-number online">{{ deviceStats.online }}</span>
              <span class="stats-label">在线</span>
            </div>
            <div class="stats-item">
              <span class="stats-number offline">{{ deviceStats.offline }}</span>
              <span class="stats-label">离线</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 报警统计卡片 -->
      <div class="stats-card">
        <div class="stats-icon alarm-icon"></div>
        <div class="stats-content">
          <h3 class="stats-title">报警统计</h3>
          <div class="stats-values">
            <div class="stats-item">
              <span class="stats-number">{{ alarmStats.total }}</span>
              <span class="stats-label">总报警</span>
            </div>
            <div class="stats-item">
              <span class="stats-number critical">{{ alarmStats.critical }}</span>
              <span class="stats-label">紧急</span>
            </div>
            <div class="stats-item">
              <span class="stats-number pending">{{ alarmStats.pending }}</span>
              <span class="stats-label">未处理</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 位置统计卡片 -->
      <div class="stats-card">
        <div class="stats-icon location-icon"></div>
        <div class="stats-content">
          <h3 class="stats-title">位置统计</h3>
          <div class="stats-values">
            <div class="stats-item">
              <span class="stats-number">{{ locationStats.total }}</span>
              <span class="stats-label">总位置</span>
            </div>
            <div class="stats-item">
              <span class="stats-number inside">{{ locationStats.inside }}</span>
              <span class="stats-label">围栏内</span>
            </div>
            <div class="stats-item">
              <span class="stats-number outside">{{ locationStats.outside }}</span>
              <span class="stats-label">围栏外</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 内容区域 -->
    <div class="content-section">
      <!-- 热力图 -->
      <div class="content-card">
        <h3 class="card-title">热力图</h3>
        <div class="map-container">
          <div class="map-tools">
            <el-button size="small" class="map-tool-btn" @click="zoomIn">+</el-button>
            <el-button size="small" class="map-tool-btn" @click="zoomOut">-</el-button>
            <el-button size="small" class="map-tool-btn" @click="resetView">重置</el-button>
          </div>
          <div id="heatmap" class="map"></div>
        </div>
      </div>

      <!-- 轨迹日历 -->
      <div class="content-card">
        <h3 class="card-title">轨迹日历</h3>
        <div class="calendar-container">
          <el-calendar :model-value="currentDate">
            <template #header="{ date, type, onChange }">
              <div class="calendar-header">
                <span class="calendar-year">{{ date instanceof Date ? date.getFullYear() : new Date().getFullYear() }}年</span>
                <span class="calendar-month">{{ date instanceof Date ? (date.getMonth() + 1) : (new Date().getMonth() + 1) }}月</span>
                <div class="calendar-toggle">
                  <el-button size="small" @click="changeCalendarType('month')" :class="{ active: type === 'month' }">月</el-button>
                  <el-button size="small" @click="changeCalendarType('year')" :class="{ active: type === 'year' }">年</el-button>
                </div>
              </div>
            </template>
            <template #default="{ data }">
              <div class="calendar-day" 
                   :class="{
                     'is-today': data.isToday,
                     'has-location': hasLocationData(data.day),
                     'has-alarm': hasAlarmData(data.day)
                   }">
                {{ data.day.split('-').slice(2).join('-') }}
              </div>
            </template>
          </el-calendar>
        </div>
      </div>

      <!-- 最近报警 -->
      <div class="content-card">
        <h3 class="card-title">最近报警</h3>
        <div class="alarm-list">
          <el-empty v-if="recentAlarms.length === 0" description="暂无报警数据" />
          <div v-else class="alarm-item" v-for="alarm in recentAlarms" :key="alarm.id">
            <div class="alarm-time">{{ formatTime(alarm.time) }}</div>
            <div class="alarm-content">
              <div class="alarm-type" :class="alarm.level">{{ alarm.type }}</div>
              <div class="alarm-device">{{ alarm.deviceId }}</div>
              <div class="alarm-location">{{ alarm.location }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 设备状态列表 -->
      <div class="content-card">
        <h3 class="card-title">设备状态</h3>
        <div class="device-list">
          <el-empty v-if="recentDevices.length === 0" description="暂无设备数据" />
          <div v-else class="device-item" v-for="device in recentDevices" :key="device.id">
            <div class="device-status" :class="device.status"></div>
            <div class="device-info">
              <div class="device-name">{{ device.name }}</div>
              <div class="device-id">{{ device.id }}</div>
            </div>
            <div class="device-battery">
              <el-progress 
                :percentage="device.battery" 
                :color="getBatteryColor(device.battery)" 
                :stroke-width="8" 
                :show-text="false" 
              />
              <span class="battery-text">{{ device.battery }}%</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

// 当前时间
const currentTime = ref('')
const currentDate = ref(new Date())
let timeInterval = null

// 地图实例
let map = null

// 统计数据
const deviceStats = ref({
  total: 10,
  online: 8,
  offline: 2
})

const alarmStats = ref({
  total: 5,
  critical: 2,
  pending: 3
})

const locationStats = ref({
  total: 10,
  inside: 7,
  outside: 3
})

// 最近报警
const recentAlarms = ref([
  {
    id: 1,
    type: '围栏越界',
    level: 'critical',
    deviceId: 'D123456',
    location: '北京市朝阳区',
    time: new Date()
  },
  {
    id: 2,
    type: '低电量',
    level: 'warning',
    deviceId: 'D789012',
    location: '北京市海淀区',
    time: new Date(Date.now() - 3600000)
  },
  {
    id: 3,
    type: '设备离线',
    level: 'warning',
    deviceId: 'D345678',
    location: '北京市西城区',
    time: new Date(Date.now() - 7200000)
  }
])

// 最近设备
const recentDevices = ref([
  {
    id: 'D123456',
    name: '老人手表 1',
    status: 'online',
    battery: 85
  },
  {
    id: 'D789012',
    name: '老人手表 2',
    status: 'online',
    battery: 60
  },
  {
    id: 'D345678',
    name: '老人手表 3',
    status: 'offline',
    battery: 20
  },
  {
    id: 'D901234',
    name: '老人手表 4',
    status: 'online',
    battery: 90
  }
])

// 更新当前时间
const updateTime = () => {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  const hours = String(now.getHours()).padStart(2, '0')
  const minutes = String(now.getMinutes()).padStart(2, '0')
  const seconds = String(now.getSeconds()).padStart(2, '0')
  currentTime.value = `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

// 日历类型切换
const changeCalendarType = (type) => {
  // 日历类型切换逻辑，Element Plus 日历组件会自动处理
}

// 检查日期是否有位置数据
const hasLocationData = (date) => {
  // 模拟数据，实际应该从API获取
  return Math.random() > 0.5
}

// 检查日期是否有报警数据
const hasAlarmData = (date) => {
  // 模拟数据，实际应该从API获取
  return Math.random() > 0.7
}

// 格式化时间
const formatTime = (time) => {
  const now = new Date(time)
  const hours = String(now.getHours()).padStart(2, '0')
  const minutes = String(now.getMinutes()).padStart(2, '0')
  return `${hours}:${minutes}`
}

// 获取电池颜色
const getBatteryColor = (battery) => {
  if (battery > 70) return '#67C23A'
  if (battery > 30) return '#E6A23C'
  return '#F56C6C'
}

// 地图操作
const zoomIn = () => {
  if (map) {
    map.zoomIn()
  }
}

const zoomOut = () => {
  if (map) {
    map.zoomOut()
  }
}

const resetView = () => {
  if (map) {
    map.setView([39.9042, 116.4074], 11)
  }
}

// 初始化地图
const initMap = () => {
  // 创建地图实例
  map = L.map('heatmap').setView([39.9042, 116.4074], 11)

  // 添加基础图层
  L.tileLayer('https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}', {
    subdomains: ['1', '2', '3', '4'],
    attribution: '© 高德地图'
  }).addTo(map)

  // 添加示例标记
  L.marker([39.9042, 116.4074]).addTo(map)
    .bindPopup('北京')
    .openPopup()

  // 添加热力图数据（模拟）
  const heatData = [
    [39.9042, 116.4074, 0.8],
    [39.9142, 116.4174, 0.6],
    [39.9242, 116.4274, 0.7],
    [39.9342, 116.4374, 0.5],
    [39.9442, 116.4474, 0.9]
  ]

  // 检查是否有热力图插件
  if (L.HeatLayer) {
    L.heatLayer(heatData, {
      radius: 25,
      blur: 15,
      maxZoom: 17
    }).addTo(map)
  }
}

// 生命周期钩子
onMounted(() => {
  // 初始化时间
  updateTime()
  timeInterval = setInterval(updateTime, 1000)

  // 初始化地图
  initMap()
})

onUnmounted(() => {
  // 清除定时器
  if (timeInterval) {
    clearInterval(timeInterval)
  }
})
</script>

<style scoped>
.dashboard-container {
  width: 100%;
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

/* 欢迎信息区域 */
.welcome-section {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  padding: 30px;
  margin-bottom: 30px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.welcome-title {
  font-size: 24px;
  font-weight: bold;
  text-align: center;
  margin-bottom: 0;
  color: #fff;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

/* 统计卡片区域 */
.stats-section {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.stats-card {
  background-color: #fff;
  border-radius: 12px;
  padding: 25px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  display: flex;
  align-items: center;
  gap: 20px;
}

.stats-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.stats-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 24px;
  color: #fff;
}

.device-icon {
  background: linear-gradient(135deg, #4ecdc4 0%, #45b7d1 100%);
}

.alarm-icon {
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a6f 100%);
}

.location-icon {
  background: linear-gradient(135deg, #45b7d1 0%, #2196f3 100%);
}

.stats-content {
  flex: 1;
}

.stats-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 15px;
  color: #303133;
}

.stats-values {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.stats-item {
  text-align: center;
  flex: 1;
}

.stats-number {
  display: block;
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 5px;
  color: #303133;
}

.stats-number.online {
  color: #67C23A;
}

.stats-number.offline {
  color: #F56C6C;
}

.stats-number.critical {
  color: #F56C6C;
}

.stats-number.pending {
  color: #E6A23C;
}

.stats-number.inside {
  color: #67C23A;
}

.stats-number.outside {
  color: #F56C6C;
}

.stats-label {
  font-size: 12px;
  color: #909399;
}

/* 内容区域 */
.content-section {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(500px, 1fr));
  gap: 20px;
}

.content-card {
  background-color: #fff;
  border-radius: 12px;
  padding: 25px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.content-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 20px;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 10px;
}

.card-title::before {
  content: '';
  width: 4px;
  height: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 2px;
}

/* 热力图 */
.map-container {
  position: relative;
  width: 100%;
  height: 350px;
}

.map-tools {
  position: absolute;
  top: 10px;
  left: 10px;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  gap: 5px;
  background-color: rgba(255, 255, 255, 0.9);
  padding: 8px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.map-tool-btn {
  width: 36px;
  height: 36px;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 0;
  background-color: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.map-tool-btn:hover {
  background-color: #ecf5ff;
  border-color: #409eff;
}

.map {
  width: 100%;
  height: 100%;
  background-color: #f0f2f5;
  border-radius: 8px;
}

/* 轨迹日历 */
.calendar-container {
  width: 100%;
}

.calendar-header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-bottom: 15px;
}

.calendar-year {
  font-size: 14px;
  color: #606266;
}

.calendar-month {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.calendar-toggle {
  display: flex;
  gap: 5px;
  margin-left: 20px;
}

.calendar-toggle .el-button {
  padding: 4px 12px;
  font-size: 12px;
  border-radius: 16px;
}

.calendar-toggle .el-button.active {
  background-color: #ecf5ff;
  color: #409eff;
  border-color: #d9ecff;
}

.calendar-day {
  width: 100%;
  height: 36px;
  display: flex;
  justify-content: center;
  align-items: center;
  border-radius: 4px;
  transition: all 0.3s ease;
  cursor: pointer;
}

.calendar-day:hover {
  background-color: #f5f7fa;
}

.calendar-day.is-today {
  background-color: #ecf5ff;
  color: #409eff;
  font-weight: 600;
}

.calendar-day.has-location {
  background-color: #f0f9eb;
  color: #67C23A;
}

.calendar-day.has-alarm {
  background-color: #fef0f0;
  color: #F56C6C;
}

/* 最近报警 */
.alarm-list {
  max-height: 300px;
  overflow-y: auto;
}

.alarm-item {
  display: flex;
  align-items: flex-start;
  gap: 15px;
  padding: 15px;
  border-bottom: 1px solid #f0f2f5;
  transition: all 0.3s ease;
}

.alarm-item:hover {
  background-color: #f5f7fa;
  border-radius: 8px;
}

.alarm-item:last-child {
  border-bottom: none;
}

.alarm-time {
  font-size: 12px;
  color: #909399;
  min-width: 60px;
}

.alarm-content {
  flex: 1;
}

.alarm-type {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 5px;
  padding: 2px 8px;
  border-radius: 12px;
  display: inline-block;
}

.alarm-type.critical {
  background-color: #fef0f0;
  color: #F56C6C;
}

.alarm-type.warning {
  background-color: #fdf6ec;
  color: #E6A23C;
}

.alarm-device {
  font-size: 14px;
  color: #303133;
  margin-bottom: 3px;
}

.alarm-location {
  font-size: 12px;
  color: #909399;
}

/* 设备状态列表 */
.device-list {
  max-height: 300px;
  overflow-y: auto;
}

.device-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 15px;
  border-bottom: 1px solid #f0f2f5;
  transition: all 0.3s ease;
}

.device-item:hover {
  background-color: #f5f7fa;
  border-radius: 8px;
}

.device-item:last-child {
  border-bottom: none;
}

.device-status {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.device-status.online {
  background-color: #67C23A;
  box-shadow: 0 0 10px rgba(103, 194, 58, 0.5);
}

.device-status.offline {
  background-color: #909399;
}

.device-info {
  flex: 1;
}

.device-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 3px;
}

.device-id {
  font-size: 12px;
  color: #909399;
}

.device-battery {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 100px;
}

.battery-text {
  font-size: 12px;
  color: #909399;
  min-width: 35px;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .content-section {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .dashboard-container {
    padding: 10px;
  }
  
  .welcome-section {
    padding: 20px;
  }
  
  .welcome-title {
    font-size: 20px;
  }
  
  .stats-section {
    grid-template-columns: 1fr;
  }
  
  .stats-card {
    padding: 20px;
  }
  
  .content-card {
    padding: 20px;
  }
  
  .map-container {
    height: 300px;
  }
  
  .stats-values {
    flex-direction: column;
    gap: 10px;
  }
  
  .stats-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}
</style>
