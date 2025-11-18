<template>
  <div class="card">
    <!-- 设备详情：展示设备基础信息、最近位置与日志摘要 -->
    <h2 class="card-title">设备详情</h2>
    <h2>设备详情</h2>
      <div class="mb-12">
        <div><strong>ID:</strong> {{ device.id }}</div>
        <div><strong>IMEI:</strong> {{ device.imei }}</div>
        <div><strong>创建时间:</strong> {{ formatDate(device.createdAt) }}</div>
      </div>

      <section class="mb-12">
        <div v-else>暂无位置信息</div>
      </section>
        <div v-else>暂无法获取位置</div>
        <h3>最近日志</h3>

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
import NavBar from '../components/NavBar.vue'
import api from '../api/device.js'

export default {
  name: 'DeviceDetail',
  components: { NavBar },
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
        // TODO: 调用真实 API 获取位置信息和日志摘要；目前用 mock
        // TODO: 通过真实 API 获取位置与日志；目前为 mock
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

<style scoped>
</style>
