<template>
  <div class="card">
    <!-- 设备列表页面：展示设备表格，支持搜索与刷新 -->
    <h2 class="card-title">设备管理</h2>

    <div class="input-row mb-12">
      <div class="actions">
        <input v-model="q" placeholder="按 IMEI 搜索" />
        <button class="btn btn-secondary" @click="search">搜索</button>
        <button class="btn btn-ghost" @click="reload">刷新</button>
      </div>
    </div>

    <table class="device-table">
      <thead>
        <tr><th>ID</th><th>IMEI</th><th>创建时间</th><th>操作</th></tr>
      </thead>
      <tbody>
        <tr v-for="d in devices" :key="d.id">
          <td>{{ d.id }}</td>
          <td>{{ d.imei }}</td>
          <td>{{ formatDate(d.createdAt) }}</td>
          <td>
            <router-link :to="`/devices/${d.id}`">详情</router-link>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script>
import NavBar from '../components/NavBar.vue'
import api from '../api/device.js'

export default {
  name: 'Devices',
  components: { NavBar },
  data() {
    return {
      devices: [],
      q: ''
    }
  },
  methods: {
    // 从后端获取设备列表（简单示例，pageSize/pageOffset 固定）
    async reload() {
      try {
        const res = await api.list(100, 0)
        // 支持后端返回 { data: [...] } 或直接返回数组
        this.devices = (res && res.data) ? res.data : (res || [])
      } catch (err) {
        console.error('获取设备失败', err)
        alert('获取设备失败，查看控制台')
      }
    },
    // 简单按 imei 搜索：如果 q 为空则刷新全部
    search() {
      if (!this.q) return this.reload()
      // 这里示例将路由到后端查询页面，实际可调用 API 并更新 devices
      this.$router.push(`/devices?imei=${encodeURIComponent(this.q)}`)
    },
    formatDate(s) {
      if (!s) return ''
      try { return new Date(s).toLocaleString() } catch(e){ return s }
    }
  },
  mounted() {
    this.reload()
  }
}
</script>

<style scoped>
.actions { display:flex; gap:8px; align-items:center; margin-bottom:12px }
.device-table { width:100%; border-collapse:collapse }
.device-table th, .device-table td { padding:8px; border-bottom:1px solid #eee }
</style>
