<template>
  <div class="card">
    <!-- 实时监控页面：显示在线设备的简要信息与地图 -->
    <h2 class="card-title">实时监控</h2>
    <p class="text-muted">展示当前在线设备及其实时位置（通过轮询后端获取最新位置）</p>

    <div class="mb-12">
      <button class="btn btn-ghost" @click="refresh">刷新</button>
      <span style="margin-left:12px">上次更新时间: {{ lastUpdatedText }}</span>
    </div>

    <ul class="mt-12">
      <li v-for="d in online" :key="d.imei">{{ d.imei }} - {{ d.lat }}, {{ d.lon }} - {{ d.speed }} km/h</li>
    </ul>

    <leaflet-map :markers="mapMarkers" :height="'480px'" />
  </div>
</template>

<script>
import NavBar from '../components/NavBar.vue'
import LeafletMap from '../components/LeafletMap.vue'
import downlink from '../api/downlink.js'
import deviceApi from '../api/device.js'
import locationApi from '../api/location.js'

export default {
  name: 'Realtime',
  components: { NavBar, LeafletMap },
  data() {
    return {
      online: [],
      mapMarkers: [],
      pollIntervalId: null,
      lastUpdatedText: ''
    }
  },
  methods: {
    async refresh() {
      // fetch online imeis
      try {
        const imeis = await downlink.getOnline()
        const results = []
        const markers = []
        for (const imei of imeis) {
          try {
            const dev = await deviceApi.getByImei(imei).catch(() => null)
            let deviceId = dev && dev.id ? dev.id : null
            if (deviceId) {
              const locs = await locationApi.recentByDevice(deviceId, 1)
              if (Array.isArray(locs) && locs.length > 0) {
                const p = locs[0]
                results.push({ imei, lat: p.latitude, lon: p.longitude, speed: p.batteryLevel || 'n/a' })
                markers.push({ id: imei, lat: p.latitude, lon: p.longitude, label: imei })
              } else {
                results.push({ imei, lat: null, lon: null, speed: 'n/a' })
              }
            } else {
              results.push({ imei, lat: null, lon: null, speed: 'n/a' })
            }
          } catch (e) {
            results.push({ imei, lat: null, lon: null, speed: 'n/a' })
          }
        }
        this.online = results
        this.mapMarkers = markers
        this.lastUpdatedText = new Date().toLocaleString()
      } catch (e) {
        console.warn('刷新在线设备失败', e)
      }
    }
  },
  mounted() {
    this.refresh()
    // poll every 10 seconds
    this.pollIntervalId = setInterval(() => this.refresh(), 10000)
  },
  unmounted() {
    if (this.pollIntervalId) clearInterval(this.pollIntervalId)
  }
}
</script>

<style scoped>
.map-placeholder {
  margin-top:12px;
  height:300px; background:#f2f2f2; border:1px dashed #ccc; display:flex;align-items:center;justify-content:center;
}
</style>
