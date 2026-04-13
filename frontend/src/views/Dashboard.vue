<template>
  <div class="dashboard-container">
    <!-- 未处理报警横幅 -->
    <div v-if="alarmStats.pending > 0" class="alarm-banner" @click="goToAlarms">
      <span class="alarm-banner-icon">⚠️</span>
      <span class="alarm-banner-text">您有 <strong>{{ alarmStats.pending }}</strong> 条未处理报警，请及时处理！</span>
      <span class="alarm-banner-action">点击查看 →</span>
    </div>

    <!-- 欢迎信息区域 -->
    <div class="welcome-section">
      <h1 class="welcome-title">智能定位监控系统</h1>
      <p class="welcome-subtitle">{{ currentTime }}</p>
    </div>

    <!-- 统计卡片区域 -->
    <div class="stats-section">
      <!-- 病人状态卡片 -->
      <div class="stats-card">
        <div class="stats-icon patient-icon"></div>
        <div class="stats-content">
          <h3 class="stats-title">病人状态</h3>
          <div class="stats-values">
            <div class="stats-item">
              <span class="stats-number">{{ patientStats.total }}</span>
              <span class="stats-label">总病人</span>
            </div>
            <div class="stats-item">
              <span class="stats-number online">{{ patientStats.monitored }}</span>
              <span class="stats-label">已监控</span>
            </div>
            <div class="stats-item">
              <span class="stats-number offline">{{ patientStats.unmonitored }}</span>
              <span class="stats-label">未监控</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 报警统计卡片 -->
      <div class="stats-card" :class="{ 'has-pending': alarmStats.pending > 0 }">
        <div class="stats-icon alarm-icon"></div>
        <div class="stats-content">
          <h3 class="stats-title">报警统计</h3>
          <div class="stats-values">
            <div class="stats-item">
              <span class="stats-number">{{ alarmStats.total }}</span>
              <span class="stats-label">总报警</span>
            </div>
            <div class="stats-item">
              <span class="stats-number pending">{{ alarmStats.pending }}</span>
              <span class="stats-label">未处理</span>
            </div>
            <div class="stats-item">
              <span class="stats-number handled">{{ alarmStats.handled }}</span>
              <span class="stats-label">已处理</span>
            </div>
          </div>
        </div>
      </div>

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
    </div>

    <!-- 内容区域 -->
    <div class="content-section">
      <!-- 病人位置地图 -->
      <div class="content-card">
        <h3 class="card-title">病人位置分布</h3>
        <div class="map-container">
          <div class="map-tools">
            <el-button size="small" class="map-tool-btn" @click="zoomIn">+</el-button>
            <el-button size="small" class="map-tool-btn" @click="zoomOut">−</el-button>
            <el-button size="small" class="map-tool-btn" @click="resetView">重置</el-button>
          </div>
          <div id="heatmap" class="map"></div>
        </div>
      </div>

      <!-- 最近报警 -->
      <div class="content-card">
        <h3 class="card-title">
          最近报警
          <el-badge v-if="alarmStats.pending > 0" :value="alarmStats.pending" class="alarm-badge" />
        </h3>
        <div class="alarm-list">
          <el-empty v-if="recentAlarms.length === 0" description="暂无报警数据" />
          <div v-else class="alarm-item" v-for="alarm in recentAlarms" :key="alarm.id"
               :class="[alarm.level, { 'alarm-unhandled': alarm.status === 'pending' }]">
            <div class="alarm-time">{{ formatTime(alarm.time) }}</div>
            <div class="alarm-content">
              <div class="alarm-type" :class="alarm.level">{{ alarm.typeName }}</div>
              <div class="alarm-patient">{{ alarm.patientName }}<span v-if="alarm.ward" class="alarm-ward">{{ alarm.ward }}</span></div>
              <div class="alarm-location">{{ alarm.location }}</div>
            </div>
            <el-tag v-if="alarm.status === 'pending'" type="danger" size="small">未处理</el-tag>
            <el-tag v-else type="success" size="small">已处理</el-tag>
          </div>
        </div>
      </div>

      <!-- 病人状态列表 -->
      <div class="content-card">
        <h3 class="card-title">病人监控状态</h3>
        <div class="device-list">
          <el-empty v-if="recentPatients.length === 0" description="暂无病人数据" />
          <div v-else class="device-item" v-for="patient in recentPatients" :key="patient.id">
            <div class="device-status" :class="patient.status"></div>
            <div class="device-info">
              <div class="device-name">{{ patient.name }}</div>
              <div class="device-id">{{ patient.ward }} {{ patient.bed ? '床位' + patient.bed : '' }}</div>
            </div>
            <div class="device-battery" v-if="patient.battery > 0">
              <el-progress
                :percentage="patient.battery"
                :color="getBatteryColor(patient.battery)"
                :stroke-width="8"
                :show-text="false"
              />
              <span class="battery-text">{{ patient.battery }}%</span>
            </div>
            <el-tag v-if="patient.hasAlarm" type="danger" size="small">报警</el-tag>
          </div>
        </div>
      </div>

      <!-- 病区统计 -->
      <div class="content-card">
        <h3 class="card-title">病区概览</h3>
        <div class="ward-list">
          <el-empty v-if="wardStats.length === 0" description="暂无病区数据" />
          <div v-else class="ward-item" v-for="ward in wardStats" :key="ward.name">
            <div class="ward-name">{{ ward.name }}</div>
            <div class="ward-stats">
              <span class="ward-stat">{{ ward.total }}人</span>
              <span class="ward-stat online-stat">{{ ward.online }}在线</span>
              <span class="ward-stat alarm-stat" v-if="ward.alarmCount > 0">{{ ward.alarmCount }}报警</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { alarmApi } from '../api/alarm'
import { deviceApi } from '../api/device'
import { patientApi } from '../api/patient'
import { locationApi } from '../api/location'

const router = useRouter()

const currentTime = ref('')
let timeInterval = null
let map = null
let markers = []

const patientStats = ref({ total: 0, monitored: 0, unmonitored: 0 })
const alarmStats = ref({ total: 0, pending: 0, handled: 0 })
const deviceStats = ref({ total: 0, online: 0, offline: 0 })
const recentAlarms = ref([])
const recentPatients = ref([])
const wardStats = ref([])

const ALARM_TYPE_MAP = {
  'fence_breach': '围栏越界',
  'low_battery': '低电量',
  'sos': 'SOS报警',
  'fall': '跌倒报警',
  'heart_rate_abnormal': '心率异常',
  'blood_pressure_abnormal': '血压异常',
  'blood_oxygen_abnormal': '血氧异常',
  'body_temperature_abnormal': '体温异常'
}

/**
 * 跳转到报警页面
 */
const goToAlarms = () => {
  router.push('/alarm')
}

/**
 * 加载Dashboard数据
 */
const fetchDashboardData = async () => {
  let allDevices = []
  let allPatients = []

  try {
    const stats = await alarmApi.getAlarmStats()
    if (stats) {
      alarmStats.value = {
        total: stats.total || 0,
        pending: stats.pending || 0,
        handled: stats.handled || 0
      }
    }
  } catch (e) {
    console.warn('Failed to load alarm stats:', e)
  }

  try {
    const data = await deviceApi.getDevices(100, 0)
    if (data && Array.isArray(data.content)) {
      allDevices = data.content
    } else if (Array.isArray(data)) {
      allDevices = data
    }
    const onlineCount = allDevices.filter(d => d.isOnline).length
    deviceStats.value = {
      total: allDevices.length,
      online: onlineCount,
      offline: allDevices.length - onlineCount
    }
  } catch (e) {
    console.warn('Failed to load device stats:', e)
  }

  try {
    const response = await patientApi.getPatients({ limit: 100, offset: 0 })
    if (response && Array.isArray(response.content)) {
      allPatients = response.content
    } else if (Array.isArray(response)) {
      allPatients = response
    }
    const monitoredCount = allPatients.filter(p => p.deviceId).length
    patientStats.value = {
      total: allPatients.length,
      monitored: monitoredCount,
      unmonitored: allPatients.length - monitoredCount
    }
  } catch (e) {
    console.warn('Failed to load patient stats:', e)
  }

  try {
    const result = await alarmApi.getAlarms(10, 0)
    let alarms = []
    if (result && Array.isArray(result.content)) {
      alarms = result.content
    } else if (Array.isArray(result)) {
      alarms = result
    }
    recentAlarms.value = alarms.slice(0, 8).map(a => ({
      id: a.id,
      typeName: ALARM_TYPE_MAP[a.alarmType || a.alertType] || a.alarmType || '未知',
      type: a.alarmType || a.alertType || '',
      level: a.alarmLevel === 'warning' ? 'warning' : 'critical',
      patientName: a.patient?.name || '未知病人',
      ward: a.patient?.ward || '',
      deviceId: a.device?.imei || a.deviceId || '',
      location: a.address || '',
      time: a.triggeredTime || a.alertTime ? new Date(a.triggeredTime || a.alertTime) : new Date(),
      status: a.status || 'pending'
    }))
  } catch (e) {
    console.warn('Failed to load recent alarms:', e)
  }

  buildPatientList(allDevices, allPatients)
  buildWardStats(allPatients, allDevices)
  updateMapMarkers(allDevices)
}

/**
 * 构建病人监控状态列表
 */
const buildPatientList = (devices, patients) => {
  const patientMap = new Map()
  patients.forEach(p => patientMap.set(p.id, p))

  const devicePatientMap = new Map()
  devices.forEach(d => {
    if (d.patient) {
      devicePatientMap.set(d.patient.id, d)
    }
  })

  recentPatients.value = patients.slice(0, 8).map(p => {
    const device = devicePatientMap.get(p.id)
    return {
      id: p.id,
      name: p.name || '未知',
      ward: p.ward || '',
      bed: p.bed || '',
      status: device ? (device.isOnline ? 'online' : 'offline') : 'offline',
      battery: device?.batteryLevel || 0,
      hasAlarm: recentAlarms.value.some(a => a.patientName === p.name && a.status === 'pending')
    }
  })
}

/**
 * 构建病区统计
 */
const buildWardStats = (patients, devices) => {
  const wardMap = new Map()
  const devicePatientMap = new Map()
  devices.forEach(d => {
    if (d.patient) devicePatientMap.set(d.patient.id, d)
  })

  patients.forEach(p => {
    const ward = p.ward || '未分配'
    if (!wardMap.has(ward)) {
      wardMap.set(ward, { name: ward, total: 0, online: 0, alarmCount: 0 })
    }
    const wardData = wardMap.get(ward)
    wardData.total++
    const device = devicePatientMap.get(p.id)
    if (device && device.isOnline) wardData.online++
  })

  recentAlarms.value.forEach(a => {
    if (a.ward && a.status === 'pending') {
      const wardData = wardMap.get(a.ward)
      if (wardData) wardData.alarmCount++
    }
  })

  wardStats.value = Array.from(wardMap.values()).sort((a, b) => b.alarmCount - a.alarmCount)
}

/**
 * 更新地图标记为真实病人位置
 */
const updateMapMarkers = async (devices) => {
  if (!map) return

  markers.forEach(m => map.removeLayer(m))
  markers = []

  for (const device of devices) {
    if (!device.isOnline || !device.patient) continue
    try {
      const location = await locationApi.getLatestLocation(device.id)
      if (location && location.latitude && location.longitude) {
        const customIcon = L.divIcon({
          className: 'custom-marker',
          html: `<div class="marker-patient"><span>${device.patient?.name || '?'}</span></div>`,
          iconSize: [40, 40],
          iconAnchor: [20, 40]
        })
        const marker = L.marker([location.latitude, location.longitude], { icon: customIcon })
          .bindPopup(`<b>${device.patient?.name || '未知'}</b><br>${device.patient?.ward || ''} ${device.patient?.bed ? '床位' + device.patient.bed : ''}<br>位置: ${location.address || '未知'}`)
          .addTo(map)
        markers.push(marker)
      }
    } catch (e) {
      // 忽略单个设备位置获取失败
    }
  }

  if (markers.length > 0) {
    const group = L.featureGroup(markers)
    map.fitBounds(group.getBounds().pad(0.2))
  }
}

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

const formatTime = (time) => {
  const now = new Date(time)
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  const hours = String(now.getHours()).padStart(2, '0')
  const minutes = String(now.getMinutes()).padStart(2, '0')
  return `${month}-${day} ${hours}:${minutes}`
}

const getBatteryColor = (battery) => {
  if (battery > 70) return '#67C23A'
  if (battery > 30) return '#E6A23C'
  return '#F56C6C'
}

const zoomIn = () => { if (map) map.zoomIn() }
const zoomOut = () => { if (map) map.zoomOut() }
const resetView = () => { if (map) map.setView([39.9042, 116.4074], 11) }

const initMap = () => {
  map = L.map('heatmap').setView([39.9042, 116.4074], 11)
  L.tileLayer('https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}', {
    subdomains: ['1', '2', '3', '4'],
    attribution: '© 高德地图'
  }).addTo(map)
}

onMounted(() => {
  updateTime()
  timeInterval = setInterval(updateTime, 1000)
  initMap()
  fetchDashboardData()
})

onUnmounted(() => {
  if (timeInterval) clearInterval(timeInterval)
})
</script>

<style scoped>
/* ==========================================
   Dashboard — Light Professional Theme
   Aligned with DESIGN.md design system
   ========================================== */

/* --- Design Tokens (scoped) --- */
.dashboard-container {
  --color-primary: #2563EB;
  --color-primary-light: #DBEAFE;
  --color-primary-subtle: #EFF6FF;
  --color-accent: #FF6B6B;
  --color-accent-light: #FFF0F0;
  --color-success: #10B981;
  --color-success-bg: #ECFDF5;
  --color-warning: #F59E0B;
  --color-warning-bg: #FFFBEB;
  --color-danger: #EF4444;
  --color-danger-bg: #FEF2F2;
  --color-bg: #F8FAFC;
  --color-surface: #FFFFFF;
  --color-surface-raised: #F1F5F9;
  --color-border: #E2E8F0;
  --color-border-strong: #CBD5E1;
  --color-text-primary: #1E293B;
  --color-text-secondary: #64748B;
  --color-text-muted: #94A3B8;
  --color-text-inverse: #FFFFFF;
  --font-sans: "Inter", "PingFang SC", "Microsoft YaHei", system-ui, -apple-system, sans-serif;
  --elevation-1: 0 1px 3px rgba(0, 0, 0, 0.06), 0 1px 2px rgba(0, 0, 0, 0.04);
  --elevation-2: 0 4px 6px rgba(0, 0, 0, 0.05), 0 2px 4px rgba(0, 0, 0, 0.04);
  --elevation-3: 0 10px 15px rgba(0, 0, 0, 0.07), 0 4px 6px rgba(0, 0, 0, 0.04);
}

.dashboard-container {
  width: 100%;
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
  background-color: var(--color-bg);
  font-family: var(--font-sans);
  font-size: 14px;
  color: var(--color-text-primary);
}

/* ============================
   Alarm Banner
   ============================ */
.alarm-banner {
  background: var(--color-accent);
  border-radius: 12px;
  padding: 14px 24px;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: background-color 0.2s ease;
  box-shadow: var(--elevation-1);
  animation: banner-breathe 3s ease-in-out infinite;
}

.alarm-banner:hover {
  background: #E85555;
  box-shadow: var(--elevation-2);
}

.alarm-banner-icon {
  font-size: 18px;
}

.alarm-banner-text {
  flex: 1;
  color: var(--color-text-inverse);
  font-size: 14px;
}

.alarm-banner-text strong {
  font-size: 18px;
  font-variant-numeric: tabular-nums;
}

.alarm-banner-action {
  color: var(--color-text-inverse);
  font-size: 14px;
  opacity: 0.85;
}

@keyframes banner-breathe {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.93; }
}

/* ============================
   Welcome Section
   ============================ */
.welcome-section {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: 16px;
  padding: 24px 32px;
  margin-bottom: 24px;
  box-shadow: var(--elevation-1);
  text-align: center;
}

.welcome-title {
  font-size: 24px;
  font-weight: 700;
  text-align: center;
  margin-bottom: 8px;
  color: var(--color-text-primary);
}

.welcome-subtitle {
  font-size: 14px;
  color: var(--color-text-secondary);
  text-align: center;
  margin: 0;
  font-variant-numeric: tabular-nums;
}

/* ============================
   Stats Cards
   ============================ */
.stats-section {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.stats-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: 16px;
  padding: 24px;
  box-shadow: var(--elevation-1);
  transition: box-shadow 0.2s ease;
  display: flex;
  align-items: center;
  gap: 20px;
}

.stats-card.has-pending {
  border-color: var(--color-accent);
}

.stats-card:hover {
  box-shadow: var(--elevation-2);
}

/* Stats icon — soft tinted background */
.stats-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  justify-content: center;
  align-items: center;
  flex-shrink: 0;
}

.patient-icon {
  background: var(--color-primary-light);
}

.alarm-icon {
  background: var(--color-accent-light);
}

.device-icon {
  background: var(--color-success-bg);
}

/* Icon symbols via pseudo-elements */
.patient-icon::after {
  content: '👤';
  font-size: 24px;
}

.alarm-icon::after {
  content: '🔔';
  font-size: 24px;
}

.device-icon::after {
  content: '📱';
  font-size: 24px;
}

.stats-content { flex: 1; }

.stats-title {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 12px;
  color: var(--color-text-secondary);
}

.stats-values {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.stats-item { text-align: center; flex: 1; }

.stats-number {
  display: block;
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 4px;
  color: var(--color-text-primary);
  font-variant-numeric: tabular-nums;
}

.stats-number.online { color: var(--color-success); }
.stats-number.offline { color: var(--color-text-muted); }
.stats-number.pending { color: var(--color-warning); }
.stats-number.handled { color: var(--color-success); }

.stats-label {
  font-size: 14px;
  color: var(--color-text-muted);
}

/* ============================
   Content Cards
   ============================ */
.content-section {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(480px, 1fr));
  gap: 20px;
}

.content-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: 16px;
  padding: 24px;
  box-shadow: var(--elevation-1);
  transition: box-shadow 0.2s ease;
}

.content-card:hover {
  box-shadow: var(--elevation-2);
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 20px;
  color: var(--color-text-primary);
  display: flex;
  align-items: center;
  gap: 10px;
}

.card-title::before {
  content: '';
  width: 4px;
  height: 18px;
  background: var(--color-primary);
  border-radius: 2px;
}

.alarm-badge { margin-left: auto; }

/* ============================
   Map
   ============================ */
.map-container {
  position: relative;
  width: 100%;
  height: 350px;
  border-radius: 12px;
  overflow: hidden;
}

.map-tools {
  position: absolute;
  top: 12px;
  left: 12px;
  z-index: 1000;
  display: flex;
  gap: 4px;
  background: var(--color-surface);
  padding: 4px;
  border-radius: 24px;
  box-shadow: var(--elevation-2);
}

/* Override Element Plus button styles for pill toolbar */
:deep(.map-tool-btn) {
  min-width: 36px !important;
  height: 36px !important;
  padding: 0 14px !important;
  background: var(--color-surface) !important;
  border: none !important;
  border-radius: 20px !important;
  color: var(--color-text-secondary) !important;
  font-size: 14px !important;
  font-family: var(--font-sans) !important;
  box-shadow: none !important;
  transition: all 0.15s ease !important;
}

:deep(.map-tool-btn:hover) {
  background: var(--color-surface-raised) !important;
  color: var(--color-primary) !important;
}

.map {
  width: 100%;
  height: 100%;
  background-color: var(--color-surface-raised);
}

/* Leaflet custom marker reset */
:deep(.custom-marker) {
  background: none !important;
  border: none !important;
}

:deep(.marker-patient) {
  background: var(--color-primary);
  border-radius: 50% 50% 50% 0;
  transform: rotate(-45deg);
  width: 36px;
  height: 36px;
  display: flex;
  justify-content: center;
  align-items: center;
  box-shadow: 0 2px 6px rgba(37, 99, 235, 0.3);
}

:deep(.marker-patient span) {
  transform: rotate(45deg);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  max-width: 30px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ============================
   Alarm List
   ============================ */
.alarm-list { max-height: 350px; overflow-y: auto; }

.alarm-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 12px 12px 16px;
  border-bottom: 1px solid var(--color-border);
  border-left: 4px solid transparent;
  transition: background-color 0.15s ease;
}

.alarm-item:hover {
  background-color: var(--color-surface-raised);
}

.alarm-item:last-child { border-bottom: none; }

/* Left color stripe by severity */
.alarm-item.critical {
  border-left-color: var(--color-danger);
}

.alarm-item.warning {
  border-left-color: var(--color-warning);
}

/* Unhandled alarm highlight */
.alarm-item.alarm-unhandled {
  background-color: var(--color-danger-bg);
}

.alarm-time {
  font-size: 12px;
  color: var(--color-text-muted);
  min-width: 70px;
  font-variant-numeric: tabular-nums;
}

.alarm-content { flex: 1; }

.alarm-type {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 4px;
  padding: 2px 10px;
  border-radius: 12px;
  display: inline-block;
}

.alarm-type.critical {
  background-color: var(--color-danger-bg);
  color: var(--color-danger);
}

.alarm-type.warning {
  background-color: var(--color-warning-bg);
  color: var(--color-warning);
}

.alarm-patient {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-primary);
  margin-bottom: 2px;
}

.alarm-ward {
  font-size: 14px;
  color: var(--color-text-muted);
  margin-left: 8px;
}

.alarm-location {
  font-size: 14px;
  color: var(--color-text-muted);
}

/* ============================
   Patient / Device List
   ============================ */
.device-list { max-height: 350px; overflow-y: auto; }

.device-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 12px;
  border-bottom: 1px solid var(--color-border);
  transition: background-color 0.15s ease;
}

.device-item:hover {
  background-color: var(--color-surface-raised);
}

.device-item:last-child { border-bottom: none; }

.device-status {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.device-status.online {
  background-color: var(--color-success);
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.15);
  animation: status-pulse 2.5s ease-in-out infinite;
}

.device-status.offline {
  background-color: var(--color-text-muted);
}

@keyframes status-pulse {
  0%, 100% { box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.15); }
  50% { box-shadow: 0 0 0 5px rgba(16, 185, 129, 0.06); }
}

.device-info { flex: 1; }

.device-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-primary);
  margin-bottom: 2px;
}

.device-id {
  font-size: 14px;
  color: var(--color-text-muted);
}

.device-battery {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 80px;
}

/* Battery progress bar — green→yellow→red gradient */
.device-battery :deep(.el-progress-bar__outer) {
  border-radius: 4px;
  background-color: var(--color-surface-raised);
}

.device-battery :deep(.el-progress-bar__inner) {
  background: linear-gradient(90deg, #EF4444 0%, #F59E0B 45%, #10B981 100%) !important;
  border-radius: 4px !important;
}

.battery-text {
  font-size: 12px;
  color: var(--color-text-muted);
  min-width: 32px;
  font-variant-numeric: tabular-nums;
}

/* ============================
   Ward Overview
   ============================ */
.ward-list { max-height: 350px; overflow-y: auto; }

.ward-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  border: 1px solid var(--color-border);
  border-radius: 12px;
  margin-bottom: 8px;
  transition: background-color 0.15s ease;
}

.ward-item:hover {
  background-color: var(--color-surface-raised);
}

.ward-item:last-child { margin-bottom: 0; }

.ward-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-primary);
}

.ward-stats { display: flex; gap: 12px; }

.ward-stat {
  font-size: 14px;
  color: var(--color-text-muted);
}

.ward-stat.online-stat { color: var(--color-success); }

.ward-stat.alarm-stat {
  color: var(--color-danger);
  font-weight: 600;
}

/* ============================
   Responsive
   ============================ */
@media (max-width: 1200px) {
  .content-section { grid-template-columns: 1fr; }
}

@media (max-width: 768px) {
  .dashboard-container { padding: 12px; }
  .welcome-section { padding: 20px; }
  .welcome-title { font-size: 20px; }
  .stats-section { grid-template-columns: 1fr; }
  .stats-card { padding: 20px; }
  .content-card { padding: 20px; }
  .map-container { height: 300px; }
  .content-section { grid-template-columns: 1fr; }
}
</style>
