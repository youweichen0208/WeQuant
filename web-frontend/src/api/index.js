import api from './request'
import { stockServiceApi } from './stock'

// 用户认证相关API
export const authApi = {
  // 用户注册
  register(data) {
    return api.post('/users/register', data)
  },

  // 用户登录
  login(data) {
    return api.post('/users/login', data)
  },

  // 用户登出
  logout() {
    return api.post('/users/logout')
  },

  // 刷新令牌
  refreshToken(refreshToken) {
    return api.post('/users/refresh', { refreshToken })
  },

  // 获取用户信息
  getUserProfile() {
    return api.get('/users/profile')
  },

  // 更新用户信息
  updateUserProfile(data) {
    return api.put('/users/profile', data)
  },

  // 检查用户名可用性
  checkUsername(username) {
    return api.get('/users/check-username', { params: { username } })
  },

  // 检查邮箱可用性
  checkEmail(email) {
    return api.get('/users/check-email', { params: { email } })
  },
}

// 健康检查API
export const healthApi = {
  // 服务健康检查
  check() {
    return api.get('/health')
  },
}

// 导出股票服务API
export { stockServiceApi }

export default {
  auth: authApi,
  health: healthApi,
  stock: stockServiceApi,
}