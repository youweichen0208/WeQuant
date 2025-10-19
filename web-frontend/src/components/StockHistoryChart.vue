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
            <!-- 热门股票快捷按钮 -->
            <div class="popular-stocks">
              <el-button
                v-for="stock in popularStocks"
                :key="stock.code"
                :type="stockCode === stock.code ? 'primary' : 'default'"
                size="small"
                @click="selectStock(stock.code)"
                class="stock-btn"
              >
                {{ stock.name }}
              </el-button>
            </div>

            <!-- 搜索和控制区域 -->
            <div class="search-controls">
              <el-autocomplete
                v-model="stockCode"
                :fetch-suggestions="querySearch"
                placeholder="搜索股票代码/名称"
                style="width: 220px; margin-right: 10px"
                @select="handleSelect"
                @keyup.enter="fetchData"
                clearable
              >
                <template #prepend>
                  <el-icon><Search /></el-icon>
                </template>
                <template #default="{ item }">
                  <div class="stock-item">
                    <span class="stock-code">{{ item.code }}</span>
                    <span class="stock-name">{{ item.name }}</span>
                  </div>
                </template>
              </el-autocomplete>

              <el-select v-model="days" placeholder="时间范围" style="width: 120px; margin-right: 10px" @change="fetchData">
                <el-option label="5天" :value="5" />
                <el-option label="10天" :value="10" />
                <el-option label="30天" :value="30" />
                <el-option label="60天" :value="60" />
                <el-option label="90天" :value="90" />
                <el-option label="180天" :value="180" />
              </el-select>

              <el-button type="primary" @click="fetchData" :loading="loading">
                <el-icon><Refresh /></el-icon>
                {{ loading ? '加载中' : '查询' }}
              </el-button>
            </div>
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

      <!-- 错误状态 -->
      <el-result
        v-if="!loading && error"
        icon="error"
        title="数据加载失败"
        :sub-title="error"
      >
        <template #extra>
          <el-button type="primary" @click="fetchData">
            <el-icon><Refresh /></el-icon>
            重新加载
          </el-button>
        </template>
      </el-result>

      <!-- 空状态 -->
      <el-empty
        v-else-if="!loading && !historyData"
        description="请输入股票代码查询数据"
        :image-size="120"
      >
        <template #description>
          <span style="color: #999; font-size: 14px;">
            支持搜索股票代码或名称，如：000001 或 平安银行
          </span>
        </template>
        <el-button type="primary" @click="selectStock('000001.SZ')">
          查看示例：平安银行
        </el-button>
      </el-empty>
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
const error = ref(null)
const historyData = ref(null)
const latestData = ref(null)
const stockInfo = ref(null)
const chartRef = ref(null)
let chartInstance = null

// 热门股票数据
const popularStocks = ref([
  { code: '000001.SZ', name: '平安银行' },
  { code: '000002.SZ', name: '万科A' },
  { code: '600036.SH', name: '招商银行' },
  { code: '600519.SH', name: '贵州茅台' },
  { code: '000858.SZ', name: '五粮液' },
  { code: '002415.SZ', name: '海康威视' }
])

// 股票搜索数据库（实际项目中应该从API获取）
const stockDatabase = ref([
  { code: '000001.SZ', name: '平安银行' },
  { code: '000002.SZ', name: '万科A' },
  { code: '600036.SH', name: '招商银行' },
  { code: '600519.SH', name: '贵州茅台' },
  { code: '000858.SZ', name: '五粮液' },
  { code: '002415.SZ', name: '海康威视' },
  { code: '600000.SH', name: '浦发银行' },
  { code: '601318.SH', name: '中国平安' },
  { code: '600887.SH', name: '伊利股份' },
  { code: '000858.SZ', name: '五粮液' },
  { code: '002304.SZ', name: '洋河股份' },
  { code: '601166.SH', name: '兴业银行' }
])

// 股票搜索功能
const querySearch = (queryString, cb) => {
  const results = queryString
    ? stockDatabase.value.filter(stock =>
        stock.code.toLowerCase().includes(queryString.toLowerCase()) ||
        stock.name.includes(queryString)
      )
    : stockDatabase.value
  cb(results.slice(0, 10)) // 限制显示前10个结果
}

// 选择股票
const handleSelect = (item) => {
  stockCode.value = item.code
  fetchData()
}

// 快捷选择热门股票
const selectStock = (code) => {
  stockCode.value = code
  fetchData()
}

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
  error.value = null

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
  } catch (err) {
    console.error('获取股票数据失败:', err)

    // 根据错误类型显示不同的错误信息
    if (err.response?.status === 404) {
      error.value = '未找到该股票代码，请检查代码是否正确'
    } else if (err.response?.status === 500) {
      error.value = '服务器内部错误，请稍后重试'
    } else if (err.code === 'NETWORK_ERROR') {
      error.value = '网络连接失败，请检查网络连接'
    } else {
      error.value = err.message || '获取股票数据失败，请稍后重试'
    }

    historyData.value = null
    latestData.value = null
    stockInfo.value = null

    ElMessage.error(error.value)
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

  // 获取当前容器尺寸来优化图表配置
  const containerRect = chartRef.value.getBoundingClientRect()
  const containerHeight = containerRect.height
  const isMobile = window.innerWidth <= 767
  const isTablet = window.innerWidth > 767 && window.innerWidth <= 1199

  // 根据容器大小调整图表配置
  const gridConfig = {
    // K线图网格配置
    main: {
      left: isMobile ? '50' : '60',
      right: isMobile ? '40' : '50',
      top: containerHeight > 500 ? '12%' : '10%',
      height: containerHeight > 600 ? '60%' : containerHeight > 400 ? '65%' : '70%'
    },
    // 成交量网格配置
    volume: {
      left: isMobile ? '50' : '60',
      right: isMobile ? '40' : '50',
      top: containerHeight > 600 ? '76%' : containerHeight > 400 ? '79%' : '82%',
      height: containerHeight > 600 ? '16%' : containerHeight > 400 ? '13%' : '10%'
    }
  }

  // 配置图表选项
  const option = {
    title: {
      text: `${historyData.value.stock_code} ${historyData.value.stock_name || ''}`,
      left: 'center',
      textStyle: {
        fontSize: isMobile ? 14 : isTablet ? 16 : 18,
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
        color: '#333',
        fontSize: isMobile ? 11 : 12
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
          <div style="padding: ${isMobile ? '6px' : '8px'};">
            <div style="font-weight: bold; margin-bottom: ${isMobile ? '6px' : '8px'};">${date}</div>
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
      top: isMobile ? 25 : 30,
      textStyle: {
        fontSize: isMobile ? 11 : 12
      }
    },
    grid: [
      {
        left: gridConfig.main.left,
        right: gridConfig.main.right,
        top: gridConfig.main.top,
        height: gridConfig.main.height
      },
      {
        left: gridConfig.volume.left,
        right: gridConfig.volume.right,
        top: gridConfig.volume.top,
        height: gridConfig.volume.height
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
          showMaxLabel: true,
          fontSize: isMobile ? 10 : 11
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
        },
        axisLabel: {
          fontSize: isMobile ? 10 : 11
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
        show: containerHeight > 400,
        xAxisIndex: [0, 1],
        type: 'slider',
        top: '94%',
        start: 0,
        end: 100,
        height: containerHeight > 600 ? 25 : 20
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
          borderWidth: isMobile ? 1 : 2
        },
        barMaxWidth: isMobile ? 8 : isTablet ? 12 : 15
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

  chartInstance.setOption(option, true) // 使用 true 参数强制重新渲染

  // 确保图表填满容器
  setTimeout(() => {
    if (chartInstance) {
      chartInstance.resize()
    }
  }, 100)
}

// 生命周期
onMounted(() => {
  // 初始加载数据
  if (props.defaultStockCode) {
    fetchData()
  }

  // 监听窗口大小变化
  window.addEventListener('resize', handleResize)

  // 初始化动态尺寸
  updateDynamicSizing()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (chartInstance) {
    chartInstance.dispose()
  }
})

// 动态计算图表尺寸
const updateDynamicSizing = () => {
  const viewport = {
    width: window.innerWidth,
    height: window.innerHeight,
    ratio: window.innerWidth / window.innerHeight
  }

  const chartContainer = chartRef.value
  if (!chartContainer) return

  // 计算其他元素占用的高度
  const header = document.querySelector('.card-header')?.offsetHeight || 0
  const overview = document.querySelector('.stock-overview')?.offsetHeight || 0
  const cardPadding = 48 // el-card padding
  const margins = 60 // margins and gaps

  const usedHeight = header + overview + cardPadding + margins

  // 根据屏幕比例和可用空间计算最优高度
  let optimalHeight

  if (viewport.ratio >= 21/9) {
    // 超宽屏：更多空间给图表
    optimalHeight = Math.max(viewport.height - usedHeight - 200, 400)
  } else if (viewport.ratio >= 16/9) {
    // 标准宽屏
    optimalHeight = Math.max(viewport.height - usedHeight - 250, 350)
  } else if (viewport.ratio >= 4/3) {
    // 普通屏幕
    optimalHeight = Math.max(viewport.height - usedHeight - 300, 300)
  } else {
    // 竖屏或方屏
    optimalHeight = Math.max(viewport.height - usedHeight - 350, 250)
  }

  // 限制最大和最小高度
  const maxHeight = Math.min(1200, viewport.height * 0.8)
  const minHeight = viewport.width <= 767 ? 250 : 300

  optimalHeight = Math.min(Math.max(optimalHeight, minHeight), maxHeight)

  // 应用动态高度
  chartContainer.style.setProperty('--dynamic-chart-height', `${optimalHeight}px`)
  chartContainer.style.height = `var(--dynamic-chart-height)`

  console.log(`动态图表尺寸: ${optimalHeight}px (视窗: ${viewport.width}x${viewport.height}, 比例: ${viewport.ratio.toFixed(2)})`)
}

// 处理窗口大小变化
const handleResize = () => {
  updateDynamicSizing()

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
  flex-direction: column;
  gap: 15px;
  min-width: 0;
  flex: 1;
}

.popular-stocks {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.stock-btn {
  border-radius: 20px;
  transition: all 0.3s ease;
  font-size: 12px;
  padding: 4px 12px;
}

.stock-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.search-controls {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.stock-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.stock-code {
  font-weight: bold;
  color: #409eff;
  margin-right: 10px;
}

.stock-name {
  color: #666;
  font-size: 14px;
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

/* 智能响应式图表容器 */
.chart-container {
  width: 100%;
  margin-top: 20px;
  position: relative;

  /* 基于JavaScript动态计算的高度优先，CSS媒体查询作为后备 */
  height: var(--dynamic-chart-height, calc(100vh - var(--chart-offset, 400px)));
  min-height: 300px;

  /* 使用CSS自定义属性来动态调整 */
  --chart-height-ratio: 0.6;
  --chart-min-height: 300px;
  --chart-max-height: 1000px;

  /* 平滑过渡 */
  transition: height 0.3s ease-out;
}

/* 宽屏显示器 (16:9 或更宽) */
@media (min-aspect-ratio: 16/9) {
  .chart-container {
    --chart-height-ratio: 0.7;
    --chart-offset: 350px;
    height: calc(100vh - var(--chart-offset));
    max-height: var(--chart-max-height);
  }
}

/* 超宽屏 (21:9) */
@media (min-aspect-ratio: 21/9) {
  .chart-container {
    --chart-height-ratio: 0.65;
    --chart-offset: 300px;
    height: calc(100vh - var(--chart-offset));
  }
}

/* 方形或竖屏比例 */
@media (max-aspect-ratio: 4/3) {
  .chart-container {
    --chart-height-ratio: 0.5;
    --chart-offset: 450px;
    height: calc(100vh - var(--chart-offset));
  }
}

/* 竖屏手机 */
@media (max-aspect-ratio: 9/16) {
  .chart-container {
    --chart-height-ratio: 0.4;
    --chart-offset: 500px;
    height: calc(100vh - var(--chart-offset));
    min-height: 250px;
  }
}

/* 基于视窗宽度的响应式断点 */
@media (min-width: 1920px) {
  .chart-container {
    --chart-offset: 320px;
    --chart-max-height: 1200px;
  }
}

@media (min-width: 1400px) and (max-width: 1919px) {
  .chart-container {
    --chart-offset: 360px;
    --chart-max-height: 1000px;
  }
}

@media (min-width: 1200px) and (max-width: 1399px) {
  .chart-container {
    --chart-offset: 380px;
    --chart-max-height: 800px;
  }
}

@media (min-width: 768px) and (max-width: 1199px) {
  .chart-container {
    --chart-offset: 420px;
    --chart-max-height: 700px;
  }

  .card-header {
    flex-direction: column;
    gap: 15px;
    align-items: stretch;
  }

  .title-section {
    justify-content: center;
  }

  .controls {
    gap: 12px;
  }

  .popular-stocks {
    justify-content: center;
  }

  .stock-btn {
    font-size: 11px;
    padding: 2px 8px;
  }

  .search-controls {
    justify-content: center;
  }

  .search-controls .el-autocomplete {
    width: 180px !important;
  }

  .search-controls .el-select {
    width: 100px !important;
  }

  .stock-overview {
    padding: 16px;
  }

  .stock-overview .el-col {
    margin-bottom: 16px;
  }
}

@media (max-width: 767px) {
  .chart-container {
    --chart-offset: 480px;
    --chart-max-height: 600px;
    min-height: 250px;
  }

  .popular-stocks {
    flex-wrap: wrap;
    gap: 6px;
  }

  .stock-btn {
    font-size: 10px;
    padding: 2px 6px;
  }

  .search-controls {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
  }

  .search-controls .el-autocomplete,
  .search-controls .el-select,
  .search-controls .el-button {
    width: 100% !important;
  }

  .stock-overview {
    padding: 12px;
  }

  .stock-overview .el-row {
    gap: 12px;
  }

  .stock-overview .el-col {
    span: 12;
    margin-bottom: 12px;
  }

  .stat-item .label {
    font-size: 11px;
  }

  .stat-item .value {
    font-size: 14px;
  }

  .stat-item .value.price {
    font-size: 18px;
  }
}

/* 横屏手机或小平板 */
@media (max-width: 767px) and (orientation: landscape) {
  .chart-container {
    --chart-offset: 250px;
    height: calc(100vh - var(--chart-offset));
    min-height: 200px;
  }
}
</style>
