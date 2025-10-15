# Quant Trading Platform - Web Frontend

Vue 3 + Vite 前端应用，为量化交易平台提供现代化的用户界面。

## 技术栈

- **Vue 3** - 渐进式JavaScript框架
- **Vite** - 快速构建工具
- **Vue Router 4** - 官方路由管理器
- **Pinia** - 状态管理
- **Element Plus** - UI组件库
- **Axios** - HTTP客户端
- **ECharts** - 数据可视化

## 功能特性

### 用户认证
- 用户注册/登录
- JWT令牌管理
- 自动登录状态检查
- 安全登出

### 仪表盘
- 个人资产概览
- 实时交易数据
- 快速操作入口
- 最近交易记录

### 交易功能
- 实时行情展示
- 交易执行界面
- 投资组合管理
- 数据分析工具

### 用户管理
- 个人资料编辑
- 交易设置配置
- 风险等级设置
- 账户统计信息

## 项目结构

```
src/
├── api/               # API接口
├── assets/           # 静态资源
├── components/       # 可复用组件
│   ├── auth/        # 认证相关
│   ├── common/      # 通用组件
│   ├── dashboard/   # 仪表盘组件
│   └── trading/     # 交易组件
├── router/          # 路由配置
├── store/           # 状态管理
├── utils/           # 工具函数
└── views/           # 页面组件
    ├── auth/        # 认证页面
    ├── dashboard/   # 仪表盘页面
    ├── trading/     # 交易页面
    └── user/        # 用户页面
```

## 开发指南

### 安装依赖
```bash
npm install
```

### 开发模式
```bash
npm run dev
```
应用将在 http://localhost:3000 启动

### 构建生产版本
```bash
npm run build
```

### 代码格式化
```bash
npm run format
```

## API 集成

前端通过代理配置与后端API通信：
- 开发环境：`/api` -> `http://localhost:8081/api/v1`
- 生产环境：需配置实际的API服务器地址

### 请求拦截器
- 自动添加JWT令牌
- 统一错误处理
- 请求/响应日志

### 响应拦截器
- 业务状态码处理
- 认证失效自动跳转
- 友好的错误提示

## 状态管理

使用Pinia进行状态管理：

### AuthStore
- 用户认证状态
- 用户信息缓存
- 令牌管理
- 登录/登出逻辑

## 路由设计

### 公开路由
- `/login` - 用户登录
- `/register` - 用户注册

### 受保护路由
- `/dashboard` - 仪表盘首页
- `/dashboard/portfolio` - 投资组合
- `/dashboard/trading` - 交易中心
- `/dashboard/analysis` - 数据分析
- `/dashboard/settings` - 系统设置
- `/dashboard/profile` - 个人资料

## 组件设计

### 认证组件
- 登录表单 (`Login.vue`)
- 注册表单 (`Register.vue`)
- 表单验证和错误处理

### 仪表盘组件
- 主布局 (`Layout.vue`)
- 导航菜单
- 用户信息展示
- 数据概览卡片

### 通用组件
- 404页面
- 加载状态
- 错误边界

## 样式设计

### CSS变量
- 统一的颜色系统
- 响应式断点
- 主题色配置

### 组件样式
- BEM命名约定
- Scoped样式
- 响应式设计

## 开发规范

### 代码风格
- ESLint + Prettier
- Vue 3 Composition API
- TypeScript支持（可选）

### 组件开发
- 单文件组件 (SFC)
- Props验证
- 事件命名规范
- 组件文档

### 错误处理
- 全局错误捕获
- 用户友好的错误提示
- 错误日志记录

## 性能优化

### 代码分割
- 路由级别懒加载
- 组件库按需导入
- 第三方库分包

### 缓存策略
- HTTP缓存配置
- 浏览器缓存利用
- API响应缓存

### 构建优化
- Tree shaking
- 资源压缩
- 图片优化

## 部署说明

### 环境变量
```env
VITE_API_BASE_URL=http://your-api-server.com
VITE_APP_TITLE=量化交易平台
```

### Docker部署
```dockerfile
FROM nginx:alpine
COPY dist/ /usr/share/nginx/html/
COPY nginx.conf /etc/nginx/conf.d/default.conf
```

### 反向代理配置
```nginx
location /api/ {
    proxy_pass http://backend-service:8081/api/v1/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
}
```

## 浏览器支持

- Chrome >= 87
- Firefox >= 78
- Safari >= 14
- Edge >= 88

## 开发团队

- 前端架构：Vue 3 + Vite
- UI设计：Element Plus
- 状态管理：Pinia
- 构建工具：Vite