# 量化交易平台 - 快速开始指南

## 项目简介

基于微服务架构的个人量化交易分析平台

## 技术栈

- **前端**: Vue 3
- **后端**: Java Spring Boot + Dubbo + Zookeeper
- **消息队列**: Kafka
- **数据服务**: Python
- **数据库**: MySQL + Redis
- **容器化**: Docker + Docker Compose

---

## 第一步：基础设施搭建 ✅

### 前置要求

1. **Docker Desktop** (必需)

   - macOS: 下载安装 [Docker Desktop for Mac](https://www.docker.com/products/docker-desktop/)
   - 安装后启动 Docker Desktop

2. **检查 Docker 是否安装成功**
   ```bash
   docker --version
   docker compose --version
   ```

### 启动基础设施

1. **进入项目目录**

   ```bash
   cd quant-trading-platform
   ```

2. **启动所有基础设施容器**

   ```bash
   docker compose up -d
   ```

   这将启动以下服务：

   - ✅ Zookeeper (端口 2181)
   - ✅ Kafka (端口 9092/9093)
   - ✅ MySQL (端口 3306)
   - ✅ Redis (端口 6379)
   - ✅ Kafka UI (端口 8080)

3. **查看容器状态**

   ```bash
   docker compose ps
   ```

   所有容器状态应该为 `Up`

4. **查看容器日志**

   ```bash
   # 查看所有日志
   docker-compose logs

   # 查看特定服务日志
   docker-compose logs mysql
   docker-compose logs kafka
   ```

### 验证服务

1. **验证 MySQL**

   ```bash
   docker exec -it quant-mysql mysql -uroot -proot123456 -e "SHOW DATABASES;"
   ```

   应该能看到 `quant_trading` 数据库

2. **验证 Redis**

   ```bash
   docker exec -it quant-redis redis-cli -a redis123456 ping
   ```

   应该返回 `PONG`

3. **验证 Kafka UI**
   浏览器访问: http://localhost:8080
   可以看到 Kafka 的管理界面

### 停止/重启服务

```bash
# 停止所有容器
docker-compose stop

# 重启所有容器
docker-compose restart

# 停止并删除所有容器
docker-compose down

# 停止并删除所有容器和数据卷（⚠️ 会删除数据）
docker-compose down -v
```

---

## 数据库说明

### 已创建的表

- `users` - 用户表
- `user_accounts` - 用户资金账户表
- `stocks` - 股票信息表
- `kline_daily` - 日 K 线数据表
- `strategies` - 策略配置表
- `orders` - 订单表
- `positions` - 持仓表
- `trades` - 交易记录表

### 测试账户

- 用户名: `test_user`
- 邮箱: `test@example.com`
- 初始资金: 100,000 元

---

## 下一步开发计划

### 第二步：Python 数据服务 (进行中...)

- [ ] 接入 Tushare/AKShare API
- [ ] Kafka Producer 实现
- [ ] 实时行情数据获取

### 第三步：用户服务

- [ ] 用户注册/登录
- [ ] JWT 认证

### 第四步：数据分析服务

- [ ] K 线数据存储
- [ ] 技术指标计算

---

## 项目结构

```
quant-trading-platform/
├── docker-compose.yml          # Docker编排文件 ✅
├── .env                        # 环境变量配置 ✅
├── README.md                   # 本文档 ✅
├── infrastructure/             # 基础设施配置 ✅
│   └── mysql/
│       └── init.sql           # 数据库初始化脚本 ✅
├── frontend/                   # Vue前端 (待开发)
├── api-gateway/                # API网关 (待开发)
├── services/                   # 后端微服务 (待开发)
└── data-service/               # Python数据服务 (待开发)
```

---

## 常见问题

### Q: Docker 容器启动失败？

A: 检查端口是否被占用，特别是 3306(MySQL)、6379(Redis)、9092(Kafka)

### Q: 如何修改数据库密码？

A: 修改 `.env` 文件中的 `MYSQL_ROOT_PASSWORD` 和 `REDIS_PASSWORD`

### Q: 如何连接数据库？

A:

- Host: localhost
- Port: 3306
- User: root
- Password: root123456
- Database: quant_trading

---

## 开发工具推荐

- **数据库管理**: DBeaver / DataGrip
- **API 测试**: Postman / Apifox
- **代码编辑器**: VSCode (前端/Python) / IntelliJ IDEA (Java)

---

**当前进度**: 第一步基础设施搭建 ✅ 完成
