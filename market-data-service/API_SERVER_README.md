# Market Data API Service - Python Flask API

## 📋 概述

这是一个基于Flask的REST API服务，用于提供股票历史数据查询功能。它作为AKShare数据源的封装层，为Java后端服务提供统一的数据接口。

## 🏗️ 架构定位

```
Java Backend (port 8081) → Python Flask API (port 5000) → AKShare → 数据源
```

## 🚀 快速启动

### 方式1: 使用启动脚本（推荐）

```bash
cd /Users/youweichen/quant-trading-platform/market-data-service
./start_api.sh
```

### 方式2: 手动启动

```bash
# 1. 安装依赖
pip3 install flask flask-cors pydantic-settings

# 2. 启动服务
python3 api_server.py
```

服务将在 `http://localhost:5000` 启动。

## 📡 API端点

### 1. 健康检查
```bash
GET /api/health

# 响应
{
  "status": "ok",
  "service": "market-data-api",
  "timestamp": "2025-10-12T10:30:00"
}
```

### 2. 获取股票历史数据
```bash
GET /api/stocks/{stock_code}/history?days={days}

# 示例
curl http://localhost:5000/api/stocks/000001.SZ/history?days=7

# 响应
{
  "stock_code": "000001.SZ",
  "count": 5,
  "data": [
    {
      "date": "2025-10-09",
      "open": 11.32,
      "high": 11.45,
      "low": 11.28,
      "close": 11.40,
      "volume": 123456.0,
      "amount": 1408000.0,
      "pct_change": 0.53
    }
  ]
}
```

**参数说明:**
- `stock_code`: 股票代码，格式如 `000001.SZ` 或 `600519.SH`
- `days`: 获取天数，默认30天，最大365天

### 3. 获取股票最新数据
```bash
GET /api/stocks/{stock_code}/latest

# 示例
curl http://localhost:5000/api/stocks/000001.SZ/latest

# 响应
{
  "stock_code": "000001.SZ",
  "trade_date": "2025-10-09",
  "open": 11.32,
  "high": 11.45,
  "low": 11.28,
  "close": 11.40,
  "volume": 123456.0,
  "amount": 1408000.0,
  "pct_change": 0.53
}
```

### 4. 批量获取股票数据
```bash
POST /api/stocks/batch
Content-Type: application/json

{
  "stock_codes": ["000001.SZ", "600519.SH"],
  "days": 30
}

# 示例
curl -X POST http://localhost:5000/api/stocks/batch \
  -H "Content-Type: application/json" \
  -d '{"stock_codes": ["000001.SZ", "600519.SH"], "days": 7}'

# 响应
{
  "000001.SZ": {
    "latest": {
      "date": "2025-10-09",
      "close": 11.40,
      "pct_change": 0.53
    },
    "count": 5
  },
  "600519.SH": {
    "latest": {
      "date": "2025-10-09",
      "close": 1436.78,
      "pct_change": -0.50
    },
    "count": 5
  }
}
```

**限制:**
- 最多一次查询50只股票

### 5. 获取指数历史数据
```bash
GET /api/index/{index_code}/history?days={days}

# 示例 - 上证指数
curl http://localhost:5000/api/index/000001.SH/history?days=7

# 响应
{
  "index_code": "000001.SH",
  "count": 5,
  "data": [
    {
      "date": "2025-10-09",
      "open": 3250.12,
      "high": 3280.45,
      "low": 3240.30,
      "close": 3275.88,
      "volume": 320000000.0,
      "amount": 450000000000.0
    }
  ]
}
```

**常用指数代码:**
- `000001.SH` - 上证指数
- `399001.SZ` - 深证成指
- `399006.SZ` - 创业板指

## 🔧 配置说明

### 环境变量
API服务使用项目的 `.env` 文件配置，但不依赖Kafka等外部服务。

### 端口配置
默认端口: `5000`

如需修改，编辑 `api_server.py` 文件末尾:
```python
app.run(host='0.0.0.0', port=5000, debug=True)
```

## 📦 依赖说明

核心依赖:
- `flask>=2.3.0` - Web框架
- `flask-cors>=4.0.0` - 跨域支持
- `akshare>=1.11.0` - 数据源
- `pandas>=2.0.0` - 数据处理

完整依赖列表见 `requirements.txt`

## 🧪 测试

### 1. 单元测试
```bash
# 测试单只股票
curl http://localhost:5000/api/stocks/000001.SZ/latest

# 测试历史数据
curl http://localhost:5000/api/stocks/000001.SZ/history?days=7

# 测试指数数据
curl http://localhost:5000/api/index/000001.SH/history?days=7
```

### 2. 批量测试脚本
```bash
# 使用现有的测试脚本
python3 test_akshare_standalone.py
```

## ⚠️ 已知问题

### 1. 周末/非交易日
- 周末和节假日无法获取当日数据
- API会自动返回最近一个交易日的数据
- 示例: 2025年10月12日（周六）查询会返回10月9日（周五）数据

### 2. 某些API不稳定
- `stock_zh_a_spot_em()` (东方财富) 可能被防火墙屏蔽
- 当前使用更稳定的 `stock_zh_a_hist()` 接口
- 如遇到问题，检查网络连接

### 3. 数据延迟
- 历史数据通常延迟1个交易日
- 实时数据功能需要配合Kafka流式服务（见主README）

## 🔍 故障排查

### 问题: 无法启动服务
```bash
# 检查端口占用
lsof -i :5000

# 如果被占用，杀死进程或更改端口
```

### 问题: 找不到模块
```bash
# 重新安装依赖
pip3 install -r requirements.txt

# 检查Python路径
python3 -c "import sys; print(sys.path)"
```

### 问题: 数据获取失败
```bash
# 运行诊断脚本
python3 debug_akshare.py

# 查看详细错误日志（开启debug模式）
```

## 📚 相关文档

- [AKShare官方文档](https://akshare.akfamily.xyz/)
- [Flask官方文档](https://flask.palletsprojects.com/)
- [项目主README](../README.md)
- [Java服务实现指南](../STOCK_HISTORY_SERVICE_GUIDE.md)

## 🎯 与Java服务集成

### Java端调用示例
```java
RestTemplate restTemplate = new RestTemplate();
String url = "http://localhost:5000/api/stocks/000001.SZ/history?days=7";
StockHistoryResponse response = restTemplate.getForObject(url, StockHistoryResponse.class);
```

### 错误处理
API遵循标准HTTP状态码:
- `200 OK` - 成功
- `404 Not Found` - 股票代码不存在或无数据
- `500 Internal Server Error` - 服务器错误

错误响应格式:
```json
{
  "error": "错误描述信息",
  "stock_code": "000001.SZ"
}
```

## 🚦 生产环境建议

### 1. 使用生产级WSGI服务器
```bash
# 安装gunicorn
pip3 install gunicorn

# 启动（4个worker进程）
gunicorn -w 4 -b 0.0.0.0:5000 api_server:app
```

### 2. 添加缓存
- 使用Redis缓存热门股票数据
- 设置适当的TTL（如5分钟）

### 3. 添加限流
- 防止API滥用
- 使用Flask-Limiter

### 4. 监控和日志
- 添加APM工具（如Prometheus）
- 结构化日志输出

## 📝 开发说明

### 添加新的API端点

1. 在 `api_server.py` 添加路由:
```python
@app.route('/api/your-endpoint', methods=['GET'])
def your_handler():
    # 实现逻辑
    return jsonify(result)
```

2. 使用 `AKShareProvider` 获取数据:
```python
from src.data_providers.akshare_provider import AKShareProvider
provider = AKShareProvider()
data = provider.get_stock_daily(...)
```

3. 测试新端点:
```bash
curl http://localhost:5000/api/your-endpoint
```

## 💡 提示

1. **首次运行可能较慢** - AKShare需要下载数据
2. **建议本地缓存** - 频繁查询相同数据时
3. **注意API限制** - 某些数据源有调用频率限制
4. **开发环境使用debug=True** - 生产环境务必设置为False

## 🤝 与其他服务的关系

- **market-data-service/src/main.py**: Kafka实时数据流服务
- **api_server.py**: 历史数据REST API服务（本文件）
- **stock-history-service (Java)**: 业务逻辑层，调用本API

这两个Python服务可以同时运行，互不干扰。
