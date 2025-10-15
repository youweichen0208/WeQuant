<template>
  <div class="stock-analysis-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>股票分析</h1>
      <p class="description">实时股票数据分析与可视化</p>
    </div>

    <!-- 股票选择器 -->
    <div class="stock-selector-section">
      <el-card class="selector-card">
        <template #header>
          <span>股票选择</span>
        </template>

        <div class="selector-content">
          <div class="search-area">
            <el-select
              v-model="selectedStock"
              filterable
              remote
              reserve-keyword
              placeholder="请输入股票代码或名称"
              style="width: 300px"
              @change="handleStockChange"
            >
              <el-option
                v-for="stock in popularStocks"
                :key="stock.code"
                :label="`${stock.code} - ${stock.name}`"
                :value="stock.code"
              />
            </el-select>

            <el-button
              type="primary"
              @click="handleSearch"
              :loading="chartLoading"
              style="margin-left: 10px"
            >
              查询
            </el-button>
          </div>

          <div class="quick-select">
            <span class="label">热门股票：</span>
            <el-tag
              v-for="stock in popularStocks.slice(0, 6)"
              :key="stock.code"
              :type="selectedStock === stock.code ? 'success' : 'info'"
              @click="selectStock(stock.code)"
              style="margin-right: 8px; cursor: pointer;"
            >
              {{ stock.name }}
            </el-tag>
          </div>
        </div>
      </el-card>
    </div>

    <!-- API状态检查 -->
    <div class="status-section" v-if="showStatus">
      <el-alert
        :title="apiStatus.title"
        :type="apiStatus.type"
        :description="apiStatus.description"
        show-icon
        :closable="false"
      />
    </div>

    <!-- 股票图表区域 -->
    <div class="chart-section" v-if="selectedStock">
      <StockChart
        ref="stockChartRef"
        :stock-code="selectedStock"
        :key="chartKey"
      />
    </div>

    <!-- 数据概览 -->
    <div class="overview-section" v-if="stockOverview">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-card class="overview-card">
            <div class="overview-item">
              <div class="label">当前价格</div>
              <div class="value price" :class="priceChangeClass">
                ¥{{ stockOverview.close?.toFixed(2) }}
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="overview-card">
            <div class="overview-item">
              <div class="label">涨跌幅</div>
              <div class="value" :class="priceChangeClass">
                {{ stockOverview.pct_change >= 0 ? '+' : '' }}{{ stockOverview.pct_change?.toFixed(2) }}%
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="overview-card">
            <div class="overview-item">
              <div class="label">成交量</div>
              <div class="value">
                {{ formatVolume(stockOverview.volume) }}
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="overview-card">
            <div class="overview-item">
              <div class="label">交易日期</div>
              <div class="value">
                {{ stockOverview.trade_date }}
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import StockChart from '@/components/StockChart.vue'
import { getStockLatest, healthCheck } from '@/api/stockHistory'

// 响应式数据
const selectedStock = ref('000001.SZ')
const chartLoading = ref(false)
const chartKey = ref(0) // 用于强制重新渲染图表
const stockChartRef = ref(null)
const stockOverview = ref(null)
const showStatus = ref(true)

// API状态
const apiStatus = ref({
  type: 'info',
  title: '正在检查API状态...',
  description: ''
})

// 热门股票列表
const popularStocks = ref([
  { code: '000001.SZ', name: '平安银行' },
  { code: '000002.SZ', name: '万科A' },
  { code: '600519.SH', name: '贵州茅台' },
  { code: '600036.SH', name: '招商银行' },
  { code: '000858.SZ', name: '五粮液' },
  { code: '600887.SH', name: '伊利股份' },
  { code: '000725.SZ', name: '京东方A' },
  { code: '600276.SH', name: '恒瑞医药' }
])

// 计算属性
const priceChangeClass = computed(() => {
  if (!stockOverview.value) return ''
  return stockOverview.value.pct_change >= 0 ? 'up' : 'down'
})

// 方法
const selectStock = (stockCode) => {
  selectedStock.value = stockCode
  handleSearch()
}

const handleStockChange = () => {
  handleSearch()
}

const handleSearch = async () => {
  if (!selectedStock.value) {
    ElMessage.warning('请选择股票')
    return
  }

  chartLoading.value = true
  try {
    // 获取股票概览数据
    const overview = await getStockLatest(selectedStock.value)
    stockOverview.value = overview

    // 强制重新渲染图表
    chartKey.value++

    ElMessage.success('数据加载成功')
  } catch (error) {
    console.error('Failed to load stock data:', error)
    ElMessage.error(error.message || '加载股票数据失败')
  } finally {
    chartLoading.value = false
  }
}

const formatVolume = (volume) => {
  if (!volume) return '-'
  if (volume >= 100000000) {
    return `${(volume / 100000000).toFixed(2)}亿`
  } else if (volume >= 10000) {
    return `${(volume / 10000).toFixed(2)}万`
  }
  return volume.toString()
}

const checkApiStatus = async () => {
  try {
    await healthCheck()
    apiStatus.value = {
      type: 'success',
      title: '✅ 服务连接正常',
      description: '股票数据服务运行正常，可以正常获取数据'
    }

    // 3秒后隐藏状态提示
    setTimeout(() => {
      showStatus.value = false
    }, 3000)
  } catch (error) {
    apiStatus.value = {
      type: 'error',
      title: '❌ 服务连接失败',
      description: `无法连接到股票数据服务: ${error.message}`
    }
  }
}

// 生命周期
onMounted(async () => {
  // 检查API状态
  await checkApiStatus()

  // 加载默认股票数据
  handleSearch()
})
</script>

<style scoped>
.stock-analysis-page {
  padding: 20px;
  background: #f5f5f5;
  min-height: calc(100vh - 60px);
}

.page-header {
  margin-bottom: 20px;
  text-align: center;
}

.page-header h1 {
  color: #303133;
  margin: 0;
  font-size: 28px;
}

.page-header .description {
  color: #909399;
  margin: 8px 0 0 0;
  font-size: 14px;
}

.stock-selector-section {
  margin-bottom: 20px;
}

.selector-card {
  max-width: 800px;
  margin: 0 auto;
}

.selector-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.search-area {
  display: flex;
  justify-content: center;
  align-items: center;
}

.quick-select {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 8px;
}

.quick-select .label {
  color: #606266;
  font-size: 14px;
  margin-right: 8px;
}

.status-section {
  margin-bottom: 20px;
  max-width: 800px;
  margin-left: auto;
  margin-right: auto;
}

.chart-section {
  margin-bottom: 30px;
  max-width: 1200px;
  margin-left: auto;
  margin-right: auto;
}

.overview-section {
  max-width: 1200px;
  margin: 0 auto;
}

.overview-card {
  text-align: center;
}

.overview-item {
  padding: 10px;
}

.overview-item .label {
  color: #909399;
  font-size: 14px;
  margin-bottom: 8px;
}

.overview-item .value {
  font-size: 20px;
  font-weight: bold;
  color: #303133;
}

.overview-item .value.price {
  font-size: 24px;
}

.overview-item .value.up {
  color: #ef5350;
}

.overview-item .value.down {
  color: #26a69a;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .stock-analysis-page {
    padding: 10px;
  }

  .search-area {
    flex-direction: column;
    gap: 10px;
  }

  .overview-section :deep(.el-col) {
    margin-bottom: 10px;
  }
}
</style>