# Kafka 教程 - 量化交易平台实战

## 什么是 Kafka？

**Kafka** 是一个分布式消息队列（消息中间件），可以理解为**微服务之间的快递系统**。

### 形象比喻
- **没有Kafka**：Python数据服务获取股票数据后，要主动通知所有需要的服务（数据分析、策略、前端），其中一个服务挂了就会丢数据
- **有Kafka**：Python把数据扔到Kafka，各个服务自己来取，谁挂了重启后继续取，不会丢失

---

## 在你的量化交易平台中的作用

```
Python数据服务                     数据分析服务
    ↓                                  ↑
获取股票行情 → 发送到Kafka Topic → 订阅消费
    ↓              ↓                   ↓
          策略服务订阅            前端WebSocket订阅
```

### 核心场景

1. **实时行情推送**
   ```
   Python → Kafka(stock-realtime) → 数据分析服务 → 计算指标
                                  → 前端WebSocket → 用户看到K线
   ```

2. **交易信号传递**
   ```
   策略服务 → Kafka(trade-signal) → 交易服务 → 执行模拟交易
   ```

3. **解耦服务**
   - 服务之间不直接调用，通过Kafka传递消息
   - 新增服务只需订阅相应Topic，不影响现有服务

---

## Kafka 核心概念

### 1. Topic（主题）
**类比**：快递分拣中心的不同区域

```
Kafka集群
├── stock-realtime    (实时行情Topic)
│   ├── 平安银行 数据
│   ├── 贵州茅台 数据
│   └── ...
├── trade-signal      (交易信号Topic)
│   ├── 买入信号
│   └── 卖出信号
└── user-events       (用户事件Topic)
    ├── 登录事件
    └── 注册事件
```

### 2. Producer（生产者）
发送消息的服务
```
Python数据服务 → Producer → Kafka
```

### 3. Consumer（消费者）
接收消息的服务
```
Kafka → Consumer → 数据分析服务
```

### 4. Partition（分区）
Topic的并行处理单元
```
stock-realtime Topic
├── Partition 0: 沪市股票
├── Partition 1: 深市股票
└── Partition 2: 港股
```

### 5. Consumer Group（消费者组）
多个消费者协同工作
```
数据分析服务 (group: analysis)
├── Consumer-1 → 读取 Partition 0
└── Consumer-2 → 读取 Partition 1
```

---

## 实战：使用你的 Docker Kafka

### 1. 启动 Kafka（会自动启动 Zookeeper）

```bash
cd quant-trading-platform
docker compose up -d zookeeper kafka
```

### 2. 验证 Kafka 是否启动

```bash
# 查看容器状态
docker compose ps kafka

# 查看日志
docker compose logs kafka | grep "started (kafka.server.KafkaServer)"
```

看到 "started" 表示成功！

### 3. 进入 Kafka 容器

```bash
docker exec -it quant-kafka bash
```

---

## Kafka 命令行工具实战

### 1. Topic 管理

#### 创建 Topic（模拟实时行情）
```bash
kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --topic stock-realtime \
  --partitions 3 \
  --replication-factor 1
```

**参数说明**：
- `--topic`：Topic名称
- `--partitions 3`：3个分区（可并行处理）
- `--replication-factor 1`：1个副本（单机只能是1）

#### 查看所有 Topic
```bash
kafka-topics --list --bootstrap-server localhost:9092
```

#### 查看 Topic 详细信息
```bash
kafka-topics --describe \
  --bootstrap-server localhost:9092 \
  --topic stock-realtime
```

输出示例：
```
Topic: stock-realtime    PartitionCount: 3    ReplicationFactor: 1
  Partition: 0    Leader: 1    Replicas: 1    Isr: 1
  Partition: 1    Leader: 1    Replicas: 1    Isr: 1
  Partition: 2    Leader: 1    Replicas: 1    Isr: 1
```

#### 删除 Topic
```bash
kafka-topics --delete \
  --bootstrap-server localhost:9092 \
  --topic stock-realtime
```

---

### 2. 生产者（发送消息）

#### 启动控制台生产者
```bash
kafka-console-producer \
  --bootstrap-server localhost:9092 \
  --topic stock-realtime
```

**开始输入消息**（模拟实时行情）：
```json
{"symbol":"000001","price":10.52,"time":"2025-10-08 14:30:00"}
{"symbol":"600519","price":1680.00,"time":"2025-10-08 14:30:01"}
{"symbol":"600000","price":8.95,"time":"2025-10-08 14:30:02"}
```

每输入一行按回车，消息就发送到Kafka！

按 `Ctrl+C` 退出生产者。

---

### 3. 消费者（接收消息）

#### 从最新消息开始消费
```bash
kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic stock-realtime
```

**打开另一个终端**，重新启动生产者发送消息，你会在消费者终端看到实时输出！

#### 从头开始消费（查看历史消息）
```bash
kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic stock-realtime \
  --from-beginning
```

你会看到之前发送的所有消息！

#### 使用消费者组
```bash
kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic stock-realtime \
  --group analysis-service \
  --from-beginning
```

**消费者组的特性**：
- 同一组内的消费者不会重复消费同一条消息
- 适合多实例部署（负载均衡）

---

## 实战练习：模拟量化交易数据流

### 练习1：实时行情推送

**终端1**（创建Topic）：
```bash
docker exec -it quant-kafka bash

# 创建实时行情Topic
kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --topic stock-realtime \
  --partitions 2 \
  --replication-factor 1
```

**终端2**（模拟Python数据服务发送行情）：
```bash
docker exec -it quant-kafka bash

kafka-console-producer \
  --bootstrap-server localhost:9092 \
  --topic stock-realtime

# 输入以下消息
{"symbol":"000001","name":"平安银行","price":10.52,"volume":15000,"time":"14:30:00"}
{"symbol":"600519","name":"贵州茅台","price":1680.00,"volume":8000,"time":"14:30:01"}
```

**终端3**（模拟数据分析服务消费）：
```bash
docker exec -it quant-kafka bash

kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic stock-realtime \
  --group analysis-service
```

你会看到实时消息！

---

### 练习2：交易信号传递

**创建交易信号Topic**：
```bash
docker exec -it quant-kafka bash

kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --topic trade-signal \
  --partitions 1 \
  --replication-factor 1
```

**终端1**（模拟策略服务发出买入信号）：
```bash
kafka-console-producer \
  --bootstrap-server localhost:9092 \
  --topic trade-signal

# 输入交易信号
{"signal":"BUY","symbol":"000001","price":10.50,"volume":1000,"strategy":"MA_CROSS"}
{"signal":"SELL","symbol":"600519","price":1690.00,"volume":100,"strategy":"MACD"}
```

**终端2**（模拟交易服务接收信号）：
```bash
kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic trade-signal \
  --group trading-service \
  --from-beginning
```

---

### 练习3：查看消费者组状态

```bash
# 列出所有消费者组
kafka-consumer-groups --list --bootstrap-server localhost:9092

# 查看消费者组详情
kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group analysis-service \
  --describe
```

输出示例：
```
GROUP            TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG
analysis-service stock-realtime  0          5               5               0
analysis-service stock-realtime  1          3               3               0
```

**关键指标**：
- `CURRENT-OFFSET`：已消费到的位置
- `LOG-END-OFFSET`：最新消息位置
- `LAG`：未消费消息数（0表示已追上）

---

## Kafka 在项目中的实际应用

### 应用1：Python 生产者（发送股票数据）

```python
# data-service/kafka_producer.py
from kafka import KafkaProducer
import json

producer = KafkaProducer(
    bootstrap_servers=['localhost:9093'],
    value_serializer=lambda v: json.dumps(v).encode('utf-8')
)

# 发送实时行情
data = {
    "symbol": "000001",
    "price": 10.52,
    "volume": 15000,
    "time": "2025-10-08 14:30:00"
}

producer.send('stock-realtime', value=data)
producer.flush()
```

### 应用2：Java 消费者（接收股票数据）

```java
// analysis-service
@Service
public class StockDataConsumer {

    @KafkaListener(topics = "stock-realtime", groupId = "analysis-service")
    public void consume(String message) {
        // 解析JSON
        JSONObject data = JSON.parseObject(message);
        String symbol = data.getString("symbol");
        Double price = data.getDouble("price");

        // 计算技术指标
        calculateMA(symbol, price);
    }
}
```

### 应用3：多个服务消费同一消息

```
Python → Kafka(stock-realtime)
              ↓
         ┌────┴────┬─────────┐
         ↓         ↓         ↓
    数据分析服务  策略服务  WebSocket服务
   (存储K线)   (生成信号) (推送前端)
```

每个服务用不同的`group-id`，都能收到完整数据！

---

## Kafka UI 可视化管理

你的项目已包含 Kafka UI！

### 1. 启动 Kafka UI
```bash
docker compose up -d kafka-ui
```

### 2. 访问 Web 界面
浏览器打开：http://localhost:8080

### 3. 功能
- ✅ 查看所有 Topic
- ✅ 查看消息内容
- ✅ 监控消费者组状态
- ✅ 创建/删除 Topic
- ✅ 查看 Broker 状态

---

## Kafka 常用命令速查表

| 功能 | 命令 |
|------|------|
| **Topic管理** | |
| 创建Topic | `kafka-topics --create --bootstrap-server localhost:9092 --topic <name> --partitions 3 --replication-factor 1` |
| 列出所有Topic | `kafka-topics --list --bootstrap-server localhost:9092` |
| 查看Topic详情 | `kafka-topics --describe --bootstrap-server localhost:9092 --topic <name>` |
| 删除Topic | `kafka-topics --delete --bootstrap-server localhost:9092 --topic <name>` |
| **生产者** | |
| 启动控制台生产者 | `kafka-console-producer --bootstrap-server localhost:9092 --topic <name>` |
| **消费者** | |
| 从最新消费 | `kafka-console-consumer --bootstrap-server localhost:9092 --topic <name>` |
| 从头消费 | `kafka-console-consumer --bootstrap-server localhost:9092 --topic <name> --from-beginning` |
| 使用消费者组 | `kafka-console-consumer --bootstrap-server localhost:9092 --topic <name> --group <group-id>` |
| **消费者组** | |
| 列出所有消费者组 | `kafka-consumer-groups --list --bootstrap-server localhost:9092` |
| 查看消费者组详情 | `kafka-consumer-groups --bootstrap-server localhost:9092 --group <group-id> --describe` |
| 重置消费位移 | `kafka-consumer-groups --bootstrap-server localhost:9092 --group <group-id> --topic <name> --reset-offsets --to-earliest --execute` |

---

## Docker 配置说明

### docker-compose.yml 解析

```yaml
kafka:
  image: confluentinc/cp-kafka:7.5.0
  ports:
    - "9092:9092"  # 容器内部访问
    - "9093:9093"  # 宿主机访问（你的Python/Java代码用这个）
  environment:
    - KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181
    - KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://kafka:9092,PLAINTEXT_HOST://localhost:9093
```

### 连接方式

| 场景 | 地址 |
|------|------|
| 容器内访问（Java服务在Docker内） | `kafka:9092` |
| 宿主机访问（本地Python脚本） | `localhost:9093` |

---

## 常见问题

### Q1: 如何查看某个Topic有多少消息？

```bash
kafka-run-class kafka.tools.GetOffsetShell \
  --broker-list localhost:9092 \
  --topic stock-realtime
```

### Q2: 消息会永久保存吗？

默认保留7天，可修改：
```bash
kafka-configs --alter \
  --bootstrap-server localhost:9092 \
  --entity-type topics \
  --entity-name stock-realtime \
  --add-config retention.ms=86400000  # 1天
```

### Q3: 如何清空Topic数据？

```bash
# 方法1：删除重建
kafka-topics --delete --bootstrap-server localhost:9092 --topic stock-realtime
kafka-topics --create --bootstrap-server localhost:9092 --topic stock-realtime --partitions 3 --replication-factor 1

# 方法2：设置保留时间为1秒，等待清空后恢复
kafka-configs --alter --bootstrap-server localhost:9092 --entity-type topics --entity-name stock-realtime --add-config retention.ms=1000
# 等待10秒
kafka-configs --alter --bootstrap-server localhost:9092 --entity-type topics --entity-name stock-realtime --delete-config retention.ms
```

### Q4: 如何保证消息不丢失？

**生产者端**：
```python
producer = KafkaProducer(
    acks='all',  # 等待所有副本确认
    retries=3    # 失败重试3次
)
```

**消费者端**：
```python
consumer = KafkaConsumer(
    enable_auto_commit=False  # 手动提交offset
)
# 处理完业务逻辑后
consumer.commit()
```

---

## 性能优化建议

1. **合理设置分区数**
   - 一般设为消费者数量的2-3倍
   - 你的项目：`stock-realtime` 可设3个分区

2. **批量发送**
   ```python
   # 不推荐：每条消息单独发送
   for data in stock_list:
       producer.send('stock-realtime', data)

   # 推荐：批量发送
   for data in stock_list:
       producer.send('stock-realtime', data)
   producer.flush()  # 一次性刷新
   ```

3. **压缩消息**
   ```python
   producer = KafkaProducer(
       compression_type='gzip'  # 节省网络带宽
   )
   ```

---

## 下一步学习

1. ✅ 完成实战练习
2. 📝 开发Python数据服务（集成Kafka Producer）
3. ☕ 开发Java数据分析服务（集成Kafka Consumer）
4. 🔄 实现完整的数据流：Python → Kafka → Java → 前端

---

## 参考资源

- [Kafka 官方文档](https://kafka.apache.org/documentation/)
- [Kafka UI GitHub](https://github.com/provectus/kafka-ui)
- 项目文档：`量化交易平台架构设计.md`

---

**提示**：所有练习完成后，记得更新 `DEVLOG.md`！
