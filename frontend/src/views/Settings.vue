<template>
  <div class="card">
    <!-- 设置页面：管理系统与设备同步相关配置（仅示例） -->
    <h2 class="card-title">系统设置</h2>
    <form class="form" @submit.prevent="save">
      <div class="form-group">
        <label class="label" for="saveDir">数据保存目录</label>
        <input id="saveDir" v-model="settings.saveDir" placeholder="例如：mpband_data" />
        <small class="text-muted">示例：mpband_data（后端需对应配置 app.mpband.saveDir）</small>
      </div>
      <div class="form-group">
        <label class="label" for="poll">定位轮询间隔（秒）</label>
        <input id="poll" type="number" v-model.number="settings.pollInterval" />
      </div>
      <div class="actions">
        <button type="submit" class="btn btn-primary">保存</button>
        <button type="button" class="btn btn-ghost" @click="reset">重置</button>
      </div>
    </form>
  </div>
</template>

<script>
import NavBar from '../components/NavBar.vue'

export default {
  name: 'Settings',
  components: { NavBar },
  data() {
    // 前端配置示例，实际需通过后端 API 获取并保存
    return {
      settings: {
        saveDir: 'mpband_data',
        pollInterval: 60
      }
    }
  },
  methods: {
    save() {
      alert('已保存（示例，当前仅前端显示）')
    },
    reset() {
      this.settings = { saveDir: 'mpband_data', pollInterval: 60 }
    }
  }
}
</script>

<style scoped>
.field { margin-bottom:12px }
label { display:block; font-weight:600 }
input { width:320px; padding:6px; }
</style>
