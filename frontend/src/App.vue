<template>
  <div>
    <header style="padding:12px;background:#2d8cf0;color:#fff;">demo2 - Vue 前端示例</header>
    <main style="padding:16px;">
      <router-view />
    </main>
  </div>
</template>

<script>
// 简单根组件：包含 header 与 router-view
export default {
  name: 'App'
}
</script>

<style>
body { font-family: Arial, Helvetica, "Microsoft Yahei", sans-serif; }
</style>

