package com.quant.stock.controller;

import com.quant.stock.dto.*;
import com.quant.stock.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

/**
 * 股票数据控制器
 * 提供股票历史数据、实时数据等REST API接口
 *
 * @author Quant Trading Platform
 */
@RestController
@RequestMapping("/api/v1/stocks")
@Tag(name = "股票数据", description = "股票历史数据和实时数据API")
public class StockController {

    private static final Logger log = LoggerFactory.getLogger(StockController.class);

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    /**
     * 获取股票历史数据
     *
     * @param stockCode 股票代码 (如: 000001.SZ)
     * @param days      查询天数 (1-365)
     * @return 股票历史数据
     */
    @GetMapping("/{stockCode}/history")
    @Operation(summary = "获取股票历史数据", description = "根据股票代码和天数获取历史数据")
    public ResponseEntity<ApiResponse<StockHistoryResponse>> getStockHistory(
            @Parameter(description = "股票代码", example = "000001.SZ")
            @PathVariable String stockCode,
            @Parameter(description = "查询天数", example = "30")
            @RequestParam(defaultValue = "30") @Min(1) @Max(365) Integer days) {

        log.info("获取股票历史数据请求: stockCode={}, days={}", stockCode, days);

        // 验证股票代码
        if (!stockService.isValidStockCode(stockCode)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("无效的股票代码格式: " + stockCode));
        }

        try {
            StockHistoryResponse data = stockService.getStockHistory(stockCode, days);
            return ResponseEntity.ok(ApiResponse.success(data));

        } catch (Exception e) {
            log.error("获取股票历史数据失败: stockCode={}, days={}", stockCode, days, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("获取股票历史数据失败: " + e.getMessage()));
        }
    }

    /**
     * 获取股票最新数据
     *
     * @param stockCode 股票代码 (如: 000001.SZ)
     * @return 股票最新数据
     */
    @GetMapping("/{stockCode}/latest")
    @Operation(summary = "获取股票最新数据", description = "获取指定股票的最新交易数据")
    public ResponseEntity<ApiResponse<StockLatestResponse>> getStockLatest(
            @Parameter(description = "股票代码", example = "000001.SZ")
            @PathVariable String stockCode) {

        log.info("获取股票最新数据请求: stockCode={}", stockCode);

        // 验证股票代码
        if (!stockService.isValidStockCode(stockCode)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("无效的股票代码格式: " + stockCode));
        }

        try {
            StockLatestResponse data = stockService.getStockLatest(stockCode);
            return ResponseEntity.ok(ApiResponse.success(data));

        } catch (Exception e) {
            log.error("获取股票最新数据失败: stockCode={}", stockCode, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("获取股票最新数据失败: " + e.getMessage()));
        }
    }

    /**
     * 获取股票基础信息
     *
     * @param stockCode 股票代码
     * @return 股票基础信息
     */
    @GetMapping("/{stockCode}/info")
    @Operation(summary = "获取股票基础信息", description = "获取股票市场类型等基础信息")
    public ResponseEntity<ApiResponse<StockInfoResponse>> getStockInfo(
            @Parameter(description = "股票代码", example = "000001.SZ")
            @PathVariable String stockCode) {

        log.info("获取股票基础信息请求: stockCode={}", stockCode);

        // 验证股票代码
        if (!stockService.isValidStockCode(stockCode)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("无效的股票代码格式: " + stockCode));
        }

        try {
            StockInfoResponse info = new StockInfoResponse();
            info.setStockCode(stockCode);
            info.setMarketType(stockService.getMarketType(stockCode));
            info.setValid(true);

            return ResponseEntity.ok(ApiResponse.success(info));

        } catch (Exception e) {
            log.error("获取股票基础信息失败: stockCode={}", stockCode, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("获取股票基础信息失败: " + e.getMessage()));
        }
    }

    /**
     * 批量获取股票数据
     *
     * @param request 批量查询请求
     * @return 批量查询结果
     */
    @PostMapping("/batch/latest")
    @Operation(summary = "批量获取股票数据", description = "批量获取多只股票的最新数据或历史数据")
    public ResponseEntity<ApiResponse<BatchStockResponse>> getBatchStockData(
            @Valid @RequestBody BatchStockRequest request) {

        log.info("批量获取股票数据请求: queryType={}, stockCodes={}",
                request.getQueryType(), request.getStockCodes());

        try {
            // 验证请求
            if (request.getStockCodes() == null || request.getStockCodes().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("股票代码列表不能为空"));
            }

            if (request.getStockCodes().size() > 100) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("单次查询股票数量不能超过100只"));
            }

            // 验证股票代码格式
            for (String stockCode : request.getStockCodes()) {
                if (!stockService.isValidStockCode(stockCode)) {
                    return ResponseEntity.badRequest()
                            .body(ApiResponse.error("无效的股票代码格式: " + stockCode));
                }
            }

            BatchStockResponse data = stockService.getBatchStockData(request);
            return ResponseEntity.ok(ApiResponse.success(data));

        } catch (Exception e) {
            log.error("批量获取股票数据失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("批量获取股票数据失败: " + e.getMessage()));
        }
    }

    /**
     * 异步获取股票历史数据
     *
     * @param stockCode 股票代码
     * @param days      查询天数
     * @return 异步结果
     */
    @GetMapping("/{stockCode}/history/async")
    @Operation(summary = "异步获取股票历史数据", description = "异步获取股票历史数据，适合大量数据查询")
    public CompletableFuture<ResponseEntity<ApiResponse<StockHistoryResponse>>> getStockHistoryAsync(
            @Parameter(description = "股票代码", example = "000001.SZ")
            @PathVariable String stockCode,
            @Parameter(description = "查询天数", example = "30")
            @RequestParam(defaultValue = "30") @Min(1) @Max(365) Integer days) {

        log.info("异步获取股票历史数据请求: stockCode={}, days={}", stockCode, days);

        // 验证股票代码
        if (!stockService.isValidStockCode(stockCode)) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.badRequest()
                            .body(ApiResponse.error("无效的股票代码格式: " + stockCode))
            );
        }

        return stockService.getStockHistoryAsync(stockCode, days)
                .thenApply(data -> ResponseEntity.ok(ApiResponse.success(data)))
                .exceptionally(e -> {
                    log.error("异步获取股票历史数据失败: stockCode={}, days={}", stockCode, days, e);
                    return ResponseEntity.internalServerError()
                            .body(ApiResponse.error("异步获取股票历史数据失败: " + e.getMessage()));
                });
    }

    /**
     * 异步获取股票最新数据
     *
     * @param stockCode 股票代码
     * @return 异步结果
     */
    @GetMapping("/{stockCode}/latest/async")
    @Operation(summary = "异步获取股票最新数据", description = "异步获取股票最新数据")
    public CompletableFuture<ResponseEntity<ApiResponse<StockLatestResponse>>> getStockLatestAsync(
            @Parameter(description = "股票代码", example = "000001.SZ")
            @PathVariable String stockCode) {

        log.info("异步获取股票最新数据请求: stockCode={}", stockCode);

        // 验证股票代码
        if (!stockService.isValidStockCode(stockCode)) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.badRequest()
                            .body(ApiResponse.error("无效的股票代码格式: " + stockCode))
            );
        }

        return stockService.getStockLatestAsync(stockCode)
                .thenApply(data -> ResponseEntity.ok(ApiResponse.success(data)))
                .exceptionally(e -> {
                    log.error("异步获取股票最新数据失败: stockCode={}", stockCode, e);
                    return ResponseEntity.internalServerError()
                            .body(ApiResponse.error("异步获取股票最新数据失败: " + e.getMessage()));
                });
    }

    /**
     * 计算股票收益率
     *
     * @param stockCode 股票代码
     * @param days      计算天数
     * @return 收益率
     */
    @GetMapping("/{stockCode}/return")
    @Operation(summary = "计算股票收益率", description = "计算指定时间段内的股票收益率")
    public ResponseEntity<ApiResponse<StockReturnResponse>> getStockReturn(
            @Parameter(description = "股票代码", example = "000001.SZ")
            @PathVariable String stockCode,
            @Parameter(description = "计算天数", example = "30")
            @RequestParam(defaultValue = "30") @Min(1) @Max(365) Integer days) {

        log.info("计算股票收益率请求: stockCode={}, days={}", stockCode, days);

        // 验证股票代码
        if (!stockService.isValidStockCode(stockCode)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("无效的股票代码格式: " + stockCode));
        }

        try {
            StockHistoryResponse historyData = stockService.getStockHistory(stockCode, days);
            BigDecimal returnRate = stockService.calculateReturn(historyData);

            StockReturnResponse response = new StockReturnResponse();
            response.setStockCode(stockCode);
            response.setDays(days);
            response.setReturnRate(returnRate);
            response.setDataCount(historyData.getCount());

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("计算股票收益率失败: stockCode={}, days={}", stockCode, days, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("计算股票收益率失败: " + e.getMessage()));
        }
    }
}