<template>
  <div class="card">
    <h2 class="card-title">首页</h2>
    <p class="text-muted">欢迎来到 demo2 的 Vue 前端示例页面。</p>
    <div class="mt-12">
      <button class="btn btn-secondary" @click="goLogin">退出（跳回登录）</button>
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

<!-- 使用全局样式：.card / .btn / .text-muted / .mt-12 -->
