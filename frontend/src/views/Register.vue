<template>
  <div class="container page-center card card-narrow">
    <h1 class="card-title">注册</h1>

    <form class="form" @submit.prevent="onSubmit">
      <div class="form-group">
        <label class="label" for="username">用户名</label>
        <input id="username" v-model="username" required placeholder="3-20 位字母/数字/下划线" autocomplete="username" />
      </div>

      <div class="form-group">
        <label class="label" for="email">邮箱（可选）</label>
        <input id="email" v-model="email" placeholder="example@domain.com" autocomplete="email" />
      </div>

      <div class="form-group">
        <label class="label" for="password">密码</label>
        <input id="password" type="password" v-model="password" required placeholder="至少 6 位" autocomplete="new-password" />
        <div class="small text-muted mt-12" v-if="password">
          强度： <span :class="passwordStrengthClass">{{ passwordStrengthText }}</span>
        </div>
      </div>

      <div class="form-group">
        <label class="label" for="confirm">确认密码</label>
        <input id="confirm" type="password" v-model="confirmPassword" required placeholder="再次输入密码" autocomplete="new-password" />
      </div>

      <div class="actions">
        <button type="submit" class="btn btn-primary w-100" :disabled="loading">
          <span v-if="loading" class="spinner-border spinner-sm" aria-hidden="true"></span>
          <span>{{ loading ? '注册中...' : '注册' }}</span>
        </button>
        <button type="button" class="btn btn-ghost w-100" @click="$router.push('/login')">返回登录</button>
      </div>
    </form>

    <div v-if="msg" class="alert alert-success mt-12">{{ msg }}</div>
    <div v-if="err" class="alert alert-error mt-12">{{ err }}</div>
  </div>
</template>

<script>
export default {
  name: 'RegisterView',
  data() {
    return {
      username: '',
      email: '',
      password: '',
      confirmPassword: '',
      msg: '',
      err: '',
      loading: false
    }
  },
  computed: {
    passwordStrengthText() {
      const s = this._calcPasswordScore(this.password)
      if (s < 2) return '弱'
      if (s === 2) return '中'
      return '强'
    },
    passwordStrengthClass() {
      const s = this._calcPasswordScore(this.password)
      if (s < 2) return 'error'
      if (s === 2) return 'label'
      return 'success'
    }
  },
  methods: {
    _calcPasswordScore(pwd) {
      if (!pwd) return 0
      let score = 0
      if (pwd.length >= 6) score++
      if (/[A-Z]/.test(pwd) && /[a-z]/.test(pwd)) score++
      if (/[0-9]/.test(pwd) && /[^A-Za-z0-9]/.test(pwd)) score++
      return score
    },
    async onSubmit() {
      this.err = ''
      this.msg = ''
      // 客户端基本校验
      const uname = (this.username || '').trim()
      if (!uname || uname.length < 3 || uname.length > 20 || !/^\w+$/.test(uname)) {
        this.err = '用户名必须是 3-20 个字母/数字/下划线'
        return
      }
      if (this.password.length < 6) {
        this.err = '密码至少 6 位'
        return
      }
      if (this.password !== this.confirmPassword) {
        this.err = '两次密码不一致'
        return
      }
      if (this.email && !/^\S+@\S+\.\S+$/.test(this.email)) {
        this.err = '邮箱格式不正确'
        return
      }

      this.loading = true
      try {
        const res = await fetch('/api/register', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          credentials: 'include',
          body: JSON.stringify({ username: uname, password: this.password, email: this.email || null })
        })
        if (res.ok) {
          this.msg = '注册成功，正在跳转到登录页...'
          setTimeout(() => this.$router.push('/login'), 1000)
          return
        }
        if (res.status === 400) {
          const json = await res.json().catch(() => null)
          this.err = (json && json.error) ? json.error : '注册失败（参数错误）'
          return
        }
        if (res.status === 404) {
          // 后端没有 REST 注册接口，回退为表单提交
          this.fallbackFormSubmit()
          return
        }
        const text = await res.text().catch(() => '')
        this.err = text || ('注册失败，状态码：' + res.status)
      } catch (e) {
        this.err = '网络或接口错误，尝试回退到表单提交...'
        this.fallbackFormSubmit()
      } finally {
        this.loading = false
      }
    },
    // 回退方案（与后端原有表单注册兼容）
    fallbackFormSubmit() {
      try {
        const form = document.createElement('form')
        form.method = 'POST'
        form.action = '/register'

        const inUser = document.createElement('input')
        inUser.type = 'hidden'
        inUser.name = 'username'
        inUser.value = this.username
        form.appendChild(inUser)

        const inPwd = document.createElement('input')
        inPwd.type = 'hidden'
        inPwd.name = 'password'
        inPwd.value = this.password
        form.appendChild(inPwd)

        if (this.email) {
          const inEmail = document.createElement('input')
          inEmail.type = 'hidden'
          inEmail.name = 'email'
          inEmail.value = this.email
          form.appendChild(inEmail)
        }

        document.body.appendChild(form)
        form.submit()
      } catch (e) {
        this.err = '回退表单提交失败: ' + e.message
      }
    }
  }
}
</script>
