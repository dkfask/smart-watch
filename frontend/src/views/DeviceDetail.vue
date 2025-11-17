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
        <h3>最近位置</h3>
        <div v-if="location">经度：{{ location.longitude }}，纬度：{{ location.latitude }}，速度：{{ location.speed }}</div>
        <div v-else>暂无法获取位置</div>
      </section>

      <section>
        <h3>最近日志</h3>
        <ul>
          <li v-for="(l, idx) in logs" :key="idx">{{ l }}</li>
        </ul>
      </section>
    </div>
    <div v-else>
      加载中...
    </div>
  </div>
</template>

<script>
import api from '../api/device.js'

export default {
  name: 'DeviceDetail',
  data() {
    return {
      device: null,
      location: null,
      logs: []
    }
  },
  methods: {
    async load() {
      const id = this.$route.params.id
      try {
        const res = await api.get(id)
        this.device = res.data || res
        // TODO: 通过真实 API 获取位置与日志；目前为 mock
        this.location = { latitude: '22.3830', longitude: '114.0823', speed: '0.1' }
        this.logs = [
          'AP01 - 2025-11-17 09:12:03',
          'AP03 - 2025-11-17 08:50:10'
        ]
      } catch (e) {
        console.error('获取设备失败', e)
        alert('获取设备失败')
      }
    },
    formatDate(s) { if (!s) return ''; try { return new Date(s).toLocaleString() } catch(e){ return s } }
  },
  mounted() { this.load() }
}
</script>
