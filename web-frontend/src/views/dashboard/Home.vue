<template>
  <div class="dashboard-home">
    <!-- 欢迎区域 -->
    <div class="welcome-section">
      <div class="welcome-content">
        <h1 class="welcome-title">
          欢迎回来，{{ authStore.userInfo?.fullName || authStore.userInfo?.username }}！
        </h1>
        <p class="welcome-subtitle">
          今天是 {{ formatDate(new Date()) }}，开始您的量化交易之旅
        </p>
      </div>
      <div class="welcome-stats">
        <div class="stat-item">
          <div class="stat-value">{{ formatCurrency(userStats.totalBalance) }}</div>
          <div class="stat-label">总资产</div>
        </div>
        <div class="stat-item">
          <div class="stat-value positive">+{{ userStats.dailyReturn }}%</div>
          <div class="stat-label">今日收益率</div>
        </div>
      </div>
    </div>

    <!-- 数据卡片 -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-card-header">
          <el-icon class="stat-icon balance"><Wallet /></el-icon>
          <div class="stat-info">
            <div class="stat-title">账户余额</div>
            <div class="stat-number">{{ formatCurrency(userStats.accountBalance) }}</div>
          </div>
        </div>
        <div class="stat-change positive">
          <el-icon><TrendCharts /></el-icon>
          +{{ userStats.balanceChange }}%
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-card-header">
          <el-icon class="stat-icon profit"><PieChart /></el-icon>
          <div class="stat-info">
            <div class="stat-title">持仓市值</div>
            <div class="stat-number">{{ formatCurrency(userStats.marketValue) }}</div>
          </div>
        </div>
        <div class="stat-change positive">
          <el-icon><TrendCharts /></el-icon>
          +{{ userStats.marketChange }}%
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-card-header">
          <el-icon class="stat-icon orders"><DataAnalysis /></el-icon>
          <div class="stat-info">
            <div class="stat-title">今日交易</div>
            <div class="stat-number">{{ userStats.todayTrades }}</div>
          </div>
        </div>
        <div class="stat-change">
          <el-icon><TrendCharts /></el-icon>
          比昨日 +{{ userStats.tradeChange }}
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-card-header">
          <el-icon class="stat-icon risk"><Warning /></el-icon>
          <div class="stat-info">
            <div class="stat-title">风险等级</div>
            <div class="stat-text">{{ getRiskLevelText(authStore.userInfo?.riskLevel) }}</div>
          </div>
        </div>
        <div class="stat-change">
          <el-tag :type="getRiskTagType(authStore.userInfo?.riskLevel)" size="small">
            {{ getRiskLevelText(authStore.userInfo?.riskLevel) }}
          </el-tag>
        </div>
      </div>
    </div>

    <!-- 快速操作 -->
    <div class="quick-actions">
      <div class="section-header">
        <h2>快速操作</h2>
      </div>
      <div class="action-grid">
        <div class="action-card" @click="router.push('/dashboard/trading')">
          <el-icon class="action-icon"><TrendCharts /></el-icon>
          <div class="action-title">开始交易</div>
          <div class="action-desc">查看市场行情并执行交易</div>
        </div>

        <div class="action-card" @click="router.push('/dashboard/portfolio')">
          <el-icon class="action-icon"><PieChart /></el-icon>
          <div class="action-title">投资组合</div>
          <div class="action-desc">管理您的投资组合</div>
        </div>

        <div class="action-card" @click="router.push('/dashboard/analysis')">
          <el-icon class="action-icon"><DataAnalysis /></el-icon>
          <div class="action-title">数据分析</div>
          <div class="action-desc">查看详细的交易分析</div>
        </div>

        <div class="action-card" @click="router.push('/dashboard/settings')">
          <el-icon class="action-icon"><Setting /></el-icon>
          <div class="action-title">账户设置</div>
          <div class="action-desc">调整交易参数和风险控制</div>
        </div>
      </div>
    </div>

    <!-- 股票历史数据图表 -->
    <div class="stock-chart-section">
      <StockHistoryChart
        title="股票实时行情"
        default-stock-code="000001.SZ"
        :default-days="30"
      />
    </div>

    <!-- 最近交易 -->
    <div class="recent-trades">
      <div class="section-header">
        <h2>最近交易</h2>
        <el-link type="primary" @click="router.push('/dashboard/trading')">查看全部</el-link>
      </div>
      <el-table :data="recentTrades" style="width: 100%">
        <el-table-column prop="symbol" label="代码" width="100" />
        <el-table-column prop="name" label="名称" width="150" />
        <el-table-column prop="type" label="类型" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.type === 'buy' ? 'success' : 'danger'" size="small">
              {{ scope.row.type === 'buy' ? '买入' : '卖出' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="100" />
        <el-table-column prop="price" label="价格" width="100">
          <template #default="scope">
            ¥{{ scope.row.price.toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column prop="time" label="时间" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import StockHistoryChart from '@/components/StockHistoryChart.vue'
import {
  Wallet,
  PieChart,
  DataAnalysis,
  TrendCharts,
  Warning,
  Setting,
} from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()

// 用户统计数据
const userStats = reactive({
  totalBalance: 125680.50,
  accountBalance: 89320.30,
  marketValue: 36360.20,
  dailyReturn: 2.34,
  balanceChange: 1.8,
  marketChange: 3.2,
  todayTrades: 8,
  tradeChange: 3,
})

// 最近交易数据
const recentTrades = ref([
  {
    symbol: '000001',
    name: '平安银行',
    type: 'buy',
    quantity: 1000,
    price: 13.45,
    time: '10:30:25',
  },
  {
    symbol: '000002',
    name: '万科A',
    type: 'sell',
    quantity: 500,
    price: 18.20,
    time: '09:45:12',
  },
  {
    symbol: '600000',
    name: '浦发银行',
    type: 'buy',
    quantity: 800,
    price: 8.95,
    time: '09:15:30',
  },
])

// 格式化日期
const formatDate = (date) => {
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'long',
  })
}

// 格式化货币
const formatCurrency = (amount) => {
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
  }).format(amount)
}

// 获取风险等级文本
const getRiskLevelText = (riskLevel) => {
  const levels = {
    CONSERVATIVE: '保守型',
    MODERATE: '稳健型',
    AGGRESSIVE: '激进型',
  }
  return levels[riskLevel] || '未设置'
}

// 获取风险标签类型
const getRiskTagType = (riskLevel) => {
  const types = {
    CONSERVATIVE: 'success',
    MODERATE: 'warning',
    AGGRESSIVE: 'danger',
  }
  return types[riskLevel] || 'info'
}

onMounted(() => {
  // 这里可以加载用户的实际数据
  console.log('仪表盘加载完成')
})
</script>

<style scoped>
.dashboard-home {
  max-width: 100%;
  margin: 0 auto;
  padding: 0 20px;
}

.welcome-section {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 32px;
  border-radius: 12px;
  margin-bottom: 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.welcome-title {
  font-size: 28px;
  font-weight: bold;
  margin: 0 0 8px 0;
}

.welcome-subtitle {
  font-size: 16px;
  opacity: 0.9;
  margin: 0;
}

.welcome-stats {
  display: flex;
  gap: 32px;
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 4px;
}

.stat-value.positive {
  color: #52c41a;
}

.stat-label {
  font-size: 14px;
  opacity: 0.8;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
  margin-bottom: 32px;
}

.stat-card {
  background: white;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: transform 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
}

.stat-card-header {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}

.stat-icon {
  font-size: 32px;
  margin-right: 16px;
  padding: 12px;
  border-radius: 8px;
}

.stat-icon.balance {
  background: #e6f7ff;
  color: #1890ff;
}

.stat-icon.profit {
  background: #f6ffed;
  color: #52c41a;
}

.stat-icon.orders {
  background: #fff7e6;
  color: #fa8c16;
}

.stat-icon.risk {
  background: #fff2e8;
  color: #fa541c;
}

.stat-title {
  font-size: 14px;
  color: #666;
  margin-bottom: 4px;
}

.stat-number {
  font-size: 24px;
  font-weight: bold;
  color: #333;
}

.stat-text {
  font-size: 18px;
  font-weight: 500;
  color: #333;
}

.stat-change {
  display: flex;
  align-items: center;
  font-size: 14px;
  color: #666;
}

.stat-change.positive {
  color: #52c41a;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-header h2 {
  margin: 0;
  font-size: 20px;
  color: #333;
}

.quick-actions {
  margin-bottom: 32px;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 16px;
}

.action-card {
  background: white;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: all 0.2s;
  text-align: center;
}

.action-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

.action-icon {
  font-size: 32px;
  color: #1890ff;
  margin-bottom: 12px;
}

.action-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}

.action-desc {
  font-size: 14px;
  color: #666;
}

.recent-trades {
  background: white;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.stock-chart-section {
  margin-bottom: 32px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .welcome-section {
    flex-direction: column;
    text-align: center;
    gap: 20px;
  }

  .welcome-stats {
    gap: 20px;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }

  .action-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 480px) {
  .welcome-section {
    padding: 20px;
  }

  .action-grid {
    grid-template-columns: 1fr;
  }
}
</style>