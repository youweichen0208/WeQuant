package com.quant.stock.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.*;
import java.util.List;

/**
 * 批量股票查询请求 DTO
 *
 * @author Quant Trading Platform
 */
@Schema(description = "批量股票查询请求")
public class BatchStockRequest {

    @Schema(description = "股票代码列表", example = "[\"000001.SZ\", \"600519.SH\", \"300750.SZ\"]")
    @NotNull(message = "股票代码列表不能为空")
    @NotEmpty(message = "股票代码列表不能为空")
    @Size(min = 1, max = 50, message = "股票代码数量必须在1-50之间")
    private List<@NotBlank(message = "股票代码不能为空") @Pattern(regexp = "^\\d{6}\\.(SZ|SH)$", message = "股票代码格式错误") String> stockCodes;

    @Schema(description = "查询天数（仅历史数据查询时使用）", example = "30")
    @Min(value = 1, message = "查询天数不能小于1")
    @Max(value = 365, message = "查询天数不能大于365")
    private Integer days;

    @Schema(description = "查询类型：latest-最新数据，history-历史数据", example = "latest", allowableValues = {"latest", "history"})
    @NotBlank(message = "查询类型不能为空")
    @Pattern(regexp = "^(latest|history)$", message = "查询类型必须是latest或history")
    private String queryType = "latest";

    // Constructors
    public BatchStockRequest() {}

    public BatchStockRequest(List<String> stockCodes, Integer days, String queryType) {
        this.stockCodes = stockCodes;
        this.days = days;
        this.queryType = queryType;
    }

    // Getters and Setters
    public List<String> getStockCodes() {
        return stockCodes;
    }

    public void setStockCodes(List<String> stockCodes) {
        this.stockCodes = stockCodes;
    }

    public Integer getDays() {
        return days;
    }

    public void setDays(Integer days) {
        this.days = days;
    }

    public String getQueryType() {
        return queryType;
    }

    public void setQueryType(String queryType) {
        this.queryType = queryType;
    }

    /**
     * 验证请求参数
     */
    public boolean isValid() {
        return stockCodes != null && !stockCodes.isEmpty() &&
                stockCodes.size() <= 50 &&
                queryType != null && (queryType.equals("latest") || queryType.equals("history"));
    }

    /**
     * 是否为历史数据查询
     */
    public boolean isHistoryQuery() {
        return "history".equals(queryType);
    }

    /**
     * 是否为最新数据查询
     */
    public boolean isLatestQuery() {
        return "latest".equals(queryType);
    }

    /**
     * 获取有效天数（默认30天）
     */
    public int getValidDays() {
        if (days == null || days < 1) {
            return 30;
        }
        return Math.min(days, 365);
    }
}