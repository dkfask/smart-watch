<template>
  <div class="device-detail-container">
    <el-card shadow="hover" v-loading="loading">
      <template #header>
        <div class="card-header">
          <h2>设备详情</h2>
          <el-button type="primary" @click="goBack">返回列表</el-button>
        </div>
      </template>
      <div class="device-detail-content">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-descriptions title="设备基本信息" :column="2" border>
              <el-descriptions-item label="ID">{{ device.id }}</el-descriptions-item>
              <el-descriptions-item label="IMEI">{{ device.imei }}</el-descriptions-item>
              <el-descriptions-item label="MCC">{{ device.mcc || '-' }}</el-descriptions-item>
              <el-descriptions-item label="MNC">{{ device.mnc || '-' }}</el-descriptions-item>
              <el-descriptions-item label="APN">{{ device.apn || '-' }}</el-descriptions-item>
              <el-descriptions-item label="ICCID">{{ device.iccid || '-' }}</el-descriptions-item>
              <el-descriptions-item label="IMSI">{{ device.imsi || '-' }}</el-descriptions-item>
              <el-descriptions-item label="创建时间">{{ formatDate(device.createdAt) }}</el-descriptions-item>
            </el-descriptions>
          </el-col>
          <el-col :span="12">
            <el-descriptions title="病人信息" :column="1" border>
              <el-descriptions-item label="姓名">{{ patient?.name || '未关联' }}</el-descriptions-item>
              <el-descriptions-item label="年龄">{{ patient?.age || '-' }}</el-descriptions-item>
              <el-descriptions-item label="性别">{{ patient?.gender === 'male' ? '男' : patient?.gender === 'female' ? '女' : '-' }}</el-descriptions-item>
              <el-descriptions-item label="病房">{{ patient?.ward || '-' }}</el-descriptions-item>
              <el-descriptions-item label="床位">{{ patient?.bed || '-' }}</el-descriptions-item>
              <el-descriptions-item label="关联时间">{{ formatDate(patient?.assignedAt) }}</el-descriptions-item>
            </el-descriptions>
          </el-col>
        </el-row>
        <el-row :gutter="20" style="margin-top: 20px;">
          <el-col :span="24">
            <el-descriptions title="最新位置信息" :column="4" border>
              <el-descriptions-item label="时间">{{ formatDate(latestLocation?.time) }}</el-descriptions-item>
              <el-descriptions-item label="经度">{{ latestLocation?.longitude || '-' }}</el-descriptions-item>
              <el-descriptions-item label="纬度">{{ latestLocation?.latitude || '-' }}</el-descriptions-item>
              <el-descriptions-item label="地址">{{ latestLocation?.address || '-' }}</el-descriptions-item>
              <el-descriptions-item label="精度">{{ latestLocation?.accuracy || '-' }} 米</el-descriptions-item>
              <el-descriptions-item label="海拔">{{ latestLocation?.altitude || '-' }} 米</el-descriptions-item>
              <el-descriptions-item label="电池电量">{{ latestLocation?.batteryLevel ?? '-' }}%</el-descriptions-item>
              <el-descriptions-item label="定位来源">{{ latestLocation?.source || '-' }}</el-descriptions-item>
            </el-descriptions>
          </el-col>
        </el-row>
        <div class="map-section">
          <h3>设备位置</h3>
          <div id="device-map" class="device-map"></div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { deviceApi } from '../api/device'
import { locationApi } from '../api/location'
import { patientApi } from '../api/patient'
import { ElMessage } from 'element-plus'
import { formatBeijingTime } from '../utils/time'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

const route = useRoute()
const router = useRouter()
const deviceId = ref(route.params.id)
const device = ref({})
const patient = ref(null)
const latestLocation = ref(null)
const loading = ref(false)
let map = null

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return ''
  return formatBeijingTime(dateString)
}

// 获取设备详情
const fetchDeviceDetail = async () => {
  loading.value = true
  try {
    const deviceData = await deviceApi.getDevice(deviceId.value)
    device.value = deviceData
    await Promise.all([fetchPatientInfo(), fetchLatestLocation()])
  } catch (error) {
    ElMessage.error('获取设备详情失败')
    console.error('Failed to fetch device detail:', error)
  } finally {
    loading.value = false
  }
}

// 获取设备关联的病人信息
const fetchPatientInfo = async () => {
  try {
    const patientData = await patientApi.getPatientByDeviceId(deviceId.value)
    patient.value = patientData
  } catch (error) {
    console.error('Failed to fetch patient info:', error)
    // 病人信息获取失败不影响设备详情展示
    patient.value = null
  }
}

// 获取设备最新位置
const fetchLatestLocation = async () => {
  try {
    const locationData = await locationApi.getLatestLocationWithAmap(deviceId.value)
    latestLocation.value = locationData
    initMap()
  } catch (error) {
    console.error('Failed to fetch latest location:', error)
    // 位置获取失败不影响设备详情展示
  }
}

// 初始化地图
const initMap = () => {
  if (!latestLocation.value || !latestLocation.value.latitude || !latestLocation.value.longitude) {
    return
  }

  // 销毁现有地图实例
  if (map) {
    map.remove()
  }

  // 创建新地图实例
  map = L.map('device-map').setView([latestLocation.value.latitude, latestLocation.value.longitude], 15)

  // 添加瓦片图层（使用高德地图作为备用）
  L.tileLayer('https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}', {
    subdomains: ['1', '2', '3', '4'],
    attribution: '© 高德地图'
  }).addTo(map)

  // 创建自定义水滴图标
  const customIcon = L.divIcon({
    className: 'custom-marker',
    html: `
      <div class="marker-drop">
        <div class="marker-number">${device.value.id}</div>
      </div>
    `,
    iconSize: [30, 30],
    iconAnchor: [15, 30]
  });

  // 添加标记
  L.marker([latestLocation.value.latitude, latestLocation.value.longitude], { icon: customIcon })
    .addTo(map)
    .bindPopup(`
      <b>设备: ${device.value.imei}</b><br>
      位置: ${latestLocation.value.address || '未知'}<br>
      时间: ${formatDate(latestLocation.value.time)}
    `)
    .openPopup()
}

// 返回列表
const goBack = () => {
  router.push('/devices')
}

// 定时刷新位置
let refreshTimer = null

onMounted(() => {
  fetchDeviceDetail()
  // 每30秒刷新一次位置
  refreshTimer = setInterval(fetchLatestLocation, 30000)
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
.device-detail-container {
  width: 100%;
  padding: 20px;
  background: transparent;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.device-detail-content {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 20px;
  backdrop-filter: blur(10px);
}

.map-section {
  margin-top: 20px;
}

.map-section h3 {
  margin-bottom: 10px;
  font-size: 16px;
  font-weight: bold;
  color: var(--text-primary);
}

.device-map {
  height: 400px;
  border-radius: var(--radius-md);
  overflow: hidden;
  border: 1px solid var(--border-color);
}

/* 自定义水滴标记样式 */
:deep(.custom-marker) {
  display: flex;
  justify-content: center;
  align-items: center;
}

:deep(.marker-drop) {
  position: relative;
  width: 30px;
  height: 30px;
  background: var(--color-primary);
  border-radius: 50% 50% 50% 0;
  transform: rotate(-45deg);
  display: flex;
  justify-content: center;
  align-items: center;
  box-shadow: var(--shadow-sm);
}

:deep(.marker-number) {
  position: absolute;
  bottom: -12px;
  right: -12px;
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
