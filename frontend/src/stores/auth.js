import { defineStore } from 'pinia'
import api from '../api/axios'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null,
    isAuthenticated: false
  }),

  getters: {
    isLoggedIn: (state) => state.isAuthenticated
  },

  actions: {
    async login(username, password) {
      // 本地开发模式：跳过 API 调用
      if (import.meta.env.DEV) {
        console.log('开发模式：模拟登录成功')
        const mockUser = {
          id: 1,
          username: username || 'admin',
          name: username || '管理员',
          role: 'admin'
        }
        this.user = mockUser
        this.isAuthenticated = true
        localStorage.setItem('user', JSON.stringify({ data: mockUser }))
        return true
      }

      try {
        console.log('authStore login 方法被调用，用户名:', username)
        // 去掉重复的/api前缀，因为axios已经配置了baseURL为/api
        const response = await api.post('/auth/login', {
          username,
          password
        })
        console.log('登录API返回结果:', response)

        // 确保获取到正确的用户数据
        const userData = response.data?.data || response.data || {};
        console.log('提取的用户数据:', userData);

        this.user = userData;
        console.log('设置 isAuthenticated 为 true')
        this.isAuthenticated = true
        localStorage.setItem('user', JSON.stringify(userData))
        console.log('localStorage 已更新')
        console.log('login 方法返回 true')
        return true
      } catch (error) {
        console.error('Login failed:', error)
        return false
      }
    },

    async logout() {
      try {
        // 去掉重复的/api前缀，因为axios已经配置了baseURL为/api
        await api.post('/auth/logout')
      } catch (error) {
        console.error('Logout failed:', error)
      } finally {
        this.user = null
        this.isAuthenticated = false
        localStorage.removeItem('user')
      }
    },

    async checkAuth() {
      try {
        // 去掉重复的/api前缀，因为axios已经配置了baseURL为/api
        const response = await api.get('/auth/me')
        
        // 确保获取到正确的用户数据
        const userData = response.data?.data || response.data || {};
        
        this.user = userData
        this.isAuthenticated = true
        localStorage.setItem('user', JSON.stringify(userData))
        return true
      } catch (error) {
        this.user = null
        this.isAuthenticated = false
        localStorage.removeItem('user')
        return false
      }
    },

    loadUserFromStorage() {
      const user = localStorage.getItem('user')
      if (user) {
        try {
          const userData = JSON.parse(user)
          console.log('从localStorage加载的原始数据:', userData);
          
          // 处理各种可能的数据结构
          let finalUserData = {};
          
          // 情况1：如果是完整的axios响应对象（包含status等字段）
          if (userData.status !== undefined && userData.data !== undefined) {
            console.log('检测到完整的axios响应对象');
            // 从响应中提取数据
            finalUserData = userData.data?.data || userData.data || {};
          } 
          // 情况2：如果是已经处理过的数据对象
          else {
            console.log('检测到普通对象');
            finalUserData = userData?.data || userData || {};
          }
          
          console.log('最终提取的用户数据:', finalUserData);
          
          // 验证用户数据的有效性，必须包含username或name字段
          if (finalUserData.username || finalUserData.name) {
            console.log('用户数据有效，设置用户名:', finalUserData.username || finalUserData.name);
            this.user = finalUserData;
            this.isAuthenticated = true;
          } else {
            console.log('用户数据无效，清除localStorage');
            // 用户数据无效，清除localStorage
            localStorage.removeItem('user');
            this.user = null;
            this.isAuthenticated = false;
          }
        } catch (error) {
          console.error('解析用户数据失败:', error);
          // JSON解析失败，清除localStorage
          localStorage.removeItem('user');
          this.user = null;
          this.isAuthenticated = false;
        }
      }
    }
  }
})