<template>
  <div class="dashboard-layout">
    <!-- 侧边栏 -->
    <div class="sidebar">
      <div class="sidebar-header">
        <h2 class="logo">量化交易</h2>
      </div>

      <el-menu
        :default-active="activeMenu"
        class="sidebar-menu"
        @select="handleMenuSelect"
      >
        <el-menu-item index="dashboard">
          <el-icon><House /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>

        <el-menu-item index="portfolio">
          <el-icon><PieChart /></el-icon>
          <span>投资组合</span>
        </el-menu-item>

        <el-menu-item index="trading">
          <el-icon><TrendCharts /></el-icon>
          <span>交易中心</span>
        </el-menu-item>

        <el-menu-item index="analysis">
          <el-icon><DataAnalysis /></el-icon>
          <span>数据分析</span>
        </el-menu-item>

        <el-menu-item index="settings">
          <el-icon><Setting /></el-icon>
          <span>设置</span>
        </el-menu-item>
      </el-menu>
    </div>

    <!-- 主内容区 -->
    <div class="main-content">
      <!-- 顶部导航 -->
      <div class="header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ getCurrentPageTitle() }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" :src="userAvatar">
                {{ authStore.userInfo?.username?.charAt(0).toUpperCase() }}
              </el-avatar>
              <span class="username">{{ authStore.userInfo?.username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人资料</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <!-- 内容区域 -->
      <div class="content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import {
  House,
  PieChart,
  TrendCharts,
  DataAnalysis,
  Setting,
  ArrowDown,
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const userAvatar = ref('')

// 当前激活的菜单项
const activeMenu = computed(() => {
  const path = route.path
  if (path.includes('portfolio')) return 'portfolio'
  if (path.includes('trading')) return 'trading'
  if (path.includes('analysis')) return 'analysis'
  if (path.includes('settings')) return 'settings'
  return 'dashboard'
})

// 获取当前页面标题
const getCurrentPageTitle = () => {
  const titles = {
    dashboard: '仪表盘',
    portfolio: '投资组合',
    trading: '交易中心',
    analysis: '数据分析',
    settings: '设置',
  }
  return titles[activeMenu.value] || '仪表盘'
}

// 菜单选择处理
const handleMenuSelect = (index) => {
  const routes = {
    dashboard: '/dashboard',
    portfolio: '/dashboard/portfolio',
    trading: '/dashboard/trading',
    analysis: '/dashboard/analysis',
    settings: '/dashboard/settings',
  }

  const targetRoute = routes[index]
  if (targetRoute && targetRoute !== route.path) {
    router.push(targetRoute)
  }
}

// 用户下拉菜单处理
const handleCommand = (command) => {
  switch (command) {
    case 'profile':
      router.push('/dashboard/profile')
      break
    case 'logout':
      authStore.logout()
      break
  }
}

onMounted(() => {
  // 检查用户认证状态
  if (!authStore.isAuthenticated) {
    router.push('/login')
  }
})
</script>

<style scoped>
.dashboard-layout {
  display: flex;
  height: 100vh;
  background: #f0f2f5;
}

.sidebar {
  width: 240px;
  background: #fff;
  box-shadow: 2px 0 6px rgba(0, 21, 41, 0.08);
  z-index: 100;
}

.sidebar-header {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid #f0f0f0;
}

.logo {
  font-size: 20px;
  font-weight: bold;
  color: #1890ff;
  margin: 0;
}

.sidebar-menu {
  border: none;
  padding: 16px 0;
}

.sidebar-menu .el-menu-item {
  height: 48px;
  line-height: 48px;
  margin: 0 12px 8px;
  border-radius: 6px;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.header {
  height: 64px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  z-index: 99;
}

.header-left {
  flex: 1;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 6px;
  transition: background-color 0.3s;
}

.user-info:hover {
  background-color: #f5f5f5;
}

.username {
  margin: 0 8px;
  font-size: 14px;
  color: #333;
}

.content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  background: #f0f2f5;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .sidebar {
    width: 200px;
  }

  .content {
    padding: 16px;
  }

  .header {
    padding: 0 16px;
  }
}

@media (max-width: 576px) {
  .sidebar {
    position: fixed;
    left: -240px;
    transition: left 0.3s;
    z-index: 1000;
  }

  .sidebar.open {
    left: 0;
  }

  .main-content {
    margin-left: 0;
  }
}
</style>