<template>
  <div class="not-found">
    <div class="not-found-content">
      <div class="not-found-image">
        <el-icon class="not-found-icon"><Warning /></el-icon>
        <h1 class="not-found-title">404</h1>
      </div>

      <div class="not-found-text">
        <h2>页面不存在</h2>
        <p>抱歉，您访问的页面不存在或已被移除。</p>
      </div>

      <div class="not-found-actions">
        <el-button type="primary" @click="goHome">
          返回首页
        </el-button>
        <el-button @click="goBack">
          返回上页
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { Warning } from '@element-plus/icons-vue'

const router = useRouter()

const goHome = () => {
  router.push('/dashboard')
}

const goBack = () => {
  router.go(-1)
}
</script>

<style scoped>
.not-found {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  padding: 20px;
}

.not-found-content {
  text-align: center;
  max-width: 500px;
}

.not-found-image {
  margin-bottom: 32px;
}

.not-found-icon {
  font-size: 64px;
  color: #ccc;
  margin-bottom: 16px;
}

.not-found-title {
  font-size: 72px;
  font-weight: bold;
  color: #999;
  margin: 0;
}

.not-found-text h2 {
  font-size: 24px;
  color: #333;
  margin: 0 0 12px 0;
}

.not-found-text p {
  font-size: 16px;
  color: #666;
  margin: 0 0 32px 0;
}

.not-found-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
}
</style>