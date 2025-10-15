<template>
  <div class="register-container">
    <div class="register-card">
      <div class="register-header">
        <h1 class="register-title">创建账户</h1>
        <p class="register-subtitle">加入量化交易平台</p>
      </div>

      <el-form
        ref="registerFormRef"
        :model="registerForm"
        :rules="registerRules"
        class="register-form"
        @submit.prevent="handleRegister"
      >
        <el-form-item prop="username">
          <el-input
            v-model="registerForm.username"
            placeholder="用户名"
            size="large"
            :prefix-icon="User"
            clearable
            @blur="checkUsername"
          />
        </el-form-item>

        <el-form-item prop="email">
          <el-input
            v-model="registerForm.email"
            placeholder="邮箱"
            size="large"
            :prefix-icon="Message"
            clearable
            @blur="checkEmail"
          />
        </el-form-item>

        <el-form-item prop="fullName">
          <el-input
            v-model="registerForm.fullName"
            placeholder="姓名"
            size="large"
            :prefix-icon="UserFilled"
            clearable
          />
        </el-form-item>

        <el-form-item prop="phoneNumber">
          <el-input
            v-model="registerForm.phoneNumber"
            placeholder="手机号码"
            size="large"
            :prefix-icon="Phone"
            clearable
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="registerForm.password"
            type="password"
            placeholder="密码"
            size="large"
            :prefix-icon="Lock"
            show-password
            clearable
          />
        </el-form-item>

        <el-form-item prop="confirmPassword">
          <el-input
            v-model="registerForm.confirmPassword"
            type="password"
            placeholder="确认密码"
            size="large"
            :prefix-icon="Lock"
            show-password
            clearable
            @keyup.enter="handleRegister"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="register-button"
            :loading="authStore.isLoading"
            @click="handleRegister"
          >
            {{ authStore.isLoading ? '注册中...' : '注册' }}
          </el-button>
        </el-form-item>

        <div class="register-footer">
          <el-link type="primary" @click="goToLogin">
            已有账号？立即登录
          </el-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import { authApi } from '@/api'
import { User, UserFilled, Lock, Message, Phone } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()
const registerFormRef = ref()

// 表单数据
const registerForm = reactive({
  username: '',
  email: '',
  fullName: '',
  phoneNumber: '',
  password: '',
  confirmPassword: '',
})

// 自定义验证函数
const validateUsername = async (rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入用户名'))
    return
  }
  if (!/^[a-zA-Z0-9_]{3,20}$/.test(value)) {
    callback(new Error('用户名只能包含字母、数字和下划线，长度3-20位'))
    return
  }
  callback()
}

const validateEmail = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入邮箱'))
    return
  }
  const emailReg = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailReg.test(value)) {
    callback(new Error('请输入正确的邮箱格式'))
    return
  }
  callback()
}

const validatePhone = (rule, value, callback) => {
  if (value && !/^1[3-9]\d{9}$/.test(value)) {
    callback(new Error('请输入正确的手机号码'))
    return
  }
  callback()
}

const validatePassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入密码'))
    return
  }
  if (value.length < 8) {
    callback(new Error('密码长度至少8位'))
    return
  }
  if (!/(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/.test(value)) {
    callback(new Error('密码必须包含大小写字母和数字'))
    return
  }
  callback()
}

const validateConfirmPassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请确认密码'))
    return
  }
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
    return
  }
  callback()
}

// 表单验证规则
const registerRules = reactive({
  username: [{ validator: validateUsername, trigger: 'blur' }],
  email: [{ validator: validateEmail, trigger: 'blur' }],
  fullName: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { max: 50, message: '姓名长度不能超过50字符', trigger: 'blur' },
  ],
  phoneNumber: [{ validator: validatePhone, trigger: 'blur' }],
  password: [{ validator: validatePassword, trigger: 'blur' }],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }],
})

// 检查用户名可用性
const checkUsername = async () => {
  if (!registerForm.username) return

  try {
    const response = await authApi.checkUsername(registerForm.username)
    if (!response.data) {
      ElMessage.warning('用户名已被使用')
    }
  } catch (error) {
    console.error('检查用户名失败:', error)
  }
}

// 检查邮箱可用性
const checkEmail = async () => {
  if (!registerForm.email) return

  try {
    const response = await authApi.checkEmail(registerForm.email)
    if (!response.data) {
      ElMessage.warning('邮箱已被注册')
    }
  } catch (error) {
    console.error('检查邮箱失败:', error)
  }
}

// 注册处理
const handleRegister = async () => {
  if (!registerFormRef.value) return

  try {
    await registerFormRef.value.validate()
    await authStore.register(registerForm)
  } catch (error) {
    if (error.errors) {
      // 表单验证错误
      return
    }
    console.error('注册失败:', error)
  }
}

// 跳转到登录页面
const goToLogin = () => {
  router.push('/login')
}
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.register-card {
  width: 100%;
  max-width: 450px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  padding: 40px 32px;
}

.register-header {
  text-align: center;
  margin-bottom: 32px;
}

.register-title {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 8px;
}

.register-subtitle {
  color: #909399;
  font-size: 14px;
}

.register-form {
  width: 100%;
}

.register-button {
  width: 100%;
  height: 44px;
  font-size: 16px;
  font-weight: 500;
}

.register-footer {
  text-align: center;
  margin-top: 20px;
}

/* 响应式设计 */
@media (max-width: 480px) {
  .register-card {
    padding: 30px 20px;
  }

  .register-title {
    font-size: 24px;
  }
}
</style>