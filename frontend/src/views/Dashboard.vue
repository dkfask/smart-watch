<template>
  <div class="card">
    <!-- 仪表盘页面：展示系统概览（设备总数、在线数、最近报警等） -->
    <h2 class="card-title">系统仪表盘</h2>
    <h2>系统仪表盘</h2>
    <div class="cards">
      <div class="card">
        <div class="metric-value">{{ stats.totalDevices }}</div>
        <div class="value">{{ stats.totalDevices }}</div>
      </div>
      <div class="card">
        <div class="metric-value">{{ stats.onlineDevices }}</div>
        <div class="value">{{ stats.onlineDevices }}</div>
      </div>
      <div class="card">
        <div class="metric-value">{{ stats.todayAlarms }}</div>
        <div class="value">{{ stats.todayAlarms }}</div>
      </div>
    </div>
    <section>
      <h3>最近日志（按设备）</h3>
      <ul>
        <li v-for="log in recentLogs" :key="log.id">{{ log }}</li>
      </ul>
    </section>
  </div>
</template>

<script>
import NavBar from '../components/NavBar.vue'

// 仪表盘示例：本地使用模拟数据，实际可通过 /api 读取统计
export default {
  name: 'Dashboard',
  components: { NavBar },
  data() {
    return {
      stats: {
        totalDevices: 0,
        onlineDevices: 0,
        todayAlarms: 0
      },
      recentLogs: []
    }
  },
  mounted() {
    // TODO: 使用真实 API 获取数据；当前为演示 mock
    this.stats = { totalDevices: 12, onlineDevices: 4, todayAlarms: 1 }
    this.recentLogs = [
      '353456789012345 - 2025-11-17 09:12:03 - AP01',
      '357653050858997 - 2025-11-17 08:50:10 - AP03'
    ]
    // 依赖全局样式 (assets/styles.css)：.card/.cards/.metric-value
  }
}
</script>

<style scoped>
.cards { display:flex; gap:12px; margin:12px 0; }
.card { background:#fff; padding:12px; border-radius:6px; box-shadow:0 1px 3px rgba(0,0,0,0.1); width:160px; }
.value { font-size:24px; font-weight:700; margin-top:8px }
</style>
