<template>
  <div class="card">
    <!-- 设备详情：展示设备基础信息、最近位置与日志摘要 -->
    <h2 class="card-title">设备详情</h2>

    <div v-if="device">
      <div class="mb-12">
        <div><strong>ID:</strong> {{ device.id }}</div>
        <div><strong>IMEI:</strong> {{ device.imei }}</div>
        <div><strong>创建时间:</strong> {{ formatDate(device.createdAt) }}</div>
      </div>

      <section class="mb-12">
        <div v-if="location">
          <div><strong>最近位置:</strong> {{ location.latitude || location.lat }}, {{ location.longitude || location.lon }}</div>
          <div><strong>速度:</strong> {{ location.speed || 'n/a' }}</div>
        </div>
        <div v-else>暂无位置信息</div>
      </section>

      <section>
        <h3>轨迹 & 围栏</h3>
        <p class="text-muted">在地图上可以查看最近轨迹，并绘制围栏（绘制圆形会持久化）。</p>
        <leaflet-map :markers="mapMarkers" :tracks="trackPoints" :fences="fences" :editable="true" :height="'480px'"
                     @fence-created="onFenceCreated" />
      </section>

      <section class="mt-12">
        <h3>下发命令</h3>
        <div class="mb-8">
          <div><strong>设备在线:</strong> <span v-if="isOnline" style="color:green">在线</span><span v-else style="color:#888">离线</span></div>
        </div>

        <div class="form-group">
          <label class="label">同步时间（BP00）时区偏移（小时）</label>
          <input v-model.number="bp00Timezone" type="number" style="width:120px" />
          <button class="btn btn-primary" @click="sendTimeSync" :disabled="!device || !isOnline">发送 BP00</button>
        </div>

        <div class="form-group">
          <label class="label">设置 SOS 号码（BP12）</label>
          <div>
            <input v-model="bp12.seq" placeholder="流水号(可选)" style="width:120px;margin-right:8px" />
            <input v-model="bp12.sos1" placeholder="SOS1" style="width:180px;margin-right:8px" />
            <input v-model="bp12.sos2" placeholder="SOS2" style="width:180px;margin-right:8px" />
            <input v-model="bp12.sos3" placeholder="SOS3" style="width:180px" />
          </div>
          <div style="margin-top:8px">
            <button class="btn btn-primary" @click="sendBp12" :disabled="!device || !isOnline">发送 BP12</button>
          </div>
        </div>

        <div class="form-group">
          <label class="label">自定义原始命令（完整包，例如 IWBP10,imei,seq,xxx#）</label>
          <textarea v-model="customCommand" rows="3" style="width:100%"></textarea>
          <div style="margin-top:8px">
            <button class="btn btn-primary" @click="sendCustomCommand" :disabled="!device || !isOnline">发送自定义命令</button>
          </div>
        </div>

        <div class="mt-8">
          <div v-if="downlinkStatus" class="text-muted">状态: {{ downlinkStatus }}</div>
          <div v-if="downlinkError" style="color:red">错误: {{ downlinkError }}</div>
        </div>
      </section>

    </div>

    <div v-else>
      加载中...
    </div>
  </div>
</template>

<script>
import NavBar from '../components/NavBar.vue'
import api from '../api/device.js'
import downlink from '../api/downlink.js'
import LeafletMap from '../components/LeafletMap.vue'
import locationApi from '../api/location.js'
import fenceApi from '../api/fence.js'

export default {
  name: 'DeviceDetail',
  components: { NavBar, LeafletMap },
  data() {
    return {
      device: null,
      location: null,
      logs: [],

      // map / track
      mapMarkers: [],
      trackPoints: [],
      fences: [],

      // downlink related
      isOnline: false,
      bp00Timezone: new Date().getTimezoneOffset() / -60 || 8,
      bp12: { seq: '1', sos1: '', sos2: '', sos3: '' },
      customCommand: '',
      downlinkStatus: '',
      downlinkError: ''
    }
  },
  methods: {
    async load() {
      const id = this.$route.params.id
      try {
        const res = await api.get(id)
        this.device = res.data || res
        // load latest location and recent track
        await this.loadLocationAndTrack()
        // load online state
        this.loadOnlineStatus()
      } catch (e) {
        console.error('获取设备失败', e)
        alert('获取设备失败')
      }
    },

    async loadLocationAndTrack() {
      if (!this.device || !this.device.id) return
      try {
        // fetch recent single for display
        const recent = await locationApi.recentByDevice(this.device.id, 1)
        if (Array.isArray(recent) && recent.length > 0) {
          this.location = recent[0]
          this.mapMarkers = [{ id: this.device.imei, lat: this.location.latitude, lon: this.location.longitude, label: this.device.imei }]
        }
        // fetch last 24 hours track
        const end = new Date()
        const start = new Date(end.getTime() - 24 * 3600 * 1000)
        const tracks = await locationApi.rangeByDevice(this.device.id, start.toISOString(), end.toISOString(), 500)
        // normalize to {latitude, longitude}
        this.trackPoints = (Array.isArray(tracks) ? tracks : []).map(p => ({ latitude: p.latitude, longitude: p.longitude }))
      } catch (e) {
        console.warn('加载轨迹失败', e)
      }
    },

    async onFenceCreated(payload) {
      // only handle circle persistence for now
      try {
        if (payload.type === 'circle') {
          const center = payload.center // [lat, lon]
          const radius = payload.radius // meters
          // build GeoFence payload expected by backend
          const gf = {
            userId: 1, // TODO: replace with current user id if available
            name: 'map-fence-' + Date.now(),
            centerLatitude: center[0],
            centerLongitude: center[1],
            radius: radius,
            triggerType: 'both',
            isActive: true
          }
          const id = await fenceApi.createFence(gf)
          // append geojson to fences list for rendering
          if (payload.geojson) this.fences.push(payload.geojson)
          alert('围栏已创建 id=' + id)
        } else {
          alert('当前仅支持圆形围栏持久化（请使用圆形工具）')
        }
      } catch (e) {
        console.error('创建围栏失败', e)
        alert('创建围栏失败: ' + (e.message || e))
      }
    },

    async loadOnlineStatus() {
      try {
        const online = await downlink.getOnline()
        this.isOnline = Array.isArray(online) && this.device && online.includes(this.device.imei)
      } catch (e) {
        console.warn('获取在线设备失败', e)
        this.isOnline = false
      }
    },

    async sendTimeSync() {
      if (!this.device) return
      this.downlinkStatus = '发送中...'
      this.downlinkError = ''
      try {
        await downlink.sendBp00(this.device.imei, this.bp00Timezone)
        this.downlinkStatus = 'BP00 已发送'
      } catch (e) {
        console.error(e)
        this.downlinkError = e.message || String(e)
        this.downlinkStatus = ''
      }
    },

    async sendBp12() {
      if (!this.device) return
      this.downlinkStatus = '发送中...'
      this.downlinkError = ''
      try {
        const body = {
          imei: this.device.imei,
          seq: this.bp12.seq,
          sos: [this.bp12.sos1 || '', this.bp12.sos2 || '', this.bp12.sos3 || '']
        }
        await downlink.sendBp12(body)
        this.downlinkStatus = 'BP12 已发送'
      } catch (e) {
        console.error(e)
        this.downlinkError = e.message || String(e)
        this.downlinkStatus = ''
      }
    },

    async sendCustomCommand() {
      if (!this.device) return
      if (!this.customCommand || !this.customCommand.trim()) {
        this.downlinkError = '自定义命令不能为空'
        return
      }
      this.downlinkStatus = '发送中...'
      this.downlinkError = ''
      try {
        const body = { imei: this.device.imei, payload: this.customCommand.trim() }
        await downlink.sendCustom(body)
        this.downlinkStatus = '自定义命令已发送'
      } catch (e) {
        console.error(e)
        this.downlinkError = e.message || String(e)
        this.downlinkStatus = ''
      }
    },

    formatDate(s) { if (!s) return ''; try { return new Date(s).toLocaleString() } catch(e){ return s } }
  },
  mounted() { this.load() }
}
</script>

<style scoped>
.mb-12 { margin-bottom: 12px }
.form-group { margin-bottom: 12px }
.mt-12 { margin-top: 12px }
.text-muted { color: #666 }
</style>
