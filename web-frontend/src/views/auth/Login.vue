<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <h1 class="login-title">量化交易平台</h1>
        <p class="login-subtitle">专业的量化交易解决方案</p>
      </div>

      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        class="login-form"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="usernameOrEmail">
          <el-input
            v-model="loginForm.usernameOrEmail"
            placeholder="用户名或邮箱"
            size="large"
            :prefix-icon="User"
            clearable
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="密码"
            size="large"
            :prefix-icon="Lock"
            show-password
            clearable
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="login-button"
            :loading="authStore.isLoading"
            @click="handleLogin"
          >
            {{ authStore.isLoading ? '登录中...' : '登录' }}
          </el-button>
        </el-form-item>

        <div class="login-footer">
          <el-link type="primary" @click="goToRegister">
            还没有账号？立即注册
          </el-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()
const loginFormRef = ref()

// 表单数据
const loginForm = reactive({
  usernameOrEmail: '',
  password: '',
})

// 表单验证规则
const loginRules = reactive({
  usernameOrEmail: [
    { required: true, message: '请输入用户名或邮箱', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' },
  ],
})

// 登录处理
const handleLogin = async () => {
  if (!loginFormRef.value) return

  try {
    await loginFormRef.value.validate()
    await authStore.login(loginForm)
  } catch (error) {
    if (error.errors) {
      // 表单验证错误
      return
    }
    console.error('登录失败:', error)
  }
}

// 跳转到注册页面
const goToRegister = () => {
  router.push('/register')
}

// 页面挂载时检查是否已登录
onMounted(() => {
  if (authStore.isAuthenticated) {
    router.push('/dashboard')
  }
})
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.login-card {
  width: 100%;
  max-width: 400px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  padding: 40px 32px;
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-title {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 8px;
}

.login-subtitle {
  color: #909399;
  font-size: 14px;
}

.login-form {
  width: 100%;
}

.login-button {
  width: 100%;
  height: 44px;
  font-size: 16px;
  font-weight: 500;
}

.login-footer {
  text-align: center;
  margin-top: 20px;
}

/* 响应式设计 */
@media (max-width: 480px) {
  .login-card {
    padding: 30px 20px;
  }

  .login-title {
    font-size: 24px;
  }
}
</style>