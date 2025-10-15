package com.quant.stock.exception;

import com.quant.stock.config.MarketDataResponseErrorHandler.*;
import com.quant.stock.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一处理控制器层抛出的异常
 *
 * @author Quant Trading Platform
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 处理Market Data Service异常
     */
    @ExceptionHandler(MarketDataServiceException.class)
    public ResponseEntity<ApiResponse<Object>> handleMarketDataServiceException(MarketDataServiceException e) {
        log.error("Market Data Service异常: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.error("数据服务异常: " + e.getMessage()));
    }

    /**
     * 处理Market Data异常
     */
    @ExceptionHandler(MarketDataException.class)
    public ResponseEntity<ApiResponse<Object>> handleMarketDataException(MarketDataException e) {
        log.error("Market Data异常: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("数据异常: " + e.getMessage()));
    }

    /**
     * 处理参数验证异常 (RequestBody)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        List<String> errors = e.getBindingResult().getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());

        log.warn("参数验证失败: {}", errors);
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("参数验证失败: " + String.join(", ", errors)));
    }

    /**
     * 处理参数绑定异常
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Object>> handleBindException(BindException e) {
        List<String> errors = e.getBindingResult().getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());

        log.warn("参数绑定异常: {}", errors);
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("参数绑定异常: " + String.join(", ", errors)));
    }

    /**
     * 处理约束违反异常 (PathVariable/RequestParam)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolationException(ConstraintViolationException e) {
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        List<String> errors = violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toList());

        log.warn("约束验证失败: {}", errors);
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("参数约束违反: " + String.join(", ", errors)));
    }

    /**
     * 处理参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        String message = String.format("参数 '%s' 类型错误，期望类型: %s",
                e.getName(), e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "unknown");

        log.warn("参数类型不匹配: {}", message);
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(message));
    }

    /**
     * 处理404异常
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoHandlerFoundException(NoHandlerFoundException e) {
        String message = String.format("请求的接口 '%s %s' 不存在", e.getHttpMethod(), e.getRequestURL());

        log.warn("404异常: {}", message);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(message));
    }

    /**
     * 处理IllegalArgument异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("参数异常: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("参数错误: " + e.getMessage()));
    }

    /**
     * 处理运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Object>> handleRuntimeException(RuntimeException e) {
        log.error("运行时异常: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("系统内部异常，请稍后重试"));
    }

    /**
     * 处理所有未捕获异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception e) {
        log.error("系统异常: {}", e.getMessage(), e);
        // 打印完整堆栈跟踪以便调试
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("系统异常: " + e.getMessage()));
    }
}