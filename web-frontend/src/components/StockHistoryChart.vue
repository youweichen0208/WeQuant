<template>
  <div class="stock-history-chart">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="title-section">
            <h3>{{ title }}</h3>
            <el-tag v-if="stockInfo" type="info" size="small">{{ stockInfo.market_type }}</el-tag>
          </div>
          <div class="controls">
            <el-input
              v-model="stockCode"
              placeholder="股票代码 (如: 000001.SZ)"
              style="width: 200px; margin-right: 10px"
              @keyup.enter="fetchData"
            >
              <template #prepend>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-select v-model="days" placeholder="时间范围" style="width: 120px; margin-right: 10px" @change="fetchData">
              <el-option label="5天" :value="5" />
              <el-option label="10天" :value="10" />
              <el-option label="30天" :value="30" />
              <el-option label="60天" :value="60" />
              <el-option label="90天" :value="90" />
            </el-select>
            <el-button type="primary" @click="fetchData" :loading="loading">
              <el-icon><Refresh /></el-icon>
              查询
            </el-button>
          </div>
        </div>
      </template>

      <!-- 加载状态 -->
      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="6" animated />
      </div>

      <!-- 股票信息概览 -->
      <div v-else-if="latestData && !loading" class="stock-overview">
        <el-row :gutter="20">
          <el-col :span="4">
            <div class="stat-item">
              <div class="label">最新价</div>
              <div class="value price" :class="priceChangeClass">
                {{ latestData.close }}
              </div>
            </div>
          </el-col>
          <el-col :span="4">
            <div class="stat-item">
              <div class="label">涨跌幅</div>
              <div class="value" :class="priceChangeClass">
                {{ latestData.pct_change }}%
              </div>
            </div>
          </el-col>
          <el-col :span="4">
            <div class="stat-item">
              <div class="label">开盘</div>
              <div class="value">{{ latestData.open }}</div>
            </div>
          </el-col>
          <el-col :span="4">
            <div class="stat-item">
              <div class="label">最高</div>
              <div class="value">{{ latestData.high }}</div>
            </div>
          </el-col>
          <el-col :span="4">
            <div class="stat-item">
              <div class="label">最低</div>
              <div class="value">{{ latestData.low }}</div>
            </div>
          </el-col>
          <el-col :span="4">
            <div class="stat-item">
              <div class="label">成交量</div>
              <div class="value">{{ formatVolume(latestData.volume) }}</div>
            </div>
          </el-col>
        </el-row>
      </div>

      <!-- 图表容器 -->
      <div ref="chartRef" class="chart-container" v-show="!loading && historyData"></div>

      <!-- 空状态 -->
      <el-empty v-if="!loading && !historyData" description="请输入股票代码查询数据" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { stockServiceApi } from '@/api/stock'

// Props
const props = defineProps({
  title: {
    type: String,
    default: '股票历史数据'
  },
  defaultStockCode: {
    type: String,
    default: '000001.SZ'
  },
  defaultDays: {
    type: Number,
    default: 30
  }
})

// 响应式数据
const stockCode = ref(props.defaultStockCode)
const days = ref(props.defaultDays)
const loading = ref(false)
const historyData = ref(null)
const latestData = ref(null)
const stockInfo = ref(null)
const chartRef = ref(null)
let chartInstance = null

// 计算属性
const priceChangeClass = computed(() => {
  if (!latestData.value || !latestData.value.pct_change) return ''
  return latestData.value.pct_change > 0 ? 'up' : latestData.value.pct_change < 0 ? 'down' : ''
})

// 格式化成交量
const formatVolume = (volume) => {
  if (!volume) return '0'
  if (volume >= 100000000) {
    return (volume / 100000000).toFixed(2) + '亿'
  }
  if (volume >= 10000) {
    return (volume / 10000).toFixed(2) + '万'
  }
  return volume.toFixed(0)
}

// 获取数据
const fetchData = async () => {
  if (!stockCode.value) {
    ElMessage.warning('请输入股票代码')
    return
  }

  loading.value = true
  try {
    // 并行获取历史数据和最新数据
    const [historyResponse, latestResponse, infoResponse] = await Promise.all([
      stockServiceApi.getStockHistory(stockCode.value, days.value),
      stockServiceApi.getStockLatest(stockCode.value),
      stockServiceApi.getStockInfo(stockCode.value)
    ])

    historyData.value = historyResponse
    latestData.value = latestResponse
    stockInfo.value = infoResponse

    // 渲染图表
    renderChart()

    ElMessage.success('数据加载成功')
  } catch (error) {
    console.error('获取股票数据失败:', error)
    historyData.value = null
    latestData.value = null
  } finally {
    loading.value = false
  }
}

// 渲染图表
const renderChart = () => {
  if (!chartRef.value || !historyData.value || !historyData.value.data) {
    return
  }

  // 初始化图表实例
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }

  // 准备数据
  const dates = historyData.value.data.map(item => item.date).reverse()
  const prices = historyData.value.data.map(item => item.close).reverse()
  const volumes = historyData.value.data.map(item => item.volume).reverse()
  const klineData = historyData.value.data.map(item => [
    item.open,
    item.close,
    item.low,
    item.high
  ]).reverse()

  // 配置图表选项
  const option = {
    title: {
      text: `${historyData.value.stock_code} ${historyData.value.stock_name || ''}`,
      left: 'center',
      textStyle: {
        fontSize: 18,
        fontWeight: 'bold'
      }
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross',
        crossStyle: {
          color: '#999'
        }
      },
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#ccc',
      borderWidth: 1,
      textStyle: {
        color: '#333'
      },
      formatter: function(params) {
        const data = params[0]
        if (!data) return ''

        const klineData = data.data
        const date = data.axisValue
        const volumeData = params[1]?.data || 0

        const formatNumber = (num) => {
          if (num >= 100000000) return (num / 100000000).toFixed(2) + '亿'
          if (num >= 10000) return (num / 10000).toFixed(2) + '万'
          return num.toFixed(2)
        }

        return `
          <div style="padding: 8px;">
            <div style="font-weight: bold; margin-bottom: 8px;">${date}</div>
            <div style="margin-bottom: 4px;">开盘: ${klineData[0]}</div>
            <div style="margin-bottom: 4px;">收盘: ${klineData[1]}</div>
            <div style="margin-bottom: 4px;">最低: ${klineData[2]}</div>
            <div style="margin-bottom: 4px;">最高: ${klineData[3]}</div>
            <div style="margin-bottom: 4px;">涨跌: ${(klineData[1] - klineData[0]).toFixed(2)}</div>
            <div>成交量: ${formatNumber(volumeData)}</div>
          </div>
        `
      }
    },
    legend: {
      data: ['K线', '成交量'],
      top: 30
    },
    grid: [
      {
        left: '60',
        right: '50',
        top: '12%',
        height: '60%'
      },
      {
        left: '60',
        right: '50',
        top: '76%',
        height: '16%'
      }
    ],
    xAxis: [
      {
        type: 'category',
        data: dates,
        boundaryGap: ['2%', '2%'],
        axisLine: { onZero: false },
        splitLine: { show: false },
        axisLabel: {
          showMaxLabel: true
        }
      },
      {
        type: 'category',
        gridIndex: 1,
        data: dates,
        boundaryGap: ['2%', '2%'],
        axisLine: { onZero: false },
        axisTick: { show: false },
        splitLine: { show: false },
        axisLabel: { show: false }
      }
    ],
    yAxis: [
      {
        scale: true,
        splitArea: {
          show: true
        }
      },
      {
        scale: true,
        gridIndex: 1,
        splitNumber: 2,
        axisLabel: { show: false },
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: { show: false }
      }
    ],
    dataZoom: [
      {
        type: 'inside',
        xAxisIndex: [0, 1],
        start: 0,
        end: 100
      },
      {
        show: true,
        xAxisIndex: [0, 1],
        type: 'slider',
        top: '94%',
        start: 0,
        end: 100,
        height: 25
      }
    ],
    series: [
      {
        name: 'K线',
        type: 'candlestick',
        data: klineData,
        itemStyle: {
          color: '#ec4d4d',
          color0: '#00c087',
          borderColor: '#ec4d4d',
          borderColor0: '#00c087',
          borderWidth: 2
        },
        barMaxWidth: 15
      },
      {
        name: '成交量',
        type: 'bar',
        xAxisIndex: 1,
        yAxisIndex: 1,
        data: volumes,
        itemStyle: {
          color: function(params) {
            const dataIndex = params.dataIndex
            if (dataIndex === 0) return 'rgba(0, 192, 135, 0.5)'
            return klineData[dataIndex][1] >= klineData[dataIndex][0] ? 'rgba(236, 77, 77, 0.5)' : 'rgba(0, 192, 135, 0.5)'
          }
        }
      }
    ]
  }

  chartInstance.setOption(option)
}

// 生命周期
onMounted(() => {
  // 初始加载数据
  if (props.defaultStockCode) {
    fetchData()
  }

  // 监听窗口大小变化
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (chartInstance) {
    chartInstance.dispose()
  }
})

// 处理窗口大小变化
const handleResize = () => {
  if (chartInstance) {
    chartInstance.resize()
  }
}
</script>

<style scoped>
.stock-history-chart {
  width: 100%;
}

.stock-history-chart :deep(.el-card) {
  width: 100%;
}

.stock-history-chart :deep(.el-card__body) {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.title-section {
  display: flex;
  align-items: center;
  gap: 10px;
}

.title-section h3 {
  margin: 0;
  font-size: 18px;
}

.controls {
  display: flex;
  align-items: center;
}

.loading-container {
  padding: 20px;
}

.stock-overview {
  margin-bottom: 20px;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 4px;
}

.stat-item {
  text-align: center;
}

.stat-item .label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}

.stat-item .value {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}

.stat-item .value.price {
  font-size: 24px;
}

.stat-item .value.up {
  color: #f56c6c;
}

.stat-item .value.down {
  color: #67c23a;
}

.chart-container {
  width: 100%;
  height: 800px;
  margin-top: 20px;
  min-height: 800px;
}
</style>
