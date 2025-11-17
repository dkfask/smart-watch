<template>
  <div>
    <h2>首页</h2>
    <p>欢迎来到 demo2 的 Vue 前端示例页面。</p>
    <div style="margin-top:12px;">
      <button @click="goLogin">登出（跳回登录）</button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'HomeView',
  methods: {
    goLogin() {
      // 这里采用简单的跳转到 /login，真实登出应调用后端的 /logout 并清除 session
      this.$router.push('/login')
    }
  }
}
</script>

