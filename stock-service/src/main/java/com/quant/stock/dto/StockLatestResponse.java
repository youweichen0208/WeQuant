package com.quant.stock.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.quant.stock.config.LocalDateFormatter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 股票最新数据响应 DTO
 *
 * @author Quant Trading Platform
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "股票最新数据响应")
public class StockLatestResponse {

    @Schema(description = "股票代码", example = "000001.SZ")
    @JsonProperty("stock_code")
    private String stockCode;

    @Schema(description = "股票名称", example = "平安银行")
    @JsonProperty("stock_name")
    private String stockName;

    @Schema(description = "交易日期", example = "2024-01-15")
    @JsonProperty("trade_date")
    @JsonSerialize(using = LocalDateFormatter.StandardSerializer.class)
    @JsonDeserialize(using = LocalDateFormatter.CompactDeserializer.class)
    private LocalDate tradeDate;

    @Schema(description = "开盘价", example = "11.32")
    private BigDecimal open;

    @Schema(description = "最高价", example = "11.45")
    private BigDecimal high;

    @Schema(description = "最低价", example = "11.28")
    private BigDecimal low;

    @Schema(description = "收盘价", example = "11.40")
    private BigDecimal close;

    @Schema(description = "昨日收盘价", example = "11.34")
    @JsonProperty("pre_close")
    private BigDecimal preClose;

    @Schema(description = "成交量", example = "123456.0")
    private BigDecimal volume;

    @Schema(description = "成交额", example = "1408000.0")
    private BigDecimal amount;

    @Schema(description = "涨跌幅(%)", example = "0.53")
    @JsonProperty("pct_change")
    private BigDecimal pctChange;

    @Schema(description = "涨跌额", example = "0.06")
    @JsonProperty("change_amount")
    private BigDecimal changeAmount;

    @Schema(description = "换手率(%)", example = "0.85")
    @JsonProperty("turnover_rate")
    private BigDecimal turnoverRate;

    @Schema(description = "市盈率(TTM)", example = "8.5")
    @JsonProperty("pe_ttm")
    private BigDecimal peTtm;

    @Schema(description = "市净率", example = "0.65")
    @JsonProperty("pb_ratio")
    private BigDecimal pbRatio;

    @Schema(description = "总市值", example = "123456789.00")
    @JsonProperty("total_market_value")
    private BigDecimal totalMarketValue;

    @Schema(description = "流通市值", example = "98765432.00")
    @JsonProperty("circulation_market_value")
    private BigDecimal circulationMarketValue;

    @Schema(description = "数据获取时间")
    @JsonProperty("fetch_time")
    private LocalDateTime fetchTime;

    @Schema(description = "数据来源", example = "market-data-service")
    @JsonProperty("data_source")
    private String dataSource;

    /**
     * 计算涨跌额
     */
    public void calculateChangeAmount() {
        if (close != null && preClose != null) {
            this.changeAmount = close.subtract(preClose);
        }
    }

    /**
     * 计算涨跌幅
     */
    public void calculatePctChange() {
        if (close != null && preClose != null && preClose.compareTo(BigDecimal.ZERO) != 0) {
            if (changeAmount == null) {
                calculateChangeAmount();
            }
            if (changeAmount != null) {
                this.pctChange = changeAmount.divide(preClose, 4, BigDecimal.ROUND_HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }
        }
    }

    /**
     * 验证数据完整性
     */
    public boolean isValid() {
        return stockCode != null && !stockCode.trim().isEmpty() &&
                tradeDate != null &&
                open != null && open.compareTo(BigDecimal.ZERO) > 0 &&
                high != null && high.compareTo(BigDecimal.ZERO) > 0 &&
                low != null && low.compareTo(BigDecimal.ZERO) > 0 &&
                close != null && close.compareTo(BigDecimal.ZERO) > 0 &&
                volume != null && volume.compareTo(BigDecimal.ZERO) >= 0;
    }

    /**
     * 设置默认值
     */
    public void setDefaults() {
        if (fetchTime == null) {
            fetchTime = LocalDateTime.now();
        }
        if (dataSource == null) {
            dataSource = "market-data-service";
        }
        // 只在数据缺失时才计算衍生字段
        if (changeAmount == null && close != null && preClose != null) {
            calculateChangeAmount();
        }
        if (pctChange == null && close != null && preClose != null) {
            calculatePctChange();
        }
    }

    /**
     * 判断是否上涨
     */
    public boolean isRising() {
        return pctChange != null && pctChange.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 判断是否下跌
     */
    public boolean isFalling() {
        return pctChange != null && pctChange.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * 判断是否平盘
     */
    public boolean isFlat() {
        return pctChange != null && pctChange.compareTo(BigDecimal.ZERO) == 0;
    }
}