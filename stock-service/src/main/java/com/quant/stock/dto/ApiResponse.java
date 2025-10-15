package com.quant.stock.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 统一 API 响应 DTO
 *
 * @author Quant Trading Platform
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "统一API响应格式")
public class ApiResponse<T> {

    @Schema(description = "响应状态码", example = "200")
    private Integer code;

    @Schema(description = "响应消息", example = "Success")
    private String message;

    @Schema(description = "响应数据")
    private T data;

    @Schema(description = "响应时间")
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    @Schema(description = "请求追踪ID", example = "req-123456789")
    @JsonProperty("trace_id")
    private String traceId;

    @Schema(description = "响应耗时(毫秒)", example = "125")
    @JsonProperty("duration_ms")
    private Long durationMs;

    public ApiResponse(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * 成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "Success", data);
    }

    /**
     * 成功响应（带消息）
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data);
    }

    /**
     * 成功响应（无数据）
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(200, "Success", null);
    }

    /**
     * 错误响应（默认500）
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(500, message, null);
    }

    /**
     * 错误响应
     */
    public static <T> ApiResponse<T> error(Integer code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    /**
     * 服务器内部错误
     */
    public static <T> ApiResponse<T> serverError(String message) {
        return new ApiResponse<>(500, message, null);
    }

    /**
     * 参数错误
     */
    public static <T> ApiResponse<T> badRequest(String message) {
        return new ApiResponse<>(400, message, null);
    }

    /**
     * 资源未找到
     */
    public static <T> ApiResponse<T> notFound(String message) {
        return new ApiResponse<>(404, message, null);
    }

    /**
     * 服务不可用
     */
    public static <T> ApiResponse<T> serviceUnavailable(String message) {
        return new ApiResponse<>(503, message, null);
    }

    /**
     * 是否成功
     */
    public boolean isSuccess() {
        return code != null && code == 200;
    }

    /**
     * 是否为错误响应
     */
    public boolean isError() {
        return code == null || code >= 400;
    }

    /**
     * 设置追踪ID
     */
    public ApiResponse<T> withTraceId(String traceId) {
        this.traceId = traceId;
        return this;
    }

    /**
     * 设置响应耗时
     */
    public ApiResponse<T> withDuration(long startTime) {
        this.durationMs = System.currentTimeMillis() - startTime;
        return this;
    }

    /**
     * 设置时间戳
     */
    public ApiResponse<T> withTimestamp() {
        this.timestamp = LocalDateTime.now();
        return this;
    }
}