<template>
  <div>
    <div class="container">
      <NavBar />
    </div>
    <main class="container mt-12">
      <router-view />
    </main>
  </div>
</template>

<script>
import NavBar from './components/NavBar.vue'

export default {
  name: 'App',
  components: { NavBar }
}
</script>

<style>
body { font-family: Arial, Helvetica, "Microsoft Yahei", sans-serif; }
</style>
