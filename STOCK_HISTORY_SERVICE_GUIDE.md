# 股票历史数据服务 - 实现指南

## 📋 概述

本指南帮助你实现一个独立的**股票历史数据查询微服务**（stock-history-service），用于提供股票历史数据的查询API，供前端进行数据可视化。

## 🏗️ 系统架构

```
┌─────────────┐         ┌──────────────────┐         ┌──────────────────┐         ┌──────────┐
│             │         │                  │         │                  │         │          │
│  Web        │ HTTP    │  Stock History   │  HTTP   │  Market Data     │  API    │ AKShare  │
│  Frontend   ├────────▶│  Service (Java)  ├────────▶│  Service (Python)├────────▶│  数据源   │
│  (Vue.js)   │         │  Spring Boot     │         │  Flask API       │         │          │
│             │         │  Port: 8081      │         │  Port: 5000      │         │          │
└─────────────┘         └──────────────────┘         └──────────────────┘         └──────────┘
```

### 服务职责划分

| 服务 | 职责 | 技术栈 |
|-----|------|--------|
| **stock-history-service** | - 提供股票历史数据REST API<br>- 业务逻辑处理<br>- 数据格式转换<br>- 错误处理和日志 | Spring Boot<br>RestTemplate<br>Lombok |
| **market-data-service** | - 调用AKShare获取原始数据<br>- 数据清洗和格式化<br>- 缓存管理 | Python/Flask<br>AKShare<br>Flask-CORS |
| **web-frontend** | - 数据可视化展示<br>- 用户交互<br>- 图表渲染 | Vue.js<br>ECharts<br>Axios |

## 📦 1. 创建新的Spring Boot项目

### 项目结构

```
stock-history-service/
├── pom.xml
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── quant/
│       │           └── stockhistory/
│       │               ├── StockHistoryApplication.java
│       │               ├── controller/
│       │               │   └── StockHistoryController.java
│       │               ├── service/
│       │               │   └── StockHistoryService.java
│       │               ├── dto/
│       │               │   ├── StockHistoryResponse.java
│       │               │   ├── StockLatestResponse.java
│       │               │   └── StockDataPoint.java
│       │               ├── config/
│       │               │   └── RestTemplateConfig.java
│       │               └── exception/
│       │                   ├── StockDataNotFoundException.java
│       │                   └── MarketDataServiceException.java
│       └── resources/
│           └── application.yml
└── README.md
```

### pom.xml 依赖

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.7.14</version>
        <relativePath/>
    </parent>

    <groupId>com.quant</groupId>
    <artifactId>stock-history-service</artifactId>
    <version>1.0.0</version>
    <name>Stock History Service</name>
    <description>股票历史数据查询服务</description>

    <properties>
        <java.version>17</java.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

## 🔧 2. 配置文件

### application.yml

```yaml
server:
  port: 8081

spring:
  application:
    name: stock-history-service

# Python Market Data Service 配置
market-data:
  service:
    url: http://localhost:5000
    connection-timeout: 5000
    read-timeout: 30000

# 日志配置
logging:
  level:
    root: INFO
    com.quant.stockhistory: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

## 💾 3. DTO类设计

### StockDataPoint.java
```java
package com.quant.stockhistory.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 股票单个数据点
 */
@Data
public class StockDataPoint {
    private String date;
    private Double open;
    private Double high;
    private Double low;
    private Double close;
    private Double volume;
    private Double amount;

    @JsonProperty("pct_change")
    private Double pctChange;
}
```

### StockHistoryResponse.java
```java
package com.quant.stockhistory.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

/**
 * 股票历史数据响应
 */
@Data
public class StockHistoryResponse {
    @JsonProperty("stock_code")
    private String stockCode;

    private Integer count;
    private List<StockDataPoint> data;
}
```

### StockLatestResponse.java
```java
package com.quant.stockhistory.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 股票最新数据响应
 */
@Data
public class StockLatestResponse {
    @JsonProperty("stock_code")
    private String stockCode;

    @JsonProperty("trade_date")
    private String tradeDate;

    private Double open;
    private Double high;
    private Double low;
    private Double close;
    private Double volume;
    private Double amount;

    @JsonProperty("pct_change")
    private Double pctChange;
}
```

## ⚙️ 4. Configuration配置

### RestTemplateConfig.java
```java
package com.quant.stockhistory.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * RestTemplate配置
 */
@Configuration
public class RestTemplateConfig {

    @Value("${market-data.service.connection-timeout:5000}")
    private int connectionTimeout;

    @Value("${market-data.service.read-timeout:30000}")
    private int readTimeout;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofMillis(connectionTimeout))
                .setReadTimeout(Duration.ofMillis(readTimeout))
                .build();
    }
}
```

## 🎯 5. Service层实现

### StockHistoryService.java

这是核心业务逻辑层，需要实现以下功能：

```java
package com.quant.stockhistory.service;

import com.quant.stockhistory.dto.StockHistoryResponse;
import com.quant.stockhistory.dto.StockLatestResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * 股票历史数据服务
 */
@Service
@Slf4j
public class StockHistoryService {

    private final RestTemplate restTemplate;

    @Value("${market-data.service.url}")
    private String marketDataServiceUrl;

    public StockHistoryService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 获取股票历史数据
     *
     * @param stockCode 股票代码，如 000001.SZ
     * @param days 获取天数
     * @return 历史数据
     */
    public StockHistoryResponse getStockHistory(String stockCode, Integer days) {
        try {
            // TODO: 实现调用Python API的逻辑
            // 1. 构建URL
            // 2. 发送GET请求
            // 3. 处理响应
            // 4. 异常处理

            String url = String.format("%s/api/stocks/%s/history?days=%d",
                    marketDataServiceUrl, stockCode, days);

            log.info("Fetching stock history from: {}", url);

            ResponseEntity<StockHistoryResponse> response = restTemplate.getForEntity(
                    url, StockHistoryResponse.class);

            if (response.getBody() == null) {
                throw new RuntimeException("Empty response from market data service");
            }

            log.info("Successfully fetched {} data points for stock {}",
                    response.getBody().getCount(), stockCode);

            return response.getBody();

        } catch (Exception e) {
            log.error("Error fetching stock history for {}: {}", stockCode, e.getMessage());
            throw new RuntimeException("Failed to fetch stock history: " + e.getMessage(), e);
        }
    }

    /**
     * 获取股票最新数据
     *
     * @param stockCode 股票代码
     * @return 最新数据
     */
    public StockLatestResponse getStockLatest(String stockCode) {
        try {
            // TODO: 实现逻辑
            String url = String.format("%s/api/stocks/%s/latest",
                    marketDataServiceUrl, stockCode);

            log.info("Fetching latest stock data from: {}", url);

            ResponseEntity<StockLatestResponse> response = restTemplate.getForEntity(
                    url, StockLatestResponse.class);

            if (response.getBody() == null) {
                throw new RuntimeException("Empty response from market data service");
            }

            return response.getBody();

        } catch (Exception e) {
            log.error("Error fetching latest stock data for {}: {}", stockCode, e.getMessage());
            throw new RuntimeException("Failed to fetch latest stock data: " + e.getMessage(), e);
        }
    }
}
```

## 🌐 6. Controller层实现

### StockHistoryController.java

```java
package com.quant.stockhistory.controller;

import com.quant.stockhistory.dto.StockHistoryResponse;
import com.quant.stockhistory.dto.StockLatestResponse;
import com.quant.stockhistory.service.StockHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 股票历史数据控制器
 */
@RestController
@RequestMapping("/api/v1/stocks")
@CrossOrigin(origins = "*")
@Slf4j
public class StockHistoryController {

    @Autowired
    private StockHistoryService stockHistoryService;

    /**
     * 获取股票历史数据
     *
     * @param stockCode 股票代码，如 000001.SZ
     * @param days 获取天数，默认30天
     * @return 历史数据
     */
    @GetMapping("/{stockCode}/history")
    public ResponseEntity<StockHistoryResponse> getStockHistory(
            @PathVariable String stockCode,
            @RequestParam(defaultValue = "30") Integer days) {

        log.info("Request to get stock history: code={}, days={}", stockCode, days);

        // TODO: 添加参数验证
        // - 股票代码格式验证
        // - days范围验证（1-365）

        StockHistoryResponse response = stockHistoryService.getStockHistory(stockCode, days);
        return ResponseEntity.ok(response);
    }

    /**
     * 获取股票最新数据
     *
     * @param stockCode 股票代码
     * @return 最新数据
     */
    @GetMapping("/{stockCode}/latest")
    public ResponseEntity<StockLatestResponse> getStockLatest(
            @PathVariable String stockCode) {

        log.info("Request to get latest stock data: code={}", stockCode);

        StockLatestResponse response = stockHistoryService.getStockLatest(stockCode);
        return ResponseEntity.ok(response);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "service", "stock-history-service",
                "timestamp", System.currentTimeMillis()
        ));
    }
}
```

## 🚀 7. 主应用类

### StockHistoryApplication.java

```java
package com.quant.stockhistory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 股票历史数据服务主应用
 */
@SpringBootApplication
public class StockHistoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(StockHistoryApplication.class, args);
        System.out.println("====================================");
        System.out.println("Stock History Service Started!");
        System.out.println("Port: 8081");
        System.out.println("API: http://localhost:8081/api/v1/stocks");
        System.out.println("====================================");
    }
}
```

## 🧪 8. 测试计划

### 8.1 单元测试示例

```java
package com.quant.stockhistory.service;

import com.quant.stockhistory.dto.StockHistoryResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class StockHistoryServiceTest {

    @Autowired
    private StockHistoryService stockHistoryService;

    @Test
    void testGetStockHistory() {
        // Given
        String stockCode = "000001.SZ";
        Integer days = 7;

        // When
        StockHistoryResponse response = stockHistoryService.getStockHistory(stockCode, days);

        // Then
        assertNotNull(response);
        assertEquals(stockCode, response.getStockCode());
        assertTrue(response.getCount() > 0);
        assertNotNull(response.getData());
    }
}
```

### 8.2 手动测试步骤

1. **启动Python服务**
```bash
cd /Users/youweichen/quant-trading-platform/market-data-service
python3 api_server.py
```

2. **启动Java服务**
```bash
cd /Users/youweichen/quant-trading-platform/stock-history-service
mvn spring-boot:run
```

3. **测试API端点**

```bash
# 健康检查
curl http://localhost:8081/api/v1/stocks/health

# 获取历史数据
curl http://localhost:8081/api/v1/stocks/000001.SZ/history?days=7

# 获取最新数据
curl http://localhost:8081/api/v1/stocks/000001.SZ/latest
```

## 📝 9. API接口文档

### 9.1 获取股票历史数据

**请求**
```
GET /api/v1/stocks/{stockCode}/history?days={days}
```

**参数**
| 参数 | 类型 | 必填 | 说明 | 示例 |
|-----|------|------|------|------|
| stockCode | String | 是 | 股票代码 | 000001.SZ |
| days | Integer | 否 | 天数（默认30） | 7 |

**响应示例**
```json
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

### 9.2 获取股票最新数据

**请求**
```
GET /api/v1/stocks/{stockCode}/latest
```

**响应示例**
```json
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

## 🔍 10. 进阶优化建议

### 10.1 异常处理
```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(StockDataNotFoundException.class)
    public ResponseEntity<?> handleStockDataNotFound(StockDataNotFoundException e) {
        return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(MarketDataServiceException.class)
    public ResponseEntity<?> handleMarketDataServiceException(MarketDataServiceException e) {
        return ResponseEntity.status(503).body(Map.of("error", e.getMessage()));
    }
}
```

### 10.2 缓存机制
```java
@EnableCaching
public class StockHistoryService {

    @Cacheable(value = "stockHistory", key = "#stockCode + '_' + #days")
    public StockHistoryResponse getStockHistory(String stockCode, Integer days) {
        // ...
    }
}
```

### 10.3 参数验证
```java
@GetMapping("/{stockCode}/history")
public ResponseEntity<StockHistoryResponse> getStockHistory(
        @PathVariable @Pattern(regexp = "\\d{6}\\.(SZ|SH)") String stockCode,
        @RequestParam @Min(1) @Max(365) Integer days) {
    // ...
}
```

## 📚 11. 学习资源

- **Spring Boot官方文档**: https://spring.io/projects/spring-boot
- **RestTemplate使用指南**: https://www.baeldung.com/rest-template
- **Spring MVC最佳实践**: https://www.baeldung.com/spring-mvc

## ✅ 12. 完成检查清单

- [ ] 创建Maven项目并配置pom.xml
- [ ] 创建application.yml配置文件
- [ ] 实现所有DTO类
- [ ] 实现RestTemplateConfig配置类
- [ ] 实现StockHistoryService业务逻辑
- [ ] 实现StockHistoryController控制器
- [ ] 创建主应用类
- [ ] 编写单元测试
- [ ] 手动测试所有API端点
- [ ] 添加异常处理
- [ ] 添加日志记录
- [ ] 编写README文档

## 🎓 实现提示

1. **先实现基础功能，再添加高级特性**
2. **每完成一个类就测试一次**
3. **注意异常处理和日志记录**
4. **参考user-service的代码风格**
5. **使用Postman测试API**

祝你实现顺利！💪
