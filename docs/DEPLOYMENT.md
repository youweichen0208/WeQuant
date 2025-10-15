# 量化交易平台部署文档

## 目录
- [系统要求](#系统要求)
- [生产环境部署](#生产环境部署)
- [性能优化配置](#性能优化配置)
- [监控与日志](#监控与日志)
- [运维指南](#运维指南)
- [故障排查](#故障排查)

## 系统要求

### 最低配置
- CPU: 4核
- 内存: 8GB
- 硬盘: 50GB SSD
- 操作系统: Linux (Ubuntu 20.04+ / CentOS 7+)
- Docker: 20.10+
- Docker Compose: 2.0+

### 推荐配置
- CPU: 8核
- 内存: 16GB
- 硬盘: 100GB SSD
- 网络: 100Mbps+

## 生产环境部署

### 1. 准备工作

```bash
# 克隆代码
git clone <repository-url>
cd quant-trading-platform

# 创建数据目录
mkdir -p data/{mysql,redis,zookeeper,kafka,market-data}
mkdir -p logs/{nginx,market-data}

# 配置环境变量
cp .env.prod.example .env.prod
vim .env.prod  # 修改为实际的密码和配置
```

### 2. 环境变量配置

编辑 `.env.prod` 文件：

```bash
# MySQL配置 - 使用强密码
MYSQL_ROOT_PASSWORD=YourSecurePassword123!@#

# Redis配置 - 使用强密码
REDIS_PASSWORD=YourSecureRedisPassword456!@#

# JWT密钥 - 至少32字符
JWT_SECRET=your_jwt_secret_key_at_least_32_characters_long_here

# Tushare Token - 从tushare官网获取
TUSHARE_TOKEN=your_tushare_token_from_tushare_pro_website
```

### 3. 启动服务

```bash
# 使用生产环境配置启动
docker-compose -f docker-compose.prod.yml --env-file .env.prod up -d

# 查看服务状态
docker-compose -f docker-compose.prod.yml ps

# 查看日志
docker-compose -f docker-compose.prod.yml logs -f
```

### 4. 验证部署

```bash
# 检查MySQL
docker exec quant-mysql-prod mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "SHOW DATABASES;"

# 检查Redis
docker exec quant-redis-prod redis-cli -a ${REDIS_PASSWORD} ping

# 检查Kafka
docker exec quant-kafka-prod kafka-topics --bootstrap-server localhost:9092 --list

# 访问前端
curl http://localhost

# 检查后端API
curl http://localhost:8081/actuator/health
```

## 性能优化配置

### MySQL优化

生产环境MySQL配置已包含以下优化：

```yaml
# docker-compose.prod.yml 中的配置
--max_connections=1000           # 最大连接数
--innodb_buffer_pool_size=1G     # InnoDB缓冲池大小
--character-set-server=utf8mb4   # 字符集
```

### Redis优化

```yaml
# Redis配置优化
--maxmemory 512mb                 # 最大内存
--maxmemory-policy allkeys-lru    # 内存淘汰策略
```

### Kafka优化

```yaml
KAFKA_LOG_RETENTION_HOURS: 168         # 日志保留7天
KAFKA_LOG_SEGMENT_BYTES: 1073741824    # 日志段大小1GB
KAFKA_NUM_PARTITIONS: 3                # 默认分区数
```

### JVM优化 (user-service)

Dockerfile中已配置：

```dockerfile
-XX:MaxRAMPercentage=75.0    # 使用容器75%内存
-XX:+UseG1GC                 # 使用G1垃圾收集器
-XX:+UseContainerSupport     # 容器支持
```

## 监控与日志

### 日志位置

```
logs/
├── nginx/          # Nginx访问日志和错误日志
└── market-data/    # 市场数据服务日志
```

### 查看实时日志

```bash
# 查看所有服务日志
docker-compose -f docker-compose.prod.yml logs -f

# 查看特定服务日志
docker-compose -f docker-compose.prod.yml logs -f user-service
docker-compose -f docker-compose.prod.yml logs -f market-data-service

# 查看最近100行日志
docker-compose -f docker-compose.prod.yml logs --tail=100 user-service
```

### 健康检查

所有服务都配置了健康检查：

```bash
# 查看服务健康状态
docker-compose -f docker-compose.prod.yml ps

# 服务健康检查端点
- MySQL: mysqladmin ping
- Redis: redis-cli ping
- Zookeeper: zkServer.sh status
- Kafka: kafka-broker-api-versions
- User Service: http://localhost:8081/actuator/health
- Web Frontend: http://localhost:80
```

## 运维指南

### 服务管理

```bash
# 启动所有服务
docker-compose -f docker-compose.prod.yml up -d

# 停止所有服务
docker-compose -f docker-compose.prod.yml stop

# 重启特定服务
docker-compose -f docker-compose.prod.yml restart user-service

# 停止并删除所有容器
docker-compose -f docker-compose.prod.yml down

# 停止并删除所有容器和数据卷（危险操作！）
docker-compose -f docker-compose.prod.yml down -v
```

### 数据备份

#### MySQL备份

```bash
# 备份数据库
docker exec quant-mysql-prod mysqldump -uroot -p${MYSQL_ROOT_PASSWORD} \
  quant_trading > backup_$(date +%Y%m%d_%H%M%S).sql

# 恢复数据库
docker exec -i quant-mysql-prod mysql -uroot -p${MYSQL_ROOT_PASSWORD} \
  quant_trading < backup_20231210_120000.sql
```

#### Redis备份

```bash
# Redis会自动创建RDB快照在 data/redis/dump.rdb

# 手动触发备份
docker exec quant-redis-prod redis-cli -a ${REDIS_PASSWORD} BGSAVE

# 复制备份文件
cp data/redis/dump.rdb backup/redis_backup_$(date +%Y%m%d).rdb
```

### 更新部署

```bash
# 拉取最新代码
git pull

# 重新构建并启动
docker-compose -f docker-compose.prod.yml up -d --build

# 或者分步骤：
# 1. 构建镜像
docker-compose -f docker-compose.prod.yml build

# 2. 停止旧容器
docker-compose -f docker-compose.prod.yml stop

# 3. 启动新容器
docker-compose -f docker-compose.prod.yml up -d
```

### 扩容

```bash
# 扩展user-service实例
docker-compose -f docker-compose.prod.yml up -d --scale user-service=3

# 扩展market-data-service实例
docker-compose -f docker-compose.prod.yml up -d --scale market-data-service=2
```

## 故障排查

### 常见问题

#### 1. 服务启动失败

```bash
# 查看服务日志
docker-compose -f docker-compose.prod.yml logs service-name

# 查看容器状态
docker ps -a

# 进入容器排查
docker exec -it container-name sh
```

#### 2. 数据库连接失败

```bash
# 检查MySQL是否正常运行
docker-compose -f docker-compose.prod.yml ps mysql

# 检查MySQL日志
docker-compose -f docker-compose.prod.yml logs mysql

# 测试连接
docker exec quant-mysql-prod mysql -uroot -p${MYSQL_ROOT_PASSWORD} -e "SELECT 1;"
```

#### 3. Redis连接失败

```bash
# 检查Redis状态
docker-compose -f docker-compose.prod.yml ps redis

# 测试连接
docker exec quant-redis-prod redis-cli -a ${REDIS_PASSWORD} ping
```

#### 4. Kafka连接失败

```bash
# 检查Zookeeper
docker-compose -f docker-compose.prod.yml logs zookeeper

# 检查Kafka
docker-compose -f docker-compose.prod.yml logs kafka

# 查看Topics
docker exec quant-kafka-prod kafka-topics --bootstrap-server localhost:9092 --list
```

#### 5. 内存不足

```bash
# 查看容器资源使用
docker stats

# 调整服务内存限制（在docker-compose.prod.yml中）
services:
  user-service:
    deploy:
      resources:
        limits:
          memory: 2G
        reservations:
          memory: 1G
```

#### 6. 磁盘空间不足

```bash
# 查看磁盘使用
df -h

# 清理Docker未使用的资源
docker system prune -a

# 清理日志文件
find logs/ -name "*.log" -mtime +7 -delete
```

### 性能监控

```bash
# 查看容器资源使用情况
docker stats

# 查看特定容器的资源使用
docker stats quant-user-service-prod quant-mysql-prod

# 查看网络连接
docker exec quant-user-service-prod netstat -an | grep ESTABLISHED
```

### 安全建议

1. **修改默认密码**
   - 修改.env.prod中所有密码为强密码
   - 定期更换密码

2. **网络安全**
   - 仅暴露必要的端口
   - 使用防火墙限制访问
   - 考虑使用VPN或内网

3. **SSL/TLS**
   - 生产环境建议配置HTTPS
   - 使用Let's Encrypt获取免费证书

4. **数据备份**
   - 设置自动备份任务
   - 异地备份重要数据
   - 定期测试恢复流程

5. **日志管理**
   - 定期清理旧日志
   - 考虑使用日志收集系统（ELK）

## 性能调优建议

### 数据库优化
- 定期分析慢查询日志
- 添加适当的索引
- 考虑分表分库策略

### Redis优化
- 合理设置过期时间
- 避免大key
- 使用连接池

### Kafka优化
- 调整分区数量
- 配置合适的副本因子
- 监控消费延迟

### 应用优化
- 启用JVM参数优化
- 使用连接池
- 实施缓存策略
- 异步处理耗时操作

## 联系支持

如遇到问题，请：
1. 查看日志文件
2. 检查服务健康状态
3. 参考故障排查章节
4. 提交Issue到项目仓库
