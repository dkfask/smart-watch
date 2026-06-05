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

const markerPalette = [
  '#2563EB',
  '#059669',
  '#EA580C',
  '#7C3AED',
  '#DC2626',
  '#0891B2',
  '#CA8A04',
  '#DB2777'
]

const CLUSTER_RADIUS_METERS = 35

const hashMarkerSeed = (value) => {
  const text = String(value || '')
  let hash = 0
  for (let i = 0; i < text.length; i++) {
    hash = ((hash << 5) - hash) + text.charCodeAt(i)
    hash |= 0
  }
  return Math.abs(hash)
}

const getPatientMarkerVisual = (device) => {
  const seed = hashMarkerSeed(device.patient?.id || device.imei || device.id)
  const patientName = device.patient?.name || ''
  const shortImei = String(device.imei || device.id || '').slice(-4)

  return {
    color: markerPalette[seed % markerPalette.length],
    initial: (patientName || shortImei || '?').charAt(0),
    label: device.patient?.bedNumber || device.patient?.bed || shortImei || '设备'
  }
}

const escapeHtml = (value) => String(value ?? '')
  .replace(/&/g, '&amp;')
  .replace(/</g, '&lt;')
  .replace(/>/g, '&gt;')
  .replace(/"/g, '&quot;')
  .replace(/'/g, '&#039;')

const getBedText = (patient) => patient?.bedNumber || patient?.bed || ''

const getPatientLocationSummary = (entry) => {
  const patient = entry.device.patient || {}
  const ward = patient.ward || ''
  const bed = getBedText(patient)
  return [ward, bed ? `${bed}床` : ''].filter(Boolean).join(' ')
}

const buildSinglePopup = (entry) => {
  const patient = entry.device.patient || {}
  const summary = getPatientLocationSummary(entry) || '未分配病区'
  return `
    <div class="dashboard-map-popup">
      <div class="map-popup-title">${escapeHtml(patient.name || '未知病人')}</div>
      <div class="map-popup-meta">${escapeHtml(summary)}</div>
      <div class="map-popup-row">设备：${escapeHtml(entry.device.imei || entry.device.id || '-')}</div>
      <div class="map-popup-row">位置：${escapeHtml(entry.location.address || '未知')}</div>
    </div>
  `
}

const buildClusterPopup = (entries) => {
  const list = entries.map((entry) => {
    const patient = entry.device.patient || {}
    const summary = getPatientLocationSummary(entry) || '未分配病区'
    return `
      <div class="map-popup-patient">
        <span class="map-popup-dot"></span>
        <div>
          <div class="map-popup-patient-name">${escapeHtml(patient.name || '未知病人')}</div>
          <div class="map-popup-meta">${escapeHtml(summary)} · ${escapeHtml(entry.device.imei || '-')}</div>
        </div>
      </div>
    `
  }).join('')

  return `
    <div class="dashboard-map-popup cluster">
      <div class="map-popup-title">该区域 ${entries.length} 名病人</div>
      <div class="map-popup-meta">${escapeHtml(entries[0]?.location?.address || '点击病人查看详情')}</div>
      <div class="map-popup-list">${list}</div>
    </div>
  `
}

const clusterPatientLocations = (entries) => {
  const clusters = []

  entries.forEach((entry) => {
    const point = L.latLng(entry.location.latitude, entry.location.longitude)
    const existingCluster = clusters.find(cluster => (
      point.distanceTo(L.latLng(cluster.center.latitude, cluster.center.longitude)) <= CLUSTER_RADIUS_METERS
    ))

    if (existingCluster) {
      existingCluster.entries.push(entry)
      const count = existingCluster.entries.length
      existingCluster.center = {
        latitude: existingCluster.entries.reduce((sum, item) => sum + Number(item.location.latitude), 0) / count,
        longitude: existingCluster.entries.reduce((sum, item) => sum + Number(item.location.longitude), 0) / count
      }
    } else {
      clusters.push({
        center: {
          latitude: Number(entry.location.latitude),
          longitude: Number(entry.location.longitude)
        },
        entries: [entry]
      })
    }
  })

  return clusters
}

const hasValidMarkerLocation = (location) => {
  const latitude = Number(location?.latitude)
  const longitude = Number(location?.longitude)
  return (
    Number.isFinite(latitude) &&
    Number.isFinite(longitude) &&
    latitude >= -90 &&
    latitude <= 90 &&
    longitude >= -180 &&
    longitude <= 180 &&
    !(latitude === 0 && longitude === 0)
  )
}

const sleep = (ms) => new Promise(resolve => setTimeout(resolve, ms))

const runLimited = async (items, worker, concurrency = 2, gapMs = 200) => {
  const results = []
  for (let index = 0; index < items.length; index += concurrency) {
    const batch = items.slice(index, index + concurrency)
    const batchResults = await Promise.all(batch.map(worker))
    results.push(...batchResults)
    if (index + concurrency < items.length) {
      await sleep(gapMs)
    }
  }
  return results
}

/**
 * 跳转到报警页面
 */
const goToAlarms = () => {
  router.push('/alarms')
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
    const monitoredPatientIds = new Set(
      allDevices
        .map(device => device.patient?.id)
        .filter(Boolean)
    )
    const monitoredCount = allPatients.filter(p => p.deviceId || monitoredPatientIds.has(p.id)).length
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

  const targetDevices = devices.filter(device => device.isOnline && device.patient)
  const locationEntries = (await runLimited(targetDevices, async (device) => {
    const fallbackLocation = {
      latitude: device.lastLatitude,
      longitude: device.lastLongitude,
      time: device.lastLocationTime,
      batteryLevel: device.batteryLevel,
      source: 'device-status'
    }

    try {
      const location = await locationApi.getLatestLocation(device.id)
      const candidate = hasValidMarkerLocation(location) ? location : fallbackLocation
      return hasValidMarkerLocation(candidate) ? { device, location: candidate } : null
    } catch (e) {
      return hasValidMarkerLocation(fallbackLocation) ? { device, location: fallbackLocation } : null
    }
  }, 2, 200)).filter(Boolean)

  const clusters = clusterPatientLocations(locationEntries)

  clusters.forEach((cluster) => {
    if (cluster.entries.length === 1) {
      const entry = cluster.entries[0]
      const { device, location } = entry
      const markerVisual = getPatientMarkerVisual(device)
      const customIcon = L.divIcon({
        className: 'custom-marker',
        html: `
          <div class="care-marker single" style="--marker-color: ${markerVisual.color};">
            <div class="care-marker-pin">
              <span class="care-marker-initial">${markerVisual.initial}</span>
            </div>
            <div class="care-marker-label">${escapeHtml(device.patient?.name || markerVisual.label)}</div>
          </div>
        `,
        iconSize: [96, 68],
        iconAnchor: [48, 56]
      })
      const marker = L.marker([location.latitude, location.longitude], { icon: customIcon })
        .bindPopup(buildSinglePopup(entry))
        .addTo(map)
      markers.push(marker)
      return
    }

    const customIcon = L.divIcon({
      className: 'custom-marker',
      html: `
        <div class="care-cluster">
          <div class="care-cluster-ring"></div>
          <div class="care-cluster-count">${cluster.entries.length}</div>
          <div class="care-cluster-label">名病人</div>
        </div>
      `,
      iconSize: [76, 76],
      iconAnchor: [38, 38]
    })
    const marker = L.marker([cluster.center.latitude, cluster.center.longitude], { icon: customIcon })
      .bindPopup(buildClusterPopup(cluster.entries), { maxWidth: 320 })
      .addTo(map)
    markers.push(marker)
  })

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

<style>
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
  width: 100% !important;
  padding: var(--space-6);
  font-family: var(--font-sans);
  font-size: 14px;
  color: var(--color-text-primary);
  /* 背景色由 App.vue 的 #app 容器统一设置，避免双层背景 */
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
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.content-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: 16px;
  padding: 24px;
  box-shadow: var(--elevation-1);
  min-height: 400px;
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
  right: 12px;
  z-index: 1000;
  display: flex;
  background: rgba(255, 255, 255, 0.94);
  padding: 3px;
  border-radius: 24px;
  box-shadow: var(--elevation-2);
  backdrop-filter: blur(8px);
}

/* Override Element Plus button styles for pill toolbar */
:deep(.map-tool-btn) {
  height: 30px !important;
  padding: 0 13px !important;
  background: transparent !important;
  border: 1px solid rgba(148, 163, 184, 0.28) !important;
  border-radius: 999px !important;
  color: #475569 !important;
  font-size: 14px !important;
  font-weight: 700 !important;
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

/* High-density patient map: single pins for isolated patients, clusters for crowded areas. */
:deep(.custom-marker) {
  background: none !important;
  border: none !important;
}

:deep(.care-marker) {
  position: relative;
  width: 96px;
  height: 68px;
  pointer-events: auto;
}

:deep(.care-marker-pin) {
  position: absolute;
  left: 32px;
  top: 0;
  width: 34px;
  height: 34px;
  background: var(--marker-color, var(--color-primary));
  border-radius: 50% 50% 50% 0;
  transform: rotate(-45deg);
  display: flex;
  justify-content: center;
  align-items: center;
  border: 3px solid #fff;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.28);
}

:deep(.care-marker-pin::after) {
  content: '';
  position: absolute;
  inset: 5px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.6);
}

:deep(.care-marker-initial) {
  transform: rotate(45deg);
  color: #fff;
  font-size: 13px;
  font-weight: 800;
  line-height: 1;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
  z-index: 1;
}

:deep(.care-marker-label) {
  position: absolute;
  left: 50%;
  top: 36px;
  transform: translateX(-50%);
  max-width: 76px;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid color-mix(in srgb, var(--marker-color, var(--color-primary)) 45%, #fff);
  color: #0f172a;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.16);
  font-size: 11px;
  font-weight: 700;
  line-height: 16px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

:deep(.care-cluster) {
  position: relative;
  width: 76px;
  height: 76px;
  border-radius: 24px;
  background: linear-gradient(145deg, #2563eb 0%, #0f766e 100%);
  border: 4px solid rgba(255, 255, 255, 0.96);
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.34), 0 0 0 8px rgba(37, 99, 235, 0.16);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #fff;
  isolation: isolate;
}

:deep(.care-cluster-ring) {
  position: absolute;
  inset: -10px;
  border-radius: 28px;
  border: 2px solid rgba(37, 99, 235, 0.26);
  background: rgba(37, 99, 235, 0.08);
  z-index: -1;
}

:deep(.care-cluster-count) {
  font-size: 24px;
  font-weight: 900;
  line-height: 1;
  letter-spacing: -0.04em;
  font-variant-numeric: tabular-nums;
}

:deep(.care-cluster-label) {
  margin-top: 3px;
  font-size: 11px;
  font-weight: 800;
  opacity: 0.9;
}

:deep(.leaflet-popup-content-wrapper) {
  border-radius: 16px;
  box-shadow: 0 18px 45px rgba(15, 23, 42, 0.22);
}

:deep(.leaflet-popup-content) {
  margin: 14px 16px;
}

:deep(.dashboard-map-popup) {
  min-width: 210px;
  color: #0f172a;
  font-family: var(--font-sans);
}

:deep(.map-popup-title) {
  font-size: 15px;
  font-weight: 900;
  margin-bottom: 5px;
}

:deep(.map-popup-meta),
:deep(.map-popup-row) {
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
}

:deep(.map-popup-list) {
  max-height: 220px;
  overflow-y: auto;
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

:deep(.map-popup-patient) {
  display: grid;
  grid-template-columns: 10px 1fr;
  gap: 8px;
  align-items: start;
  padding: 8px 10px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

:deep(.map-popup-dot) {
  width: 8px;
  height: 8px;
  margin-top: 5px;
  border-radius: 999px;
  background: #2563eb;
  box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.12);
}

:deep(.map-popup-patient-name) {
  font-size: 13px;
  font-weight: 800;
  color: #0f172a;
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
@media (max-width: 1024px) {
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

@media (max-width: 480px) {
  .stats-values { flex-direction: column; gap: 16px; }
  .stats-number { font-size: 24px; }
  .welcome-title { font-size: 18px; }
  .map-container { height: 250px; }
}
</style>
