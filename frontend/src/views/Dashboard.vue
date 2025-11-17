<template>
  <div class="card">
    <!-- 仪表盘页面：展示系统概览（设备总数、在线数、最近报警等） -->
    <h2 class="card-title">系统仪表盘</h2>
    <div class="cards">
      <div class="card">
        <h3>设备总数</h3>
        <div class="metric-value">{{ stats.totalDevices }}</div>
      </div>
      <div class="card">
        <h3>在线设备</h3>
        <div class="metric-value">{{ stats.onlineDevices }}</div>
      </div>
      <div class="card">
        <h3>今日报警</h3>
        <div class="metric-value">{{ stats.todayAlarms }}</div>
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
// 仪表盘示例：本地使用模拟数据，实际可通过 /api 读取统计
export default {
  name: 'Dashboard',
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
  }
}
</script>

<!-- 依赖全局样式（assets/styles.css）：.card/.cards/.metric-value -->
