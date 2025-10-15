import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/store/auth'

// 导入组件
const Login = () => import('@/views/auth/Login.vue')
const Register = () => import('@/views/auth/Register.vue')
const DashboardLayout = () => import('@/views/dashboard/Layout.vue')
const DashboardHome = () => import('@/views/dashboard/Home.vue')

const routes = [
  {
    path: '/',
    redirect: '/dashboard',
  },
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: {
      title: '登录',
      requiresAuth: false,
    },
  },
  {
    path: '/register',
    name: 'Register',
    component: Register,
    meta: {
      title: '注册',
      requiresAuth: false,
    },
  },
  {
    path: '/dashboard',
    component: DashboardLayout,
    meta: {
      requiresAuth: true,
    },
    children: [
      {
        path: '',
        name: 'Dashboard',
        component: DashboardHome,
        meta: {
          title: '仪表盘',
          requiresAuth: true,
        },
      },
      {
        path: 'portfolio',
        name: 'Portfolio',
        component: () => import('@/views/dashboard/Portfolio.vue'),
        meta: {
          title: '投资组合',
          requiresAuth: true,
        },
      },
      {
        path: 'trading',
        name: 'Trading',
        component: () => import('@/views/dashboard/Trading.vue'),
        meta: {
          title: '交易中心',
          requiresAuth: true,
        },
      },
      {
        path: 'analysis',
        name: 'Analysis',
        component: () => import('@/views/dashboard/Analysis.vue'),
        meta: {
          title: '数据分析',
          requiresAuth: true,
        },
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/dashboard/Settings.vue'),
        meta: {
          title: '设置',
          requiresAuth: true,
        },
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/user/Profile.vue'),
        meta: {
          title: '个人资料',
          requiresAuth: true,
        },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/common/NotFound.vue'),
    meta: {
      title: '页面不存在',
    },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    } else {
      return { top: 0 }
    }
  },
})

// 全局前置守卫
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()

  // 设置页面标题
  if (to.meta.title) {
    document.title = `${to.meta.title} - 量化交易平台`
  } else {
    document.title = '量化交易平台'
  }

  // 检查路由是否需要认证
  if (to.meta.requiresAuth) {
    if (authStore.isAuthenticated) {
      next()
    } else {
      // 未登录，重定向到登录页
      next({
        path: '/login',
        query: { redirect: to.fullPath },
      })
    }
  } else {
    // 不需要认证的路由
    if (authStore.isAuthenticated && (to.path === '/login' || to.path === '/register')) {
      // 已登录用户访问登录/注册页，重定向到仪表盘
      next('/dashboard')
    } else {
      next()
    }
  }
})

export default router