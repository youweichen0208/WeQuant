# Stock Service 运行指南

## 项目概览

Stock Service 是量化交易平台的股票数据微服务，提供股票历史数据查询、实时数据获取、收益率计算等核心功能。

## 功能特性

- 📈 股票历史数据查询（支持1-365天）
- 📊 股票实时数据获取（带缓存优化）
- 💰 股票收益率计算（含年化收益率）
- 🔄 批量股票数据查询（最多50只）
- ⚡ 异步数据查询支持
- 📝 完整的API文档（Swagger）
- 🛡️ 全局异常处理
- 🏥 健康检查接口
- 🎯 数据缓存优化

## 项目结构

```
stock-service/
├── src/main/java/com/quant/stock/
│   ├── StockServiceApplication.java      # 主启动类
│   ├── controller/                       # 控制器层
│   │   ├── StockController.java         # 股票数据API
│   │   └── HealthController.java        # 健康检查API
│   ├── service/                         # 业务逻辑层
│   │   └── StockService.java           # 股票数据服务
│   ├── dto/                            # 数据传输对象
│   │   ├── StockHistoryResponse.java   # 历史数据响应
│   │   ├── StockLatestResponse.java    # 最新数据响应
│   │   ├── BatchStockRequest.java      # 批量查询请求
│   │   ├── BatchStockResponse.java     # 批量查询响应
│   │   ├── StockInfoResponse.java      # 基础信息响应
│   │   ├── StockReturnResponse.java    # 收益率响应
│   │   └── ApiResponse.java            # 统一响应格式
│   ├── config/                         # 配置类
│   │   ├── RestTemplateConfig.java     # HTTP客户端配置
│   │   ├── CacheConfig.java           # 缓存配置
│   │   ├── AsyncConfig.java           # 异步配置
│   │   └── MarketDataResponseErrorHandler.java # 错误处理
│   └── exception/                      # 异常处理
│       └── GlobalExceptionHandler.java
├── src/main/resources/
│   └── application.yml                 # 应用配置
├── pom.xml                            # Maven依赖配置
└── start_service.sh                   # 启动脚本
```

## 依赖服务

1. **Market Data Service** (必需)
   - 地址: http://localhost:5001
   - 状态: ✅ 运行中
   - 功能: 提供原始股票数据

2. **Redis** (可选，用于缓存)
   - 地址: localhost:6379
   - 密码: redis123456
   - 状态: ⚠️  未检测到，缓存功能将被禁用

3. **MySQL** (可选，用于数据持久化)
   - 地址: localhost:3306
   - 数据库: quant_trading
   - 状态: 未来版本将集成

## 运行方式

### 方式一: 使用IDE运行（推荐）

1. **在IDE中打开项目**
   - 打开 `/Users/youweichen/quant-trading-platform/stock-service`

2. **配置运行参数**
   - 主类: `com.quant.stock.StockServiceApplication`
   - JVM参数: `-Dspring.profiles.active=dev`
   - 工作目录: `/Users/youweichen/quant-trading-platform/stock-service`

3. **运行项目**
   - 右键点击 `StockServiceApplication.java`
   - 选择 "Run" 或 "Debug"
   - 或使用 IDE 的快捷键

4. **验证启动**
   - 看到启动横幅和成功信息
   - 访问健康检查: http://localhost:8082/stock-service/api/health

### 方式二: 使用启动脚本

```bash
cd /Users/youweichen/quant-trading-platform/stock-service
chmod +x start_service.sh
./start_service.sh
```

> 注意: 需要先安装 Maven

## API 端点

### 基础端点
- **服务地址**: http://localhost:8082/stock-service
- **健康检查**: GET /api/health
- **服务信息**: GET /api/info
- **API概览**: GET /api/overview
- **API文档**: http://localhost:8082/stock-service/swagger-ui.html

### 股票数据端点
- **历史数据**: GET /api/v1/stocks/{stockCode}/history?days=30
- **最新数据**: GET /api/v1/stocks/{stockCode}/latest
- **基础信息**: GET /api/v1/stocks/{stockCode}/info
- **收益率计算**: GET /api/v1/stocks/{stockCode}/return?days=30
- **批量查询**: POST /api/v1/stocks/batch/latest
- **异步历史数据**: GET /api/v1/stocks/{stockCode}/history/async?days=30
- **异步最新数据**: GET /api/v1/stocks/{stockCode}/latest/async

## 配置说明

### 端口配置
- 服务端口: 8082
- 上下文路径: /stock-service

### 缓存配置
- 历史数据缓存: 5分钟
- 实时数据缓存: 30秒
- 基础信息缓存: 24小时

### 外部依赖配置
```yaml
market-data:
  service:
    url: http://localhost:5001
    connection-timeout: 5000
    read-timeout: 30000
    retry:
      max-attempts: 3
      delay: 1000
```

## API 使用示例

### 获取股票历史数据
```bash
curl -X GET "http://localhost:8082/stock-service/api/v1/stocks/000001.SZ/history?days=30" \
     -H "accept: application/json"
```

### 获取股票最新数据
```bash
curl -X GET "http://localhost:8082/stock-service/api/v1/stocks/000001.SZ/latest" \
     -H "accept: application/json"
```

### 批量查询股票数据
```bash
curl -X POST "http://localhost:8082/stock-service/api/v1/stocks/batch/latest" \
     -H "accept: application/json" \
     -H "Content-Type: application/json" \
     -d '{
       "stockCodes": ["000001.SZ", "600519.SH", "300750.SZ"],
       "queryType": "latest"
     }'
```

### 计算股票收益率
```bash
curl -X GET "http://localhost:8082/stock-service/api/v1/stocks/000001.SZ/return?days=30" \
     -H "accept: application/json"
```

## 监控和调试

### 健康检查
```bash
curl http://localhost:8082/stock-service/api/health
```

### 监控指标
- **Actuator端点**: /actuator/metrics
- **Prometheus监控**: /actuator/prometheus
- **健康状态**: /actuator/health

### 日志配置
- 日志级别: 开发环境 DEBUG，生产环境 INFO
- 日志文件: `logs/stock-service.log`
- 日志轮转: 10MB/文件，保留30天

## 错误处理

系统提供完整的错误处理机制:

- **参数验证错误** (400): 请求参数格式错误
- **数据未找到错误** (404): 股票代码不存在
- **服务依赖错误** (502): Market Data Service不可用
- **系统内部错误** (500): 服务内部异常

## 性能优化

1. **缓存策略**: 使用Redis缓存频繁查询的数据
2. **异步处理**: 支持异步API减少等待时间
3. **批量查询**: 支持单次查询多只股票
4. **连接池**: HTTP客户端连接池优化
5. **重试机制**: 自动重试失败的外部调用

## 开发和扩展

### 添加新的股票指标
1. 在 `StockService` 中添加计算逻辑
2. 创建对应的响应DTO
3. 在 `StockController` 中添加新端点
4. 更新API文档

### 集成数据库
1. 启用JPA配置
2. 创建实体类
3. 实现Repository接口
4. 更新业务逻辑

### 添加新的缓存策略
1. 更新 `CacheConfig`
2. 在服务方法上添加 `@Cacheable` 注解
3. 配置缓存TTL

## 故障排除

### 常见问题

1. **启动失败** - 检查端口8082是否被占用
2. **连接Market Data Service失败** - 确认服务状态
3. **缓存不工作** - 检查Redis连接
4. **Lombok编译问题** - 已手动添加getter/setter方法

### 日志查看
```bash
tail -f /Users/youweichen/quant-trading-platform/stock-service/logs/stock-service.log
```

## 下一步计划

- [ ] 集成MySQL数据库持久化
- [ ] 添加更多技术指标计算
- [ ] 实现实时股价推送
- [ ] 添加用户认证和权限控制
- [ ] 性能监控和报警

---

🎯 **Stock Service 已准备就绪！**

通过IDE运行 `StockServiceApplication` 即可启动服务，然后访问 http://localhost:8082/stock-service/api/health 验证运行状态。