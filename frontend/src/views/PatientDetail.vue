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
import { patientApi } from '../api/patient'
import { deviceApi } from '../api/device'
import { alarmApi } from '../api/alarm'
import { locationApi } from '../api/location'
import { fenceApi } from '../api/fence'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()

const patient = ref(null)
const device = ref(null)
const latestLocation = ref(null)
const patientAlarms = ref([])
const patientFences = ref([])
const loading = ref(true)
let map = null
let marker = null
let refreshTimer = null

/**
 * 未处理报警数
 */
const pendingAlarmCount = computed(() => patientAlarms.value.filter(a => a.status === 'pending').length)

/**
 * 返回病人列表
 */
const goBack = () => {
  router.push('/patients')
}

const formatDate = (dateString) => {
  if (!dateString) return ''
  return new Date(dateString).toLocaleString()
}

const formatLocationText = (location) => {
  if (!location) return '未知地址'
  if (location.address) return location.address
  if (Number.isFinite(location.latitude) && Number.isFinite(location.longitude)) {
    return `经纬度 ${location.latitude.toFixed(6)}, ${location.longitude.toFixed(6)}`
  }
  return '未知地址'
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

  try {
    const loc = await locationApi.getLatestLocationWithAmap(deviceId)
    latestLocation.value = normalizeLocation(loc)
    await nextTick()
    renderPatientMap()
  } catch (e) {
    // 位置获取失败不影响病人详情展示。
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
    L.tileLayer('https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}', {
      subdomains: ['1', '2', '3', '4'],
      attribution: '© 高德地图'
    }).addTo(map)
  } else {
    map.setView(latLng, 15)
  }

  map.invalidateSize()

  const customIcon = L.divIcon({
    className: 'custom-marker',
    html: `<div class="marker-patient"><span>${patient.value?.name?.charAt(0) || '?'}</span></div>`,
    iconSize: [40, 40],
    iconAnchor: [20, 40]
  })

  if (marker) {
    marker.setLatLng(latLng)
  } else {
    marker = L.marker(latLng, { icon: customIcon }).addTo(map)
  }

  marker
    .bindPopup(`<b>${patient.value?.name || '未知'}</b><br>${formatLocationText(latestLocation.value)}`)
    .openPopup()
}

onMounted(() => {
  fetchPatientDetail()
  refreshTimer = setInterval(() => fetchLatestLocation(), 30000)
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

:deep(.marker-patient) {
  background: var(--color-primary);
  border-radius: 50% 50% 50% 0;
  transform: rotate(-45deg);
  width: 36px; height: 36px;
  display: flex; justify-content: center; align-items: center;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.2);
}
:deep(.marker-patient span) {
  transform: rotate(45deg);
  color: #fff; font-size: 11px; font-weight: bold;
}
</style>
