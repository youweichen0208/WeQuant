/**
 * API配置管理
 * 支持在开发阶段切换不同的API后端
 */

// 配置选项
export const API_CONFIGS = {
  // 方案A: 前端 → Java → Python (推荐生产环境)
  JAVA_BACKEND: {
    name: 'Java Stock History Service',
    baseURL: 'http://localhost:8081/api/v1',
    description: '通过Java微服务调用 (推荐)',
    endpoints: {
      health: '/stocks/health',
      history: '/stocks/{stockCode}/history',
      latest: '/stocks/{stockCode}/latest'
    }
  },

  // 方案B: 前端 → Python (快速开发测试)
  PYTHON_DIRECT: {
    name: 'Python Flask API',
    baseURL: 'http://localhost:5001/api',
    description: '直接调用Python API (开发测试)',
    endpoints: {
      health: '/health',
      history: '/stocks/{stockCode}/history',
      latest: '/stocks/{stockCode}/latest'
    }
  }
}

// 当前配置 (默认使用Java后端)
let currentConfig = API_CONFIGS.JAVA_BACKEND

// 获取当前配置
export const getCurrentConfig = () => {
  // 优先使用环境变量配置
  const envConfig = import.meta.env.VITE_API_CONFIG
  if (envConfig === 'PYTHON_DIRECT') {
    return API_CONFIGS.PYTHON_DIRECT
  }

  // 检查localStorage中的配置
  const savedConfig = localStorage.getItem('api_config')
  if (savedConfig && API_CONFIGS[savedConfig]) {
    return API_CONFIGS[savedConfig]
  }

  return currentConfig
}

// 切换配置
export const switchConfig = (configKey) => {
  if (API_CONFIGS[configKey]) {
    currentConfig = API_CONFIGS[configKey]
    localStorage.setItem('api_config', configKey)
    console.log(`API配置切换到: ${currentConfig.name}`)
    return true
  }
  return false
}

// 获取完整的API URL
export const getApiUrl = (endpoint) => {
  const config = getCurrentConfig()
  return config.baseURL + endpoint
}

// 获取当前配置名称
export const getCurrentConfigName = () => {
  return getCurrentConfig().name
}

export default {
  API_CONFIGS,
  getCurrentConfig,
  switchConfig,
  getApiUrl,
  getCurrentConfigName
}