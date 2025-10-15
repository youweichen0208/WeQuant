package com.quant.stock.controller;

import com.quant.stock.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查控制器
 * 提供服务状态和版本信息
 *
 * @author Quant Trading Platform
 */
@RestController
@RequestMapping("/api")
@Tag(name = "健康检查", description = "服务状态检查API")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    @Value("${spring.application.name:stock-service}")
    private String applicationName;

    @Value("${market-data.service.url}")
    private String marketDataServiceUrl;

    private final RestTemplate restTemplate;
    private final BuildProperties buildProperties;

    public HealthController(@Qualifier("marketDataRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.buildProperties = null; // BuildProperties是可选的
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查服务运行状态")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("service", applicationName);
        healthInfo.put("status", "UP");
        healthInfo.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        healthInfo.put("uptime", getUptime());

        // 检查外部依赖
        Map<String, Object> dependencies = new HashMap<>();
        dependencies.put("market_data_service", checkMarketDataService());
        healthInfo.put("dependencies", dependencies);

        return ResponseEntity.ok(ApiResponse.success(healthInfo));
    }

    /**
     * 服务信息接口
     */
    @GetMapping("/info")
    @Operation(summary = "服务信息", description = "获取服务版本和构建信息")
    public ResponseEntity<ApiResponse<Map<String, Object>>> info() {
        Map<String, Object> info = new HashMap<>();
        info.put("service", applicationName);
        info.put("description", "股票数据微服务 - 处理股票历史数据、实时数据等业务逻辑");

        if (buildProperties != null) {
            info.put("version", buildProperties.getVersion());
            info.put("group", buildProperties.getGroup());
            info.put("artifact", buildProperties.getArtifact());
            info.put("build_time", buildProperties.getTime());
        }

        info.put("java_version", System.getProperty("java.version"));
        info.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        return ResponseEntity.ok(ApiResponse.success(info));
    }

    /**
     * API概览
     */
    @GetMapping("/overview")
    @Operation(summary = "API概览", description = "获取API功能概览")
    public ResponseEntity<ApiResponse<Map<String, Object>>> overview() {
        Map<String, Object> overview = new HashMap<>();
        overview.put("service", applicationName);
        overview.put("base_path", "/api/v1/stocks");

        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("GET /{stockCode}/history", "获取股票历史数据");
        endpoints.put("GET /{stockCode}/latest", "获取股票最新数据");
        endpoints.put("GET /{stockCode}/info", "获取股票基础信息");
        endpoints.put("GET /{stockCode}/return", "计算股票收益率");
        endpoints.put("POST /batch/latest", "批量获取股票数据");
        endpoints.put("GET /{stockCode}/history/async", "异步获取股票历史数据");
        endpoints.put("GET /{stockCode}/latest/async", "异步获取股票最新数据");

        overview.put("endpoints", endpoints);
        overview.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        return ResponseEntity.ok(ApiResponse.success(overview));
    }

    /**
     * 获取服务运行时间
     */
    private String getUptime() {
        long uptimeMs = System.currentTimeMillis() -
                       java.lang.management.ManagementFactory.getRuntimeMXBean().getStartTime();
        long uptimeSeconds = uptimeMs / 1000;
        long hours = uptimeSeconds / 3600;
        long minutes = (uptimeSeconds % 3600) / 60;
        long seconds = uptimeSeconds % 60;

        return String.format("%d小时%d分钟%d秒", hours, minutes, seconds);
    }

    /**
     * 检查Market Data Service状态
     */
    private Map<String, Object> checkMarketDataService() {
        Map<String, Object> status = new HashMap<>();
        try {
            String healthUrl = marketDataServiceUrl + "/api/health";
            ResponseEntity<Map> response = restTemplate.getForEntity(healthUrl, Map.class);
            status.put("status", "UP");
            status.put("response_code", response.getStatusCodeValue());
            status.put("url", healthUrl);
            if (response.getBody() != null) {
                status.put("response", response.getBody());
            }
        } catch (Exception e) {
            status.put("status", "DOWN");
            status.put("error", e.getMessage());
            status.put("url", marketDataServiceUrl + "/api/health");
            log.warn("Market Data Service健康检查失败: {}", e.getMessage());
        }
        return status;
    }
}