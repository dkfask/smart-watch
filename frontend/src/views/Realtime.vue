<template>
  <div class="card">
    <!-- 实时监控页面：显示在线设备的简要信息与地图占位（可接入地图组件） -->
    <h2 class="card-title">实时监控</h2>
    <p class="text-muted">以下展示当前在线设备（示例数据，实际应通过 WebSocket 或轮询获取）</p>
    <ul class="mt-12">
    <ul>
      <li v-for="d in online" :key="d.imei">{{ d.imei }} - {{ d.lat }}, {{ d.lon }} - {{ d.speed }} km/h</li>
    </ul>
    <div class="map-placeholder">地图占位（可集成 Leaflet/Mapbox/Google Maps）</div>
  </div>
</template>

<script>
import NavBar from '../components/NavBar.vue'

export default {
  name: 'Realtime',
  components: { NavBar },
  data() {
    return { online: [] }
  },
  mounted() {
    // TODO: 改为 WebSocket 或 SSE 实时订阅；当前为 mock 数据
    this.online = [
      { imei: '353456789012345', lat: '22.3830', lon: '114.0823', speed: '0.1' },
      { imei: '357653050858997', lat: '22.3850', lon: '114.0800', speed: '0.0' }
    ]
<!-- 使用全局样式：.card / .text-muted / .map-placeholder 等 -->
}
</script>

<style scoped>
.map-placeholder {
  margin-top:12px;
  height:300px; background:#f2f2f2; border:1px dashed #ccc; display:flex;align-items:center;justify-content:center;
}
</style>

