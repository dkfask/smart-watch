<template>
  <div class="login-container">
    <!-- 左侧登录表单区域 -->
    <div class="left-section">
      <div class="login-card">
        <div class="brand-info">
          <div class="brand-name"></div>
          <h1 class="page-title">欢迎回来</h1>
        </div>
        <el-form :model="loginForm" :rules="rules" ref="loginFormRef">
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="用户名"
              class="login-input"
              clearable
            >
              <template #prefix>
                <img src="@/assets/images/icons/user.svg" class="custom-icon" />
              </template>
            </el-input>
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="密码"
              class="login-input"
              clearable
              show-password
            >
              <template #prefix>
                <img src="@/assets/images/icons/lock.svg" class="custom-icon" />
              </template>
            </el-input>
          </el-form-item>
          <div class="form-actions">
            <el-checkbox v-model="loginForm.remember" class="remember-checkbox">记住用户名</el-checkbox>
            <a href="#" class="forgot-password">忘记密码？</a>
          </div>
          <el-form-item>
            <el-button
              type="primary"
              @click="handleLogin"
              :loading="loading"
              class="login-btn"
            >
              登录
            </el-button>
          </el-form-item>
          <div class="register-guide">
            还没有账号？ <a href="#" class="register-link">立即注册</a>
          </div>
        </el-form>
      </div>
    </div>

    <!-- 右侧业务配图区域 -->
    <div class="right-section">
      <div class="illustration-container">
        <div class="illustration-content">
          <div class="illustration-title">智能手环定位系统</div>
          <div class="illustration-subtitle">实时定位 · 智能监控 · 数据分析</div>
          <div class="illustration-image"></div>
          <div class="illustration-features">
            <div class="feature-item">
              <div class="feature-icon"></div>
              <div class="feature-text">精准定位</div>
            </div>
            <div class="feature-item">
              <div class="feature-icon"></div>
              <div class="feature-text">智能监控</div>
            </div>
            <div class="feature-item">
              <div class="feature-icon"></div>
              <div class="feature-text">数据分析</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()
const loginFormRef = ref()
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: '',
  remember: false
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  if (!loginFormRef.value) return
  
  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        console.log('开始登录...')
        // 清除之前可能存在的认证状态
        authStore.isAuthenticated = false
        authStore.user = null
        localStorage.removeItem('user')
        
        // 调用authStore的login方法进行实际的API登录
        const success = await authStore.login(loginForm.username, loginForm.password)
        
        if (success) {
          ElMessage.success('登录成功')
          console.log('准备跳转到 /')
          try {
            await router.push('/')
            console.log('跳转完成')
          } catch (error) {
            console.error('router.push 抛出异常:', error)
          }
        } else {
          ElMessage.error('登录失败，用户名或密码错误')
        }
      } catch (error) {
        console.error('登录异常:', error)
        ElMessage.error('登录失败，请稍后重试')
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
.login-container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  min-height: 100vh;
  background-color: #f5f7fa;
  overflow: hidden;
}

/* 左侧登录表单区域 */
.left-section {
  position: relative;
  background-color: #ffffff;
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
  box-shadow: 2px 0 10px rgba(0, 0, 0, 0.05);
}

.login-card {
  width: 100%;
  max-width: 400px;
  padding: 40px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.brand-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.brand-name {
  font-size: 18px;
  font-weight: 600;
  color: #666666;
  letter-spacing: 0.5px;
}

.page-title {
  font-size: 32px;
  font-weight: 700;
  color: #333333;
  line-height: 1.2;
}

/* 表单样式 */
.login-input {
  height: 48px;
  border-radius: 8px;
  font-size: 14px;
  border-color: #e0e0e0;
  transition: all 0.3s ease;
  background-color: #fafafa;
}

.login-input:hover {
  border-color: #9c27b0;
  box-shadow: 0 0 0 2px rgba(156, 39, 176, 0.1);
  background-color: #ffffff;
}

.login-input:focus {
  border-color: #9c27b0;
  box-shadow: 0 0 0 2px rgba(156, 39, 176, 0.2);
  background-color: #ffffff;
}

/* 表单操作区域 */
.form-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.remember-checkbox {
  font-size: 14px;
  color: #666666;
  cursor: pointer;
}

.forgot-password {
  font-size: 14px;
  color: #9c27b0;
  text-decoration: none;
  transition: color 0.3s ease;
}

.forgot-password:hover {
  color: #7b1fa2;
  text-decoration: underline;
}

/* 登录按钮 */
.login-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #9c27b0 0%, #673ab7 100%);
  border: none;
  border-radius: 8px;
  color: #ffffff;
  letter-spacing: 0.5px;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(156, 39, 176, 0.3);
}

.login-btn:hover {
  background: linear-gradient(135deg, #7b1fa2 0%, #512da8 100%);
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(156, 39, 176, 0.4);
  color: #ffffff;
}

.login-btn:active {
  transform: translateY(0);
  box-shadow: 0 2px 8px rgba(156, 39, 176, 0.3);
}

/* 注册引导 */
.register-guide {
  text-align: center;
  font-size: 14px;
  color: #666666;
  margin-top: 8px;
}

.register-link {
  color: #9c27b0;
  text-decoration: none;
  font-weight: 600;
  transition: color 0.3s ease;
}

.register-link:hover {
  color: #7b1fa2;
  text-decoration: underline;
}

/* 右侧业务配图区域 */
.right-section {
  position: relative;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
}

.illustration-container {
  width: 100%;
  max-width: 500px;
  padding: 40px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 32px;
}

.illustration-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
}

.illustration-title {
  font-size: 28px;
  font-weight: 700;
  color: #ffffff;
  letter-spacing: 0.5px;
}

.illustration-subtitle {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.8);
  letter-spacing: 0.5px;
}

.illustration-image {
  width: 300px;
  height: 200px;
  background-color: rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  position: relative;
  overflow: hidden;
  display: flex;
  justify-content: center;
  align-items: center;
  backdrop-filter: blur(10px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}

.illustration-image::before {
  content: '';
  position: absolute;
  width: 100%;
  height: 100%;
  background-image: linear-gradient(135deg, rgba(255, 255, 255, 0.1) 0%, rgba(255, 255, 255, 0.05) 100%);
  opacity: 0.5;
}

.illustration-features {
  display: flex;
  gap: 40px;
  margin-top: 16px;
}

.feature-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #ffffff;
}

.feature-icon {
  width: 40px;
  height: 40px;
  background-color: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
}

.feature-text {
  font-size: 14px;
  font-weight: 500;
}

/* 自定义图标样式 */
.custom-icon {
  width: 18px;
  height: 18px;
  color: #909399;
  vertical-align: middle;
  fill: currentColor;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .login-container {
    grid-template-columns: 1fr;
    grid-template-rows: auto 1fr;
  }

  .left-section {
    grid-row: 2;
  }

  .right-section {
    grid-row: 1;
    min-height: 300px;
  }

  .login-card {
    padding: 30px 20px;
    margin: 20px;
  }

  .page-title {
    font-size: 24px;
  }

  .illustration-container {
    padding: 30px 20px;
  }

  .illustration-title {
    font-size: 22px;
  }

  .illustration-image {
    width: 250px;
    height: 160px;
  }

  .illustration-features {
    gap: 24px;
  }
}
</style>
