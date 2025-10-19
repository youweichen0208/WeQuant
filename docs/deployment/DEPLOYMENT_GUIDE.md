# 部署指南

## 概述

WeQuant 量化交易平台的完整部署指南，包含开发环境、测试环境和生产环境的部署方案。

## 🔧 系统要求

### 硬件要求

| 环境 | CPU | 内存 | 存储 | 网络 |
|------|-----|------|------|------|
| 开发环境 | 2核+ | 8GB+ | 50GB+ | 宽带 |
| 测试环境 | 4核+ | 16GB+ | 100GB+ | 100Mbps+ |
| 生产环境 | 8核+ | 32GB+ | 500GB+ | 1Gbps+ |

### 软件依赖

- **操作系统**: Linux (Ubuntu 20.04+) / macOS / Windows 10+
- **Docker**: 20.10+
- **Docker Compose**: 1.29+
- **Java**: JDK 17+
- **Python**: 3.9+
- **Node.js**: 18+
- **Maven**: 3.8+
- **Git**: 2.30+

---

## 🚀 快速开始

### 1. 克隆项目

```bash
git clone https://github.com/youweichen0208/WeQuant.git
cd WeQuant
```

### 2. 环境准备

```bash
# 安装 Docker 和 Docker Compose
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# 启动基础设施服务
docker-compose up -d mysql redis kafka zookeeper

# 等待服务启动
sleep 30
```

### 3. 启动服务

```bash
# 启动市场数据服务 (Python)
cd market-data-service
pip3 install -r requirements.txt
python3 app.py &

# 启动模拟交易服务 (Python)
cd ../mock-trading-service
pip3 install -r requirements.txt
python3 app.py &

# 启动股票服务 (Java)
cd ../stock-service
mvn spring-boot:run &

# 启动前端服务
cd ../web-frontend
npm install
npm run dev &
```

### 4. 验证部署

```bash
# 检查服务状态
curl http://localhost:5001/api/health    # 市场数据服务
curl http://localhost:5002/api/health    # 模拟交易服务
curl http://localhost:8082/stock-service/api/health  # 股票服务
curl http://localhost:3003               # 前端服务
```

---

## 🐳 Docker 容器化部署

### 完整的 Docker Compose 配置

```yaml
# docker-compose.yml
version: '3.8'

services:
  # ==================== 前端服务 ====================
  web-frontend:
    build:
      context: ./web-frontend
      dockerfile: Dockerfile
    ports:
      - "3000:3000"
    environment:
      - NODE_ENV=production
      - VUE_APP_API_BASE_URL=http://nginx
    depends_on:
      - nginx
    networks:
      - quant-network

  # ==================== API网关 ====================
  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
      - ./nginx/ssl:/etc/nginx/ssl:ro
    depends_on:
      - user-service
      - stock-service
      - trading-service
      - market-data-service
    networks:
      - quant-network

  # ==================== Java 微服务 ====================
  user-service:
    build:
      context: ./user-service
      dockerfile: Dockerfile
    ports:
      - "8081:8081"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - DB_HOST=mysql
      - DB_PORT=3306
      - DB_NAME=quant_trading
      - DB_USERNAME=root
      - DB_PASSWORD=123456
      - REDIS_HOST=redis
      - REDIS_PORT=6379
      - REDIS_PASSWORD=redis123456
    depends_on:
      - mysql
      - redis
    networks:
      - quant-network
    restart: unless-stopped

  stock-service:
    build:
      context: ./stock-service
      dockerfile: Dockerfile
    ports:
      - "8082:8082"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - DB_HOST=mysql
      - DB_PORT=3306
      - DB_NAME=quant_trading
      - DB_USERNAME=root
      - DB_PASSWORD=123456
      - REDIS_HOST=redis
      - REDIS_PORT=6379
      - REDIS_PASSWORD=redis123456
      - MARKET_DATA_URL=http://market-data-service:5001
    depends_on:
      - mysql
      - redis
      - market-data-service
    networks:
      - quant-network
    restart: unless-stopped

  trading-service:
    build:
      context: ./trading-service
      dockerfile: Dockerfile
    ports:
      - "8083:8083"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - DB_HOST=mysql
      - DB_PORT=3306
      - DB_NAME=quant_trading
      - DB_USERNAME=root
      - DB_PASSWORD=123456
      - REDIS_HOST=redis
      - REDIS_PORT=6379
      - REDIS_PASSWORD=redis123456
      - STOCK_SERVICE_URL=http://stock-service:8082
    depends_on:
      - mysql
      - redis
      - stock-service
    networks:
      - quant-network
    restart: unless-stopped

  # ==================== Python 服务 ====================
  market-data-service:
    build:
      context: ./market-data-service
      dockerfile: Dockerfile
    ports:
      - "5001:5001"
    environment:
      - REDIS_URL=redis://redis:6379/1
      - REDIS_PASSWORD=redis123456
      - KAFKA_BOOTSTRAP_SERVERS=kafka:9092
    depends_on:
      - redis
      - kafka
    networks:
      - quant-network
    restart: unless-stopped

  mock-trading-service:
    build:
      context: ./mock-trading-service
      dockerfile: Dockerfile
    ports:
      - "5002:5002"
    environment:
      - DATABASE_URL=sqlite:///trading.db
      - MARKET_DATA_URL=http://market-data-service:5001
    depends_on:
      - market-data-service
    networks:
      - quant-network
    restart: unless-stopped

  # ==================== 数据存储 ====================
  mysql:
    image: mysql:8.0
    ports:
      - "3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: 123456
      MYSQL_DATABASE: quant_trading
      MYSQL_USER: quant_user
      MYSQL_PASSWORD: quant_pass
    volumes:
      - mysql_data:/var/lib/mysql
      - ./database/init:/docker-entrypoint-initdb.d
    networks:
      - quant-network
    restart: unless-stopped

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    command: redis-server --requirepass redis123456 --appendonly yes
    volumes:
      - redis_data:/data
    networks:
      - quant-network
    restart: unless-stopped

  # ==================== 消息队列 ====================
  zookeeper:
    image: confluentinc/cp-zookeeper:latest
    ports:
      - "2181:2181"
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    volumes:
      - zookeeper_data:/var/lib/zookeeper/data
      - zookeeper_logs:/var/lib/zookeeper/log
    networks:
      - quant-network
    restart: unless-stopped

  kafka:
    image: confluentinc/cp-kafka:latest
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092,PLAINTEXT_INTERNAL://kafka:29092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_INTERNAL:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT_INTERNAL
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: true
    volumes:
      - kafka_data:/var/lib/kafka/data
    depends_on:
      - zookeeper
    networks:
      - quant-network
    restart: unless-stopped

  # ==================== 监控服务 ====================
  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/etc/prometheus/console_libraries'
      - '--web.console.templates=/etc/prometheus/consoles'
    networks:
      - quant-network
    restart: unless-stopped

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3001:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin123
    volumes:
      - grafana_data:/var/lib/grafana
      - ./monitoring/grafana:/etc/grafana/provisioning
    depends_on:
      - prometheus
    networks:
      - quant-network
    restart: unless-stopped

# ==================== 网络配置 ====================
networks:
  quant-network:
    driver: bridge

# ==================== 数据卷 ====================
volumes:
  mysql_data:
  redis_data:
  kafka_data:
  zookeeper_data:
  zookeeper_logs:
  prometheus_data:
  grafana_data:
```

### Dockerfile 示例

#### Java 服务 Dockerfile
```dockerfile
# stock-service/Dockerfile
FROM openjdk:17-jdk-slim

WORKDIR /app

# 复制 Maven 配置文件
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# 下载依赖
RUN ./mvnw dependency:go-offline -B

# 复制源代码
COPY src src

# 构建应用
RUN ./mvnw clean package -DskipTests

# 运行应用
EXPOSE 8082
CMD ["java", "-jar", "target/stock-service-1.0.0.jar"]
```

#### Python 服务 Dockerfile
```dockerfile
# market-data-service/Dockerfile
FROM python:3.9-slim

WORKDIR /app

# 安装系统依赖
RUN apt-get update && apt-get install -y \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# 复制依赖文件
COPY requirements.txt .

# 安装 Python 依赖
RUN pip install --no-cache-dir -r requirements.txt

# 复制应用代码
COPY . .

# 暴露端口
EXPOSE 5001

# 启动应用
CMD ["python", "app.py"]
```

#### 前端 Dockerfile
```dockerfile
# web-frontend/Dockerfile
FROM node:18-alpine as builder

WORKDIR /app

# 复制 package.json
COPY package*.json ./

# 安装依赖
RUN npm ci

# 复制源代码
COPY . .

# 构建应用
RUN npm run build

# 生产环境
FROM nginx:alpine

# 复制构建结果
COPY --from=builder /app/dist /usr/share/nginx/html

# 复制 Nginx 配置
COPY nginx.conf /etc/nginx/nginx.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
```

---

## ⚙️ 环境配置

### 开发环境 (Development)

```bash
# 启动开发环境
docker-compose -f docker-compose.dev.yml up -d

# 或使用脚本启动
./scripts/start-dev.sh
```

#### 开发环境特性
- 热重载支持
- 详细日志输出
- 开发工具集成
- H2 内存数据库
- 简化的安全配置

### 测试环境 (Testing)

```bash
# 启动测试环境
docker-compose -f docker-compose.test.yml up -d

# 运行测试套件
./scripts/run-tests.sh
```

#### 测试环境特性
- 完整的集成测试
- 模拟外部服务
- 测试数据预置
- 性能测试工具
- 自动化测试流水线

### 生产环境 (Production)

```bash
# 启动生产环境
docker-compose -f docker-compose.prod.yml up -d

# 使用 K8s 部署
kubectl apply -f k8s/
```

#### 生产环境特性
- 高可用配置
- 负载均衡
- 安全加固
- 监控告警
- 日志收集
- 备份恢复

---

## 🔐 安全配置

### SSL/TLS 配置

```nginx
# nginx/nginx.conf
server {
    listen 443 ssl http2;
    server_name yourdomain.com;

    ssl_certificate /etc/nginx/ssl/cert.pem;
    ssl_certificate_key /etc/nginx/ssl/key.pem;

    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-RSA-AES256-GCM-SHA512:DHE-RSA-AES256-GCM-SHA512;
    ssl_prefer_server_ciphers off;

    location / {
        proxy_pass http://web-frontend:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /api/ {
        proxy_pass http://api-gateway:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### 环境变量安全

```bash
# .env.production
# 数据库配置
DB_HOST=mysql-prod.internal
DB_PORT=3306
DB_NAME=quant_trading_prod
DB_USERNAME=quant_user
DB_PASSWORD=${DB_PASSWORD_SECRET}

# Redis 配置
REDIS_HOST=redis-prod.internal
REDIS_PORT=6379
REDIS_PASSWORD=${REDIS_PASSWORD_SECRET}

# JWT 密钥
JWT_SECRET=${JWT_SECRET_KEY}
JWT_EXPIRATION=86400

# 外部 API 密钥
AKSHARE_API_KEY=${AKSHARE_API_KEY}
```

---

## 📊 监控配置

### Prometheus 配置

```yaml
# monitoring/prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - "rules/*.yml"

scrape_configs:
  - job_name: 'user-service'
    static_configs:
      - targets: ['user-service:8081']
    metrics_path: '/actuator/prometheus'

  - job_name: 'stock-service'
    static_configs:
      - targets: ['stock-service:8082']
    metrics_path: '/actuator/prometheus'

  - job_name: 'trading-service'
    static_configs:
      - targets: ['trading-service:8083']
    metrics_path: '/actuator/prometheus'

  - job_name: 'market-data-service'
    static_configs:
      - targets: ['market-data-service:5001']
    metrics_path: '/metrics'

alerting:
  alertmanagers:
    - static_configs:
        - targets:
          - alertmanager:9093
```

### Grafana Dashboard

```json
{
  "dashboard": {
    "title": "WeQuant 系统监控",
    "panels": [
      {
        "title": "API 请求率",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(http_requests_total[5m])",
            "legendFormat": "{{service}}"
          }
        ]
      },
      {
        "title": "响应时间",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))",
            "legendFormat": "95th percentile"
          }
        ]
      },
      {
        "title": "错误率",
        "type": "singlestat",
        "targets": [
          {
            "expr": "rate(http_requests_total{status=~\"5..\"}[5m]) / rate(http_requests_total[5m])",
            "legendFormat": "Error Rate"
          }
        ]
      }
    ]
  }
}
```

---

## 🚨 故障排除

### 常见问题

#### 1. 服务启动失败

```bash
# 检查日志
docker-compose logs -f service-name

# 检查资源使用
docker stats

# 重启服务
docker-compose restart service-name
```

#### 2. 数据库连接问题

```bash
# 检查 MySQL 状态
docker exec -it mysql mysql -u root -p
SHOW PROCESSLIST;

# 检查 Redis 连接
docker exec -it redis redis-cli -a redis123456
INFO replication
```

#### 3. 网络连接问题

```bash
# 检查容器网络
docker network ls
docker network inspect quant-network

# 测试服务连通性
docker exec -it stock-service ping market-data-service
```

#### 4. 性能问题

```bash
# 检查资源使用
docker exec -it service-name top
docker exec -it service-name df -h

# 查看 JVM 状态 (Java 服务)
docker exec -it stock-service jps -v
```

### 日志管理

```bash
# 查看实时日志
docker-compose logs -f --tail=100

# 查看特定服务日志
docker-compose logs -f stock-service

# 导出日志
docker-compose logs --no-color > system.log
```

---

## 📋 运维脚本

### 启动脚本

```bash
#!/bin/bash
# scripts/start-prod.sh

set -e

echo "🚀 启动 WeQuant 生产环境..."

# 拉取最新镜像
docker-compose -f docker-compose.prod.yml pull

# 启动基础设施
echo "📊 启动基础设施服务..."
docker-compose -f docker-compose.prod.yml up -d mysql redis kafka zookeeper

# 等待数据库准备就绪
echo "⏳ 等待数据库启动..."
./scripts/wait-for-db.sh

# 运行数据库迁移
echo "🗄️ 运行数据库迁移..."
./scripts/migrate-db.sh

# 启动微服务
echo "🔧 启动微服务..."
docker-compose -f docker-compose.prod.yml up -d

# 健康检查
echo "🏥 健康检查..."
./scripts/health-check.sh

echo "✅ WeQuant 系统启动完成！"
```

### 备份脚本

```bash
#!/bin/bash
# scripts/backup.sh

BACKUP_DIR="/backups/$(date +%Y%m%d_%H%M%S)"
mkdir -p $BACKUP_DIR

echo "📦 开始备份..."

# 备份 MySQL 数据
docker exec mysql mysqldump -u root -p123456 quant_trading > $BACKUP_DIR/mysql_backup.sql

# 备份 Redis 数据
docker exec redis redis-cli -a redis123456 --rdb /data/backup.rdb
docker cp redis:/data/backup.rdb $BACKUP_DIR/

# 备份配置文件
cp -r ./config $BACKUP_DIR/
cp docker-compose.prod.yml $BACKUP_DIR/

echo "✅ 备份完成: $BACKUP_DIR"
```

### 更新脚本

```bash
#!/bin/bash
# scripts/update.sh

set -e

echo "🔄 开始更新 WeQuant 系统..."

# 备份当前数据
./scripts/backup.sh

# 拉取最新代码
git pull origin main

# 重新构建镜像
docker-compose -f docker-compose.prod.yml build

# 滚动更新服务
./scripts/rolling-update.sh

echo "✅ 系统更新完成！"
```

---

## 🎯 部署清单

### 部署前检查

- [ ] 服务器资源充足
- [ ] 域名和 SSL 证书配置
- [ ] 数据库和 Redis 密码设置
- [ ] 环境变量配置完成
- [ ] 监控和告警配置
- [ ] 备份策略制定

### 部署步骤

1. [ ] 准备服务器环境
2. [ ] 克隆项目代码
3. [ ] 配置环境变量
4. [ ] 启动基础设施服务
5. [ ] 运行数据库迁移
6. [ ] 启动应用服务
7. [ ] 配置负载均衡
8. [ ] 设置监控告警
9. [ ] 执行健康检查
10. [ ] 配置备份计划

### 部署后验证

- [ ] 所有服务正常运行
- [ ] API 接口正常响应
- [ ] 前端页面正常访问
- [ ] 数据库连接正常
- [ ] 缓存功能正常
- [ ] 监控数据正常采集
- [ ] 日志正常输出

---

*最后更新: 2025-10-17*
*部署版本: v1.0*