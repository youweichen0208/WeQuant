<template>
  <div class="stock-chart-container">
    <div class="chart-header">
      <div class="stock-info">
        <h3>{{ stockCode }}</h3>
        <div class="latest-info" v-if="latestData">
          <span class="price" :class="latestData.pct_change >= 0 ? 'up' : 'down'">
            ¥{{ latestData.close?.toFixed(2) }}
          </span>
          <span class="change" :class="latestData.pct_change >= 0 ? 'up' : 'down'">
            {{ latestData.pct_change >= 0 ? '+' : '' }}{{ latestData.pct_change?.toFixed(2) }}%
          </span>
          <span class="date">{{ latestData.trade_date }}</span>
        </div>
      </div>
      <div class="chart-controls">
        <el-radio-group v-model="selectedDays" @change="handleDaysChange" size="small">
          <el-radio-button :label="7">7天</el-radio-button>
          <el-radio-button :label="30">30天</el-radio-button>
          <el-radio-button :label="90">90天</el-radio-button>
          <el-radio-button :label="180">180天</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <div class="chart-body" v-loading="loading">
      <v-chart
        v-if="!loading && chartOption"
        class="chart"
        :option="chartOption"
        autoresize
      />
      <el-empty v-if="!loading && !chartOption" description="暂无数据" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { CandlestickChart, LineChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  DataZoomComponent
} from 'echarts/components'
import { getStockHistory, getStockLatest } from '@/api/stockHistory'

// 注册ECharts组件
use([
  CanvasRenderer,
  CandlestickChart,
  LineChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  DataZoomComponent
])

// Props
const props = defineProps({
  stockCode: {
    type: String,
    required: true,
    default: '000001.SZ'
  },
  initialDays: {
    type: Number,
    default: 30
  }
})

// 响应式数据
const loading = ref(false)
const selectedDays = ref(props.initialDays)
const historyData = ref(null)
const latestData = ref(null)

// 图表配置
const chartOption = computed(() => {
  if (!historyData.value || !historyData.value.data) {
    return null
  }

  const data = historyData.value.data

  // 提取数据
  const dates = data.map(item => item.date)
  const values = data.map(item => [
    item.open,
    item.close,
    item.low,
    item.high
  ])
  const volumes = data.map(item => item.volume)

  return {
    animation: true,
    legend: {
      bottom: 10,
      left: 'center',
      data: ['K线', '成交量']
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross'
      },
      borderWidth: 1,
      borderColor: '#ccc',
      padding: 10,
      textStyle: {
        color: '#000'
      },
      formatter: function (params) {
        const dataIndex = params[0].dataIndex
        const item = data[dataIndex]
        return `
          <div style="font-weight: bold;">${item.date}</div>
          <div>开盘: ${item.open?.toFixed(2)}</div>
          <div>收盘: ${item.close?.toFixed(2)}</div>
          <div>最高: ${item.high?.toFixed(2)}</div>
          <div>最低: ${item.low?.toFixed(2)}</div>
          <div>涨跌幅: ${item.pct_change?.toFixed(2)}%</div>
          <div>成交量: ${(item.volume / 10000).toFixed(2)}万</div>
        `
      }
    },
    axisPointer: {
      link: [{ xAxisIndex: 'all' }],
      label: {
        backgroundColor: '#777'
      }
    },
    grid: [
      {
        left: '10%',
        right: '8%',
        height: '50%'
      },
      {
        left: '10%',
        right: '8%',
        top: '70%',
        height: '15%'
      }
    ],
    xAxis: [
      {
        type: 'category',
        data: dates,
        boundaryGap: false,
        axisLine: { onZero: false },
        splitLine: { show: false },
        min: 'dataMin',
        max: 'dataMax'
      },
      {
        type: 'category',
        gridIndex: 1,
        data: dates,
        boundaryGap: false,
        axisLine: { onZero: false },
        axisTick: { show: false },
        splitLine: { show: false },
        axisLabel: { show: false },
        min: 'dataMin',
        max: 'dataMax'
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
        bottom: 60,
        start: 0,
        end: 100
      }
    ],
    series: [
      {
        name: 'K线',
        type: 'candlestick',
        data: values,
        itemStyle: {
          color: '#ef5350',
          color0: '#26a69a',
          borderColor: '#ef5350',
          borderColor0: '#26a69a'
        }
      },
      {
        name: '成交量',
        type: 'bar',
        xAxisIndex: 1,
        yAxisIndex: 1,
        data: volumes,
        itemStyle: {
          color: function (params) {
            const dataIndex = params.dataIndex
            const item = data[dataIndex]
            return item.close >= item.open ? '#ef5350' : '#26a69a'
          }
        }
      }
    ]
  }
})

// 方法
const fetchData = async () => {
  loading.value = true
  try {
    // 并行获取历史数据和最新数据
    const [history, latest] = await Promise.all([
      getStockHistory(props.stockCode, selectedDays.value),
      getStockLatest(props.stockCode)
    ])

    historyData.value = history
    latestData.value = latest

    console.log(`Loaded ${history.count} data points for ${props.stockCode}`)
  } catch (error) {
    console.error('Failed to fetch stock data:', error)
    ElMessage.error(error.message || '加载股票数据失败')
  } finally {
    loading.value = false
  }
}

const handleDaysChange = () => {
  fetchData()
}

// 生命周期
onMounted(() => {
  fetchData()
})

// 暴露方法供父组件调用
defineExpose({
  refresh: fetchData
})
</script>

<style scoped>
.stock-chart-container {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.stock-info h3 {
  margin: 0 0 8px 0;
  font-size: 20px;
  color: #303133;
}

.latest-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.price {
  font-size: 24px;
  font-weight: bold;
}

.price.up {
  color: #ef5350;
}

.price.down {
  color: #26a69a;
}

.change {
  font-size: 16px;
  font-weight: 500;
}

.change.up {
  color: #ef5350;
}

.change.down {
  color: #26a69a;
}

.date {
  color: #909399;
  font-size: 14px;
}

.chart-body {
  min-height: 500px;
}

.chart {
  width: 100%;
  height: 500px;
}
</style>
