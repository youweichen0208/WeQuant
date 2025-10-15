package com.quant.stock.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 股票历史数据响应 DTO
 *
 * @author Quant Trading Platform
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "股票历史数据响应")
public class StockHistoryResponse {

    @Schema(description = "股票代码", example = "000001.SZ")
    @JsonProperty("stock_code")
    private String stockCode;

    @Schema(description = "股票名称", example = "平安银行")
    @JsonProperty("stock_name")
    private String stockName;

    @Schema(description = "数据条数", example = "30")
    private Integer count;

    @Schema(description = "开始日期", example = "2024-01-01")
    @JsonProperty("start_date")
    private String startDate;

    @Schema(description = "结束日期", example = "2024-01-30")
    @JsonProperty("end_date")
    private String endDate;

    @Schema(description = "历史数据列表")
    private List<StockDataPoint> data;

    @Schema(description = "数据获取时间")
    @JsonProperty("fetch_time")
    private LocalDateTime fetchTime;

    @Schema(description = "数据来源", example = "market-data-service")
    @JsonProperty("data_source")
    private String dataSource;

    /**
     * 获取数据条数
     */
    public Integer getCount() {
        return count;
    }

    /**
     * 设置数据条数
     */
    public void setCount(Integer count) {
        this.count = count;
    }

    /**
     * 获取第一个数据点（最新数据）
     */
    public StockDataPoint getLatestData() {
        return data != null && !data.isEmpty() ? data.get(0) : null;
    }

    /**
     * 获取最后一个数据点（最早数据）
     */
    public StockDataPoint getEarliestData() {
        return data != null && !data.isEmpty() ? data.get(data.size() - 1) : null;
    }

    /**
     * 验证响应数据
     */
    public boolean isValid() {
        return stockCode != null && !stockCode.trim().isEmpty() &&
                count != null && count >= 0 &&
                data != null && data.size() == count;
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
        if (count == null && data != null) {
            count = data.size();
        }
    }
}