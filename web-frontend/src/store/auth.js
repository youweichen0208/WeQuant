import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api'
import { ElMessage } from 'element-plus'
import router from '@/router'

export const useAuthStore = defineStore('auth', () => {
  // State
  const token = ref(localStorage.getItem('token') || '')
  const refreshToken = ref(localStorage.getItem('refreshToken') || '')
  const user = ref(null)
  const isLoading = ref(false)

  // Getters
  const isAuthenticated = computed(() => !!token.value)
  const userInfo = computed(() => user.value)

  // Actions
  const login = async (credentials) => {
    try {
      isLoading.value = true
      const response = await authApi.login(credentials)

      if (response.success) {
        const { accessToken, refreshToken: newRefreshToken, user: userInfo } = response.data

        // 保存令牌
        token.value = accessToken
        refreshToken.value = newRefreshToken
        user.value = userInfo

        // 持久化存储
        localStorage.setItem('token', accessToken)
        localStorage.setItem('refreshToken', newRefreshToken)
        localStorage.setItem('user', JSON.stringify(userInfo))

        ElMessage.success('登录成功')
        router.push('/dashboard')

        return response
      }
    } catch (error) {
      ElMessage.error(error.message || '登录失败')
      throw error
    } finally {
      isLoading.value = false
    }
  }

  const register = async (userData) => {
    try {
      isLoading.value = true
      const response = await authApi.register(userData)

      if (response.success) {
        ElMessage.success('注册成功，请登录')
        router.push('/login')
        return response
      }
    } catch (error) {
      ElMessage.error(error.message || '注册失败')
      throw error
    } finally {
      isLoading.value = false
    }
  }

  const logout = async () => {
    try {
      // 调用后端登出接口
      if (token.value) {
        await authApi.logout()
      }
    } catch (error) {
      console.error('登出请求失败:', error)
    } finally {
      // 清除本地状态
      clearAuth()
      ElMessage.success('已退出登录')
      router.push('/login')
    }
  }

  const clearAuth = () => {
    token.value = ''
    refreshToken.value = ''
    user.value = null

    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('user')
  }

  const checkAuth = async () => {
    const storedToken = localStorage.getItem('token')
    const storedUser = localStorage.getItem('user')

    if (storedToken && storedUser) {
      token.value = storedToken
      user.value = JSON.parse(storedUser)

      try {
        // 验证令牌有效性
        const response = await authApi.getUserProfile()
        if (response.success) {
          user.value = response.data
          localStorage.setItem('user', JSON.stringify(response.data))
        }
      } catch (error) {
        // 令牌无效，清除认证状态
        clearAuth()
      }
    }
  }

  const updateProfile = async (profileData) => {
    try {
      isLoading.value = true
      const response = await authApi.updateUserProfile(profileData)

      if (response.success) {
        user.value = response.data
        localStorage.setItem('user', JSON.stringify(response.data))
        ElMessage.success('个人信息更新成功')
        return response
      }
    } catch (error) {
      ElMessage.error(error.message || '更新失败')
      throw error
    } finally {
      isLoading.value = false
    }
  }

  const refreshAuthToken = async () => {
    try {
      if (!refreshToken.value) {
        throw new Error('没有刷新令牌')
      }

      const response = await authApi.refreshToken(refreshToken.value)

      if (response.success) {
        const { accessToken, refreshToken: newRefreshToken } = response.data

        token.value = accessToken
        refreshToken.value = newRefreshToken

        localStorage.setItem('token', accessToken)
        localStorage.setItem('refreshToken', newRefreshToken)

        return accessToken
      }
    } catch (error) {
      clearAuth()
      router.push('/login')
      throw error
    }
  }

  return {
    // State
    token,
    refreshToken,
    user,
    isLoading,

    // Getters
    isAuthenticated,
    userInfo,

    // Actions
    login,
    register,
    logout,
    clearAuth,
    checkAuth,
    updateProfile,
    refreshAuthToken,
  }
})