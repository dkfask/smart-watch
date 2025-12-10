<template>
  <div class="dashboard-container">
    <!-- 欢迎信息区域 -->
    <div class="welcome-section">
      <h1 class="welcome-title">你好 东软, 你的本地时间是 {{ currentTime }}</h1>
      <div class="welcome-content">
        <div class="welcome-left">
          <h2 class="start-title">开始使用</h2>
          <p class="start-description">
            欢迎您加入我们！我们是专业的设备管理系统及设备解决方案的一员。
            为使您更高效使用，请点击以下按钮，我们将为您提供专业的系统使用指导。
            通过这一流程，您将迅速了解如何操作和使用我们的产品。
            如在操作过程中遇到任何问题，欢迎随时联系我们的客服热线或技术支持团队，
            我们将竭诚为您服务。
            <a href="http://localhost:3000/admin/" target="_blank">http://localhost:3000/admin/</a>
          </p>
          <el-button type="primary" size="large" class="start-button">
            让我们开始吧
          </el-button>
        </div>
        <div class="welcome-right">
          <div class="assistant-icon">
            <div class="speech-bubble">...</div>
            <div class="assistant-avatar">
              <div class="avatar-image"></div>
              <div class="avatar-mic"></div>
            </div>
            <div class="avatar-laptop"></div>
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
            <el-button size="small" class="map-tool-btn">+</el-button>
            <el-button size="small" class="map-tool-btn">-</el-button>
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
              <div class="calendar-day" :class="{ 'is-today': data.isToday }">
                {{ data.day.split('-').slice(2).join('-') }}
              </div>
            </template>
          </el-calendar>
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

// 初始化地图
const initMap = () => {
  // 创建地图实例
  const map = L.map('heatmap').setView([39.9042, 116.4074], 11)

  // 添加基础图层
  L.tileLayer('https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}', {
    subdomains: ['1', '2', '3', '4'],
    attribution: '© 高德地图'
  }).addTo(map)

  // 添加示例标记
  L.marker([39.9042, 116.4074]).addTo(map)
    .bindPopup('北京')
    .openPopup()
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
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

/* 欢迎信息区域 */
.welcome-section {
  background-color: #f5f7fa;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.welcome-title {
  font-size: 20px;
  font-weight: bold;
  text-align: center;
  margin-bottom: 20px;
  color: #333;
}

.welcome-content {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.welcome-left {
  flex: 1;
  margin-right: 40px;
}

.start-title {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 10px;
  color: #333;
}

.start-description {
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: 20px;
  color: #666;
}

.start-description a {
  color: #409eff;
  text-decoration: none;
}

.start-button {
  background-color: #409eff;
  border-color: #409eff;
}

.welcome-right {
  width: 200px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.assistant-icon {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.speech-bubble {
  width: 80px;
  height: 40px;
  background-color: #e6f3ff;
  border-radius: 20px;
  position: absolute;
  top: -50px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 16px;
  color: #409eff;
}

.assistant-avatar {
  width: 120px;
  height: 120px;
  background-color: #e0f7fa;
  border-radius: 50%;
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  border: 4px solid #409eff;
}

.avatar-image {
  width: 80px;
  height: 80px;
  background-color: #81d4fa;
  border-radius: 50%;
  position: relative;
  overflow: hidden;
}

.avatar-mic {
  width: 20px;
  height: 20px;
  background-color: #ff6b6b;
  border-radius: 50%;
  position: absolute;
  bottom: 10px;
  right: 10px;
}

.avatar-laptop {
  width: 100px;
  height: 60px;
  background-color: #e0e0e0;
  border-radius: 8px;
  margin-top: -20px;
  z-index: -1;
  border: 2px solid #bdbdbd;
}

/* 内容区域 */
.content-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.content-card {
  background-color: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.card-title {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 15px;
  color: #333;
}

/* 热力图 */
.map-container {
  position: relative;
  width: 100%;
  height: 300px;
}

.map-tools {
  position: absolute;
  top: 10px;
  left: 10px;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.map-tool-btn {
  width: 30px;
  height: 30px;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 0;
  background-color: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  cursor: pointer;
}

.map-tool-btn:hover {
  background-color: #ecf5ff;
}

.map {
  width: 100%;
  height: 100%;
  background-color: #f0f2f5;
  border-radius: 4px;
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
  margin-bottom: 10px;
}

.calendar-year {
  font-size: 14px;
  color: #606266;
}

.calendar-month {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

.calendar-toggle {
  display: flex;
  gap: 5px;
  margin-left: 20px;
}

.calendar-toggle .el-button {
  padding: 4px 8px;
  font-size: 12px;
}

.calendar-toggle .el-button.active {
  background-color: #ecf5ff;
  color: #409eff;
  border-color: #d9ecff;
}

.calendar-day {
  width: 100%;
  height: 30px;
  display: flex;
  justify-content: center;
  align-items: center;
  border-radius: 4px;
}

.calendar-day.is-today {
  background-color: #ecf5ff;
  color: #409eff;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .welcome-content {
    flex-direction: column;
    align-items: center;
  }

  .welcome-left {
    margin-right: 0;
    margin-bottom: 20px;
  }

  .content-section {
    grid-template-columns: 1fr;
  }
}
</style>
