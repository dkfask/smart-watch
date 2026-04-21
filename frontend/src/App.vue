<template>
  <div id="app" :class="{ 'sidebar-collapsed': isSidebarCollapsed }">
    <!-- 侧边栏 -->
    <aside v-if="isAuthenticated && $route.meta.requiresAuth" class="sidebar">
      <div class="sidebar-header">
        <div class="sidebar-brand" @click="navigateToDashboard">
          <svg class="brand-icon" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <path d="M12 6v6l4 2"/>
          </svg>
          <span v-show="!isSidebarCollapsed" class="brand-text">智能定位</span>
        </div>
        <button class="sidebar-toggle" @click="toggleSidebar" :title="isSidebarCollapsed ? '展开菜单' : '收起菜单'">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
            <path v-if="isSidebarCollapsed" d="M6 3l5 5-5 5V3z"/>
            <path v-else d="M10 3l-5 5 5 5V3z"/>
          </svg>
        </button>
      </div>

      <nav class="sidebar-nav">
        <div class="nav-section">
          <div v-if="!isSidebarCollapsed" class="nav-section-title">概览</div>
          <router-link to="/" class="nav-item" :class="{ active: activeMenu === '/' }">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></svg>
            <span v-show="!isSidebarCollapsed" class="nav-text">仪表盘</span>
          </router-link>
        </div>

        <div class="nav-section">
          <div v-if="!isSidebarCollapsed" class="nav-section-title">业务管理</div>
          <router-link to="/patients" class="nav-item" :class="{ active: activeMenu.startsWith('/patient') }">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
            <span v-show="!isSidebarCollapsed" class="nav-text">病人管理</span>
          </router-link>
          <router-link to="/devices" class="nav-item" :class="{ active: activeMenu.startsWith('/device') }">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="7" width="20" height="14" rx="2" ry="2"/><path d="M16 2l-4 5-4-5"/></svg>
            <span v-show="!isSidebarCollapsed" class="nav-text">设备管理</span>
          </router-link>
        </div>

        <div class="nav-section">
          <div v-if="!isSidebarCollapsed" class="nav-section-title">监控中心</div>
          <router-link to="/realtime" class="nav-item" :class="{ active: activeMenu === '/realtime' }">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>
            <span v-show="!isSidebarCollapsed" class="nav-text">实时定位</span>
          </router-link>
          <router-link to="/fences" class="nav-item" :class="{ active: activeMenu === '/fences' }">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M8 12l2 2 4-4"/></svg>
            <span v-show="!isSidebarCollapsed" class="nav-text">电子围栏</span>
          </router-link>
          <router-link to="/alarms" class="nav-item" :class="{ active: activeMenu === '/alarms' }">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
            <span v-show="!isSidebarCollapsed" class="nav-text">报警管理</span>
          </router-link>
        </div>
      </nav>

      <div class="sidebar-footer">
        <div class="nav-item" @click="logout">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
          <span v-show="!isSidebarCollapsed" class="nav-text">退出登录</span>
        </div>
      </div>
    </aside>

    <!-- 主内容区 -->
    <div class="main-wrapper" v-if="isAuthenticated && $route.meta.requiresAuth">
      <header class="top-bar">
        <div class="top-bar-left">
          <h2 class="page-title">{{ pageTitle }}</h2>
        </div>
        <div class="top-bar-right">
          <div class="user-menu">
            <div class="user-avatar">{{ currentUsername.charAt(0).toUpperCase() }}</div>
            <span class="user-name">{{ currentUsername }}</span>
          </div>
        </div>
      </header>
      <main class="main-content">
        <router-view />
      </main>
    </div>

    <!-- 未认证页面 (login) -->
    <div v-else>
      <router-view />
    </div>
  </div>
</template>

<script setup>
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from './stores/auth'
import { onMounted, ref, computed } from 'vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const isSidebarCollapsed = ref(false)

const isAuthenticated = computed(() => authStore.isAuthenticated)

const activeMenu = computed(() => route.path)

const currentUsername = computed(() => {
  try {
    const userStr = localStorage.getItem('user')
    if (userStr) {
      const userData = JSON.parse(userStr)
      if (userData.data && userData.data.username) return userData.data.username
      if (userData.data && userData.data.data && userData.data.data.username) return userData.data.data.username
      if (userData.username) return userData.username
    }
  } catch (error) {
    console.error('读取用户信息出错:', error)
  }
  return 'admin'
})

const pageTitle = computed(() => {
  const titles = {
    '/': '仪表盘',
    '/patients': '病人管理',
    '/devices': '设备管理',
    '/alarms': '报警管理',
    '/realtime': '实时定位',
    '/fences': '电子围栏'
  }
  if (route.path.startsWith('/patients/')) return '病人详情'
  if (route.path.startsWith('/devices/')) return '设备详情'
  if (route.path.startsWith('/history/')) return '历史轨迹'
  return titles[route.path] || '智能定位监控系统'
})

const toggleSidebar = () => {
  isSidebarCollapsed.value = !isSidebarCollapsed.value
}

const navigateToDashboard = () => {
  router.push('/')
}

const logout = async () => {
  await authStore.logout()
  router.push('/login')
}

onMounted(() => {
  authStore.loadUserFromStorage()
})
</script>

<style>
/* ============================================
   Sidebar Layout
   ============================================ */
#app {
  min-height: 100vh;
  background: var(--color-bg);
}

/* Sidebar */
.sidebar {
  width: 240px;
  min-width: 240px;
  background: var(--color-surface);
  border-right: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  transition: width 0.2s ease, min-width 0.2s ease;
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  z-index: 100;
}

.sidebar-collapsed .sidebar {
  width: 64px;
  min-width: 64px;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid var(--color-border);
  height: 60px;
}

.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  color: var(--color-primary);
  transition: opacity 0.2s ease;
}

.sidebar-brand:hover {
  opacity: 0.8;
}

.brand-icon {
  flex-shrink: 0;
}

.brand-text {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text-primary);
  white-space: nowrap;
  overflow: hidden;
}

.sidebar-toggle {
  background: none;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 4px;
  cursor: pointer;
  color: var(--color-text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s ease;
  flex-shrink: 0;
}

.sidebar-toggle:hover {
  background: var(--color-surface-raised);
  color: var(--color-text-primary);
  border-color: var(--color-border-strong);
}

/* Navigation */
.sidebar-nav {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.nav-section {
  margin-bottom: 4px;
}

.nav-section-title {
  font-size: 11px;
  font-weight: 600;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  padding: 12px 12px 6px;
  white-space: nowrap;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: var(--radius-sm);
  color: var(--color-text-secondary);
  text-decoration: none;
  font-size: var(--text-body);
  font-weight: 500;
  transition: all 0.15s ease;
  cursor: pointer;
  white-space: nowrap;
  position: relative;
}

.nav-item:hover {
  background: var(--color-surface-raised);
  color: var(--color-text-primary);
}

.nav-item.active {
  background: var(--color-primary-light);
  color: var(--color-primary);
  font-weight: 600;
}

.nav-item.active::before {
  content: '';
  position: absolute;
  left: -8px;
  top: 6px;
  bottom: 6px;
  width: 3px;
  background: var(--color-primary);
  border-radius: 0 2px 2px 0;
}

.nav-text {
  overflow: hidden;
  text-overflow: ellipsis;
}

.sidebar-collapsed .nav-section-title {
  display: none;
}

.sidebar-collapsed .nav-item {
  justify-content: center;
  padding: 10px;
}

.sidebar-collapsed .nav-item.active::before {
  left: 0;
}

/* Sidebar Footer */
.sidebar-footer {
  border-top: 1px solid var(--color-border);
  padding: 8px;
}

/* Main content */
.main-wrapper {
  margin-left: 240px;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  transition: margin-left 0.2s ease;
}

.sidebar-collapsed .main-wrapper {
  margin-left: 64px;
}

/* Top bar */
.top-bar {
  height: 60px;
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-6);
  position: sticky;
  top: 0;
  z-index: 50;
}

.page-title {
  font-size: var(--text-h3);
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0;
  font-family: var(--font-sans);
}

.top-bar-right {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.user-menu {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  cursor: pointer;
  padding: 6px 12px;
  border-radius: var(--radius-md);
  transition: background 0.15s ease;
}

.user-menu:hover {
  background: var(--color-surface-raised);
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--color-primary-light);
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--text-small);
  font-weight: 600;
  font-family: var(--font-sans);
}

.user-name {
  font-size: var(--text-body);
  font-weight: 500;
  color: var(--color-text-primary);
  font-family: var(--font-sans);
}

/* Main content */
.main-content {
  flex: 1;
  padding: var(--space-6);
}

/* ============================================
   Responsive
   ============================================ */
@media (max-width: 768px) {
  .sidebar {
    transform: translateX(-100%);
    transition: transform 0.2s ease;
  }

  .main-wrapper {
    margin-left: 0 !important;
  }

  .sidebar.mobile-open {
    transform: translateX(0);
    box-shadow: var(--elevation-4);
  }

  .top-bar {
    padding: 0 var(--space-4);
  }

  .main-content {
    padding: var(--space-4);
  }

  .user-name {
    display: none;
  }
}

@media (min-width: 769px) and (max-width: 1279px) {
  .sidebar {
    width: 64px;
    min-width: 64px;
  }

  .main-wrapper {
    margin-left: 64px;
  }

  .brand-text,
  .nav-text,
  .nav-section-title {
    display: none;
  }

  .nav-item {
    justify-content: center;
    padding: 10px;
  }
}
</style>
