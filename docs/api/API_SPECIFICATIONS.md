# API 规范文档

## 概述

WeQuant 量化交易平台 API 规范，包含所有微服务的接口定义、请求/响应格式、错误处理和认证机制。

## 🔧 通用规范

### 基础URL

| 服务 | 环境 | URL |
|------|------|-----|
| user-service | 开发 | http://localhost:8081/user-service/api/v1 |
| stock-service | 开发 | http://localhost:8082/stock-service/api/v1 |
| trading-service | 开发 | http://localhost:8083/trading-service/api/v1 |
| market-data-service | 开发 | http://localhost:5001/api |
| mock-trading-service | 开发 | http://localhost:5002/api |

### 通用响应格式

```json
{
  "success": true,
  "message": "操作成功",
  "data": {},
  "timestamp": "2025-10-17T15:30:00Z",
  "code": 200
}
```

### 错误响应格式

```json
{
  "success": false,
  "message": "错误描述",
  "error": "具体错误信息",
  "code": 400,
  "timestamp": "2025-10-17T15:30:00Z"
}
```

### HTTP状态码

| 状态码 | 含义 | 使用场景 |
|-------|------|----------|
| 200 | OK | 请求成功 |
| 201 | Created | 资源创建成功 |
| 400 | Bad Request | 请求参数错误 |
| 401 | Unauthorized | 未认证 |
| 403 | Forbidden | 无权限 |
| 404 | Not Found | 资源不存在 |
| 500 | Internal Server Error | 服务器内部错误 |

---

## 🔐 用户服务 (user-service)

### 认证相关

#### 1. 用户注册
```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "username": "testuser",
  "email": "test@example.com",
  "password": "password123",
  "fullName": "测试用户"
}
```

**响应**:
```json
{
  "success": true,
  "message": "注册成功",
  "data": {
    "userId": "12345",
    "username": "testuser",
    "email": "test@example.com",
    "fullName": "测试用户",
    "createdAt": "2025-10-17T15:30:00Z"
  }
}
```

#### 2. 用户登录
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "testuser",
  "password": "password123"
}
```

**响应**:
```json
{
  "success": true,
  "message": "登录成功",
  "data": {
    "token": "jwt_token_here",
    "refreshToken": "refresh_token_here",
    "user": {
      "userId": "12345",
      "username": "testuser",
      "email": "test@example.com",
      "fullName": "测试用户"
    },
    "expiresAt": "2025-10-18T15:30:00Z"
  }
}
```

#### 3. 用户登出
```http
POST /api/v1/auth/logout
Authorization: Bearer {jwt_token}
```

### 用户管理

#### 4. 获取用户信息
```http
GET /api/v1/user/profile
Authorization: Bearer {jwt_token}
```

**响应**:
```json
{
  "success": true,
  "data": {
    "userId": "12345",
    "username": "testuser",
    "email": "test@example.com",
    "fullName": "测试用户",
    "phone": "13800138000",
    "status": "ACTIVE",
    "riskLevel": "MODERATE",
    "preferences": {
      "theme": "LIGHT",
      "language": "zh-CN",
      "timezone": "Asia/Shanghai"
    }
  }
}
```

#### 5. 更新用户信息
```http
PUT /api/v1/user/profile
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "fullName": "新的姓名",
  "phone": "13900139000"
}
```

---

## 📈 股票服务 (stock-service)

### 股票数据

#### 1. 获取股票最新价格
```http
GET /api/v1/stocks/{code}/latest
```

**示例**: `GET /api/v1/stocks/000001.SZ/latest`

**响应**:
```json
{
  "success": true,
  "data": {
    "code": "000001.SZ",
    "name": "平安银行",
    "price": 11.40,
    "change": 0.06,
    "changePct": 0.53,
    "volume": 12847500,
    "turnover": 146543000.00,
    "open": 11.33,
    "high": 11.41,
    "low": 11.27,
    "preClose": 11.34,
    "timestamp": "2025-10-17T15:00:00Z"
  }
}
```

#### 2. 获取股票历史数据
```http
GET /api/v1/stocks/{code}/history?days={days}&period={period}
```

**参数**:
- `days`: 天数 (默认: 30)
- `period`: 周期 daily|weekly|monthly (默认: daily)

**示例**: `GET /api/v1/stocks/000001.SZ/history?days=30`

**响应**:
```json
{
  "success": true,
  "data": {
    "code": "000001.SZ",
    "name": "平安银行",
    "history": [
      {
        "date": "2025-10-17",
        "open": 11.33,
        "high": 11.41,
        "low": 11.27,
        "close": 11.40,
        "volume": 12847500,
        "turnover": 146543000.00
      },
      {
        "date": "2025-10-16",
        "open": 11.30,
        "high": 11.38,
        "low": 11.25,
        "close": 11.34,
        "volume": 11235800,
        "turnover": 127456000.00
      }
    ]
  }
}
```

#### 3. 获取股票基本信息
```http
GET /api/v1/stocks/{code}/info
```

**响应**:
```json
{
  "success": true,
  "data": {
    "code": "000001.SZ",
    "name": "平安银行",
    "fullName": "平安银行股份有限公司",
    "industry": "银行业",
    "sector": "金融",
    "marketCap": 254820000000.00,
    "pe": 5.23,
    "pb": 0.67,
    "eps": 2.18,
    "roe": 12.85,
    "description": "中国平安银行股份有限公司..."
  }
}
```

### 市场数据

#### 4. 获取涨跌排行榜
```http
GET /api/v1/market/ranking?type={type}&limit={limit}
```

**参数**:
- `type`: gainers|losers|volume|turnover (默认: gainers)
- `limit`: 数量限制 (默认: 50)

**响应**:
```json
{
  "success": true,
  "data": {
    "type": "gainers",
    "updateTime": "2025-10-17T15:00:00Z",
    "stocks": [
      {
        "code": "000001.SZ",
        "name": "平安银行",
        "price": 11.40,
        "changePct": 5.23,
        "volume": 12847500
      }
    ]
  }
}
```

#### 5. 获取板块数据
```http
GET /api/v1/market/sectors?type={type}
```

**参数**:
- `type`: industry|concept (默认: industry)

**响应**:
```json
{
  "success": true,
  "data": {
    "sectors": [
      {
        "code": "BK0475",
        "name": "银行",
        "changePct": 2.15,
        "stockCount": 42,
        "leadingStocks": [
          {
            "code": "000001.SZ",
            "name": "平安银行",
            "changePct": 5.23
          }
        ]
      }
    ]
  }
}
```

---

## 💰 交易服务 (trading-service)

### 账户管理

#### 1. 创建交易账户
```http
POST /api/v1/accounts
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "initialBalance": 1000000.00
}
```

**响应**:
```json
{
  "success": true,
  "message": "账户创建成功",
  "data": {
    "accountId": "acc_123456789",
    "userId": "user_123",
    "balance": 1000000.00,
    "totalAssets": 1000000.00,
    "status": "ACTIVE",
    "createdAt": "2025-10-17T15:30:00Z"
  }
}
```

#### 2. 获取账户信息
```http
GET /api/v1/accounts/{accountId}
Authorization: Bearer {jwt_token}
```

**响应**:
```json
{
  "success": true,
  "data": {
    "accountId": "acc_123456789",
    "balance": 980000.00,
    "totalAssets": 1015000.00,
    "marketValue": 35000.00,
    "frozenAmount": 0.00,
    "positionCount": 3,
    "totalProfit": 15000.00,
    "totalProfitPct": 1.5,
    "positions": [
      {
        "stockCode": "000001.SZ",
        "stockName": "平安银行",
        "quantity": 1000,
        "avgCost": 11.20,
        "currentPrice": 11.40,
        "marketValue": 11400.00,
        "profitLoss": 200.00,
        "profitLossPct": 1.79
      }
    ]
  }
}
```

### 交易操作

#### 3. 执行交易
```http
POST /api/v1/trades
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "accountId": "acc_123456789",
  "stockCode": "000001.SZ",
  "tradeType": "BUY",
  "quantity": 1000,
  "price": 11.40
}
```

**响应**:
```json
{
  "success": true,
  "message": "交易执行成功",
  "data": {
    "tradeId": "trade_123456789",
    "accountId": "acc_123456789",
    "stockCode": "000001.SZ",
    "stockName": "平安银行",
    "tradeType": "BUY",
    "quantity": 1000,
    "price": 11.40,
    "amount": 11400.00,
    "commission": 3.42,
    "totalCost": 11403.42,
    "status": "COMPLETED",
    "tradeTime": "2025-10-17T15:30:00Z"
  }
}
```

#### 4. 获取交易历史
```http
GET /api/v1/trades/{accountId}?page={page}&size={size}&type={type}
Authorization: Bearer {jwt_token}
```

**参数**:
- `page`: 页码 (从0开始，默认: 0)
- `size`: 每页大小 (默认: 20)
- `type`: BUY|SELL (可选)

**响应**:
```json
{
  "success": true,
  "data": {
    "trades": [
      {
        "tradeId": "trade_123456789",
        "stockCode": "000001.SZ",
        "stockName": "平安银行",
        "tradeType": "BUY",
        "quantity": 1000,
        "price": 11.40,
        "amount": 11400.00,
        "commission": 3.42,
        "status": "COMPLETED",
        "tradeTime": "2025-10-17T15:30:00Z"
      }
    ],
    "pagination": {
      "page": 0,
      "size": 20,
      "total": 25,
      "totalPages": 2
    }
  }
}
```

---

## 📊 市场数据服务 (market-data-service)

### 数据获取

#### 1. 获取实时股价
```http
GET /api/stocks/{code}/latest
```

**示例**: `GET /api/stocks/000001.SZ/latest`

**响应**:
```json
{
  "code": "000001.SZ",
  "name": "平安银行",
  "price": 11.40,
  "change": 0.06,
  "change_pct": 0.53,
  "volume": 12847500,
  "amount": 146543000.00,
  "open": 11.33,
  "high": 11.41,
  "low": 11.27,
  "pre_close": 11.34,
  "timestamp": "2025-10-17 15:00:00"
}
```

#### 2. 获取K线数据
```http
GET /api/stocks/{code}/history?days={days}
```

**响应**:
```json
{
  "code": "000001.SZ",
  "name": "平安银行",
  "data": [
    {
      "date": "2025-10-17",
      "open": 11.33,
      "high": 11.41,
      "low": 11.27,
      "close": 11.40,
      "volume": 12847500
    }
  ]
}
```

#### 3. 搜索股票
```http
GET /api/search?q={keyword}&limit={limit}
```

**参数**:
- `q`: 搜索关键词 (股票代码或名称)
- `limit`: 结果数量限制 (默认: 10)

**响应**:
```json
{
  "results": [
    {
      "code": "000001.SZ",
      "name": "平安银行",
      "market": "深圳"
    },
    {
      "code": "600036.SH",
      "name": "招商银行",
      "market": "上海"
    }
  ]
}
```

---

## 🎮 模拟交易服务 (mock-trading-service)

### 账户管理

#### 1. 用户注册
```http
POST /api/register
Content-Type: application/json

{
  "username": "testuser",
  "email": "test@example.com"
}
```

**响应**:
```json
{
  "message": "账户创建成功！",
  "account_id": "acc_12345",
  "username": "testuser",
  "initial_balance": 1000000.00
}
```

#### 2. 获取账户信息
```http
GET /api/account/{account_id}
```

**响应**:
```json
{
  "account_id": "acc_12345",
  "username": "testuser",
  "balance": 980000.00,
  "total_assets": 1015000.00,
  "market_value": 35000.00,
  "position_count": 3,
  "positions": [
    {
      "stock_code": "000001.SZ",
      "stock_name": "平安银行",
      "quantity": 1000,
      "avg_cost": 11.20,
      "current_price": 11.40,
      "market_value": 11400.00,
      "profit_loss": 200.00,
      "profit_loss_pct": 1.79
    }
  ]
}
```

### 交易操作

#### 3. 执行交易
```http
POST /api/trade
Content-Type: application/json

{
  "account_id": "acc_12345",
  "stock_code": "000001.SZ",
  "trade_type": "buy",
  "quantity": 1000
}
```

**响应**:
```json
{
  "message": "买入成功！",
  "trade_id": "trade_67890",
  "account_id": "acc_12345",
  "stock_code": "000001.SZ",
  "trade_type": "buy",
  "quantity": 1000,
  "price": 11.40,
  "amount": 11400.00,
  "commission": 3.42,
  "trade_time": "2025-10-17 15:30:00"
}
```

#### 4. 获取交易历史
```http
GET /api/trades/{account_id}
```

**响应**:
```json
{
  "trades": [
    {
      "trade_id": "trade_67890",
      "stock_code": "000001.SZ",
      "stock_name": "平安银行",
      "trade_type": "buy",
      "quantity": 1000,
      "price": 11.40,
      "amount": 11400.00,
      "commission": 3.42,
      "status": "completed",
      "trade_time": "2025-10-17 15:30:00"
    }
  ]
}
```

#### 5. 获取市场数据
```http
GET /api/market/{stock_code}
```

**响应**:
```json
{
  "code": "000001.SZ",
  "name": "平安银行",
  "price": 11.40,
  "change": 0.06,
  "change_pct": 0.53,
  "volume": 12847500,
  "timestamp": "2025-10-17 15:00:00"
}
```

---

## 🔒 认证与授权

### JWT Token

所有需要认证的接口都需要在请求头中携带JWT Token：

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Token 刷新

```http
POST /api/v1/auth/refresh
Content-Type: application/json

{
  "refreshToken": "refresh_token_here"
}
```

**响应**:
```json
{
  "success": true,
  "data": {
    "token": "new_jwt_token_here",
    "expiresAt": "2025-10-18T15:30:00Z"
  }
}
```

---

## 🛡️ 错误处理

### 常见错误码

| 错误码 | 含义 | 处理建议 |
|-------|------|----------|
| 1001 | 用户名已存在 | 更换用户名 |
| 1002 | 邮箱已存在 | 更换邮箱或找回密码 |
| 1003 | 用户名或密码错误 | 检查登录信息 |
| 1004 | Token已过期 | 重新登录或刷新Token |
| 1005 | 权限不足 | 联系管理员 |
| 2001 | 股票代码不存在 | 检查股票代码格式 |
| 2002 | 市场数据暂时不可用 | 稍后重试 |
| 3001 | 账户余额不足 | 减少交易数量 |
| 3002 | 持仓数量不足 | 检查可卖数量 |
| 3003 | 交易时间段不正确 | 等待开市时间 |
| 5000 | 服务内部错误 | 联系技术支持 |

### 错误响应示例

```json
{
  "success": false,
  "message": "账户余额不足",
  "error": "当前余额: ¥5000.00，需要: ¥11403.42",
  "code": 3001,
  "timestamp": "2025-10-17T15:30:00Z",
  "path": "/api/v1/trades"
}
```

---

## 📋 API测试

### Postman集合

推荐使用以下测试环境变量：

```json
{
  "base_url_user": "http://localhost:8081/user-service/api/v1",
  "base_url_stock": "http://localhost:8082/stock-service/api/v1",
  "base_url_trading": "http://localhost:8083/trading-service/api/v1",
  "base_url_market": "http://localhost:5001/api",
  "base_url_mock": "http://localhost:5002/api",
  "jwt_token": "",
  "account_id": ""
}
```

### 测试流程

1. **用户注册** → 获取userId
2. **用户登录** → 获取JWT Token
3. **创建交易账户** → 获取accountId
4. **查询股票价格** → 获取实时价格
5. **执行买入交易** → 测试交易功能
6. **查看账户状态** → 验证交易结果
7. **查看交易历史** → 确认记录完整

---

*最后更新: 2025-10-17*
*API版本: v1.0*