/**
 * 股票历史数据API服务
 * 支持调用Java后端或直接调用Python API
 */
import axios from 'axios'
import { getCurrentConfig } from '@/config/apiConfig'

// 动态获取API配置

// 创建动态axios实例
const createStockApi = () => {
  const config = getCurrentConfig()
  return axios.create({
    baseURL: config.baseURL,
    timeout: 30000,
    headers: {
      'Content-Type': 'application/json'
    }
  })
}

// 配置拦截器
const setupInterceptors = (apiInstance) => {
  // 请求拦截器 - 添加token
  apiInstance.interceptors.request.use(
    config => {
      const token = localStorage.getItem('token')
      if (token) {
        config.headers.Authorization = `Bearer ${token}`
      }
      return config
    },
    error => {
      return Promise.reject(error)
    }
  )

  // 响应拦截器 - 统一错误处理
  apiInstance.interceptors.response.use(
    response => response.data,
    error => {
      console.error('Stock History API Error:', error)

      if (error.response) {
        const { status, data } = error.response

        switch (status) {
          case 404:
            return Promise.reject(new Error(data.error || '股票数据不存在'))
          case 500:
            return Promise.reject(new Error(data.error || '服务器错误'))
          default:
            return Promise.reject(new Error(data.error || '请求失败'))
        }
      }

      return Promise.reject(new Error('网络错误，请检查连接'))
    }
  )
}

/**
 * 获取股票历史数据
 * @param {string} stockCode - 股票代码，如 000001.SZ
 * @param {number} days - 获取天数，默认30天
 * @returns {Promise} 历史数据
 */
export const getStockHistory = (stockCode, days = 30) => {
  const api = createStockApi()
  setupInterceptors(api)
  const config = getCurrentConfig()

  const url = config.endpoints.history.replace('{stockCode}', stockCode)
  return api.get(url, { params: { days } })
}

/**
 * 获取股票最新数据
 * @param {string} stockCode - 股票代码
 * @returns {Promise} 最新数据
 */
export const getStockLatest = (stockCode) => {
  const api = createStockApi()
  setupInterceptors(api)
  const config = getCurrentConfig()

  const url = config.endpoints.latest.replace('{stockCode}', stockCode)
  return api.get(url)
}

/**
 * 健康检查
 * @returns {Promise}
 */
export const healthCheck = () => {
  const api = createStockApi()
  setupInterceptors(api)
  const config = getCurrentConfig()

  return api.get(config.endpoints.health)
}

export default {
  getStockHistory,
  getStockLatest,
  healthCheck
}
