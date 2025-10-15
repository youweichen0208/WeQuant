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

/**
 * 股票单个数据点 DTO
 *
 * @author Quant Trading Platform
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "股票单个数据点")
public class StockDataPoint {

    @Schema(description = "交易日期", example = "2024-01-15")
    @JsonSerialize(using = LocalDateFormatter.StandardSerializer.class)
    @JsonDeserialize(using = LocalDateFormatter.CompactDeserializer.class)
    private LocalDate date;

    @Schema(description = "开盘价", example = "11.32")
    private BigDecimal open;

    @Schema(description = "最高价", example = "11.45")
    private BigDecimal high;

    @Schema(description = "最低价", example = "11.28")
    private BigDecimal low;

    @Schema(description = "收盘价", example = "11.40")
    private BigDecimal close;

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

    /**
     * 计算涨跌额
     */
    public void calculateChangeAmount() {
        if (close != null && pctChange != null) {
            this.changeAmount = close.multiply(pctChange).divide(BigDecimal.valueOf(100), 4, BigDecimal.ROUND_HALF_UP);
        }
    }

    /**
     * 验证数据完整性
     */
    public boolean isValid() {
        return date != null &&
                open != null && open.compareTo(BigDecimal.ZERO) > 0 &&
                high != null && high.compareTo(BigDecimal.ZERO) > 0 &&
                low != null && low.compareTo(BigDecimal.ZERO) > 0 &&
                close != null && close.compareTo(BigDecimal.ZERO) > 0 &&
                volume != null && volume.compareTo(BigDecimal.ZERO) >= 0;
    }
}