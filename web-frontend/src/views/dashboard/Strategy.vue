<template>
  <div class="strategy-manager">
    <el-card class="header-card">
      <div class="page-header">
        <h2>策略管理</h2>
        <el-button type="primary" @click="showCreateDialog = true">
          <el-icon><Plus /></el-icon>
          创建新策略
        </el-button>
      </div>
    </el-card>

    <!-- 策略列表 -->
    <el-card class="strategy-list-card">
      <el-table :data="strategies" v-loading="loading" stripe>
        <el-table-column prop="name" label="策略名称" width="180" />
        <el-table-column prop="type" label="策略类型" width="150">
          <template #default="{ row }">
            <el-tag>{{ getStrategyTypeName(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="350">
          <template #default="{ row }">
            <el-button
              size="small"
              type="success"
              @click="generateSignal(row)"
              :loading="row.generating"
            >
              <el-icon><Lightning /></el-icon>
              生成信号
            </el-button>
            <el-button
              size="small"
              type="info"
              @click="viewSignals(row)"
            >
              <el-icon><View /></el-icon>
              查看信号
            </el-button>
            <el-button
              v-if="row.status === 'STOPPED'"
              size="small"
              type="primary"
              @click="startStrategy(row)"
            >
              <el-icon><VideoPlay /></el-icon>
              启动
            </el-button>
            <el-button
              v-else
              size="small"
              type="warning"
              @click="stopStrategy(row)"
            >
              <el-icon><VideoPause /></el-icon>
              停止
            </el-button>
            <el-button
              size="small"
              type="danger"
              @click="deleteStrategy(row)"
            >
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 信号显示卡片 -->
    <el-card v-if="selectedStrategy" class="signals-card">
      <template #header>
        <div class="card-header">
          <span>{{ selectedStrategy.name }} - 交易信号</span>
          <el-button size="small" @click="selectedStrategy = null">关闭</el-button>
        </div>
      </template>

      <el-table :data="signals" v-loading="loadingSignals">
        <el-table-column prop="stockCode" label="股票代码" width="120" />
        <el-table-column prop="stockName" label="股票名称" width="120" />
        <el-table-column prop="signalType" label="信号类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getSignalType(row.signalType)">
              {{ row.signalType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="price" label="价格" width="100">
          <template #default="{ row }">
            ¥{{ row.price }}
          </template>
        </el-table-column>
        <el-table-column prop="signalStrength" label="信号强度" width="100">
          <template #default="{ row }">
            {{ row.signalStrength ? row.signalStrength.toFixed(2) + '%' : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="信号原因" min-width="200" />
        <el-table-column prop="signalTime" label="生成时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.signalTime) }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建策略对话框 -->
    <el-dialog
      v-model="showCreateDialog"
      title="创建新策略"
      width="600px"
    >
      <el-form :model="newStrategy" label-width="120px">
        <el-form-item label="策略名称">
          <el-input v-model="newStrategy.name" placeholder="请输入策略名称" />
        </el-form-item>

        <el-form-item label="策略类型">
          <el-select v-model="newStrategy.type" placeholder="选择策略类型" @change="onStrategyTypeChange">
            <el-option
              v-for="st in strategyTypes"
              :key="st.type"
              :label="st.name"
              :value="st.type"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="策略描述">
          <el-input
            v-model="newStrategy.description"
            type="textarea"
            :rows="3"
            placeholder="请输入策略描述"
          />
        </el-form-item>

        <!-- 双均线策略参数 -->
        <template v-if="newStrategy.type === 'MA_CROSS'">
          <el-form-item label="短期均线周期">
            <el-input-number
              v-model="newStrategy.parameters.shortPeriod"
              :min="2"
              :max="100"
            />
            <span class="param-hint">建议: 5日或10日</span>
          </el-form-item>

          <el-form-item label="长期均线周期">
            <el-input-number
              v-model="newStrategy.parameters.longPeriod"
              :min="5"
              :max="200"
            />
            <span class="param-hint">建议: 20日或60日</span>
          </el-form-item>
        </template>
      </el-form>

      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="createStrategy" :loading="creating">
          创建
        </el-button>
      </template>
    </el-dialog>

    <!-- 生成信号对话框 -->
    <el-dialog
      v-model="showSignalDialog"
      title="生成交易信号"
      width="500px"
    >
      <el-form label-width="100px">
        <el-form-item label="股票代码">
          <el-input
            v-model="signalStockCode"
            placeholder="请输入股票代码，如: 000001.SZ"
          />
          <div class="stock-hints">
            <el-button
              size="small"
              @click="signalStockCode = '000001.SZ'"
            >
              平安银行
            </el-button>
            <el-button
              size="small"
              @click="signalStockCode = '600036.SH'"
            >
              招商银行
            </el-button>
            <el-button
              size="small"
              @click="signalStockCode = '600519.SH'"
            >
              贵州茅台
            </el-button>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showSignalDialog = false">取消</el-button>
        <el-button
          type="primary"
          @click="confirmGenerateSignal"
          :loading="generating"
        >
          生成
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus,
  Lightning,
  View,
  VideoPlay,
  VideoPause,
  Delete
} from '@element-plus/icons-vue'
import axios from 'axios'

const API_BASE = 'http://localhost:8083/trading-service/api'

// 数据
const strategies = ref([])
const signals = ref([])
const strategyTypes = ref([])
const selectedStrategy = ref(null)
const loading = ref(false)
const loadingSignals = ref(false)
const creating = ref(false)
const generating = ref(false)

// 对话框
const showCreateDialog = ref(false)
const showSignalDialog = ref(false)
const currentStrategy = ref(null)
const signalStockCode = ref('')

// 新建策略表单
const newStrategy = ref({
  name: '',
  type: 'MA_CROSS',
  description: '',
  parameters: {
    shortPeriod: 5,
    longPeriod: 20
  }
})

// 获取用户策略列表
const loadStrategies = async () => {
  loading.value = true
  try {
    const response = await axios.get(`${API_BASE}/strategy/user/1`)
    if (response.data.success) {
      strategies.value = response.data.strategies
    }
  } catch (error) {
    ElMessage.error('加载策略列表失败: ' + error.message)
  } finally {
    loading.value = false
  }
}

// 获取支持的策略类型
const loadStrategyTypes = async () => {
  try {
    const response = await axios.get(`${API_BASE}/strategy/types`)
    if (response.data.success) {
      strategyTypes.value = response.data.strategies
    }
  } catch (error) {
    console.error('加载策略类型失败:', error)
  }
}

// 创建策略
const createStrategy = async () => {
  if (!newStrategy.value.name) {
    ElMessage.warning('请输入策略名称')
    return
  }

  creating.value = true
  try {
    const response = await axios.post(`${API_BASE}/strategy/create`, {
      name: newStrategy.value.name,
      type: newStrategy.value.type,
      description: newStrategy.value.description,
      parameters: JSON.stringify(newStrategy.value.parameters),
      userId: 1
    })

    if (response.data.success) {
      ElMessage.success('策略创建成功')
      showCreateDialog.value = false
      // 重置表单
      newStrategy.value = {
        name: '',
        type: 'MA_CROSS',
        description: '',
        parameters: { shortPeriod: 5, longPeriod: 20 }
      }
      loadStrategies()
    }
  } catch (error) {
    ElMessage.error('创建策略失败: ' + error.message)
  } finally {
    creating.value = false
  }
}

// 生成信号
const generateSignal = (strategy) => {
  currentStrategy.value = strategy
  signalStockCode.value = ''
  showSignalDialog.value = true
}

const confirmGenerateSignal = async () => {
  if (!signalStockCode.value) {
    ElMessage.warning('请输入股票代码')
    return
  }

  generating.value = true
  try {
    const response = await axios.post(
      `${API_BASE}/strategy/${currentStrategy.value.id}/signal/${signalStockCode.value}`
    )

    if (response.data.success) {
      ElMessage.success('信号生成成功: ' + response.data.signal.signalType)
      showSignalDialog.value = false

      // 如果当前正在查看这个策略的信号，刷新信号列表
      if (selectedStrategy.value && selectedStrategy.value.id === currentStrategy.value.id) {
        loadSignals(currentStrategy.value)
      }
    }
  } catch (error) {
    ElMessage.error('生成信号失败: ' + error.message)
  } finally {
    generating.value = false
  }
}

// 查看信号
const viewSignals = async (strategy) => {
  selectedStrategy.value = strategy
  loadSignals(strategy)
}

const loadSignals = async (strategy) => {
  loadingSignals.value = true
  try {
    const response = await axios.get(`${API_BASE}/strategy/${strategy.id}/signals`)
    if (response.data.success) {
      signals.value = response.data.signals
    }
  } catch (error) {
    ElMessage.error('加载信号失败: ' + error.message)
  } finally {
    loadingSignals.value = false
  }
}

// 启动策略
const startStrategy = async (strategy) => {
  try {
    const response = await axios.post(`${API_BASE}/strategy/${strategy.id}/start`)
    if (response.data.success) {
      ElMessage.success('策略已启动')
      loadStrategies()
    }
  } catch (error) {
    ElMessage.error('启动策略失败: ' + error.message)
  }
}

// 停止策略
const stopStrategy = async (strategy) => {
  try {
    const response = await axios.post(`${API_BASE}/strategy/${strategy.id}/stop`)
    if (response.data.success) {
      ElMessage.success('策略已停止')
      loadStrategies()
    }
  } catch (error) {
    ElMessage.error('停止策略失败: ' + error.message)
  }
}

// 删除策略
const deleteStrategy = async (strategy) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除策略 "${strategy.name}" 吗？此操作将同时删除所有相关信号记录。`,
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const response = await axios.delete(`${API_BASE}/strategy/${strategy.id}`)
    if (response.data.success) {
      ElMessage.success('策略已删除')
      if (selectedStrategy.value && selectedStrategy.value.id === strategy.id) {
        selectedStrategy.value = null
      }
      loadStrategies()
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除策略失败: ' + error.message)
    }
  }
}

// 工具函数
const getStrategyTypeName = (type) => {
  const names = {
    'MA_CROSS': '双均线交叉'
  }
  return names[type] || type
}

const getStatusType = (status) => {
  const types = {
    'RUNNING': 'success',
    'STOPPED': 'info',
    'PAUSED': 'warning'
  }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = {
    'RUNNING': '运行中',
    'STOPPED': '已停止',
    'PAUSED': '已暂停'
  }
  return texts[status] || status
}

const getSignalType = (type) => {
  const types = {
    'BUY': 'success',
    'SELL': 'danger',
    'HOLD': 'info'
  }
  return types[type] || 'info'
}

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

const onStrategyTypeChange = () => {
  // 重置参数
  if (newStrategy.value.type === 'MA_CROSS') {
    newStrategy.value.parameters = { shortPeriod: 5, longPeriod: 20 }
  }
}

// 初始化
onMounted(() => {
  loadStrategies()
  loadStrategyTypes()
})
</script>

<style scoped>
.strategy-manager {
  padding: 20px;
}

.header-card {
  margin-bottom: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-header h2 {
  margin: 0;
  font-size: 24px;
}

.strategy-list-card {
  margin-bottom: 20px;
}

.signals-card {
  margin-top: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.param-hint {
  margin-left: 10px;
  font-size: 12px;
  color: #909399;
}

.stock-hints {
  margin-top: 10px;
  display: flex;
  gap: 10px;
}
</style>
