# Market Data Service

市场数据采集服务 - 从AKShare/Tushare获取股票数据并发送到Kafka

## 状态 ✅

### 已完成功能
- ✅ AKShare数据源集成（无需Token）
- ✅ Tushare数据源集成（需配置Token）
- ✅ Kafka消息发送（stock_realtime, stock_daily, index_data topics）
- ✅ 自动定时采集（实时数据60秒/次，日线数据18:00）
- ✅ 完整日志记录

### 测试结果（2025-10-12）
```
✓ AKShare数据采集：成功
✓ Kafka连接：成功（localhost:9093）
✓ 消息发送：成功
✓ 测试股票：
  - 000001.SZ (平安银行): 20240115 开盘9.16 收盘9.21 +0.22%
  - 600519.SH (贵州茅台): 20240115 开盘1635 收盘1640 -0.19%
```

## 快速开始

### 1. 安装依赖
```bash
pip3 install -r requirements.txt
```

### 2. 配置环境变量

编辑 `.env` 文件：
```bash
# Tushare配置（可选，留空则仅使用AKShare）
TUSHARE_TOKEN=

# AKShare配置
AKSHARE_ENABLED=true

# Kafka配置（重要：使用9093端口连接外部客户端）
KAFKA_BOOTSTRAP_SERVERS=localhost:9093

# 股票代码列表
STOCK_CODES=000001.SZ,000002.SZ,600000.SH,600519.SH,000858.SZ

# 数据采集间隔（秒）
COLLECT_REALTIME_INTERVAL=60

# 日线数据采集时间
COLLECT_DAILY_TIME=18:00
```

### 3. 启动服务
```bash
python3 -m src.main
```

### 4. 测试数据采集
```bash
# 测试历史数据采集和Kafka发送
python3 test_kafka_flow.py

# 测试AKShare数据源
python3 test_data_sources.py
```

## Kafka配置说明

### 重要：端口配置

Docker Kafka容器配置了两个监听器：
- `kafka:9092` - 容器内部通信（PLAINTEXT）
- `localhost:9093` - 外部客户端连接（PLAINTEXT_HOST）

**Python服务必须使用 `localhost:9093` 端口！**

### Kafka Topics

已创建的topics（3个分区，1个副本）：
- `stock_realtime` - 实时股票数据
- `stock_daily` - 日线股票数据
- `index_data` - 指数数据

### 查看Kafka消息

```bash
# 查看topics列表
docker exec quant-kafka kafka-topics --bootstrap-server localhost:9092 --list

# 消费stock_daily消息
docker exec quant-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic stock_daily \
  --from-beginning \
  --max-messages 10
```

## 数据格式

### StockData (stock_daily, stock_realtime)
```json
{
  "ts_code": "000001.SZ",
  "trade_date": "20240115",
  "open": 9.16,
  "high": 9.29,
  "low": 9.13,
  "close": 9.21,
  "pre_close": null,
  "change": null,
  "pct_chg": 0.22,
  "vol": 745133.0,
  "amount": 685950951.45,
  "timestamp": "2025-10-12 01:48:16.145564"
}
```

### IndexData (index_data)
```json
{
  "ts_code": "000001.SH",
  "trade_date": "20240115",
  "close": 3200.5,
  "open": 3180.2,
  "high": 3210.8,
  "low": 3175.6,
  "vol": 123456789.0,
  "amount": 987654321.0,
  "pct_chg": 0.5
}
```

## 已知问题和限制

### AKShare实时行情接口限制
- `ak.stock_zh_a_spot()` 接口容易被限流（返回HTML而非JSON）
- 当前解决方案：使用 `ak.stock_zh_a_hist()` 获取当日数据作为"实时"数据
- 建议：注册Tushare Token作为主要数据源，AKShare作为备份

### 数据可用性
- 仅在交易时段有实时数据（周一至周五 9:30-15:00）
- 周末和节假日无数据
- 日线数据通常在收盘后30分钟内可用

## 服务日志

日志文件位置: `logs/market_data_service.log`

```bash
# 查看实时日志
tail -f logs/market_data_service.log

# 查看错误日志
grep ERROR logs/market_data_service.log
```

## 监控和调试

### 检查服务状态
```bash
# 查看Python进程
ps aux | grep "python3 -m src.main"

# 查看Docker容器
docker compose ps
```

### Kafka UI
访问 http://localhost:8080 查看Kafka消息

### 常见问题

1. **Kafka连接超时**
   - 确认使用 `localhost:9093` 端口
   - 检查Kafka容器状态: `docker compose ps kafka`

2. **AKShare数据获取失败**
   - 网络问题：检查代理设置
   - API限流：增加采集间隔或使用Tushare

3. **无实时数据**
   - 检查是否交易时段
   - 查看日志确认具体错误

## 下一步计划

- [ ] 添加Tushare Token配置和测试
- [ ] 实现自动重连机制
- [ ] 添加数据质量验证
- [ ] 支持更多数据源
- [ ] 添加Prometheus监控指标

## 参考文档

- [AKShare官方文档](https://akshare.akfamily.xyz)
- [Tushare官方文档](https://tushare.pro/document/2)
- [Stock Data API Tutorial](../docs/STOCK_DATA_API_TUTORIAL.md)
