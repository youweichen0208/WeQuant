<template>
  <div class="trading-interface">
    <!-- 账户信息卡片 -->
    <el-row :gutter="20" class="account-section">
      <el-col :span="24">
        <el-card class="account-card">
          <template #header>
            <div class="card-header">
              <h3>模拟交易账户</h3>
              <el-button v-if="!accountId" type="primary" @click="createAccount">
                创建模拟账户
              </el-button>
            </div>
          </template>

          <div v-if="accountInfo" class="account-info">
            <el-row :gutter="20">
              <el-col :span="6">
                <div class="stat-item">
                  <div class="label">总资产</div>
                  <div class="value">{{ formatCurrency(accountInfo.total_assets) }}</div>
                </div>
              </el-col>
              <el-col :span="6">
                <div class="stat-item">
                  <div class="label">可用资金</div>
                  <div class="value">{{ formatCurrency(accountInfo.balance) }}</div>
                </div>
              </el-col>
              <el-col :span="6">
                <div class="stat-item">
                  <div class="label">持仓市值</div>
                  <div class="value">{{ formatCurrency(accountInfo.market_value) }}</div>
                </div>
              </el-col>
              <el-col :span="6">
                <div class="stat-item">
                  <div class="label">持仓数量</div>
                  <div class="value">{{ accountInfo.position_count }}</div>
                </div>
              </el-col>
            </el-row>
          </div>

          <div v-else-if="!accountId" class="no-account">
            <el-empty description="请先创建模拟交易账户">
              <el-button type="primary" @click="createAccount">
                创建账户（获得100万虚拟资金）
              </el-button>
            </el-empty>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 交易区域 -->
    <el-row :gutter="20" class="trading-section" v-if="accountId">
      <el-col :span="12">
        <!-- 买卖交易面板 -->
        <el-card class="trading-panel">
          <template #header>
            <div class="card-header">
              <h4>股票交易</h4>
            </div>
          </template>

          <el-form :model="tradeForm" label-width="80px">
            <el-form-item label="股票代码">
              <el-autocomplete
                v-model="tradeForm.stockCode"
                :fetch-suggestions="queryStocks"
                placeholder="输入股票代码或名称"
                style="width: 100%"
                @select="handleStockSelect"
                clearable
              >
                <template #prepend>
                  <el-icon><Search /></el-icon>
                </template>
                <template #default="{ item }">
                  <div class="stock-suggestion">
                    <span class="code">{{ item.code }}</span>
                    <span class="name">{{ item.name }}</span>
                  </div>
                </template>
              </el-autocomplete>
            </el-form-item>

            <el-form-item label="当前价格" v-if="currentPrice">
              <div class="price-info">
                <span class="price">¥{{ currentPrice.price }}</span>
                <span :class="['change', currentPrice.change_pct >= 0 ? 'up' : 'down']">
                  {{ currentPrice.change_pct >= 0 ? '+' : '' }}{{ currentPrice.change_pct }}%
                </span>
              </div>
            </el-form-item>

            <el-form-item label="交易类型">
              <el-radio-group v-model="tradeForm.type">
                <el-radio label="buy" size="large">买入</el-radio>
                <el-radio label="sell" size="large">卖出</el-radio>
              </el-radio-group>
            </el-form-item>

            <el-form-item label="数量">
              <el-input-number
                v-model="tradeForm.quantity"
                :min="100"
                :step="100"
                placeholder="最少100股"
                style="width: 100%"
              />
            </el-form-item>

            <el-form-item label="预估金额" v-if="estimatedAmount">
              <div class="amount-info">
                <div>交易金额: ¥{{ estimatedAmount.amount }}</div>
                <div>手续费: ¥{{ estimatedAmount.commission }}</div>
                <div class="total">总计: ¥{{ estimatedAmount.total }}</div>
              </div>
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                :loading="trading"
                @click="executeTrade"
                :disabled="!canTrade"
                size="large"
                style="width: 100%"
              >
                {{ tradeForm.type === 'buy' ? '买入' : '卖出' }}
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="12">
        <!-- 持仓信息 -->
        <el-card class="positions-panel">
          <template #header>
            <div class="card-header">
              <h4>当前持仓</h4>
              <el-button @click="refreshAccount" :loading="loading">
                <el-icon><Refresh /></el-icon>
                刷新
              </el-button>
            </div>
          </template>

          <el-table
            :data="accountInfo?.positions || []"
            style="width: 100%"
            :height="400"
          >
            <el-table-column prop="stock_code" label="代码" width="100" />
            <el-table-column prop="stock_name" label="名称" width="100" />
            <el-table-column prop="quantity" label="持仓" width="80" />
            <el-table-column prop="avg_cost" label="成本价" width="80">
              <template #default="scope">
                ¥{{ scope.row.avg_cost.toFixed(2) }}
              </template>
            </el-table-column>
            <el-table-column prop="current_price" label="现价" width="80">
              <template #default="scope">
                ¥{{ scope.row.current_price.toFixed(2) }}
              </template>
            </el-table-column>
            <el-table-column prop="profit_loss_pct" label="盈亏" width="80">
              <template #default="scope">
                <span :class="scope.row.profit_loss >= 0 ? 'profit' : 'loss'">
                  {{ scope.row.profit_loss >= 0 ? '+' : '' }}{{ scope.row.profit_loss_pct.toFixed(2) }}%
                </span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="scope">
                <el-button
                  type="danger"
                  size="small"
                  @click="quickSell(scope.row)"
                >
                  卖出
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 交易历史 -->
    <el-row :gutter="20" class="history-section" v-if="accountId">
      <el-col :span="24">
        <el-card class="history-panel">
          <template #header>
            <div class="card-header">
              <h4>交易历史</h4>
            </div>
          </template>

          <el-table
            :data="tradeHistory"
            style="width: 100%"
            :height="300"
          >
            <el-table-column prop="trade_time" label="时间" width="180">
              <template #default="scope">
                {{ formatTime(scope.row.trade_time) }}
              </template>
            </el-table-column>
            <el-table-column prop="stock_code" label="代码" width="100" />
            <el-table-column prop="stock_name" label="名称" width="120" />
            <el-table-column prop="trade_type" label="类型" width="80">
              <template #default="scope">
                <el-tag :type="scope.row.trade_type === 'buy' ? 'success' : 'danger'">
                  {{ scope.row.trade_type === 'buy' ? '买入' : '卖出' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="quantity" label="数量" width="80" />
            <el-table-column prop="price" label="价格" width="80">
              <template #default="scope">
                ¥{{ scope.row.price.toFixed(2) }}
              </template>
            </el-table-column>
            <el-table-column prop="amount" label="金额" width="120">
              <template #default="scope">
                ¥{{ scope.row.amount.toFixed(2) }}
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="80">
              <template #default="scope">
                <el-tag type="success">{{ scope.row.status === 'completed' ? '成功' : '处理中' }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import axios from 'axios'

// 响应式数据
const accountId = ref(localStorage.getItem('mock_account_id') || '')
const accountInfo = ref(null)
const currentPrice = ref(null)
const tradeHistory = ref([])
const loading = ref(false)
const trading = ref(false)

// 交易表单
const tradeForm = reactive({
  stockCode: '',
  type: 'buy',
  quantity: 100
})

// 股票搜索数据
const stockDatabase = [
  { code: '000001.SZ', name: '平安银行' },
  { code: '000002.SZ', name: '万科A' },
  { code: '600036.SH', name: '招商银行' },
  { code: '600519.SH', name: '贵州茅台' },
  { code: '000858.SZ', name: '五粮液' },
  { code: '002415.SZ', name: '海康威视' }
]

// Mock Trading API 基础URL - 暂时使用Python后端
const API_BASE = 'http://localhost:5002/api'

// 计算属性
const canTrade = computed(() => {
  return tradeForm.stockCode &&
         tradeForm.quantity >= 100 &&
         currentPrice.value &&
         accountInfo.value
})

const estimatedAmount = computed(() => {
  if (!currentPrice.value || !tradeForm.quantity) return null

  const amount = currentPrice.value.price * tradeForm.quantity
  const commission = amount * 0.0003 // 万三手续费
  const total = tradeForm.type === 'buy' ? amount + commission : amount - commission

  return {
    amount: amount.toFixed(2),
    commission: commission.toFixed(2),
    total: total.toFixed(2)
  }
})

// 方法
const createAccount = async () => {
  try {
    const result = await ElMessageBox.prompt('请输入用户名', '创建模拟账户', {
      confirmButtonText: '创建',
      cancelButtonText: '取消',
      inputPattern: /^[a-zA-Z0-9_]{3,20}$/,
      inputErrorMessage: '用户名需要3-20位字母数字下划线'
    })

    const response = await axios.post(`${API_BASE}/register`, {
      username: result.value,
      email: ''
    })

    accountId.value = response.data.account_id
    localStorage.setItem('mock_account_id', accountId.value)

    ElMessage.success(`账户创建成功！获得${response.data.initial_balance.toLocaleString()}元虚拟资金`)

    await loadAccountInfo()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('创建账户失败：' + (error.response?.data?.error || error.message))
    }
  }
}

const loadAccountInfo = async () => {
  if (!accountId.value) return

  loading.value = true
  try {
    const response = await axios.get(`${API_BASE}/account/${accountId.value}`)
    accountInfo.value = response.data
  } catch (error) {
    ElMessage.error('加载账户信息失败：' + (error.response?.data?.error || error.message))
  } finally {
    loading.value = false
  }
}

const refreshAccount = () => {
  loadAccountInfo()
  loadTradeHistory()
}

const loadTradeHistory = async () => {
  if (!accountId.value) return

  try {
    const response = await axios.get(`${API_BASE}/trades/${accountId.value}`)
    tradeHistory.value = response.data.trades
  } catch (error) {
    console.error('加载交易历史失败：', error)
  }
}

const queryStocks = (queryString, cb) => {
  const results = queryString
    ? stockDatabase.filter(stock =>
        stock.code.toLowerCase().includes(queryString.toLowerCase()) ||
        stock.name.includes(queryString)
      )
    : stockDatabase
  cb(results.slice(0, 10))
}

const handleStockSelect = async (item) => {
  tradeForm.stockCode = item.code
  await loadStockPrice(item.code)
}

const loadStockPrice = async (stockCode) => {
  try {
    const response = await axios.get(`${API_BASE}/market/${stockCode}`)
    currentPrice.value = response.data
  } catch (error) {
    ElMessage.error('获取股价失败：' + (error.response?.data?.error || error.message))
  }
}

const executeTrade = async () => {
  if (!canTrade.value) return

  trading.value = true
  try {
    const response = await axios.post(`${API_BASE}/trade`, {
      account_id: accountId.value,
      stock_code: tradeForm.stockCode,
      trade_type: tradeForm.type,
      quantity: tradeForm.quantity
    })

    ElMessage.success(response.data.message)

    // 重置表单
    tradeForm.stockCode = ''
    tradeForm.quantity = 100
    currentPrice.value = null

    // 刷新账户信息
    await loadAccountInfo()
    await loadTradeHistory()

  } catch (error) {
    ElMessage.error('交易失败：' + (error.response?.data?.error || error.message))
  } finally {
    trading.value = false
  }
}

const quickSell = (position) => {
  tradeForm.stockCode = position.stock_code
  tradeForm.type = 'sell'
  tradeForm.quantity = position.quantity
  loadStockPrice(position.stock_code)
}

const formatCurrency = (amount) => {
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY'
  }).format(amount)
}

const formatTime = (timeStr) => {
  return new Date(timeStr).toLocaleString('zh-CN')
}

// 监听股票代码变化
watch(() => tradeForm.stockCode, (newCode) => {
  if (newCode && newCode.match(/^\d{6}\.(SZ|SH)$/)) {
    loadStockPrice(newCode)
  }
})

// 生命周期
onMounted(() => {
  if (accountId.value) {
    loadAccountInfo()
    loadTradeHistory()
  }
})
</script>

<style scoped>
.trading-interface {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.account-section,
.trading-section,
.history-section {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3,
.card-header h4 {
  margin: 0;
}

.account-info {
  padding: 20px 0;
}

.stat-item {
  text-align: center;
}

.stat-item .label {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.stat-item .value {
  font-size: 24px;
  font-weight: bold;
  color: #333;
}

.no-account {
  padding: 40px;
  text-align: center;
}

.stock-suggestion {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.stock-suggestion .code {
  font-weight: bold;
  color: #409eff;
}

.stock-suggestion .name {
  color: #666;
  font-size: 14px;
}

.price-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.price {
  font-size: 18px;
  font-weight: bold;
  color: #333;
}

.change.up {
  color: #f56c6c;
}

.change.down {
  color: #67c23a;
}

.amount-info {
  font-size: 14px;
  color: #666;
}

.amount-info .total {
  font-weight: bold;
  color: #333;
  margin-top: 5px;
}

.profit {
  color: #f56c6c;
  font-weight: bold;
}

.loss {
  color: #67c23a;
  font-weight: bold;
}

.trading-panel,
.positions-panel,
.history-panel {
  height: 100%;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .trading-interface {
    padding: 10px;
  }

  .trading-section .el-col {
    margin-bottom: 20px;
  }

  .stat-item .value {
    font-size: 18px;
  }
}
</style>