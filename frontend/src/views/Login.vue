<template>
  <div class="container page-center card card-narrow">
    <h1 class="card-title">登录</h1>
    <p class="text-muted small mb-12">使用账户登录以访问平台功能</p>

    <!-- 改为使用 REST 登录：前端通过 fetch 调用 /api/login（JSON），并使用 credentials: 'include' 以携带 session cookie -->
    <form class="form" @submit.prevent="onSubmit">
      <div class="form-group">
        <label class="label" for="username">用户名</label>
        <input id="username" v-model="username" required placeholder="请输入用户名" autocomplete="username" />
      </div>
      <div class="form-group">
        <label class="label" for="password">密码</label>
        <input id="password" type="password" v-model="password" required placeholder="请输入密码" autocomplete="current-password" />
      </div>

      <div v-if="err" class="alert alert-error mb-12" role="alert">{{ err }}</div>

      <div class="actions mt-12">
        <button type="submit" class="btn btn-primary w-100" :disabled="loading">
          <span v-if="loading" class="spinner-border spinner-sm" aria-hidden="true"></span>
          <span>{{ loading ? '登录中...' : '登录' }}</span>
        </button>
        <button type="button" class="btn btn-ghost w-100" @click="goRegister">去注册</button>
      </div>
    </form>

    <p class="text-muted small mt-12">
      注：前端使用 REST 登录（/api/login）。请确保后端服务可用且浏览器允许 Cookie（前端使用 session cookie）。
    </p>
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
          const data = await res.json().catch(() => ({}))
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
