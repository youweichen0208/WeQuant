package com.quant.stock.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * 股票基础信息响应DTO
 *
 * @author Quant Trading Platform
 */
public class StockInfoResponse {

    /**
     * 股票代码
     */
    @JsonProperty("stock_code")
    private String stockCode;

    /**
     * 股票名称
     */
    @JsonProperty("stock_name")
    private String stockName;

    /**
     * 市场类型
     */
    @JsonProperty("market_type")
    private String marketType;

    /**
     * 行业
     */
    @JsonProperty("industry")
    private String industry;

    /**
     * 上市日期
     */
    @JsonProperty("list_date")
    private String listDate;

    /**
     * 是否有效
     */
    @JsonProperty("valid")
    private Boolean valid = true;

    /**
     * 响应时间
     */
    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    public StockInfoResponse() {
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public String getStockCode() {
        return stockCode;
    }

    public void setStockCode(String stockCode) {
        this.stockCode = stockCode;
    }

    public String getStockName() {
        return stockName;
    }

    public void setStockName(String stockName) {
        this.stockName = stockName;
    }

    public String getMarketType() {
        return marketType;
    }

    public void setMarketType(String marketType) {
        this.marketType = marketType;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getListDate() {
        return listDate;
    }

    public void setListDate(String listDate) {
        this.listDate = listDate;
    }

    public Boolean getValid() {
        return valid;
    }

    public void setValid(Boolean valid) {
        this.valid = valid;
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
        if (this.valid == null) {
            this.valid = true;
        }
    }

    /**
     * 检查数据是否有效
     */
    public boolean isValid() {
        return this.valid != null && this.valid &&
               this.stockCode != null && !this.stockCode.trim().isEmpty() &&
               this.marketType != null && !this.marketType.trim().isEmpty();
    }
}