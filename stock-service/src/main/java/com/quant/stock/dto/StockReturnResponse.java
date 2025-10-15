package com.quant.stock.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 股票收益率响应DTO
 *
 * @author Quant Trading Platform
 */
public class StockReturnResponse {

    /**
     * 股票代码
     */
    @JsonProperty("stock_code")
    private String stockCode;

    /**
     * 计算天数
     */
    @JsonProperty("days")
    private Integer days;

    /**
     * 收益率 (百分比)
     */
    @JsonProperty("return_rate")
    private BigDecimal returnRate;

    /**
     * 数据点数量
     */
    @JsonProperty("data_count")
    private Integer dataCount;

    /**
     * 年化收益率
     */
    @JsonProperty("annualized_return")
    private BigDecimal annualizedReturn;

    /**
     * 响应时间
     */
    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    public StockReturnResponse() {
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public String getStockCode() {
        return stockCode;
    }

    public void setStockCode(String stockCode) {
        this.stockCode = stockCode;
    }

    public Integer getDays() {
        return days;
    }

    public void setDays(Integer days) {
        this.days = days;
    }

    public BigDecimal getReturnRate() {
        return returnRate;
    }

    public void setReturnRate(BigDecimal returnRate) {
        this.returnRate = returnRate;
    }

    public Integer getDataCount() {
        return dataCount;
    }

    public void setDataCount(Integer dataCount) {
        this.dataCount = dataCount;
    }

    public BigDecimal getAnnualizedReturn() {
        return annualizedReturn;
    }

    public void setAnnualizedReturn(BigDecimal annualizedReturn) {
        this.annualizedReturn = annualizedReturn;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * 设置默认值
     */
    public void setDefaults() {
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
        if (this.returnRate != null && this.days != null && this.days > 0) {
            // 计算年化收益率 (简单年化)
            this.annualizedReturn = this.returnRate
                    .multiply(BigDecimal.valueOf(365))
                    .divide(BigDecimal.valueOf(this.days), 4, BigDecimal.ROUND_HALF_UP);
        }
    }

    /**
     * 检查数据是否有效
     */
    public boolean isValid() {
        return this.stockCode != null && !this.stockCode.trim().isEmpty() &&
               this.days != null && this.days > 0 &&
               this.returnRate != null;
    }
}