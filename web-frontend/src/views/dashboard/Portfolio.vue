<template>
  <div class="placeholder-view">
    <div class="placeholder-content">
      <el-icon class="placeholder-icon">
        <component :is="iconComponent" />
      </el-icon>
      <h2 class="placeholder-title">{{ title }}</h2>
      <p class="placeholder-description">{{ description }}</p>
      <el-button type="primary" @click="goBack">返回仪表盘</el-button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { PieChart, TrendCharts, DataAnalysis, Setting } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const pageInfo = computed(() => {
  const info = {
    portfolio: {
      title: '投资组合',
      description: '管理您的投资组合，查看持仓情况和收益分析',
      icon: PieChart,
    },
    trading: {
      title: '交易中心',
      description: '实时行情数据和智能交易执行',
      icon: TrendCharts,
    },
    analysis: {
      title: '数据分析',
      description: '深度数据分析和量化策略回测',
      icon: DataAnalysis,
    },
    settings: {
      title: '系统设置',
      description: '账户设置和交易参数配置',
      icon: Setting,
    },
  }

  const routeName = route.name?.toLowerCase()
  return info[routeName] || {
    title: '功能开发中',
    description: '该功能正在开发中，敬请期待',
    icon: DataAnalysis,
  }
})

const title = computed(() => pageInfo.value.title)
const description = computed(() => pageInfo.value.description)
const iconComponent = computed(() => pageInfo.value.icon)

const goBack = () => {
  router.push('/dashboard')
}
</script>

<style scoped>
.placeholder-view {
  height: calc(100vh - 200px);
  display: flex;
  align-items: center;
  justify-content: center;
}

.placeholder-content {
  text-align: center;
  max-width: 400px;
  padding: 40px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.placeholder-icon {
  font-size: 64px;
  color: #1890ff;
  margin-bottom: 24px;
}

.placeholder-title {
  font-size: 24px;
  color: #333;
  margin: 0 0 16px 0;
}

.placeholder-description {
  font-size: 16px;
  color: #666;
  margin: 0 0 32px 0;
  line-height: 1.5;
}
</style>