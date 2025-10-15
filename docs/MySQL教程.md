# MySQL 教程 - 量化交易平台实战

## 什么是 MySQL？

**MySQL** 是一个关系型数据库（RDBMS），可以理解为**结构化数据的电子表格系统**。

### 形象比喻
- **Excel**：适合个人使用，数据量小
- **MySQL**：适合多人同时访问，数据量大，支持复杂查询

---

## 在你的量化交易平台中的作用

```
MySQL数据库
├── users表              (用户信息)
├── user_accounts表      (资金账户)
├── stocks表             (股票信息)
├── kline_daily表        (K线数据)
├── strategies表         (策略配置)
├── orders表             (订单记录)
├── positions表          (持仓信息)
└── trades表             (成交记录)
```

### 核心场景

1. **用户管理**：注册、登录、权限
2. **交易数据**：订单、持仓、成交记录
3. **策略配置**：存储量化策略参数
4. **历史数据**：K线、财务数据等

---

## 实战：使用你的 Docker MySQL

### 1. 启动 MySQL

```bash
cd quant-trading-platform
docker compose up -d mysql
```

### 2. 验证 MySQL 是否启动

```bash
# 查看容器状态
docker compose ps mysql

# 查看日志
docker compose logs mysql | grep "ready for connections"
```

看到 "ready for connections" 表示成功！

### 3. 连接到 MySQL

#### 方法1：使用命令行客户端
```bash
docker exec -it quant-mysql mysql -uroot -proot123456
```

成功后显示：
```
mysql>
```

#### 方法2：使用MySQL命令行（指定数据库）
```bash
docker exec -it quant-mysql mysql -uroot -proot123456 quant_trading
```

#### 方法3：从宿主机连接
```bash
mysql -h 127.0.0.1 -P 3306 -uroot -proot123456
```

---

## MySQL 基础命令

### 1. 数据库操作

```sql
-- 查看所有数据库
SHOW DATABASES;

-- 使用数据库
USE quant_trading;

-- 查看当前数据库
SELECT DATABASE();

-- 创建数据库
CREATE DATABASE test_db;

-- 删除数据库
DROP DATABASE test_db;
```

### 2. 表操作

```sql
-- 查看所有表
SHOW TABLES;

-- 查看表结构
DESC users;

-- 查看创建表的SQL
SHOW CREATE TABLE users;

-- 查看表状态
SHOW TABLE STATUS LIKE 'users';
```

---

## 实战：查询你的交易数据

### 1. 查看用户信息

```sql
-- 查看所有用户
SELECT * FROM users;

-- 查看特定用户
SELECT id, username, email, created_at
FROM users
WHERE username = 'test_user';
```

### 2. 查看资金账户

```sql
-- 查看用户资金
SELECT
    u.username,
    ua.balance AS 可用余额,
    ua.frozen AS 冻结金额,
    ua.total_assets AS 总资产
FROM users u
JOIN user_accounts ua ON u.id = ua.user_id;
```

### 3. 查看股票信息

```sql
-- 查看所有股票
SELECT symbol, name, exchange, list_date
FROM stocks
WHERE status = 1;

-- 查找特定股票
SELECT * FROM stocks WHERE symbol = '600519';

-- 按交易所分组统计
SELECT exchange, COUNT(*) as 股票数量
FROM stocks
WHERE status = 1
GROUP BY exchange;
```

### 4. K线数据查询

```sql
-- 查看最近10天的K线数据
SELECT
    symbol,
    trade_date,
    open AS 开盘,
    high AS 最高,
    low AS 最低,
    close AS 收盘,
    volume AS 成交量
FROM kline_daily
WHERE symbol = '600519'
ORDER BY trade_date DESC
LIMIT 10;

-- 计算某股票的涨跌幅
SELECT
    symbol,
    trade_date,
    close,
    ROUND(((close - LAG(close) OVER (ORDER BY trade_date)) / LAG(close) OVER (ORDER BY trade_date)) * 100, 2) AS 涨跌幅
FROM kline_daily
WHERE symbol = '600519'
ORDER BY trade_date DESC
LIMIT 5;
```

---

## 实战练习：模拟交易流程

### 练习1：插入K线数据

```sql
-- 插入贵州茅台的K线数据
INSERT INTO kline_daily (symbol, trade_date, open, high, low, close, volume, amount, change_pct)
VALUES
('600519', '2025-10-08', 1680.00, 1695.00, 1675.00, 1690.00, 50000, 8450000000, 0.60),
('600519', '2025-10-07', 1670.00, 1682.00, 1665.00, 1680.00, 48000, 8064000000, 0.72),
('600519', '2025-10-06', 1665.00, 1675.00, 1660.00, 1668.00, 45000, 7503000000, 0.18);

-- 查看插入的数据
SELECT * FROM kline_daily WHERE symbol = '600519' ORDER BY trade_date DESC;
```

### 练习2：创建订单

```sql
-- 查看用户资金
SELECT balance FROM user_accounts WHERE user_id = 1;

-- 创建买入订单
INSERT INTO orders (
    order_no, user_id, symbol, direction, price, volume, amount, status
)
VALUES (
    'ORD20251008001',
    1,
    '600519',
    0,  -- 0=买入
    1680.00,
    100,
    168000.00,
    0  -- 0=待成交
);

-- 查看订单
SELECT
    order_no,
    symbol,
    CASE direction WHEN 0 THEN '买入' ELSE '卖出' END AS 方向,
    price AS 价格,
    volume AS 数量,
    amount AS 金额,
    CASE status
        WHEN 0 THEN '待成交'
        WHEN 1 THEN '部分成交'
        WHEN 2 THEN '全部成交'
        ELSE '已撤销'
    END AS 状态
FROM orders
WHERE user_id = 1
ORDER BY created_at DESC;
```

### 练习3：成交并更新持仓

```sql
-- 1. 更新订单状态为全部成交
UPDATE orders
SET status = 2, filled_volume = volume, filled_amount = amount
WHERE order_no = 'ORD20251008001';

-- 2. 插入成交记录
INSERT INTO trades (
    trade_no, order_no, user_id, symbol, direction,
    price, volume, amount, commission
)
VALUES (
    'TRD20251008001',
    'ORD20251008001',
    1,
    '600519',
    0,
    1680.00,
    100,
    168000.00,
    50.40  -- 手续费（万分之三）
);

-- 3. 创建/更新持仓
INSERT INTO positions (user_id, symbol, volume, available_volume, avg_price, cost)
VALUES (1, '600519', 100, 100, 1680.00, 168050.40)
ON DUPLICATE KEY UPDATE
    volume = volume + 100,
    available_volume = available_volume + 100,
    avg_price = ((avg_price * volume) + 168000.00) / (volume + 100),
    cost = cost + 168050.40;

-- 4. 更新资金账户
UPDATE user_accounts
SET balance = balance - 168050.40
WHERE user_id = 1;

-- 5. 查看持仓
SELECT
    symbol AS 代码,
    volume AS 持仓数量,
    available_volume AS 可用数量,
    avg_price AS 持仓均价,
    cost AS 持仓成本
FROM positions
WHERE user_id = 1;
```

### 练习4：策略配置

```sql
-- 创建均线策略
INSERT INTO strategies (user_id, name, type, params, status, description)
VALUES (
    1,
    '双均线策略',
    'MA',
    '{"short_period": 5, "long_period": 20, "symbols": ["600519", "000001"]}',
    1,
    '5日均线上穿20日均线时买入，下穿时卖出'
);

-- 查看所有策略
SELECT
    id,
    name,
    type,
    params,
    CASE status WHEN 1 THEN '启用' ELSE '停用' END AS 状态,
    created_at
FROM strategies
WHERE user_id = 1;

-- 查询启用的策略
SELECT * FROM strategies WHERE user_id = 1 AND status = 1;
```

---

## 高级查询

### 1. 统计分析

```sql
-- 用户交易统计
SELECT
    u.username,
    COUNT(o.id) AS 订单总数,
    SUM(CASE WHEN o.direction = 0 THEN 1 ELSE 0 END) AS 买入笔数,
    SUM(CASE WHEN o.direction = 1 THEN 1 ELSE 0 END) AS 卖出笔数,
    SUM(o.amount) AS 交易总额
FROM users u
LEFT JOIN orders o ON u.id = o.user_id
WHERE o.status = 2  -- 已成交
GROUP BY u.id, u.username;

-- 持仓盈亏分析（需要实时价格）
SELECT
    p.symbol,
    p.volume AS 持仓数量,
    p.avg_price AS 成本价,
    1690.00 AS 最新价,  -- 假设当前价格
    (1690.00 - p.avg_price) * p.volume AS 浮动盈亏,
    ROUND(((1690.00 - p.avg_price) / p.avg_price) * 100, 2) AS 盈亏比例
FROM positions p
WHERE user_id = 1 AND volume > 0;
```

### 2. 时间序列分析

```sql
-- 计算移动平均线（MA5）
SELECT
    symbol,
    trade_date,
    close,
    AVG(close) OVER (
        PARTITION BY symbol
        ORDER BY trade_date
        ROWS BETWEEN 4 PRECEDING AND CURRENT ROW
    ) AS MA5
FROM kline_daily
WHERE symbol = '600519'
ORDER BY trade_date DESC
LIMIT 10;

-- 查找最近N天涨幅最大的股票
SELECT
    k1.symbol,
    s.name,
    k1.close AS 最新价,
    k2.close AS N天前价格,
    ROUND(((k1.close - k2.close) / k2.close) * 100, 2) AS 涨幅
FROM kline_daily k1
JOIN kline_daily k2 ON k1.symbol = k2.symbol
JOIN stocks s ON k1.symbol = s.symbol
WHERE k1.trade_date = '2025-10-08'
  AND k2.trade_date = '2025-10-01'
ORDER BY 涨幅 DESC
LIMIT 10;
```

### 3. 事务处理（保证数据一致性）

```sql
-- 开始事务
START TRANSACTION;

-- 执行多个操作
UPDATE user_accounts SET balance = balance - 100000 WHERE user_id = 1;
INSERT INTO orders (...) VALUES (...);
INSERT INTO positions (...) VALUES (...);

-- 检查无误后提交
COMMIT;

-- 如果出错则回滚
-- ROLLBACK;
```

---

## 数据库维护

### 1. 备份数据库

```bash
# 备份整个数据库
docker exec quant-mysql mysqldump -uroot -proot123456 quant_trading > backup.sql

# 只备份表结构
docker exec quant-mysql mysqldump -uroot -proot123456 --no-data quant_trading > schema.sql

# 备份特定表
docker exec quant-mysql mysqldump -uroot -proot123456 quant_trading users orders > backup_tables.sql
```

### 2. 恢复数据库

```bash
# 恢复数据库
docker exec -i quant-mysql mysql -uroot -proot123456 quant_trading < backup.sql
```

### 3. 查看数据库大小

```sql
-- 查看所有数据库大小
SELECT
    table_schema AS 数据库,
    ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) AS 大小MB
FROM information_schema.tables
GROUP BY table_schema;

-- 查看各表大小
SELECT
    table_name AS 表名,
    ROUND((data_length + index_length) / 1024 / 1024, 2) AS 大小MB,
    table_rows AS 行数
FROM information_schema.tables
WHERE table_schema = 'quant_trading'
ORDER BY (data_length + index_length) DESC;
```

### 4. 优化表

```sql
-- 分析表
ANALYZE TABLE kline_daily;

-- 优化表（碎片整理）
OPTIMIZE TABLE kline_daily;

-- 查看索引使用情况
SHOW INDEX FROM orders;
```

---

## 性能优化

### 1. 创建索引

```sql
-- 为常用查询字段创建索引
CREATE INDEX idx_symbol_date ON kline_daily(symbol, trade_date);
CREATE INDEX idx_user_status ON orders(user_id, status);

-- 查看执行计划
EXPLAIN SELECT * FROM orders WHERE user_id = 1 AND status = 2;
```

### 2. 分区表（处理大量K线数据）

```sql
-- 按月分区K线表
CREATE TABLE kline_daily_partitioned (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    symbol VARCHAR(20) NOT NULL,
    trade_date DATE NOT NULL,
    open DECIMAL(10, 2),
    close DECIMAL(10, 2),
    -- ... 其他字段
    INDEX idx_symbol (symbol),
    INDEX idx_date (trade_date)
)
PARTITION BY RANGE (YEAR(trade_date) * 100 + MONTH(trade_date)) (
    PARTITION p202501 VALUES LESS THAN (202502),
    PARTITION p202502 VALUES LESS THAN (202503),
    PARTITION p202503 VALUES LESS THAN (202504),
    -- ... 其他分区
    PARTITION p_max VALUES LESS THAN MAXVALUE
);
```

---

## 用户权限管理

### 1. 创建只读用户

```sql
-- 创建用户
CREATE USER 'readonly'@'%' IDENTIFIED BY 'readonly123';

-- 授予只读权限
GRANT SELECT ON quant_trading.* TO 'readonly'@'%';

-- 刷新权限
FLUSH PRIVILEGES;
```

### 2. 创建应用用户

```sql
-- 创建应用用户
CREATE USER 'app_user'@'%' IDENTIFIED BY 'app_password';

-- 授予必要权限
GRANT SELECT, INSERT, UPDATE, DELETE ON quant_trading.* TO 'app_user'@'%';

FLUSH PRIVILEGES;
```

### 3. 查看用户权限

```sql
-- 查看所有用户
SELECT user, host FROM mysql.user;

-- 查看用户权限
SHOW GRANTS FOR 'readonly'@'%';
```

---

## 常用命令速查表

| 功能 | 命令 |
|------|------|
| **连接** | |
| 连接数据库 | `docker exec -it quant-mysql mysql -uroot -proot123456` |
| 使用数据库 | `USE quant_trading;` |
| **查询** | |
| 查看所有表 | `SHOW TABLES;` |
| 查看表结构 | `DESC users;` |
| 查询数据 | `SELECT * FROM users;` |
| 条件查询 | `SELECT * FROM users WHERE id = 1;` |
| 排序查询 | `SELECT * FROM orders ORDER BY created_at DESC;` |
| 限制结果 | `SELECT * FROM orders LIMIT 10;` |
| **插入** | |
| 插入数据 | `INSERT INTO users (username, email) VALUES ('test', 'test@example.com');` |
| **更新** | |
| 更新数据 | `UPDATE users SET email = 'new@example.com' WHERE id = 1;` |
| **删除** | |
| 删除数据 | `DELETE FROM users WHERE id = 1;` |
| **统计** | |
| 计数 | `SELECT COUNT(*) FROM users;` |
| 求和 | `SELECT SUM(amount) FROM orders;` |
| 平均值 | `SELECT AVG(price) FROM kline_daily;` |
| 最大值 | `SELECT MAX(price) FROM kline_daily;` |
| 分组统计 | `SELECT symbol, COUNT(*) FROM orders GROUP BY symbol;` |

---

## Docker 配置说明

### docker-compose.yml 解析

```yaml
mysql:
  image: mysql:8.0
  ports:
    - "3306:3306"
  environment:
    - MYSQL_ROOT_PASSWORD=root123456  # root密码
    - MYSQL_DATABASE=quant_trading    # 自动创建的数据库
    - TZ=Asia/Shanghai                # 时区
  volumes:
    - mysql-data:/var/lib/mysql       # 数据持久化
    - ./infrastructure/mysql/init.sql:/docker-entrypoint-initdb.d/init.sql  # 初始化脚本
```

**初始化流程**：
1. 容器首次启动
2. 自动创建 `quant_trading` 数据库
3. 执行 `init.sql` 创建所有表
4. 插入测试数据

---

## 常见问题

### Q1: 忘记root密码怎么办？

```bash
# 停止容器
docker compose stop mysql

# 删除容器和数据
docker compose down -v

# 重新启动（会重新初始化）
docker compose up -d mysql
```

### Q2: 如何连接远程数据库工具？

使用以下信息：
- Host: `localhost`
- Port: `3306`
- User: `root`
- Password: `root123456`
- Database: `quant_trading`

推荐工具：DBeaver, DataGrip, Navicat

### Q3: 如何查看慢查询？

```sql
-- 开启慢查询日志
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 1;  -- 1秒以上的查询

-- 查看慢查询
SHOW VARIABLES LIKE 'slow_query%';
```

### Q4: 数据库占用空间太大怎么办？

```sql
-- 1. 删除历史数据
DELETE FROM kline_daily WHERE trade_date < '2024-01-01';

-- 2. 优化表
OPTIMIZE TABLE kline_daily;

-- 3. 使用分区表
-- 参考上面"性能优化"章节
```

---

## 在项目中的实际应用

### 应用1：Java Spring Boot 连接

```java
// application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/quant_trading?useUnicode=true&characterEncoding=utf8
    username: root
    password: root123456
    driver-class-name: com.mysql.cj.jdbc.Driver
```

### 应用2：Python 连接

```python
import pymysql

conn = pymysql.connect(
    host='localhost',
    port=3306,
    user='root',
    password='root123456',
    database='quant_trading',
    charset='utf8mb4'
)

cursor = conn.cursor()
cursor.execute("SELECT * FROM stocks")
results = cursor.fetchall()
```

### 应用3：MyBatis Mapper

```java
@Mapper
public interface OrderMapper {
    @Select("SELECT * FROM orders WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<Order> findByUserId(@Param("userId") Long userId);
}
```

---

## 下一步学习

1. ✅ 完成所有实战练习
2. 📊 学习如何设计数据库表结构
3. ⚡ 掌握索引和查询优化
4. 🔄 了解主从复制和读写分离

---

## 参考资源

- [MySQL 官方文档](https://dev.mysql.com/doc/)
- [SQL教程 - 菜鸟教程](https://www.runoob.com/sql/sql-tutorial.html)
- 项目文档：`infrastructure/mysql/init.sql`

---

**提示**：建议使用图形化工具（DBeaver/DataGrip）来查看和管理数据库！
