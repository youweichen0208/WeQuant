<template>
  <div class="profile-container">
    <div class="profile-header">
      <h1>个人资料</h1>
      <p>管理您的账户信息和交易设置</p>
    </div>

    <div class="profile-content">
      <!-- 基本信息 -->
      <div class="profile-section">
        <div class="section-header">
          <h2>基本信息</h2>
          <el-button v-if="!isEditing" type="primary" @click="startEdit">
            编辑资料
          </el-button>
          <div v-else class="edit-actions">
            <el-button @click="cancelEdit">取消</el-button>
            <el-button type="primary" :loading="authStore.isLoading" @click="saveProfile">
              保存
            </el-button>
          </div>
        </div>

        <el-form
          ref="profileFormRef"
          :model="profileForm"
          :rules="profileRules"
          label-width="120px"
          class="profile-form"
        >
          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="用户名">
                <el-input v-model="profileForm.username" disabled />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="邮箱">
                <el-input v-model="profileForm.email" disabled />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="姓名" prop="fullName">
                <el-input
                  v-model="profileForm.fullName"
                  :disabled="!isEditing"
                  placeholder="请输入姓名"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="手机号码" prop="phoneNumber">
                <el-input
                  v-model="profileForm.phoneNumber"
                  :disabled="!isEditing"
                  placeholder="请输入手机号码"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="注册时间">
                <el-input v-model="formattedCreatedAt" disabled />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="最后登录">
                <el-input v-model="formattedLastLogin" disabled />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
      </div>

      <!-- 交易设置 -->
      <div class="profile-section">
        <div class="section-header">
          <h2>交易设置</h2>
        </div>

        <el-form label-width="120px" class="profile-form">
          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="风险等级">
                <el-select
                  v-model="profileForm.riskLevel"
                  :disabled="!isEditing"
                  placeholder="选择风险等级"
                  style="width: 100%"
                >
                  <el-option label="保守型" value="CONSERVATIVE" />
                  <el-option label="稳健型" value="MODERATE" />
                  <el-option label="激进型" value="AGGRESSIVE" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="账户余额">
                <el-input v-model="formattedBalance" disabled />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="最大日损失">
                <el-input-number
                  v-model="profileForm.maxDailyLoss"
                  :disabled="!isEditing"
                  :min="0"
                  :precision="2"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="最大仓位比例">
                <el-input-number
                  v-model="profileForm.maxPositionRatio"
                  :disabled="!isEditing"
                  :min="0.01"
                  :max="1"
                  :step="0.01"
                  :precision="2"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="启用交易">
                <el-switch
                  v-model="profileForm.enableTrading"
                  :disabled="!isEditing"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="启用通知">
                <el-switch
                  v-model="profileForm.enableNotifications"
                  :disabled="!isEditing"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
      </div>

      <!-- 账户统计 -->
      <div class="profile-section">
        <div class="section-header">
          <h2>账户统计</h2>
        </div>

        <div class="stats-grid">
          <div class="stat-item">
            <div class="stat-label">账户状态</div>
            <div class="stat-value">
              <el-tag :type="getStatusTagType(authStore.userInfo?.status)">
                {{ getStatusText(authStore.userInfo?.status) }}
              </el-tag>
            </div>
          </div>
          <div class="stat-item">
            <div class="stat-label">用户角色</div>
            <div class="stat-value">
              <el-tag type="info">{{ getRoleText(authStore.userInfo?.role) }}</el-tag>
            </div>
          </div>
          <div class="stat-item">
            <div class="stat-label">可用余额</div>
            <div class="stat-value">{{ formattedAvailableBalance }}</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">风险等级</div>
            <div class="stat-value">
              <el-tag :type="getRiskTagType(authStore.userInfo?.riskLevel)">
                {{ getRiskLevelText(authStore.userInfo?.riskLevel) }}
              </el-tag>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useAuthStore } from '@/store/auth'
import { ElMessage } from 'element-plus'

const authStore = useAuthStore()
const profileFormRef = ref()
const isEditing = ref(false)

// 表单数据
const profileForm = reactive({
  username: '',
  email: '',
  fullName: '',
  phoneNumber: '',
  riskLevel: 'CONSERVATIVE',
  maxDailyLoss: 0,
  maxPositionRatio: 0.2,
  enableTrading: true,
  enableNotifications: true,
})

// 原始数据备份
let originalData = {}

// 表单验证规则
const profileRules = reactive({
  fullName: [
    { max: 50, message: '姓名长度不能超过50字符', trigger: 'blur' },
  ],
  phoneNumber: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' },
  ],
})

// 计算属性
const formattedCreatedAt = computed(() => {
  return authStore.userInfo?.createdAt
    ? new Date(authStore.userInfo.createdAt).toLocaleDateString('zh-CN')
    : '-'
})

const formattedLastLogin = computed(() => {
  return authStore.userInfo?.lastLoginAt
    ? new Date(authStore.userInfo.lastLoginAt).toLocaleString('zh-CN')
    : '-'
})

const formattedBalance = computed(() => {
  return authStore.userInfo?.accountBalance
    ? `¥${authStore.userInfo.accountBalance.toFixed(2)}`
    : '¥0.00'
})

const formattedAvailableBalance = computed(() => {
  return authStore.userInfo?.availableBalance
    ? `¥${authStore.userInfo.availableBalance.toFixed(2)}`
    : '¥0.00'
})

// 辅助函数
const getStatusText = (status) => {
  const texts = {
    ACTIVE: '正常',
    INACTIVE: '未激活',
    SUSPENDED: '已暂停',
    LOCKED: '已锁定',
  }
  return texts[status] || '未知'
}

const getStatusTagType = (status) => {
  const types = {
    ACTIVE: 'success',
    INACTIVE: 'warning',
    SUSPENDED: 'danger',
    LOCKED: 'danger',
  }
  return types[status] || 'info'
}

const getRoleText = (role) => {
  const texts = {
    USER: '普通用户',
    VIP: 'VIP用户',
    ADMIN: '管理员',
  }
  return texts[role] || '普通用户'
}

const getRiskLevelText = (riskLevel) => {
  const texts = {
    CONSERVATIVE: '保守型',
    MODERATE: '稳健型',
    AGGRESSIVE: '激进型',
  }
  return texts[riskLevel] || '未设置'
}

const getRiskTagType = (riskLevel) => {
  const types = {
    CONSERVATIVE: 'success',
    MODERATE: 'warning',
    AGGRESSIVE: 'danger',
  }
  return types[riskLevel] || 'info'
}

// 方法
const loadProfileData = () => {
  const userInfo = authStore.userInfo
  if (userInfo) {
    Object.assign(profileForm, {
      username: userInfo.username || '',
      email: userInfo.email || '',
      fullName: userInfo.fullName || '',
      phoneNumber: userInfo.phoneNumber || '',
      riskLevel: userInfo.riskLevel || 'CONSERVATIVE',
      maxDailyLoss: userInfo.maxDailyLoss || 0,
      maxPositionRatio: userInfo.maxPositionRatio || 0.2,
      enableTrading: userInfo.enableTrading !== false,
      enableNotifications: userInfo.enableNotifications !== false,
    })
  }
}

const startEdit = () => {
  isEditing.value = true
  originalData = { ...profileForm }
}

const cancelEdit = () => {
  isEditing.value = false
  Object.assign(profileForm, originalData)
}

const saveProfile = async () => {
  if (!profileFormRef.value) return

  try {
    await profileFormRef.value.validate()

    const updateData = {
      fullName: profileForm.fullName,
      phoneNumber: profileForm.phoneNumber,
      riskLevel: profileForm.riskLevel,
      maxDailyLoss: profileForm.maxDailyLoss,
      maxPositionRatio: profileForm.maxPositionRatio,
      enableTrading: profileForm.enableTrading,
      enableNotifications: profileForm.enableNotifications,
    }

    await authStore.updateProfile(updateData)
    isEditing.value = false
  } catch (error) {
    console.error('保存失败:', error)
  }
}

onMounted(() => {
  loadProfileData()
})
</script>

<style scoped>
.profile-container {
  max-width: 1000px;
  margin: 0 auto;
}

.profile-header {
  margin-bottom: 24px;
}

.profile-header h1 {
  font-size: 24px;
  color: #333;
  margin: 0 0 8px 0;
}

.profile-header p {
  color: #666;
  margin: 0;
}

.profile-section {
  background: white;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  margin-bottom: 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.section-header h2 {
  font-size: 18px;
  color: #333;
  margin: 0;
}

.edit-actions {
  display: flex;
  gap: 12px;
}

.profile-form {
  margin-top: 16px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 24px;
}

.stat-item {
  text-align: center;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.stat-label {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 16px;
  font-weight: 500;
  color: #333;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .profile-section {
    padding: 16px;
  }

  .section-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>