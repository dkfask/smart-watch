<template>
  <div class="patient-detail-container">
    <div class="page-header">
      <el-button @click="goBack" text>
        ← 返回病人列表
      </el-button>
      <h2 v-if="patient">{{ patient.name }} 的详细信息</h2>
    </div>

    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="8" animated />
    </div>

    <div v-else-if="patient" class="detail-content">
      <el-row :gutter="20">
        <!-- 左侧：基本信息 -->
        <el-col :span="8">
          <div class="info-card">
            <h3 class="card-title">基本信息</h3>
            <div class="patient-avatar">
              <div class="avatar-circle" :class="patient.gender === 'male' ? 'male' : 'female'">
                {{ patient.name?.charAt(0) || '?' }}
              </div>
              <div class="avatar-info">
                <div class="avatar-name">{{ patient.name }}</div>
                <div class="avatar-meta">
                  <span>{{ patient.gender === 'male' ? '男' : '女' }}</span>
                  <span v-if="patient.age">{{ patient.age }}岁</span>
                </div>
              </div>
            </div>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="病房">{{ patient.ward || '-' }}</el-descriptions-item>
              <el-descriptions-item label="床位">{{ patient.bedNumber || patient.bed || '-' }}</el-descriptions-item>
              <el-descriptions-item label="身份证">{{ patient.idCard || '-' }}</el-descriptions-item>
              <el-descriptions-item label="关联设备">
                <span v-if="device">{{ device.imei }}</span>
                <el-button v-else type="primary" size="small" @click="showAssignDevice = true">关联设备</el-button>
              </el-descriptions-item>
              <el-descriptions-item label="设备状态">
                <el-tag v-if="device" :type="device.isOnline ? 'success' : 'info'" size="small">
                  {{ device.isOnline ? '在线' : '离线' }}
                </el-tag>
                <span v-else>-</span>
              </el-descriptions-item>
            </el-descriptions>
          </div>

          <!-- 关联围栏 -->
          <div class="info-card">
            <h3 class="card-title">关联围栏</h3>
            <el-empty v-if="patientFences.length === 0" description="暂无关联围栏" :image-size="60" />
            <div v-else class="fence-list">
              <div v-for="fence in patientFences" :key="fence.id" class="fence-item">
                <div class="fence-name">{{ fence.name }}</div>
                <el-tag :type="fence.status === 'active' ? 'success' : 'info'" size="small">
                  {{ fence.status === 'active' ? '启用' : '停用' }}
                </el-tag>
              </div>
            </div>
          </div>
        </el-col>

        <!-- 右侧：位置+报警+健康 -->
        <el-col :span="16">
          <!-- 最新位置 -->
          <div class="info-card">
            <h3 class="card-title">最新位置</h3>
            <div v-if="latestLocation" class="location-info">
              <div class="location-detail">
                <span class="location-address">{{ formatLocationText(latestLocation) }}</span>
                <span class="location-time">更新于 {{ formatDate(latestLocation.time || latestLocation.createdAt) }}</span>
              </div>
              <div id="patient-map" class="patient-map"></div>
            </div>
            <el-empty v-else description="暂无位置数据" :image-size="60" />
          </div>

          <div class="info-card">
            <h3 class="card-title">健康信息</h3>
            <div v-loading="healthLoading" class="health-summary-grid">
              <div
                v-for="metric in healthMetrics"
                :key="metric.key"
                class="health-metric-card"
                :class="metric.status"
              >
                <div class="metric-head">
                  <span class="metric-label">{{ metric.label }}</span>
                  <span class="metric-status">{{ metric.statusText }}</span>
                </div>
                <div class="metric-value">
                  <span>{{ metric.value }}</span>
                  <small>{{ metric.unit }}</small>
                </div>
                <div class="metric-time">{{ metric.time }}</div>
              </div>
            </div>
          </div>

          <!-- 最近报警 -->
          <div class="info-card">
            <h3 class="card-title">
              最近报警
              <el-badge v-if="pendingAlarmCount > 0" :value="pendingAlarmCount" class="alarm-badge" />
            </h3>
            <el-empty v-if="patientAlarms.length === 0" description="暂无报警记录" :image-size="60" />
            <el-table v-else :data="patientAlarms" size="small" style="width: 100%">
              <el-table-column label="类型" width="110">
                <template #default="scope">
                  <el-tag :type="getAlarmTypeColor(scope.row)" size="small">
                    {{ getAlarmTypeName(scope.row) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="时间" width="160">
                <template #default="scope">
                  {{ formatDate(scope.row.triggeredTime) }}
                </template>
              </el-table-column>
              <el-table-column label="位置" min-width="120">
                <template #default="scope">
                  {{ scope.row.address || '-' }}
                </template>
              </el-table-column>
              <el-table-column label="状态" width="100">
                <template #default="scope">
                  <el-tag :type="scope.row.status === 'pending' ? 'danger' : 'success'" size="small">
                    {{ scope.row.status === 'pending' ? '未处理' : '已处理' }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-col>
      </el-row>
    </div>

    <el-empty v-else description="未找到病人信息" />
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { addTiandituToMap } from '../utils/tianditu'
import { patientApi } from '../api/patient'
import { deviceApi } from '../api/device'
import { alarmApi } from '../api/alarm'
import { locationApi } from '../api/location'
import { fenceApi } from '../api/fence'
import { healthApi } from '../api/health'
import { ElMessage } from 'element-plus'
import { formatBeijingTime } from '../utils/time'

const route = useRoute()
const router = useRouter()

const patient = ref(null)
const device = ref(null)
const latestLocation = ref(null)
const patientAlarms = ref([])
const patientFences = ref([])
const latestHealthData = ref({})
const healthLoading = ref(false)
const loading = ref(true)
let map = null
let marker = null
let refreshTimer = null

/**
 * 未处理报警数
 */
const pendingAlarmCount = computed(() => patientAlarms.value.filter(a => a.status === 'pending').length)

const healthMetricConfig = [
  { key: 'temperature', label: '体温', unit: '°C', normal: value => value >= 35.5 && value <= 37.5 },
  { key: 'heart_rate', label: '心率', unit: '次/分', normal: value => value >= 50 && value <= 110 },
  { key: 'blood_pressure', label: '血压', unit: 'mmHg', normal: value => isNormalBloodPressure(value) },
  { key: 'spo2', label: '血氧', unit: '%', normal: value => Number(value) >= 95 }
]

const healthMetrics = computed(() => healthMetricConfig.map(config => {
  const entry = latestHealthData.value?.[config.key]
  const rawValue = entry?.value
  const hasValue = rawValue !== undefined && rawValue !== null && rawValue !== ''
  const status = hasValue && !config.normal(rawValue) ? 'warning' : 'normal'
  return {
    ...config,
    value: hasValue ? rawValue : '-',
    time: entry?.time ? formatDate(entry.time) : '暂无数据',
    status: hasValue ? status : 'empty',
    statusText: hasValue ? (status === 'warning' ? '需关注' : '正常') : '未上报'
  }
}))

function isNormalBloodPressure(value) {
  const match = String(value || '').match(/(\d{2,3})\D+(\d{2,3})/)
  if (!match) return true
  const systolic = Number(match[1])
  const diastolic = Number(match[2])
  return systolic >= 90 && systolic <= 140 && diastolic >= 60 && diastolic <= 90
}

/**
 * 返回病人列表
 */
const goBack = () => {
  router.push('/patients')
}

const formatDate = (dateString) => {
  if (!dateString) return ''
  return formatBeijingTime(dateString)
}

const formatLocationText = (location) => {
  if (!location) return '未知地址'
  if (location.address) return location.address
  if (Number.isFinite(location.latitude) && Number.isFinite(location.longitude)) {
    return `经纬度 ${location.latitude.toFixed(6)}, ${location.longitude.toFixed(6)}`
  }
  return '未知地址'
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

const hashMarkerSeed = (value) => {
  const text = String(value || '')
  let hash = 0
  for (let i = 0; i < text.length; i++) {
    hash = ((hash << 5) - hash) + text.charCodeAt(i)
    hash |= 0
  }
  return Math.abs(hash)
}

const getPatientMarkerVisual = () => {
  const seed = hashMarkerSeed(patient.value?.id || device.value?.imei || device.value?.id)
  const shortImei = String(device.value?.imei || device.value?.id || '').slice(-4)
  const patientName = patient.value?.name || ''
  return {
    color: markerPalette[seed % markerPalette.length],
    initial: (patientName || shortImei || '?').charAt(0),
    label: patient.value?.bedNumber || patient.value?.bed || shortImei || '设备',
    offsetX: 0,
    offsetY: 0
  }
}

const getAlarmTypeName = (alarm) => {
  const type = alarm.alertType || alarm.alarmType
  const typeMap = {
    'fence_breach': '围栏越界', 'low_battery': '低电量', 'sos': 'SOS报警',
    'fall': '跌倒报警', 'heart_rate_abnormal': '心率异常',
    'blood_pressure_abnormal': '血压异常', 'blood_oxygen_abnormal': '血氧异常'
  }
  return typeMap[type] || '未知类型'
}

const getAlarmTypeColor = (alarm) => {
  const type = alarm.alertType || alarm.alarmType
  const colorMap = {
    'fence_breach': 'danger', 'low_battery': 'warning', 'sos': 'danger',
    'fall': 'danger', 'heart_rate_abnormal': 'danger',
    'blood_pressure_abnormal': 'danger', 'blood_oxygen_abnormal': 'danger'
  }
  return colorMap[type] || 'info'
}

/**
 * 加载病人详情数据
 */
const fetchPatientDetail = async () => {
  const patientId = route.params.id
  if (!patientId) return

  loading.value = true
  try {
    const data = await patientApi.getPatient(patientId)
    patient.value = data

    const devices = await patientApi.getPatientDevices(patientId)
    if (devices && devices.length > 0) {
      const deviceId = devices[0].deviceId
      try {
        const deviceData = await deviceApi.getDevices(100, 0)
        const deviceList = deviceData?.content || deviceData || []
        device.value = deviceList.find(d => d.id === deviceId)
      } catch (e) { /* 忽略 */ }

      await fetchLatestLocation(deviceId)
    }

    try {
      const result = await alarmApi.getAlarms(20, 0)
      const allAlarms = result?.content || result || []
      patientAlarms.value = allAlarms.filter(a => a.patientId == patientId || a.patient?.id == patientId)
    } catch (e) { /* 忽略 */ }

    try {
      const fences = await fenceApi.getFences()
      const allFences = Array.isArray(fences) ? fences : (fences?.content || [])
      patientFences.value = allFences.filter(f =>
        f.patientIds?.includes(Number(patientId)) || f.patientId == patientId
      )
    } catch (e) { /* 忽略 */ }

    await fetchLatestHealthData(patientId)
  } catch (error) {
    ElMessage.error('获取病人信息失败')
  } finally {
    loading.value = false
    await nextTick()
    renderPatientMap()
  }
}

/**
 * 获取最新位置
 */
const fetchLatestLocation = async (deviceId = device.value?.id) => {
  if (!deviceId) return

  const fallbackLocation = normalizeLocation({
    latitude: device.value?.lastLatitude,
    longitude: device.value?.lastLongitude,
    address: device.value?.lastLocationAddress,
    time: device.value?.lastLocationTime,
    batteryLevel: device.value?.batteryLevel,
    source: 'device-status'
  })

  try {
    const loc = await locationApi.getLatestLocationWithAddress(deviceId)
    latestLocation.value = normalizeLocation(loc) || fallbackLocation
    await nextTick()
    renderPatientMap()
  } catch (e) {
    // 没有 location_records 时，使用设备状态中的最后位置继续展示详情。
    latestLocation.value = fallbackLocation
    await nextTick()
    renderPatientMap()
  }
}

const fetchLatestHealthData = async (patientId = route.params.id) => {
  if (!patientId) return
  healthLoading.value = true
  try {
    latestHealthData.value = await healthApi.getLatestHealthRecords(patientId)
  } catch (error) {
    latestHealthData.value = {}
    console.error('Failed to fetch latest health data:', error)
  } finally {
    healthLoading.value = false
  }
}

const normalizeLocation = (location) => {
  if (!location) return null
  const latitude = normalizeCoordinate(location.latitude ?? location.lat ?? location.lastLatitude)
  const longitude = normalizeCoordinate(location.longitude ?? location.lng ?? location.lon ?? location.lastLongitude)
  return {
    ...location,
    latitude,
    longitude,
    time: location.time || location.createdAt || location.lastLocationTime || location.recvTime
  }
}

const normalizeCoordinate = (value) => {
  if (value === null || value === undefined || value === '') return null
  const coordinate = Number(value)
  return Number.isFinite(coordinate) ? coordinate : null
}

const hasValidLocation = (location) => {
  return location &&
    Number.isFinite(location.latitude) &&
    Number.isFinite(location.longitude) &&
    location.latitude >= -90 &&
    location.latitude <= 90 &&
    location.longitude >= -180 &&
    location.longitude <= 180 &&
    !(location.latitude === 0 && location.longitude === 0)
}

/**
 * DOM渲染完成后初始化或更新病人位置地图
 */
const renderPatientMap = () => {
  if (!hasValidLocation(latestLocation.value)) return

  const el = document.getElementById('patient-map')
  if (!el) return

  const lat = latestLocation.value.latitude
  const lng = latestLocation.value.longitude
  const latLng = [lat, lng]

  if (!map) {
    map = L.map('patient-map').setView(latLng, 15)
    addTiandituToMap(map)
  } else {
    map.setView(latLng, 15)
  }

  map.invalidateSize()

  const markerVisual = getPatientMarkerVisual()
  const customIcon = L.divIcon({
    className: 'custom-marker',
    html: `
      <div class="care-marker" style="--marker-color: ${markerVisual.color}; --marker-x: ${markerVisual.offsetX}px; --marker-y: ${markerVisual.offsetY}px;">
        <div class="care-marker-pin">
          <span class="care-marker-initial">${markerVisual.initial}</span>
        </div>
        <div class="care-marker-label">${markerVisual.label}</div>
      </div>
    `,
    iconSize: [96, 64],
    iconAnchor: [48, 54]
  })

  if (marker) {
    marker.setLatLng(latLng)
    marker.setIcon(customIcon)
  } else {
    marker = L.marker(latLng, { icon: customIcon }).addTo(map)
  }

  marker
    .bindPopup(`<b>${patient.value?.name || '未知'}</b><br>${formatLocationText(latestLocation.value)}`)
    .openPopup()
}

onMounted(() => {
  fetchPatientDetail()
  refreshTimer = setInterval(() => {
    fetchLatestLocation()
    fetchLatestHealthData()
  }, 30000)
})

onBeforeUnmount(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
  if (map) {
    map.remove()
    map = null
  }
})
</script>

<style scoped>
.patient-detail-container {
  width: 100%;
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  color: var(--text-primary);
}

.loading-state { padding: 40px; }

.detail-content { width: 100%; }

.info-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: var(--shadow-md);
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-title::before {
  content: '';
  width: 4px;
  height: 16px;
  background: var(--color-primary);
  border-radius: 2px;
}

.alarm-badge { margin-left: auto; }

.patient-avatar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.avatar-circle {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 24px;
  font-weight: bold;
  color: #fff;
}

.avatar-circle.male { background: linear-gradient(135deg, #4ecdc4, #45b7d1); }
.avatar-circle.female { background: linear-gradient(135deg, #ff6b6b, #ee5a6f); }

.avatar-name { font-size: 18px; font-weight: 600; color: var(--text-primary); }
.avatar-meta { font-size: 13px; color: var(--text-muted); display: flex; gap: 8px; }

.fence-list { display: flex; flex-direction: column; gap: 8px; }
.fence-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 8px 12px; border: 1px solid var(--border-color); border-radius: 8px;
}
.fence-name { font-size: 14px; color: var(--text-primary); }

.location-info { margin-bottom: 12px; }
.location-detail { display: flex; justify-content: space-between; margin-bottom: 8px; }
.location-address { font-size: 14px; font-weight: 500; color: var(--text-primary); }
.location-time { font-size: 12px; color: var(--text-muted); }
.patient-map { width: 100%; height: 250px; border-radius: 8px; border: 1px solid var(--border-color); }

.health-summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.health-metric-card {
  min-height: 112px;
  padding: 14px;
  border: 1px solid var(--border-color);
  border-radius: 14px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(248, 251, 255, 0.96));
  box-shadow: 0 8px 22px rgba(15, 23, 42, 0.06);
}

.health-metric-card.warning {
  border-color: rgba(245, 108, 108, 0.35);
  background: linear-gradient(180deg, rgba(255, 245, 245, 0.98), rgba(255, 255, 255, 0.96));
}

.health-metric-card.empty {
  background: rgba(248, 250, 252, 0.86);
}

.metric-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 12px;
}

.metric-label {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary);
}

.metric-status {
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(16, 185, 129, 0.12);
  color: #059669;
  font-size: 11px;
  font-weight: 700;
}

.health-metric-card.warning .metric-status {
  background: rgba(245, 108, 108, 0.12);
  color: #dc2626;
}

.health-metric-card.empty .metric-status {
  background: rgba(148, 163, 184, 0.14);
  color: #64748b;
}

.metric-value {
  display: flex;
  align-items: baseline;
  gap: 5px;
  color: var(--text-primary);
}

.metric-value span {
  font-size: 26px;
  line-height: 1;
  font-weight: 800;
}

.metric-value small {
  font-size: 12px;
  color: var(--text-muted);
}

.metric-time {
  margin-top: 10px;
  font-size: 12px;
  color: var(--text-muted);
}

/* High-density patient marker: color, short label, and stable identity cue. */
:deep(.custom-marker) {
  background: none !important;
  border: none !important;
}

:deep(.care-marker) {
  position: relative;
  width: 96px;
  height: 64px;
  transform: translate(var(--marker-x, 0), var(--marker-y, 0));
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

@media (max-width: 1024px) {
  .health-summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .health-summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
