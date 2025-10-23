package com.quant.trading.controller;

import com.quant.trading.entity.Strategy;
import com.quant.trading.entity.StrategySignal;
import com.quant.trading.service.StrategyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 策略控制器
 * 提供策略管理和信号生成的REST API
 */
@RestController
@RequestMapping("/api/strategy")
@CrossOrigin(origins = "*")
public class StrategyController {

    @Autowired
    private StrategyService strategyService;

    /**
     * 创建新策略
     * POST /api/strategy/create
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createStrategy(@RequestBody Map<String, Object> request) {
        try {
            String name = (String) request.get("name");
            String type = (String) request.get("type");
            String parameters = request.get("parameters") != null ?
                request.get("parameters").toString() : "{}";
            String description = (String) request.getOrDefault("description", "");
            Long userId = request.get("userId") != null ?
                ((Number) request.get("userId")).longValue() : 1L; // 默认用户ID

            Strategy strategy = strategyService.createStrategy(name, type, parameters, description, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "策略创建成功");
            response.put("strategy", strategy);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "策略创建失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 为指定股票生成交易信号
     * POST /api/strategy/{strategyId}/signal/{stockCode}
     */
    @PostMapping("/{strategyId}/signal/{stockCode}")
    public ResponseEntity<Map<String, Object>> generateSignal(
            @PathVariable Long strategyId,
            @PathVariable String stockCode) {

        try {
            StrategySignal signal = strategyService.generateSignal(strategyId, stockCode);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "信号生成成功");
            response.put("signal", signal);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "信号生成失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 批量生成信号
     * POST /api/strategy/{strategyId}/signals
     * Body: {"stockCodes": ["000001.SZ", "600036.SH"]}
     */
    @PostMapping("/{strategyId}/signals")
    public ResponseEntity<Map<String, Object>> generateSignals(
            @PathVariable Long strategyId,
            @RequestBody Map<String, Object> request) {

        try {
            @SuppressWarnings("unchecked")
            List<String> stockCodes = (List<String>) request.get("stockCodes");

            List<StrategySignal> signals = strategyService.generateSignalsForStrategy(strategyId, stockCodes);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "批量信号生成完成");
            response.put("signals", signals);
            response.put("totalCount", signals.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "批量信号生成失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 获取策略的所有信号
     * GET /api/strategy/{strategyId}/signals
     */
    @GetMapping("/{strategyId}/signals")
    public ResponseEntity<Map<String, Object>> getStrategySignals(@PathVariable Long strategyId) {
        try {
            List<StrategySignal> signals = strategyService.getStrategySignals(strategyId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("signals", signals);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "获取信号失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 获取用户的所有策略
     * GET /api/strategy/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserStrategies(@PathVariable Long userId) {
        try {
            List<Strategy> strategies = strategyService.getUserStrategies(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("strategies", strategies);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "获取策略列表失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 启动策略
     * POST /api/strategy/{strategyId}/start
     */
    @PostMapping("/{strategyId}/start")
    public ResponseEntity<Map<String, Object>> startStrategy(@PathVariable Long strategyId) {
        try {
            strategyService.startStrategy(strategyId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "策略已启动");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "启动策略失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 停止策略
     * POST /api/strategy/{strategyId}/stop
     */
    @PostMapping("/{strategyId}/stop")
    public ResponseEntity<Map<String, Object>> stopStrategy(@PathVariable Long strategyId) {
        try {
            strategyService.stopStrategy(strategyId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "策略已停止");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "停止策略失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 删除策略
     * DELETE /api/strategy/{strategyId}
     */
    @DeleteMapping("/{strategyId}")
    public ResponseEntity<Map<String, Object>> deleteStrategy(@PathVariable Long strategyId) {
        try {
            strategyService.deleteStrategy(strategyId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "策略已删除");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "删除策略失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 获取支持的策略类型列表
     * GET /api/strategy/types
     */
    @GetMapping("/types")
    public ResponseEntity<Map<String, Object>> getSupportedStrategies() {
        try {
            List<Map<String, Object>> strategies = strategyService.getSupportedStrategies();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("strategies", strategies);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "获取策略类型失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 健康检查
     * GET /api/strategy/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "strategy-service");
        return ResponseEntity.ok(response);
    }
}
