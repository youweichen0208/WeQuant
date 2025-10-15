# Redis 教程 - 量化交易平台实战

## 什么是 Redis？

**Redis** 是一个内存数据库（NoSQL），可以理解为**超快的键值存储系统**。

### 形象比喻
- **MySQL**：图书馆（数据量大，查询复杂，但稍慢）
- **Redis**：书桌（常用资料放手边，秒级访问）

### 核心特点
- ⚡ **极快**：所有数据在内存中，读写速度达到10万次/秒
- 🔄 **多数据类型**：字符串、列表、集合、哈希、有序集合
- ⏰ **过期机制**：自动删除过期数据
- 💾 **持久化**：可选择性保存到磁盘

---

## 在你的量化交易平台中的作用

```
Redis缓存
├── 实时行情缓存          (stock:600519:price → "1680.00")
├── 用户Session           (session:abc123 → {user_id: 1})
├── 热门股票排行榜        (热搜榜单)
├── 交易限流              (防止频繁下单)
├── 分布式锁              (防止重复操作)
└── 技术指标缓存          (计算结果缓存)
```

### 核心场景

1. **实时行情缓存**：避免每次都查MySQL
2. **用户登录状态**：JWT Token缓存
3. **限流防刷**：防止恶意高频交易
4. **排行榜**：实时更新热门股票
5. **分布式锁**：多个服务协调操作

---

## Redis 数据类型

### 1. String（字符串）
最简单的键值对
```
key → value
stock:600519:price → "1680.00"
```

### 2. Hash（哈希）
存储对象
```
user:1 → {
    username: "test_user",
    email: "test@example.com",
    balance: "100000"
}
```

### 3. List（列表）
有序列表
```
stock:600519:history → ["1680", "1675", "1690"]
```

### 4. Set（集合）
无序不重复集合
```
user:1:watchlist → {"600519", "000001", "600000"}
```

### 5. Sorted Set（有序集合）
带分数的集合（适合排行榜）
```
hot:stocks → {
    "600519": 9999,  # 茅台，热度9999
    "000001": 8888,  # 平安，热度8888
}
```

---

## 实战：使用你的 Docker Redis

### 1. 启动 Redis

```bash
cd quant-trading-platform
docker compose up -d redis
```

### 2. 验证 Redis 是否启动

```bash
# 查看容器状态
docker compose ps redis

# 查看日志
docker compose logs redis | grep "Ready to accept connections"
```

### 3. 连接到 Redis

```bash
# 连接Redis（需要密码）
docker exec -it quant-redis redis-cli -a redis123456
```

成功后显示：
```
127.0.0.1:6379>
```

---

## Redis 基础命令实战

### 1. String 操作（最常用）

#### 设置/获取值
```bash
# 设置股票价格
SET stock:600519:price 1680.00

# 获取股票价格
GET stock:600519:price
# 返回：1680.00

# 设置并带过期时间（60秒）
SETEX stock:600519:price 60 1680.00

# 批量设置
MSET stock:600519:price 1680 stock:000001:price 10.52

# 批量获取
MGET stock:600519:price stock:000001:price
```

#### 数值操作
```bash
# 设置用户余额
SET user:1:balance 100000

# 增加余额
INCRBY user:1:balance 50000
# 返回：150000

# 减少余额
DECRBY user:1:balance 20000
# 返回：130000

# 自增1
INCR user:1:order_count
```

#### 检查键是否存在
```bash
EXISTS stock:600519:price
# 返回：1（存在）或 0（不存在）
```

---

### 2. Hash 操作（存储对象）

#### 设置/获取哈希字段
```bash
# 存储用户信息
HSET user:1 username test_user
HSET user:1 email test@example.com
HSET user:1 balance 100000

# 批量设置
HMSET user:1 username test_user email test@example.com balance 100000

# 获取单个字段
HGET user:1 username
# 返回：test_user

# 获取所有字段
HGETALL user:1
# 返回：
# username
# test_user
# email
# test@example.com
# balance
# 100000

# 获取多个字段
HMGET user:1 username balance

# 只增加数值字段
HINCRBY user:1 balance 50000

# 获取所有键
HKEYS user:1

# 获取所有值
HVALS user:1
```

#### 实战：缓存股票详情
```bash
HMSET stock:600519 symbol 600519 name 贵州茅台 price 1680 volume 50000 exchange SH

# 获取股票详情
HGETALL stock:600519
```

---

### 3. List 操作（消息队列、历史记录）

#### 左进右出（队列）
```bash
# 添加到队列头部
LPUSH order:queue ORD001 ORD002 ORD003

# 从队列尾部取出
RPOP order:queue
# 返回：ORD001

# 查看队列长度
LLEN order:queue

# 查看队列内容（不移除）
LRANGE order:queue 0 -1
```

#### 右进左出（栈）
```bash
# 添加到队列尾部
RPUSH stock:600519:history 1680 1675 1690

# 从队列头部取出
LPOP stock:600519:history
```

#### 实战：K线历史缓存
```bash
# 存储最近10条K线
RPUSH kline:600519 1680 1675 1690 1685 1700

# 获取最近5条
LRANGE kline:600519 -5 -1

# 限制列表长度（只保留最新100条）
LTRIM kline:600519 -100 -1
```

---

### 4. Set 操作（自选股、标签）

#### 添加/查看集合
```bash
# 添加自选股
SADD user:1:watchlist 600519 000001 600000

# 查看所有自选股
SMEMBERS user:1:watchlist

# 判断是否在自选中
SISMEMBER user:1:watchlist 600519
# 返回：1（在）或 0（不在）

# 移除自选股
SREM user:1:watchlist 600000

# 获取自选股数量
SCARD user:1:watchlist
```

#### 集合运算
```bash
# 用户1的自选股
SADD user:1:watchlist 600519 000001 600000

# 用户2的自选股
SADD user:2:watchlist 600519 600036 600000

# 交集（共同自选）
SINTER user:1:watchlist user:2:watchlist
# 返回：600519 600000

# 并集（所有自选）
SUNION user:1:watchlist user:2:watchlist

# 差集（用户1独有）
SDIFF user:1:watchlist user:2:watchlist
```

---

### 5. Sorted Set 操作（排行榜）

#### 添加/查看排行榜
```bash
# 添加股票热度
ZADD hot:stocks 9999 600519 8888 000001 7777 600000

# 查看热度最高的前3名（降序）
ZREVRANGE hot:stocks 0 2 WITHSCORES
# 返回：
# 600519
# 9999
# 000001
# 8888
# 600000
# 7777

# 增加热度
ZINCRBY hot:stocks 100 600519

# 获取某股票热度
ZSCORE hot:stocks 600519

# 获取排名（从0开始）
ZREVRANK hot:stocks 600519
```

#### 实战：涨幅榜
```bash
# 添加股票涨幅（百分比*100）
ZADD gainers:today 650 600519 520 000001 -320 600036

# 查看涨幅前3（涨幅最高）
ZREVRANGE gainers:today 0 2 WITHSCORES

# 查看跌幅前3（涨幅最低）
ZRANGE gainers:today 0 2 WITHSCORES

# 查看涨幅在3%-10%之间的股票
ZRANGEBYSCORE gainers:today 300 1000 WITHSCORES
```

---

## 实战场景应用

### 场景1：实时行情缓存

```bash
# Python数据服务获取行情后存入Redis
HMSET stock:600519:realtime \
    symbol 600519 \
    name 贵州茅台 \
    price 1680.00 \
    change 10.00 \
    change_pct 0.60 \
    volume 50000 \
    timestamp 1728374400

# 设置1分钟过期
EXPIRE stock:600519:realtime 60

# 前端/后端从Redis读取
HGETALL stock:600519:realtime
```

### 场景2：用户登录Session

```bash
# 用户登录后生成Token
SET session:abc123def456 1 EX 3600  # 用户ID=1，1小时过期

# 验证Token
GET session:abc123def456

# 延长过期时间
EXPIRE session:abc123def456 3600

# 退出登录
DEL session:abc123def456
```

### 场景3：限流（防止频繁下单）

```bash
# 记录用户下单次数（1分钟内最多10次）
INCR rate:order:user:1
EXPIRE rate:order:user:1 60

# 检查是否超限
GET rate:order:user:1
# 如果返回 > 10，拒绝下单
```

### 场景4：分布式锁

```bash
# 获取锁（防止重复处理订单）
SET lock:order:ORD001 processing NX EX 10
# NX: 不存在才设置（原子操作）
# EX 10: 10秒后自动释放

# 如果返回OK，表示获取锁成功
# 如果返回nil，表示锁被占用

# 处理完成后释放锁
DEL lock:order:ORD001
```

### 场景5：最近访问记录

```bash
# 用户访问股票详情页
LPUSH user:1:recent_view 600519
LTRIM user:1:recent_view 0 9  # 只保留最近10条

# 获取最近浏览记录
LRANGE user:1:recent_view 0 9
```

---

## 高级功能

### 1. 发布/订阅（实时推送）

**终端1**（订阅者）：
```bash
docker exec -it quant-redis redis-cli -a redis123456

# 订阅实时行情频道
SUBSCRIBE channel:stock:realtime
```

**终端2**（发布者）：
```bash
docker exec -it quant-redis redis-cli -a redis123456

# 发布消息
PUBLISH channel:stock:realtime '{"symbol":"600519","price":1680}'
```

**终端1** 会立即收到消息！

### 2. 事务（批量操作）

```bash
# 开始事务
MULTI

# 添加命令到队列
SET user:1:balance 100000
DECRBY user:1:balance 50000
INCRBY user:1:frozen 50000

# 执行所有命令
EXEC

# 如果取消事务
# DISCARD
```

### 3. 管道（批量执行提升性能）

```bash
# 使用管道一次性执行多条命令
redis-cli -a redis123456 --pipe <<EOF
SET stock:600519 1680
SET stock:000001 10.52
SET stock:600000 8.95
EOF
```

---

## 数据持久化

### 1. RDB（快照）
定期保存内存快照到磁盘

```bash
# 立即保存快照
SAVE

# 后台保存快照
BGSAVE

# 查看最后保存时间
LASTSAVE
```

### 2. AOF（追加日志）
记录每个写操作

```bash
# 查看AOF配置
CONFIG GET appendonly
```

---

## 性能监控

### 1. 查看Redis信息

```bash
# 查看所有信息
INFO

# 查看内存使用
INFO memory

# 查看统计信息
INFO stats

# 查看客户端连接
INFO clients
```

### 2. 监控实时命令

```bash
# 实时显示所有执行的命令
MONITOR
```

### 3. 查看慢查询

```bash
# 查看慢查询日志
SLOWLOG GET 10
```

---

## 常用命令速查表

| 功能 | 命令 | 示例 |
|------|------|------|
| **通用** | | |
| 设置键值 | `SET key value` | `SET stock:600519 1680` |
| 获取值 | `GET key` | `GET stock:600519` |
| 删除键 | `DEL key` | `DEL stock:600519` |
| 检查存在 | `EXISTS key` | `EXISTS stock:600519` |
| 设置过期 | `EXPIRE key seconds` | `EXPIRE stock:600519 60` |
| 查看过期时间 | `TTL key` | `TTL stock:600519` |
| 查看所有键 | `KEYS pattern` | `KEYS stock:*` |
| **String** | | |
| 自增 | `INCR key` | `INCR user:1:count` |
| 增加N | `INCRBY key n` | `INCRBY user:1:balance 1000` |
| 自减 | `DECR key` | `DECR user:1:count` |
| **Hash** | | |
| 设置字段 | `HSET key field value` | `HSET user:1 name test` |
| 获取字段 | `HGET key field` | `HGET user:1 name` |
| 获取所有 | `HGETALL key` | `HGETALL user:1` |
| **List** | | |
| 左插入 | `LPUSH key value` | `LPUSH queue:order ORD001` |
| 右插入 | `RPUSH key value` | `RPUSH queue:order ORD001` |
| 左弹出 | `LPOP key` | `LPOP queue:order` |
| 右弹出 | `RPOP key` | `RPOP queue:order` |
| 查看范围 | `LRANGE key start stop` | `LRANGE queue:order 0 -1` |
| **Set** | | |
| 添加成员 | `SADD key member` | `SADD watchlist 600519` |
| 查看所有 | `SMEMBERS key` | `SMEMBERS watchlist` |
| 判断存在 | `SISMEMBER key member` | `SISMEMBER watchlist 600519` |
| **Sorted Set** | | |
| 添加成员 | `ZADD key score member` | `ZADD hot:stocks 9999 600519` |
| 降序查看 | `ZREVRANGE key start stop` | `ZREVRANGE hot:stocks 0 9` |
| 获取分数 | `ZSCORE key member` | `ZSCORE hot:stocks 600519` |

---

## Docker 配置说明

### docker-compose.yml 解析

```yaml
redis:
  image: redis:7-alpine
  ports:
    - "6379:6379"
  command: redis-server --requirepass redis123456  # 设置密码
  volumes:
    - redis-data:/data  # 数据持久化
```

### 连接方式

| 场景 | 地址 | 密码 |
|------|------|------|
| 容器内访问 | `redis:6379` | `redis123456` |
| 宿主机访问 | `localhost:6379` | `redis123456` |

---

## 常见问题

### Q1: 如何查看Redis内存使用？

```bash
docker exec -it quant-redis redis-cli -a redis123456 INFO memory | grep used_memory_human
```

### Q2: 如何清空所有数据？

```bash
# 清空当前数据库
docker exec -it quant-redis redis-cli -a redis123456 FLUSHDB

# 清空所有数据库
docker exec -it quant-redis redis-cli -a redis123456 FLUSHALL
```

### Q3: 如何查看所有键？

```bash
# 查看所有键（⚠️ 生产环境慎用）
docker exec -it quant-redis redis-cli -a redis123456 KEYS '*'

# 更安全的方式：扫描
docker exec -it quant-redis redis-cli -a redis123456 SCAN 0 MATCH stock:* COUNT 10
```

### Q4: Redis数据会丢失吗？

通过Docker volume持久化，容器重启数据不会丢失。但如果执行`docker compose down -v`会删除数据。

---

## 在项目中的实际应用

### 应用1：Java Spring Boot 连接

```java
// application.yml
spring:
  redis:
    host: localhost
    port: 6379
    password: redis123456
    database: 0
```

```java
@Autowired
private RedisTemplate<String, String> redisTemplate;

// 缓存股票价格
redisTemplate.opsForValue().set("stock:600519:price", "1680.00", 60, TimeUnit.SECONDS);

// 获取价格
String price = redisTemplate.opsForValue().get("stock:600519:price");
```

### 应用2：Python 连接

```python
import redis

r = redis.Redis(
    host='localhost',
    port=6379,
    password='redis123456',
    decode_responses=True
)

# 缓存股票价格
r.setex('stock:600519:price', 60, '1680.00')

# 获取价格
price = r.get('stock:600519:price')
```

### 应用3：实时行情推送（Spring + Redis）

```java
@Service
public class StockRealtimeService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 缓存实时行情
    public void cacheRealtime(String symbol, StockRealtime data) {
        String key = "stock:" + symbol + ":realtime";
        redisTemplate.opsForHash().putAll(key, BeanUtil.beanToMap(data));
        redisTemplate.expire(key, 60, TimeUnit.SECONDS);
    }

    // 获取实时行情
    public StockRealtime getRealtime(String symbol) {
        String key = "stock:" + symbol + ":realtime";
        Map<Object, Object> map = redisTemplate.opsForHash().entries(key);
        return BeanUtil.mapToBean(map, StockRealtime.class, false);
    }
}
```

---

## 性能优化建议

1. **合理设置过期时间**
   - 实时行情：60秒
   - Session：1小时
   - 技术指标：5分钟

2. **使用批量操作**
   ```bash
   # 不推荐：多次单独操作
   SET stock:600519 1680
   SET stock:000001 10.52

   # 推荐：批量操作
   MSET stock:600519 1680 stock:000001 10.52
   ```

3. **避免使用KEYS命令**
   生产环境用`SCAN`替代`KEYS *`

4. **使用连接池**
   ```java
   // Spring Boot 自动配置连接池
   spring.redis.lettuce.pool.max-active=8
   ```

---

## 下一步学习

1. ✅ 完成所有实战练习
2. 🔐 学习Redis高级特性（持久化、主从复制）
3. 🚀 实践Redis在实际项目中的应用
4. 📊 监控Redis性能指标

---

## 参考资源

- [Redis 官方文档](https://redis.io/documentation)
- [Redis 命令参考](https://redis.io/commands)
- [Redis 中文教程](http://www.redis.cn/)

---

**提示**：Redis非常适合做缓存，但不要把它当成主数据库使用！重要数据还是要存MySQL。
