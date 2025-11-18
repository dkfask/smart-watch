<template>
  <div class="card">
    <!--
      Menu.vue
      说明：
      - 该组件为 SPA 层面的主菜单页面，配合后端的 `/api/me` 接口展示当前用户名。
      - 提供退出登录操作：调用 POST /api/logout（credentials: 'include'），成功后清理本地缓存并跳转到 /login。
      - 页面采用统一的全局样式（卡片/按钮等），可按前端路由与业务扩展菜单项。
    -->
    <header class="input-row" style="justify-content:space-between; align-items:center;">
      <div>
        <h2 class="card-title">主菜单</h2>
        <div class="text-muted">欢迎, <strong>{{ username || '用户' }}</strong></div>
      </div>
      <div>
        <button class="btn btn-ghost" @click="doLogout" :disabled="loggingOut">退出登录</button>
      </div>
    </header>

    <nav class="mt-12">
      <ul style="list-style:none; padding:0; display:flex; gap:12px; flex-wrap:wrap;">
        <li><router-link class="btn btn-secondary" to="/dashboard">仪表盘</router-link></li>
        <li><router-link class="btn btn-secondary" to="/devices">设备管理</router-link></li>
        <li><router-link class="btn btn-secondary" to="/settings">设置</router-link></li>
      </ul>
    </nav>

    <section class="mt-12">
      <div class="text-muted">
        提示：页面通过 GET /api/me 获取当前用户名；若未认证将重定向回登录页。
        <strong>提示：</strong> 页面通过 `GET /api/me` 获取当前用户名，若未认证将重定向回登录页。
      </div>
    </section>
  </div>
</template>

<script>
export default {
  name: 'MenuView',
  data() {
    return {
      username: localStorage.getItem('username') || '',
      loggingOut: false
    }
  },
  async created() {
    // 尝试从后端确认当前会话的用户名（优先级：后端 /api/me -> localStorage -> 空）
    try {
      const res = await fetch('/api/me', { method: 'GET', credentials: 'include' })
      if (res.ok) {
        const data = await res.json().catch(() => ({}))
        if (data && data.username) {
          this.username = data.username
          try { localStorage.setItem('username', data.username) } catch (e) { /* ignore */ }
        }
      } else {
        // 未认证或返回错误，跳转到登录页
        this.$router.push('/login')
      }
    } catch (e) {
      // 网络错误时保留 localStorage 的 username（若存在）并允许页面继续显示
      console.warn('获取当前用户信息失败：', e.message)
    }
  },
  methods: {
    // 退出登录：向后端发起 POST /api/logout，忽略网络/服务端错误，保证本地状态清理并跳转到登录页
    async doLogout() {
      this.loggingOut = true
      try {
        // 1) 向后端请求登出（若失败仍继续本地清理）
        try {
          await fetch('/api/logout', { method: 'POST', credentials: 'include' })
        } catch (err) {
          // 后端登出请求失败（网络或 5xx），记录日志并继续本地清理
          console.warn('调用 /api/logout 失败：', err && err.message)
        }

        // 2) 本地清理并跳转到登录页（无论后端是否成功）
        try { localStorage.removeItem('username') } catch (e) { /* ignore */ }
        this.$router.push('/login')
      } catch (e) {
        // 捕获上面任何意外异常，保证仍然进行本地清理和跳转
        console.error('退出登录异常：', e && e.message)
        try { localStorage.removeItem('username') } catch (er) { /* ignore */ }
        try { this.$router.push('/login') } catch (_) { /* ignore */ }
      } finally {
        // 确保状态最终被重置
        this.loggingOut = false
      }
    }
  }
}
</script>
