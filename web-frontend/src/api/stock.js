import axios from 'axios'
import { ElMessage } from 'element-plus'

// 创建stock service专用的axios实例
const stockApi = axios.create({
  baseURL: 'http://localhost:8082/stock-service/api/v1',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 请求拦截器
stockApi.interceptors.request.use(
  (config) => {
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
stockApi.interceptors.response.use(
  (response) => {
    const { data } = response

    // 检查stock-service的统一响应格式
    if (data && typeof data === 'object') {
      if (data.success === false || data.error === true) {
        ElMessage.error(data.message || '请求失败')
        return Promise.reject(new Error(data.message || '请求失败'))
      }
      // 返回data字段中的实际数据
      return data.data || data
    }

    return data
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response

      switch (status) {
        case 400:
          ElMessage.error(data?.message || '请求参数错误')
          break
        case 404:
          ElMessage.error('股票数据不存在')
          break
        case 500:
          ElMessage.error(data?.message || '服务器错误')
          break
        case 502:
          ElMessage.error('数据服务暂时不可用')
          break
        default:
          ElMessage.error(data?.message || error.message || '请求失败')
      }
    } else if (error.request) {
      ElMessage.error('无法连接到股票数据服务')
    } else {
      ElMessage.error('请求配置错误')
    }

    return Promise.reject(error)
  }
)

// 股票数据API
export const stockServiceApi = {
  /**
   * 获取股票历史数据
   * @param {string} stockCode - 股票代码 (如: 000001.SZ)
   * @param {number} days - 查询天数 (默认30天)
   * @returns {Promise}
   */
  getStockHistory(stockCode, days = 30) {
    return stockApi.get(`/stocks/${stockCode}/history`, {
      params: { days }
    })
  },

  /**
   * 获取股票最新数据
   * @param {string} stockCode - 股票代码
   * @returns {Promise}
   */
  getStockLatest(stockCode) {
    return stockApi.get(`/stocks/${stockCode}/latest`)
  },

  /**
   * 获取股票基础信息
   * @param {string} stockCode - 股票代码
   * @returns {Promise}
   */
  getStockInfo(stockCode) {
    return stockApi.get(`/stocks/${stockCode}/info`)
  },

  /**
   * 计算股票收益率
   * @param {string} stockCode - 股票代码
   * @param {number} days - 计算天数
   * @returns {Promise}
   */
  getStockReturn(stockCode, days = 30) {
    return stockApi.get(`/stocks/${stockCode}/return`, {
      params: { days }
    })
  },

  /**
   * 批量获取股票数据
   * @param {Array<string>} stockCodes - 股票代码数组
   * @param {string} queryType - 查询类型 ('latest' 或 'history')
   * @param {number} days - 历史数据天数 (queryType为history时有效)
   * @returns {Promise}
   */
  getBatchStockData(stockCodes, queryType = 'latest', days = 30) {
    return stockApi.post('/stocks/batch/latest', {
      stockCodes,
      queryType,
      days
    })
  },

  /**
   * 异步获取股票历史数据
   * @param {string} stockCode - 股票代码
   * @param {number} days - 查询天数
   * @returns {Promise}
   */
  getStockHistoryAsync(stockCode, days = 30) {
    return stockApi.get(`/stocks/${stockCode}/history/async`, {
      params: { days }
    })
  },

  /**
   * 异步获取股票最新数据
   * @param {string} stockCode - 股票代码
   * @returns {Promise}
   */
  getStockLatestAsync(stockCode) {
    return stockApi.get(`/stocks/${stockCode}/latest/async`)
  },
}

export default stockServiceApi
