# Zookeeper 教程 - 量化交易平台实战

## 什么是 Zookeeper？

**Zookeeper** 是一个分布式协调服务，可以理解为**微服务的通讯录**。

### 形象比喻
想象你在一个大公司：
- **没有Zookeeper**：你要找财务部，得自己记住"财务部在3楼305室"，如果财务部搬家了，你就找不到了
- **有Zookeeper**：你去前台问"财务部在哪？"，前台告诉你最新位置，财务部搬家了前台也知道

## 在你的量化交易平台中的作用

```
用户服务 (user-service)      ─┐
交易服务 (trading-service)    ├─→ 注册到 Zookeeper
数据分析服务 (analysis-service) ─┤      ↓
策略服务 (strategy-service)   ─┘   "我在 192.168.1.100:20880"
                                        ↓
API Gateway 想调用用户服务 ─→ 问 Zookeeper ─→ 得到地址并调用
```

### 核心功能

1. **服务注册与发现**
   - 服务启动时：向Zookeeper注册自己的地址
   - 服务调用时：从Zookeeper查询目标服务的地址

2. **配置管理**
   - 统一管理配置：数据库连接、API密钥等
   - 配置修改后所有服务自动感知

3. **集群协调**
   - 选主：多个服务实例中选出Leader
   - 分布式锁：防止多个服务同时操作同一资源

---

## Zookeeper 数据模型

Zookeeper的数据结构像**文件系统**：

```
/                           (根节点)
├── dubbo                   (Dubbo服务注册路径)
│   ├── user-service
│   │   ├── providers       (服务提供者列表)
│   │   │   └── 192.168.1.100:20880
│   │   └── consumers       (服务消费者列表)
│   │       └── 192.168.1.101:8080
│   ├── trading-service
│   └── analysis-service
├── config                  (配置中心)
│   ├── mysql-config
│   └── redis-config
└── locks                   (分布式锁)
    └── order-lock
```

---

## 实战：使用你的 Docker Zookeeper

### 1. 启动 Zookeeper

```bash
cd quant-trading-platform
docker compose up -d zookeeper
```

### 2. 验证 Zookeeper 是否启动

```bash
# 查看容器状态
docker compose ps zookeeper

# 查看日志
docker compose logs zookeeper
```

### 3. 连接到 Zookeeper 命令行

```bash
# 进入容器
docker exec -it quant-zookeeper bash

# 启动 Zookeeper 客户端
zkCli.sh -server localhost:2181
```

成功后你会看到：
```
Welcome to ZooKeeper!
[zk: localhost:2181(CONNECTED) 0]
```

### 4. 基础命令实战

#### 查看根节点
```bash
[zk: localhost:2181(CONNECTED) 0] ls /
[zookeeper]
```

#### 创建节点（模拟服务注册）
```bash
# 创建 dubbo 目录
create /dubbo ""

# 创建 user-service
create /dubbo/user-service ""

# 创建服务提供者节点
create /dubbo/user-service/providers ""

# 注册一个服务实例（临时节点）
create -e /dubbo/user-service/providers/192.168.1.100:20880 "user-service-instance-1"
```

**说明**：
- `-e` 表示临时节点（ephemeral），服务断开连接后自动删除
- 这就是Dubbo自动注册服务的原理

#### 查看节点数据
```bash
# 查看 providers 下的所有服务
ls /dubbo/user-service/providers

# 查看节点详细信息
get /dubbo/user-service/providers/192.168.1.100:20880
```

#### 监听节点变化（模拟服务发现）
```bash
# 监听 providers 节点变化
ls -w /dubbo/user-service/providers
```

**现在打开另一个终端，删除服务节点**：
```bash
docker exec -it quant-zookeeper zkCli.sh -server localhost:2181
delete /dubbo/user-service/providers/192.168.1.100:20880
```

回到第一个终端，你会看到变化通知！

#### 删除节点
```bash
# 删除单个节点
delete /dubbo/user-service/providers/192.168.1.100:20880

# 递归删除（带子节点）
deleteall /dubbo
```

#### 退出客户端
```bash
quit
```

---

## Zookeeper 在你的项目中的实际应用

### 场景1：Dubbo 服务注册

**用户服务启动时**（自动执行）：
```java
// application.yml
dubbo:
  registry:
    address: zookeeper://localhost:2181
```

Dubbo会自动在Zookeeper创建：
```
/dubbo/com.quant.UserService/providers/
  └── dubbo://192.168.1.100:20880/com.quant.UserService
```

**API Gateway调用时**：
```java
@DubboReference
private UserService userService;  // Dubbo自动从Zookeeper查询服务地址
```

### 场景2：配置中心

**存储MySQL配置**：
```bash
create /config/mysql "jdbc:mysql://localhost:3306/quant_trading"
```

**Java服务读取**：
```java
String mysqlUrl = zkClient.getData("/config/mysql");
```

### 场景3：分布式锁

**防止多个策略服务同时执行同一个订单**：
```bash
# 获取锁
create -e /locks/order-12345 ""

# 执行业务逻辑
# ...

# 释放锁
delete /locks/order-12345
```

---

## Zookeeper 常用命令速查表

| 命令 | 说明 | 示例 |
|------|------|------|
| `ls /path` | 列出子节点 | `ls /dubbo` |
| `create /path data` | 创建节点 | `create /test "hello"` |
| `create -e /path data` | 创建临时节点 | `create -e /temp "test"` |
| `create -s /path data` | 创建顺序节点 | `create -s /seq "1"` |
| `get /path` | 获取节点数据 | `get /dubbo/user-service` |
| `set /path data` | 修改节点数据 | `set /test "world"` |
| `delete /path` | 删除节点 | `delete /test` |
| `deleteall /path` | 递归删除 | `deleteall /dubbo` |
| `ls -w /path` | 监听子节点变化 | `ls -w /dubbo/providers` |
| `stat /path` | 查看节点状态 | `stat /dubbo` |

---

## 实战练习：模拟微服务注册

### 练习1：注册3个用户服务实例

```bash
# 连接 Zookeeper
docker exec -it quant-zookeeper zkCli.sh -server localhost:2181

# 创建服务目录
create /dubbo ""
create /dubbo/user-service ""
create /dubbo/user-service/providers ""

# 注册3个实例
create -e /dubbo/user-service/providers/192.168.1.100:20880 "instance-1"
create -e /dubbo/user-service/providers/192.168.1.101:20880 "instance-2"
create -e /dubbo/user-service/providers/192.168.1.102:20880 "instance-3"

# 查看所有实例
ls /dubbo/user-service/providers
```

### 练习2：模拟服务下线

```bash
# 删除一个实例（模拟服务崩溃）
delete /dubbo/user-service/providers/192.168.1.100:20880

# 再次查看
ls /dubbo/user-service/providers
```

### 练习3：监听服务变化

**终端1**（监听）：
```bash
docker exec -it quant-zookeeper zkCli.sh -server localhost:2181
ls -w /dubbo/user-service/providers
```

**终端2**（模拟服务上线）：
```bash
docker exec -it quant-zookeeper zkCli.sh -server localhost:2181
create -e /dubbo/user-service/providers/192.168.1.200:20880 "new-instance"
```

回到终端1，你会看到变化通知！

---

## Zookeeper 在 Docker 中的配置说明

### docker-compose.yml 配置解析

```yaml
zookeeper:
  image: zookeeper:3.9           # 使用官方镜像
  container_name: quant-zookeeper
  ports:
    - "2181:2181"                # 客户端连接端口
  environment:
    - ALLOW_ANONYMOUS_LOGIN=yes  # 允许匿名访问（开发环境）
  volumes:
    - zookeeper-data:/data       # 持久化数据
    - zookeeper-logs:/datalog    # 日志目录
```

### 关键端口

| 端口 | 用途 |
|------|------|
| 2181 | 客户端连接端口（Java服务、zkCli都用这个） |
| 2888 | Zookeeper集群内部通信（单机不需要） |
| 3888 | Leader选举端口（单机不需要） |

---

## 常见问题

### Q1: 如何查看Zookeeper中所有Dubbo服务？

```bash
docker exec -it quant-zookeeper zkCli.sh -server localhost:2181
ls /dubbo
```

### Q2: 如何清空所有服务注册信息？

```bash
docker exec -it quant-zookeeper zkCli.sh -server localhost:2181
deleteall /dubbo
```

### Q3: 如何检查Zookeeper健康状态？

```bash
# 方法1：使用四字命令
echo stat | nc localhost 2181

# 方法2：查看容器日志
docker compose logs zookeeper | grep -i "binding to port"
```

### Q4: 临时节点和持久节点的区别？

| 类型 | 创建方式 | 生命周期 | 使用场景 |
|------|---------|---------|---------|
| 持久节点 | `create /path data` | 手动删除 | 配置数据、目录结构 |
| 临时节点 | `create -e /path data` | 会话结束自动删除 | 服务注册（服务挂了自动下线） |

---

## 下一步学习

1. **Java客户端**：使用Apache Curator操作Zookeeper
2. **Dubbo集成**：Spring Boot + Dubbo + Zookeeper完整示例
3. **配置中心**：实现动态配置更新
4. **分布式锁**：实现分布式事务控制

---

## 参考资源

- [Zookeeper 官方文档](https://zookeeper.apache.org/doc/current/)
- [Dubbo + Zookeeper 官方指南](https://dubbo.apache.org/zh/docs/)
- 项目文档：`量化交易平台架构设计.md`

---

**提示**：每次学习完可以更新 `DEVLOG.md` 记录你的进度！
