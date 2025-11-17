<template>
  <div style="max-width:800px;margin:0 auto;padding:1rem;">
    <!--
      Menu.vue
      说明：
      - 该组件为 SPA 层面的主菜单页面，与后端的 `/api/me` 接口配合展示当前用户名。
      - 提供退出登录操作：调用 POST /api/logout（credentials: 'include'），成功后清理本地缓存并跳转到 /login。
      - 该页面采用简单样式，仅作示例；可根据前端路由与业务扩展菜单项。
    -->
    <header style="display:flex;justify-content:space-between;align-items:center;margin-bottom:1rem;">
      <div>
        <h2>主菜单</h2>
        <div style="color:#666;">欢迎, <strong>{{ username || '用户' }}</strong></div>
      </div>
      <div>
        <button @click="doLogout" :disabled="loggingOut">退出登录</button>
      </div>
    </header>

    <nav>
      <ul style="list-style:none;padding:0;display:flex;gap:1rem;">
        <li><router-link to="/dashboard">仪表盘</router-link></li>
        <li><router-link to="/profile">个人资料</router-link></li>
        <li><router-link to="/settings">设置</router-link></li>
      </ul>
    </nav>

    <section style="margin-top:1.5rem;">
      <p>这是 SPA 的主菜单页面示例。若需要服务端渲染的 `/menu` 页面，后端已同时提供 Thymeleaf 版（`/menu` 模板）。</p>
      <div style="margin-top:0.8rem;color:#333;">
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
    async doLogout() {
      this.loggingOut = true
      try {
        const res = await fetch('/api/logout', { method: 'POST', credentials: 'include' })
        if (res.ok) {
          // 清理客户端缓存并跳回登录页
          try { localStorage.removeItem('username') } catch (e) { /* ignore */ }
          this.$router.push('/login')
        } else {
          // 若退出失败，仍尽量清理并跳回登录页
          try { localStorage.removeItem('username') } catch (e) { /* ignore */ }
          this.$router.push('/login')
        }
      } catch (e) {
        console.error('退出登录异常：', e.message)
        try { localStorage.removeItem('username') } catch (er) { /* ignore */ }
        this.$router.push('/login')
      } finally {
        this.loggingOut = false
      }
    }
  }
}
</script>

