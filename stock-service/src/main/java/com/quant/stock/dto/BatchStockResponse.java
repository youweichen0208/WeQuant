package com.quant.stock.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 批量股票查询响应 DTO
 *
 * @author Quant Trading Platform
 */
@Schema(description = "批量股票查询响应")
public class BatchStockResponse {

    @Schema(description = "查询类型", example = "latest")
    @JsonProperty("query_type")
    private String queryType;

    @Schema(description = "成功查询数量", example = "8")
    @JsonProperty("success_count")
    private Integer successCount;

    @Schema(description = "失败查询数量", example = "2")
    @JsonProperty("failed_count")
    private Integer failedCount;

    @Schema(description = "总查询数量", example = "10")
    @JsonProperty("total_count")
    private Integer totalCount;

    @Schema(description = "股票最新数据结果（当queryType为latest时）")
    @JsonProperty("latest_data")
    private Map<String, StockLatestResponse> latestData;

    @Schema(description = "股票历史数据结果（当queryType为history时）")
    @JsonProperty("history_data")
    private Map<String, StockHistoryResponse> historyData;

    @Schema(description = "失败的股票代码及错误信息")
    @JsonProperty("failed_stocks")
    private Map<String, String> failedStocks;

    @Schema(description = "查询时间")
    @JsonProperty("query_time")
    private LocalDateTime queryTime;

    @Schema(description = "响应时间（毫秒）", example = "1250")
    @JsonProperty("response_time_ms")
    private Long responseTimeMs;

    public BatchStockResponse() {
        this.latestData = new HashMap<>();
        this.historyData = new HashMap<>();
        this.failedStocks = new HashMap<>();
        this.queryTime = LocalDateTime.now();
        this.successCount = 0;
        this.failedCount = 0;
        this.totalCount = 0;
    }

    // Getters and Setters
    public String getQueryType() {
        return queryType;
    }

    public void setQueryType(String queryType) {
        this.queryType = queryType;
    }

    public Integer getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(Integer successCount) {
        this.successCount = successCount;
    }

    public Integer getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(Integer failedCount) {
        this.failedCount = failedCount;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public Map<String, StockLatestResponse> getLatestData() {
        return latestData;
    }

    public void setLatestData(Map<String, StockLatestResponse> latestData) {
        this.latestData = latestData;
    }

    public Map<String, StockHistoryResponse> getHistoryData() {
        return historyData;
    }

    public void setHistoryData(Map<String, StockHistoryResponse> historyData) {
        this.historyData = historyData;
    }

    public Map<String, String> getFailedStocks() {
        return failedStocks;
    }

    public void setFailedStocks(Map<String, String> failedStocks) {
        this.failedStocks = failedStocks;
    }

    public LocalDateTime getQueryTime() {
        return queryTime;
    }

    public void setQueryTime(LocalDateTime queryTime) {
        this.queryTime = queryTime;
    }

    public Long getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(Long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }

    /**
     * 添加成功的最新数据结果
     */
    public void addSuccessLatest(String stockCode, StockLatestResponse data) {
        if (latestData == null) {
            latestData = new HashMap<>();
        }
        latestData.put(stockCode, data);
        successCount++;
        totalCount++;
    }

    /**
     * 添加成功的历史数据结果
     */
    public void addSuccessHistory(String stockCode, StockHistoryResponse data) {
        if (historyData == null) {
            historyData = new HashMap<>();
        }
        historyData.put(stockCode, data);
        successCount++;
        totalCount++;
    }

    /**
     * 添加失败结果
     */
    public void addFailure(String stockCode, String errorMessage) {
        if (failedStocks == null) {
            failedStocks = new HashMap<>();
        }
        failedStocks.put(stockCode, errorMessage);
        failedCount++;
        totalCount++;
    }

    /**
     * 获取成功率
     */
    public double getSuccessRate() {
        if (totalCount == null || totalCount == 0) {
            return 0.0;
        }
        return (double) successCount / totalCount * 100;
    }

    /**
     * 是否全部成功
     */
    public boolean isAllSuccess() {
        return failedCount != null && failedCount == 0 && successCount != null && successCount > 0;
    }

    /**
     * 是否全部失败
     */
    public boolean isAllFailed() {
        return successCount != null && successCount == 0 && failedCount != null && failedCount > 0;
    }

    /**
     * 获取成功的股票代码列表
     */
    public List<String> getSuccessStockCodes() {
        if (queryType != null && queryType.equals("latest") && latestData != null) {
            return latestData.keySet().stream().toList();
        } else if (queryType != null && queryType.equals("history") && historyData != null) {
            return historyData.keySet().stream().toList();
        }
        return List.of();
    }

    /**
     * 获取失败的股票代码列表
     */
    public List<String> getFailedStockCodes() {
        return failedStocks != null ? failedStocks.keySet().stream().toList() : List.of();
    }

    /**
     * 设置响应时间
     */
    public void setResponseTime(long startTime) {
        this.responseTimeMs = System.currentTimeMillis() - startTime;
    }

    /**
     * 设置默认值
     */
    public void setDefaults() {
        if (queryTime == null) {
            queryTime = LocalDateTime.now();
        }
        if (successCount == null) {
            successCount = 0;
        }
        if (failedCount == null) {
            failedCount = 0;
        }
        if (totalCount == null) {
            totalCount = successCount + failedCount;
        }
    }
}