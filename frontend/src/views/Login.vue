<template>
  <div style="max-width:420px;margin:0 auto;">
    <h2>登录</h2>
    <!-- 改为使用 REST 登录：前端通过 fetch 调用 /api/login（JSON），并使用 credentials: 'include' 以携带 session cookie -->
    <form @submit.prevent="onSubmit">
      <div style="margin-bottom:8px;">
        <label>用户名</label><br />
        <input v-model="username" required placeholder="请输入用户名" />
      </div>
      <div style="margin-bottom:8px;">
        <label>密码</label><br />
        <input type="password" v-model="password" required placeholder="请输入密码" />
      </div>

      <div v-if="err" style="color:red;margin-bottom:8px">{{ err }}</div>

      <div style="margin-top:12px;">
        <button type="submit" :disabled="loading">{{ loading ? '登录中...' : '登录' }}</button>
        <button type="button" @click="goRegister">去注册</button>
      </div>
    </form>

    <div style="margin-top:16px;color:#666;">
      注：前端使用 REST 登录（/api/login）。请确保后端服务可用且浏览器允许 Cookie（前端使用 session cookie）。
    </div>
  </div>
</template>

<script>
export default {
  name: 'LoginView',
  data() {
    return {
      username: '',
      password: '',
      err: '',
      loading: false
    }
  },
  methods: {
    goRegister() {
      this.$router.push('/register')
    },
    // 登录逻辑：前端做简单校验后调用 /api/login（JSON）, 成功后导航到 /menu（SPA 路由）
    async onSubmit() {
      this.err = ''
      if (!this.username || !this.password) {
        this.err = '用户名和密码不能为空'
        return
      }
      if (this.username.trim().length < 3) {
        this.err = '用户名至少 3 个字符'
        return
      }
      if (this.password.length < 6) {
        this.err = '密码至少 6 个字符'
        return
      }
      this.loading = true
      try {
        const res = await fetch('/api/login', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          credentials: 'include',
          body: JSON.stringify({ username: this.username.trim(), password: this.password })
        })
        if (res.ok) {
          // 登录成功：后端返回 { username }
          // 使用 SPA 路由导航到 /menu，避免触发整页刷新；
          // Menu 页面会通过 /api/me 再次确认并显示用户名。
          const data = await res.json().catch(() => ({}))
          // 可选：把 username 暂存到 localStorage 以便在切换页面前显示（不是必须）
          if (data && data.username) {
            try { localStorage.setItem('username', data.username) } catch(e) { /* ignore */ }
          }
          this.$router.push('/menu')
        } else if (res.status === 401) {
          const data = await res.json().catch(() => ({}))
          this.err = data.error || '用户名或密码错误'
        } else {
          const text = await res.text().catch(() => '')
          this.err = text || ('登录失败，状态码：' + res.status)
        }
      } catch (e) {
        this.err = '网络错误：' + e.message
      } finally {
        this.loading = false
      }
    }
  }
}
</script>
