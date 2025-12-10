<template>
  <div id="app">
    <!-- 导航栏 -->
    <el-header v-if="isAuthenticated && $route.meta.requiresAuth" class="navbar-header">
      <div class="navbar-container">
        <!-- Logo区域 -->
        <div class="navbar-brand">
          <span class="brand-logo">T</span>
          <span class="brand-name">raxbean</span>
        </div>
        
        <!-- 导航菜单 -->
        <el-menu
          :default-active="activeMenu"
          mode="horizontal"
          background-color="#1a202c"
          text-color="#fff"
          active-text-color="#409eff"
          class="navbar-menu"
          @select="handleMenuSelect"
        >
          <!-- 仪表盘 -->
          <el-menu-item index="/" @click="navigateToDashboard">
            <el-icon><DataAnalysis /></el-icon>
            <template #title>仪表盘</template>
          </el-menu-item>
          
          <!-- 业务管理 -->
          <el-sub-menu index="business">
            <template #title>
              <el-icon><OfficeBuilding /></el-icon>
              <span>业务管理</span>
            </template>
            <el-menu-item index="/patients" @click="navigateTo('/patients')">
              <el-icon><User /></el-icon>
              <template #title>病人管理</template>
            </el-menu-item>
            <el-menu-item index="app-users">
              <el-icon><Cellphone /></el-icon>
              <template #title>App用户管理</template>
            </el-menu-item>
            <!-- 设备管理二级菜单 -->
            <el-sub-menu index="devices">
              <template #title>
                <el-icon><Cpu /></el-icon>
                <span>设备管理</span>
              </template>
              <el-menu-item index="/devices" @click="navigateTo('/devices')">
                <el-icon><List /></el-icon>
                <template #title>设备列表</template>
              </el-menu-item>
            </el-sub-menu>
            <el-menu-item index="tasks">
              <el-icon><List /></el-icon>
              <template #title>任务管理</template>
            </el-menu-item>
          </el-sub-menu>
          
          <!-- 监控中心 -->
          <el-sub-menu index="monitoring">
            <template #title>
              <el-icon><Monitor /></el-icon>
              <span>监控中心</span>
            </template>
            <el-menu-item index="/realtime" @click="navigateTo('/realtime')">
              <el-icon><Location /></el-icon>
              <template #title>实时定位</template>
            </el-menu-item>
            <el-menu-item index="/fences" @click="navigateTo('/fences')">
              <el-icon><CircleCheck /></el-icon>
              <template #title>电子围栏</template>
            </el-menu-item>
          </el-sub-menu>
          
          <!-- 报警管理 -->
          <el-menu-item index="/alarms" @click="navigateTo('/alarms')">
            <el-icon><Warning /></el-icon>
            <template #title>报警管理</template>
          </el-menu-item>
          
          <!-- 系统 -->
          <el-sub-menu index="system">
            <template #title>
              <el-icon><Setting /></el-icon>
              <span>系统</span>
            </template>
            <el-menu-item index="permissions">
              <el-icon><Lock /></el-icon>
              <template #title>权限</template>
            </el-menu-item>
            <el-menu-item index="params">
              <el-icon><EditPen /></el-icon>
              <template #title>参数</template>
            </el-menu-item>
          </el-sub-menu>
          
          <!-- 退出登录 -->
          <el-menu-item index="logout" @click="logout">
            <el-icon><SwitchButton /></el-icon>
            <template #title>退出登录</template>
          </el-menu-item>
        </el-menu>
        
        <!-- 用户名显示 -->
        <div class="navbar-user">
          <el-dropdown>
            <span class="user-info">
              <el-icon><User /></el-icon>
              <span>{{ currentUsername }}</span>
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>{{ currentUsername }}</el-dropdown-item>
                <el-dropdown-item divided @click="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </el-header>
    <!-- 主内容区域 -->
    <div v-if="isAuthenticated && $route.meta.requiresAuth">
      <el-main class="main-content">
        <div class="content-wrapper">
          <div class="content-header">
            <h2 class="content-title">工作台</h2>
            <el-tag size="small" effect="light">
              <el-icon><RefreshRight /></el-icon>
              最新
            </el-tag>
          </div>
          <router-view />
        </div>
      </el-main>
    </div>
    <div v-else>
      <router-view />
    </div>
  </div>
</template>

<script setup>
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from './stores/auth'
import { onMounted, ref, computed } from 'vue'
// Element Plus 图标
import { 
  DataAnalysis, OfficeBuilding, ArrowDown, User, Cellphone, 
  Cpu, List, ArrowRight, Monitor, Location, CircleCheck, 
  Warning, Setting, Lock, EditPen, 
  SwitchButton, RefreshRight, View 
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

// 计算属性：是否已认证
const isAuthenticated = computed(() => authStore.isAuthenticated)

// 计算属性：当前激活的菜单
const activeMenu = computed(() => {
  return route.path
})

// 计算属性：当前用户名
const currentUsername = computed(() => {
  try {
    // 直接从localStorage读取并解析用户数据
    const userStr = localStorage.getItem('user')
    if (userStr) {
      const userData = JSON.parse(userStr)
      console.log('直接从localStorage读取的用户数据:', userData)
      
      // 简单直接的处理方式，针对已知的数据结构
      let username = ''
      
      // 情况1：完整的axios响应对象 -> userData.data.username
      if (userData.data && userData.data.username) {
        username = userData.data.username
        console.log('情况1 - 从userData.data.username获取:', username)
      }
      // 情况2：嵌套结构 -> userData.data.data.username
      else if (userData.data && userData.data.data && userData.data.data.username) {
        username = userData.data.data.username
        console.log('情况2 - 从userData.data.data.username获取:', username)
      }
      // 情况3：直接的用户对象 -> userData.username
      else if (userData.username) {
        username = userData.username
        console.log('情况3 - 从userData.username获取:', username)
      }
      // 情况4：备用方案 - 直接返回admin
      else {
        console.log('情况4 - 未找到用户名，使用备用方案')
        username = 'admin' // 直接使用已知的默认用户名
      }
      
      console.log('最终确定的用户名:', username)
      return username
    }
  } catch (error) {
    console.error('直接读取localStorage出错:', error)
  }
  
  return '未知用户'
})

// 菜单选择处理
const handleMenuSelect = (key, keyPath) => {
  console.log('Menu selected:', key, keyPath)
}

// 导航到指定路径
const navigateTo = (path) => {
  router.push(path)
}

// 导航到仪表盘
const navigateToDashboard = () => {
  router.push('/')
}

// 导航到设备列表
const navigateToDevices = () => {
  router.push('/devices')
}

const logout = async () => {
  await authStore.logout()
  router.push('/login')
}

// 应用启动时检查本地存储中的用户信息
onMounted(() => {
  authStore.loadUserFromStorage()
})
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: Arial, sans-serif;
  background-color: #f5f7fa;
}

#app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.navbar-header {
  background-color: #1a202c;
  color: white;
  padding: 0;
  height: 60px;
  line-height: 60px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}



.navbar-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: bold;
  color: white;
}

.brand-logo {
  width: 32px;
  height: 32px;
  background-color: #409eff;
  border-radius: 4px;
  display: flex;
  justify-content: center;
  align-items: center;
  font-weight: bold;
}

.brand-name {
  color: white;
}

/* 导航栏容器 */
.navbar-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  width: 100%;
}

/* 导航菜单样式 */
.navbar-menu {
  background-color: transparent;
  border: none;
  height: 60px;
  line-height: 60px;
}

/* 一级菜单样式 */
.navbar-menu .el-menu-item,
.navbar-menu .el-sub-menu__title {
  height: 60px;
  line-height: 60px;
  background-color: transparent;
  padding: 0 20px;
  margin: 0;
  color: white;
  transition: all 0.3s;
}

.navbar-menu .el-menu-item:hover,
.navbar-menu .el-sub-menu__title:hover {
  background-color: #2d3748;
  color: #409eff;
}

/* 二级菜单样式 */
.navbar-menu .el-sub-menu .el-menu {
  background-color: #1a202c;
  border: 1px solid #2d3748;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.navbar-menu .el-sub-menu .el-menu-item {
  height: 40px;
  line-height: 40px;
  padding: 0 20px;
  color: white;
}

.navbar-menu .el-sub-menu .el-menu-item:hover {
  background-color: #2d3748;
  color: #409eff;
}

/* 三级菜单样式 */
.navbar-menu .el-sub-menu .el-sub-menu .el-menu {
  background-color: #1a202c;
  border: 1px solid #2d3748;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  left: 100%;
  top: 0;
  margin-left: -1px;
}

/* 图标样式 */
.navbar-menu .el-icon {
  font-size: 16px;
  margin-right: 6px;
}

.navbar-menu .el-sub-menu__title .el-icon--right {
  margin-top: 0;
  margin-left: 4px;
}

/* 用户名显示样式 */
.navbar-user {
  display: flex;
  align-items: center;
  height: 60px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  color: white;
  padding: 0 12px;
  height: 60px;
  cursor: pointer;
  transition: all 0.3s;
}

.user-info:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.user-info .el-icon {
  font-size: 16px;
  margin-right: 4px;
}

.user-info .el-icon--right {
  margin-left: 4px;
  margin-top: 0;
}

.navbar-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  width: 100%;
}

.navbar-menu {
  flex: 1;
  background-color: transparent;
  border: none;
  height: 60px;
  line-height: 60px;
  margin: 0 20px;
}

/* 下拉菜单样式 */
.el-dropdown-menu {
  background-color: #1a202c;
  border: 1px solid #2d3748;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  padding: 8px 0;
}

.el-dropdown-menu__item {
  color: white;
  height: 36px;
  line-height: 36px;
  padding: 0 16px;
  font-size: 14px;
  transition: all 0.3s;
}

.el-dropdown-menu__item:hover {
  background-color: #2d3748;
  color: #409eff;
}

.el-dropdown-menu__item.disabled {
  color: #666;
  cursor: not-allowed;
}

.el-dropdown-menu__item.divided {
  border-top: 1px solid #2d3748;
  margin-top: 4px;
  padding-top: 4px;
}

/* 主内容区域 */
.main-content {
  flex: 1;
  background-color: #f5f7fa;
  padding: 20px;
}

.content-wrapper {
  max-width: 1200px;
  margin: 0 auto;
}

.content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.content-title {
  font-size: 20px;
  font-weight: bold;
  color: #333;
  margin: 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .navbar-container {
    padding: 0 10px;
  }
  
  .navbar-menu {
    overflow-x: auto;
  }
  
  .navbar-menu .el-menu-item,
  .navbar-menu .el-sub-menu__title {
    padding: 0 10px;
    font-size: 14px;
    white-space: nowrap;
  }
  
  .brand-name {
    display: none;
  }
}
</style>
