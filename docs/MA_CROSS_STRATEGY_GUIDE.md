# 双均线交叉策略 - 完整实现与使用指南

## 📋 目录

1. [功能概述](#功能概述)
2. [技术架构](#技术架构)
3. [实现步骤](#实现步骤)
4. [代码详解](#代码详解)
5. [使用指南](#使用指南)
6. [API文档](#api文档)
7. [故障排查](#故障排查)

---

## 功能概述

### 什么是双均线交叉策略？

双均线交叉策略（Moving Average Crossover Strategy）是最经典的技术分析策略之一，通过观察短期均线和长期均线的交叉来判断买卖时机。

### 核心原理

```
📈 金叉（Golden Cross）→ 买入信号
   条件：短期均线向上穿越长期均线

   前一天: MA5 < MA20
   今   天: MA5 > MA20
   → 生成 BUY 信号

📉 死叉（Death Cross）→ 卖出信号
   条件：短期均线向下穿越长期均线

   前一天: MA5 > MA20
   今   天: MA5 < MA20
   → 生成 SELL 信号

⏸️ 无交叉 → 持有
   条件：均线保持原有相对位置
   → 生成 HOLD 信号
```

### 默认参数

- **短期均线**: MA5（5日移动平均线）
- **长期均线**: MA20（20日移动平均线）
- 参数可自定义调整

---

## 技术架构

### 系统架构图

```
┌─────────────────────────────────────────────────────────────┐
│                     前端 (Vue.js)                            │
│  http://localhost:3000/dashboard/strategy                   │
│  - 策略创建界面                                               │
│  - 信号生成按钮                                               │
│  - 信号历史展示                                               │
└─────────────────────────────────────────────────────────────┘
                              ↓ HTTP REST API
┌─────────────────────────────────────────────────────────────┐
│          后端 (Spring Boot) - trading-service               │
│              http://localhost:8083                           │
│                                                              │
│  ┌────────────────────────────────────────────────────┐    │
│  │  StrategyController                                 │    │
│  │  /api/strategy/*                                    │    │
│  └────────────────────────────────────────────────────┘    │
│                       ↓                                      │
│  ┌────────────────────────────────────────────────────┐    │
│  │  StrategyService                                    │    │
│  │  - 策略管理                                          │    │
│  │  - 信号生成                                          │    │
│  └────────────────────────────────────────────────────┘    │
│         ↓                            ↓                       │
│  ┌──────────────┐          ┌──────────────────────┐        │
│  │ TradingStrategy       │  │ IndicatorCalculator  │        │
│  │ 策略接口        │          │ 技术指标计算         │        │
│  └──────────────┘          └──────────────────────┘        │
│         ↓                                                    │
│  ┌──────────────────────────────────────────────────┐      │
│  │  MovingAverageCrossStrategy                       │      │
│  │  双均线交叉策略实现                                 │      │
│  └──────────────────────────────────────────────────┘      │
│                       ↓                                      │
│  ┌────────────────────────────────────────────────────┐    │
│  │  H2 Database (内存数据库)                            │    │
│  │  - strategies (策略表)                              │    │
│  │  - strategy_signals (信号表)                        │    │
│  └────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

### 核心模块

| 模块 | 文件 | 功能 |
|------|------|------|
| **技术指标计算** | `IndicatorCalculator.java` | MA、EMA、MACD、RSI、BOLL计算 |
| **策略接口** | `TradingStrategy.java` | 定义策略标准接口 |
| **双均线策略** | `MovingAverageCrossStrategy.java` | 实现金叉死叉判断逻辑 |
| **策略服务** | `StrategyService.java` | 策略管理、信号生成 |
| **REST API** | `StrategyController.java` | 提供HTTP接口 |
| **实体类** | `Strategy.java`, `StrategySignal.java` | 数据库实体 |
| **前端页面** | `Strategy.vue` | 策略管理界面 |

---

## 实现步骤

### 第一步：创建技术指标计算工具类

**文件位置**: `trading-service/src/main/java/com/quant/trading/indicator/IndicatorCalculator.java`

**核心功能**:
- ✅ **SMA（简单移动平均线）**: 计算指定周期的平均价格
- ✅ **EMA（指数移动平均线）**: 对近期数据赋予更高权重
- ✅ **MACD**: 趋势跟踪动量指标
- ✅ **RSI**: 相对强弱指标（0-100）
- ✅ **BOLL（布林带）**: 价格波动区间

**SMA计算示例**:
```java
public static List<BigDecimal> calculateSMA(List<BigDecimal> prices, int period) {
    List<BigDecimal> result = new ArrayList<>();

    // 前period-1个数据不足，填充null
    for (int i = 0; i < period - 1; i++) {
        result.add(null);
    }

    // 从第period个开始计算MA
    for (int i = period - 1; i < prices.size(); i++) {
        BigDecimal sum = BigDecimal.ZERO;
        for (int j = 0; j < period; j++) {
            sum = sum.add(prices.get(i - j));
        }
        BigDecimal ma = sum.divide(BigDecimal.valueOf(period), 4, RoundingMode.HALF_UP);
        result.add(ma);
    }

    return result;
}
```

### 第二步：创建数据库实体

#### 策略表 (strategies)

**文件位置**: `trading-service/src/main/java/com/quant/trading/entity/Strategy.java`

**字段说明**:
```java
@Entity
@Table(name = "strategies")
public class Strategy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                    // 策略ID

    private String name;                // 策略名称
    private String type;                // 策略类型: MA_CROSS, MACD, RSI等
    private String parameters;          // 参数JSON: {"shortPeriod": 5, "longPeriod": 20}
    private String description;         // 策略描述
    private String status;              // 状态: STOPPED, RUNNING, PAUSED
    private Long userId;                // 用户ID
    private LocalDateTime createdAt;    // 创建时间
    private LocalDateTime updatedAt;    // 更新时间
}
```

#### 信号表 (strategy_signals)

**文件位置**: `trading-service/src/main/java/com/quant/trading/entity/StrategySignal.java`

**字段说明**:
```java
@Entity
@Table(name = "strategy_signals")
public class StrategySignal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                    // 信号ID

    private Long strategyId;            // 关联的策略ID
    private String stockCode;           // 股票代码: 000001.SZ
    private String stockName;           // 股票名称: 平安银行
    private String signalType;          // 信号类型: BUY, SELL, HOLD
    private BigDecimal price;           // 信号产生时的价格
    private BigDecimal signalStrength;  // 信号强度(0-100)
    private String reason;              // 信号原因详情
    private LocalDateTime signalTime;   // 信号生成时间
    private Boolean executed;           // 是否已执行交易
    private LocalDateTime executedAt;   // 执行时间
}
```

### 第三步：定义策略接口

**文件位置**: `trading-service/src/main/java/com/quant/trading/strategy/TradingStrategy.java`

**接口定义**:
```java
public interface TradingStrategy {
    /**
     * 生成交易信号
     * @param stockCode 股票代码
     * @param prices 价格数据列表（按时间顺序，最新的在最后）
     * @param parameters 策略参数（JSON格式）
     * @return 交易信号
     */
    StrategySignal generateSignal(String stockCode, List<BigDecimal> prices, String parameters);

    /**
     * 获取策略类型
     */
    String getStrategyType();

    /**
     * 获取策略描述
     */
    String getDescription();

    /**
     * 验证策略参数是否有效
     */
    boolean validateParameters(String parameters);
}
```

### 第四步：实现双均线交叉策略

**文件位置**: `trading-service/src/main/java/com/quant/trading/strategy/MovingAverageCrossStrategy.java`

**核心逻辑**:
```java
@Component
public class MovingAverageCrossStrategy implements TradingStrategy {

    @Override
    public StrategySignal generateSignal(String stockCode, List<BigDecimal> prices, String parameters) {
        // 1. 解析参数
        int shortPeriod = 5;   // 默认5日
        int longPeriod = 20;   // 默认20日

        // 2. 计算均线
        List<BigDecimal> shortMA = IndicatorCalculator.calculateSMA(prices, shortPeriod);
        List<BigDecimal> longMA = IndicatorCalculator.calculateSMA(prices, longPeriod);

        // 3. 获取当前和前一天的均线值
        BigDecimal currentShortMA = shortMA.get(shortMA.size() - 1);
        BigDecimal currentLongMA = longMA.get(longMA.size() - 1);
        BigDecimal previousShortMA = shortMA.get(shortMA.size() - 2);
        BigDecimal previousLongMA = longMA.get(longMA.size() - 2);

        // 4. 判断金叉或死叉
        StrategySignal signal = new StrategySignal();

        // 金叉: 前一天短期MA < 长期MA, 今天短期MA > 长期MA
        if (previousShortMA.compareTo(previousLongMA) < 0 &&
            currentShortMA.compareTo(currentLongMA) > 0) {
            signal.setSignalType("BUY");
            signal.setReason("金叉信号: MA5向上穿越MA20");
        }
        // 死叉: 前一天短期MA > 长期MA, 今天短期MA < 长期MA
        else if (previousShortMA.compareTo(previousLongMA) > 0 &&
                 currentShortMA.compareTo(currentLongMA) < 0) {
            signal.setSignalType("SELL");
            signal.setReason("死叉信号: MA5向下穿越MA20");
        }
        else {
            signal.setSignalType("HOLD");
            signal.setReason("无交叉信号");
        }

        return signal;
    }
}
```

### 第五步：创建策略服务层

**文件位置**: `trading-service/src/main/java/com/quant/trading/service/StrategyService.java`

**核心方法**:
```java
@Service
public class StrategyService {

    /**
     * 创建新策略
     */
    public Strategy createStrategy(String name, String type, String parameters,
                                   String description, Long userId) {
        Strategy strategy = new Strategy();
        strategy.setName(name);
        strategy.setType(type);
        strategy.setParameters(parameters);
        strategy.setStatus("STOPPED");
        return strategyRepository.save(strategy);
    }

    /**
     * 为指定股票生成交易信号
     */
    public StrategySignal generateSignal(Long strategyId, String stockCode) {
        // 1. 获取策略配置
        Strategy strategy = strategyRepository.findById(strategyId).orElseThrow();

        // 2. 获取策略实现
        TradingStrategy tradingStrategy = getStrategyByType(strategy.getType());

        // 3. 获取历史价格数据（60天）
        List<BigDecimal> prices = getHistoricalPrices(stockCode, 60);

        // 4. 生成信号
        StrategySignal signal = tradingStrategy.generateSignal(stockCode, prices,
                                                               strategy.getParameters());
        signal.setStrategyId(strategyId);

        // 5. 保存信号
        return strategySignalRepository.save(signal);
    }
}
```

### 第六步：创建REST API控制器

**文件位置**: `trading-service/src/main/java/com/quant/trading/controller/StrategyController.java`

**关键点**:
⚠️ **注意路径配置** - 不要包含 `/trading-service`，因为在 `application.yml` 中已配置：
```yaml
server:
  servlet:
    context-path: /trading-service
```

**正确的注解**:
```java
@RestController
@RequestMapping("/api/strategy")  // ✅ 正确：最终路径是 /trading-service/api/strategy
@CrossOrigin(origins = "*")
public class StrategyController {
    // ...
}
```

**错误示例**:
```java
@RequestMapping("/trading-service/api/strategy")  // ❌ 错误：会变成 /trading-service/trading-service/api/strategy
```

### 第七步：创建前端策略管理页面

**文件位置**: `web-frontend/src/views/dashboard/Strategy.vue`

**核心功能**:
1. ✅ 策略列表展示
2. ✅ 创建策略对话框
3. ✅ 生成信号功能
4. ✅ 信号历史展示
5. ✅ 策略启动/停止/删除

**API调用示例**:
```javascript
// 创建策略
const createStrategy = async () => {
  const response = await axios.post(`${API_BASE}/strategy/create`, {
    name: newStrategy.value.name,
    type: 'MA_CROSS',
    parameters: JSON.stringify({ shortPeriod: 5, longPeriod: 20 }),
    userId: 1
  })
}

// 生成信号
const generateSignal = async (strategyId, stockCode) => {
  const response = await axios.post(
    `${API_BASE}/strategy/${strategyId}/signal/${stockCode}`
  )
}
```

### 第八步：配置路由和导航

**路由配置**: `web-frontend/src/router/index.js`
```javascript
{
  path: 'strategy',
  name: 'Strategy',
  component: () => import('@/views/dashboard/Strategy.vue'),
  meta: {
    title: '策略管理',
    requiresAuth: true,
  },
}
```

**导航菜单**: `web-frontend/src/views/dashboard/Layout.vue`
```vue
<el-menu-item index="strategy">
  <el-icon><Lightning /></el-icon>
  <span>策略管理</span>
</el-menu-item>
```

---

## 代码详解

### 技术指标计算 - MA算法

**简单移动平均线（SMA）计算原理**:

```
MA(n) = (P1 + P2 + ... + Pn) / n

其中:
- n: 周期（如5日、20日）
- P1...Pn: 最近n天的收盘价
```

**举例：计算5日均线**
```
假设最近5天股价: [10.0, 10.2, 10.5, 10.3, 10.4]

MA5 = (10.0 + 10.2 + 10.5 + 10.3 + 10.4) / 5
    = 51.4 / 5
    = 10.28
```

### 金叉死叉判断逻辑

**金叉判断**:
```java
// 前一天：短期MA在长期MA下方
boolean yesterdayBelow = previousShortMA.compareTo(previousLongMA) < 0;

// 今天：短期MA在长期MA上方
boolean todayAbove = currentShortMA.compareTo(currentLongMA) > 0;

// 金叉 = 昨天在下方 && 今天在上方
if (yesterdayBelow && todayAbove) {
    return "BUY";  // 买入信号
}
```

**死叉判断**:
```java
// 前一天：短期MA在长期MA上方
boolean yesterdayAbove = previousShortMA.compareTo(previousLongMA) > 0;

// 今天：短期MA在长期MA下方
boolean todayBelow = currentShortMA.compareTo(currentLongMA) < 0;

// 死叉 = 昨天在上方 && 今天在下方
if (yesterdayAbove && todayBelow) {
    return "SELL";  // 卖出信号
}
```

### 信号强度计算

```java
// 买入信号强度 = 短期MA超过长期MA的百分比
BigDecimal buyStrength = (currentShortMA - currentLongMA) / currentLongMA * 100

// 卖出信号强度 = 长期MA超过短期MA的百分比
BigDecimal sellStrength = (currentLongMA - currentShortMA) / currentLongMA * 100
```

**强度解读**:
- **0-1%**: 弱信号 - 刚刚交叉，可能假突破
- **1-3%**: 中等信号 - 交叉明显，可信度较高
- **3%+**: 强信号 - 大幅交叉，趋势明确

---

## 使用指南

### 前置条件

确保以下服务正在运行：

```bash
# 1. Market Data Service (Python) - 5001端口
cd market-data-service
python3 api_server.py

# 2. Mock Trading Service (Python) - 5002端口
cd mock-trading-service
python3 app.py

# 3. Trading Service (Java) - 8083端口
cd trading-service
# 在IntelliJ IDEA中运行 TradingServiceApplication
# 或使用 Maven: mvn spring-boot:run

# 4. Web Frontend (Vue.js) - 3000端口
cd web-frontend
npm run dev
```

### 使用步骤

#### 步骤1：访问策略管理页面

打开浏览器访问：
```
http://localhost:3000/dashboard/strategy
```

或从首页导航：
1. 访问 http://localhost:3000
2. 登录系统（如果需要）
3. 点击左侧菜单 **"策略管理"** （闪电图标⚡）

#### 步骤2：创建第一个策略

1. **点击 "创建新策略" 按钮**

2. **填写策略信息**:
   - **策略名称**: 我的第一个双均线策略
   - **策略类型**: 双均线交叉（MA_CROSS）
   - **策略描述**: 5日和20日均线交叉策略
   - **短期均线周期**: 5（默认值）
   - **长期均线周期**: 20（默认值）

3. **点击 "创建" 按钮**

4. **成功提示**:
   ```
   ✅ 策略创建成功
   ```

#### 步骤3：生成交易信号

1. **在策略列表中找到刚创建的策略**

2. **点击 "生成信号" 按钮**

3. **在弹出的对话框中输入股票代码**:
   - 手动输入: `000001.SZ`（平安银行）
   - 或点击快捷按钮:
     - 平安银行 (000001.SZ)
     - 招商银行 (600036.SH)
     - 贵州茅台 (600519.SH)

4. **点击 "生成" 按钮**

5. **查看信号结果**:
   ```
   ✅ 信号生成成功: BUY
   ```
   或
   ```
   ✅ 信号生成成功: SELL
   ```
   或
   ```
   ✅ 信号生成成功: HOLD
   ```

#### 步骤4：查看信号详情

1. **点击 "查看信号" 按钮**

2. **信号详情表格显示**:
   - **股票代码**: 000001.SZ
   - **股票名称**: 平安银行
   - **信号类型**: BUY / SELL / HOLD
   - **价格**: ¥11.40
   - **信号强度**: 2.5%
   - **信号原因**: 金叉信号: MA5(11.45) 向上穿越 MA20(11.18)
   - **生成时间**: 2025-10-23 16:00:00

#### 步骤5：批量生成信号（可选）

如果想一次为多只股票生成信号，可以使用API：

```bash
curl -X POST http://localhost:8083/trading-service/api/strategy/1/signals \
  -H "Content-Type: application/json" \
  -d '{
    "stockCodes": ["000001.SZ", "600036.SH", "600519.SH"]
  }'
```

#### 步骤6：启动/停止策略

1. **启动策略**: 点击 "启动" 按钮
   - 策略状态变为 "运行中"
   - 可以配置定时任务自动生成信号（未来功能）

2. **停止策略**: 点击 "停止" 按钮
   - 策略状态变为 "已停止"

#### 步骤7：删除策略

1. **点击 "删除" 按钮**

2. **确认对话框**:
   ```
   ⚠️ 确定要删除策略 "我的第一个策略" 吗？
   此操作将同时删除所有相关信号记录。
   ```

3. **点击 "确定"** 完成删除

---

## API文档

### 基础信息

**Base URL**: `http://localhost:8083/trading-service/api/strategy`

**Content-Type**: `application/json`

### API端点列表

| 方法 | 端点 | 功能 |
|------|------|------|
| GET | `/health` | 健康检查 |
| GET | `/types` | 获取支持的策略类型 |
| POST | `/create` | 创建新策略 |
| POST | `/{strategyId}/signal/{stockCode}` | 生成单个股票信号 |
| POST | `/{strategyId}/signals` | 批量生成信号 |
| GET | `/{strategyId}/signals` | 获取策略的所有信号 |
| GET | `/user/{userId}` | 获取用户的所有策略 |
| POST | `/{strategyId}/start` | 启动策略 |
| POST | `/{strategyId}/stop` | 停止策略 |
| DELETE | `/{strategyId}` | 删除策略 |

### 详细API说明

#### 1. 健康检查

```http
GET /api/strategy/health
```

**响应**:
```json
{
  "status": "UP",
  "service": "strategy-service"
}
```

#### 2. 获取支持的策略类型

```http
GET /api/strategy/types
```

**响应**:
```json
{
  "success": true,
  "strategies": [
    {
      "type": "MA_CROSS",
      "name": "双均线交叉策略",
      "description": "通过短期均线和长期均线的交叉判断买卖时机",
      "parameters": {
        "shortPeriod": {
          "type": "number",
          "default": 5,
          "description": "短期均线周期"
        },
        "longPeriod": {
          "type": "number",
          "default": 20,
          "description": "长期均线周期"
        }
      }
    }
  ]
}
```

#### 3. 创建新策略

```http
POST /api/strategy/create
Content-Type: application/json
```

**请求体**:
```json
{
  "name": "我的第一个策略",
  "type": "MA_CROSS",
  "description": "5日和20日均线交叉策略",
  "parameters": "{\"shortPeriod\": 5, \"longPeriod\": 20}",
  "userId": 1
}
```

**响应**:
```json
{
  "success": true,
  "message": "策略创建成功",
  "strategy": {
    "id": 1,
    "name": "我的第一个策略",
    "type": "MA_CROSS",
    "parameters": "{\"shortPeriod\": 5, \"longPeriod\": 20}",
    "description": "5日和20日均线交叉策略",
    "status": "STOPPED",
    "userId": 1,
    "createdAt": "2025-10-23T16:00:00",
    "updatedAt": "2025-10-23T16:00:00"
  }
}
```

#### 4. 生成交易信号

```http
POST /api/strategy/{strategyId}/signal/{stockCode}
```

**示例**:
```bash
POST /api/strategy/1/signal/000001.SZ
```

**响应**:
```json
{
  "success": true,
  "message": "信号生成成功",
  "signal": {
    "id": 1,
    "strategyId": 1,
    "stockCode": "000001.SZ",
    "stockName": "平安银行",
    "signalType": "BUY",
    "price": 11.40,
    "signalStrength": 2.5,
    "reason": "金叉信号: MA5(11.45) 向上穿越 MA20(11.18)",
    "signalTime": "2025-10-23T16:00:00",
    "executed": false
  }
}
```

**信号类型说明**:
- `BUY`: 买入信号（金叉）
- `SELL`: 卖出信号（死叉）
- `HOLD`: 持有（无交叉）

#### 5. 批量生成信号

```http
POST /api/strategy/{strategyId}/signals
Content-Type: application/json
```

**请求体**:
```json
{
  "stockCodes": ["000001.SZ", "600036.SH", "600519.SH"]
}
```

**响应**:
```json
{
  "success": true,
  "message": "批量信号生成完成",
  "totalCount": 3,
  "signals": [
    {
      "stockCode": "000001.SZ",
      "signalType": "BUY",
      "price": 11.40,
      "reason": "金叉信号: MA5向上穿越MA20"
    },
    {
      "stockCode": "600036.SH",
      "signalType": "HOLD",
      "price": 45.30,
      "reason": "无交叉信号"
    },
    {
      "stockCode": "600519.SH",
      "signalType": "SELL",
      "price": 1850.00,
      "reason": "死叉信号: MA5向下穿越MA20"
    }
  ]
}
```

#### 6. 获取策略的所有信号

```http
GET /api/strategy/{strategyId}/signals
```

**响应**:
```json
{
  "success": true,
  "signals": [
    {
      "id": 3,
      "strategyId": 1,
      "stockCode": "600519.SH",
      "signalType": "SELL",
      "signalTime": "2025-10-23T16:05:00"
    },
    {
      "id": 2,
      "strategyId": 1,
      "stockCode": "600036.SH",
      "signalType": "HOLD",
      "signalTime": "2025-10-23T16:03:00"
    },
    {
      "id": 1,
      "strategyId": 1,
      "stockCode": "000001.SZ",
      "signalType": "BUY",
      "signalTime": "2025-10-23T16:00:00"
    }
  ]
}
```

#### 7. 获取用户的所有策略

```http
GET /api/strategy/user/{userId}
```

**响应**:
```json
{
  "success": true,
  "strategies": [
    {
      "id": 1,
      "name": "我的第一个策略",
      "type": "MA_CROSS",
      "status": "STOPPED",
      "createdAt": "2025-10-23T16:00:00"
    }
  ]
}
```

---

## 故障排查

### 问题1: 前端显示 "Network Error"

**症状**: 浏览器控制台显示无法连接到后端

**解决方案**:

1. **检查trading-service是否运行**:
   ```bash
   curl http://localhost:8083/trading-service/actuator/health
   ```

   应该返回: `{"status":"UP"}`

2. **检查端口是否被占用**:
   ```bash
   lsof -i :8083
   ```

3. **在IntelliJ IDEA中重启服务**:
   - 停止服务（红色方块）
   - 重新启动（绿色三角）

### 问题2: API返回 404 Not Found

**症状**:
```json
{
  "status": 404,
  "error": "Not Found",
  "path": "/trading-service/api/strategy/health"
}
```

**原因**: Controller的RequestMapping路径配置错误

**解决方案**:

检查 `StrategyController.java` 的注解：

✅ **正确配置**:
```java
@RestController
@RequestMapping("/api/strategy")  // 不包含 /trading-service
@CrossOrigin(origins = "*")
public class StrategyController {
```

❌ **错误配置**:
```java
@RequestMapping("/trading-service/api/strategy")  // 错误！会重复
```

**修复步骤**:
1. 修改 StrategyController.java
2. 在IDE中 **Build → Rebuild Project**
3. 重启 trading-service

### 问题3: 生成信号时返回 "数据不足"

**症状**:
```json
{
  "signalType": "HOLD",
  "reason": "数据不足，无法计算均线"
}
```

**原因**: 历史价格数据少于长期均线周期（默认20天）

**解决方案**:

1. **降低均线周期**:
   - 短期: MA3
   - 长期: MA10

2. **提供更多历史数据**:
   - 修改 `StrategyService.java`:
   ```java
   List<BigDecimal> prices = getHistoricalPrices(stockCode, 60); // 获取60天数据
   ```

### 问题4: 策略列表为空

**症状**: 前端显示"暂无数据"

**原因**:
1. 还没有创建策略
2. 数据库连接问题

**解决方案**:

1. **检查数据库**:
   ```bash
   # 访问H2控制台
   http://localhost:8083/trading-service/h2-console

   # 连接信息（在application.yml中）
   JDBC URL: jdbc:h2:mem:trading_db
   Username: sa
   Password: (留空)
   ```

2. **查询策略表**:
   ```sql
   SELECT * FROM strategies;
   ```

3. **手动创建测试策略**:
   ```sql
   INSERT INTO strategies (name, type, parameters, status, user_id, created_at, updated_at)
   VALUES ('测试策略', 'MA_CROSS', '{"shortPeriod":5,"longPeriod":20}', 'STOPPED', 1, NOW(), NOW());
   ```

### 问题5: 信号都是 HOLD，没有 BUY/SELL

**症状**: 所有生成的信号都是 HOLD 类型

**原因**: 当前没有发生均线交叉

**解决方案**:

这是正常现象！双均线交叉不是每天都会发生。只有在真正出现金叉或死叉时才会生成BUY/SELL信号。

**如何测试**:
1. **使用模拟数据测试**:
   - 修改 `generateMockPrices` 方法
   - 创造金叉场景：价格持续上涨
   - 创造死叉场景：价格持续下跌

2. **等待真实交叉**:
   - 每天定时生成信号
   - 当市场出现趋势变化时会产生信号

### 问题6: CORS跨域错误

**症状**: 浏览器控制台显示 CORS policy 错误

**解决方案**:

确保Controller有 `@CrossOrigin` 注解：
```java
@RestController
@RequestMapping("/api/strategy")
@CrossOrigin(origins = "*")  // ✅ 必须有这个
public class StrategyController {
```

---

## 下一步扩展

完成双均线交叉策略后，可以继续开发：

### 1. 更多策略类型

- **MACD交叉策略**: DIF线和DEA线的金叉死叉
- **RSI超买超卖**: RSI > 70 卖出，RSI < 30 买入
- **布林带突破**: 价格突破上轨/下轨
- **KDJ指标**: K线和D线交叉

### 2. 自动交易引擎

```java
@Scheduled(fixedRate = 300000)  // 每5分钟执行一次
public void autoTrading() {
    // 1. 获取所有运行中的策略
    List<Strategy> runningStrategies = strategyRepository.findByStatus("RUNNING");

    // 2. 为每个策略生成信号
    for (Strategy strategy : runningStrategies) {
        StrategySignal signal = generateSignal(strategy.getId(), stockCode);

        // 3. 如果是BUY/SELL信号，自动执行交易
        if (signal.getSignalType().equals("BUY")) {
            executeBuyOrder(signal);
        } else if (signal.getSignalType().equals("SELL")) {
            executeSellOrder(signal);
        }
    }
}
```

### 3. 回测系统

```java
public BacktestResult runBacktest(Strategy strategy, String stockCode,
                                  LocalDate startDate, LocalDate endDate) {
    // 1. 获取历史数据
    List<StockPrice> historicalData = getHistoricalData(stockCode, startDate, endDate);

    // 2. 模拟交易
    double initialCapital = 1000000.0;  // 100万初始资金
    Portfolio portfolio = new Portfolio(initialCapital);

    for (StockPrice data : historicalData) {
        StrategySignal signal = strategy.generateSignal(data);

        if (signal.getSignalType().equals("BUY")) {
            portfolio.buy(data.getPrice(), calculateQuantity());
        } else if (signal.getSignalType().equals("SELL")) {
            portfolio.sell(data.getPrice());
        }
    }

    // 3. 计算回测指标
    return new BacktestResult(
        portfolio.getTotalReturn(),      // 总收益率
        portfolio.getMaxDrawdown(),      // 最大回撤
        portfolio.getSharpeRatio(),      // 夏普比率
        portfolio.getTradeCount()        // 交易次数
    );
}
```

### 4. 实时数据集成

替换模拟数据为真实市场数据：

```java
private List<BigDecimal> getHistoricalPrices(String stockCode, int days) {
    // 调用 market-data-service
    String url = "http://localhost:5001/api/stocks/" + stockCode + "/history?days=" + days;

    RestTemplate restTemplate = new RestTemplate();
    StockHistoryResponse response = restTemplate.getForObject(url, StockHistoryResponse.class);

    return response.getData().stream()
        .map(StockPrice::getClose)
        .collect(Collectors.toList());
}
```

---

## 总结

通过本文档，你已经学会了：

✅ **理论知识**
- 双均线交叉策略的原理
- 金叉和死叉的判断方法
- 技术指标的计算方式

✅ **技术实现**
- 技术指标计算库的开发
- 策略接口的设计
- 双均线策略的实现
- REST API的创建
- 前端管理页面的开发

✅ **实战应用**
- 如何创建策略
- 如何生成交易信号
- 如何查看信号历史
- 如何管理策略状态

✅ **问题排查**
- 常见错误的解决方案
- 配置注意事项
- 调试技巧

现在你可以：
1. 创建自己的交易策略
2. 实时生成买卖信号
3. 基于策略进行模拟交易
4. 扩展开发更多策略类型

**祝你交易成功！📈**

---

## 附录

### 完整文件清单

```
trading-service/src/main/java/com/quant/trading/
├── indicator/
│   └── IndicatorCalculator.java              # 技术指标计算
├── strategy/
│   ├── TradingStrategy.java                  # 策略接口
│   └── MovingAverageCrossStrategy.java       # 双均线策略
├── entity/
│   ├── Strategy.java                         # 策略实体
│   └── StrategySignal.java                   # 信号实体
├── repository/
│   ├── StrategyRepository.java               # 策略数据访问
│   └── StrategySignalRepository.java         # 信号数据访问
├── service/
│   └── StrategyService.java                  # 策略服务
└── controller/
    └── StrategyController.java               # REST API

web-frontend/src/
├── views/dashboard/
│   ├── Strategy.vue                          # 策略管理页面
│   └── Layout.vue                            # 导航布局
└── router/
    └── index.js                              # 路由配置
```

### 参考资料

- [技术分析指标详解](https://www.investopedia.com/terms/t/technicalindicator.asp)
- [移动平均线策略](https://www.investopedia.com/articles/active-trading/052014/how-use-moving-average-buy-stocks.asp)
- [Spring Boot官方文档](https://spring.io/projects/spring-boot)
- [Vue.js官方文档](https://vuejs.org/)
- [Element Plus组件库](https://element-plus.org/)

---

**文档版本**: 1.0
**最后更新**: 2025-10-23
**作者**: WeQuant团队
