<template>
  <!-- 导航栏组件：包含主要页面入口，所有页面复用此组件 -->
  <nav class="nav">
    <router-link to="/dashboard" class="nav-item">仪表盘</router-link>
    <router-link to="/devices" class="nav-item">设备管理</router-link>
    <router-link to="/realtime" class="nav-item">实时监控</router-link>
    <router-link to="/settings" class="nav-item">设置</router-link>
    <div class="spacer"></div>
    <router-link to="/login" class="nav-item">退出</router-link>
  </nav>
</template>

<script>
// 简单导航栏，不包含鉴权逻辑（适合开发/演示）
export default {
  name: 'NavBar'
}
</script>

<!-- 使用全局样式：.nav / .nav-item / .spacer 已在 assets/styles.css 定义，无需局部样式 -->
