package com.quant.stock.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Market Data Service 响应错误处理器
 *
 * @author Quant Trading Platform
 */
@Slf4j
public class MarketDataResponseErrorHandler implements ResponseErrorHandler {

    @Override
    public boolean hasError(@NonNull ClientHttpResponse response) throws IOException {
        HttpStatus statusCode = response.getStatusCode();
        return statusCode.series() == HttpStatus.Series.CLIENT_ERROR ||
                statusCode.series() == HttpStatus.Series.SERVER_ERROR;
    }

    @Override
    public void handleError(@NonNull ClientHttpResponse response) throws IOException {
        HttpStatus statusCode = response.getStatusCode();
        String statusText = response.getStatusText();

        // 读取响应体
        String responseBody = readResponseBody(response);

        log.error("Market Data Service调用失败 - Status: {} {}, Response: {}",
                statusCode.value(), statusText, responseBody);

        // 根据状态码抛出不同异常
        switch (statusCode.series()) {
            case CLIENT_ERROR:
                if (statusCode == HttpStatus.NOT_FOUND) {
                    throw new MarketDataNotFoundException("数据未找到: " + responseBody);
                } else if (statusCode == HttpStatus.BAD_REQUEST) {
                    throw new MarketDataBadRequestException("请求参数错误: " + responseBody);
                } else {
                    throw new MarketDataClientException("客户端错误: " + statusCode + " " + responseBody);
                }
            case SERVER_ERROR:
                throw new MarketDataServiceException("Market Data Service服务异常: " + statusCode + " " + responseBody);
            default:
                throw new MarketDataServiceException("未知错误: " + statusCode + " " + responseBody);
        }
    }

    /**
     * 读取响应体内容
     */
    private String readResponseBody(ClientHttpResponse response) {
        try (InputStream inputStream = response.getBody()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("读取响应体失败", e);
            return "无法读取响应体";
        }
    }

    /**
     * Market Data Service 异常基类
     */
    public static class MarketDataException extends RuntimeException {
        public MarketDataException(String message) {
            super(message);
        }

        public MarketDataException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Market Data Service 服务异常
     */
    public static class MarketDataServiceException extends MarketDataException {
        public MarketDataServiceException(String message) {
            super(message);
        }

        public MarketDataServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Market Data Service 客户端异常
     */
    public static class MarketDataClientException extends MarketDataException {
        public MarketDataClientException(String message) {
            super(message);
        }
    }

    /**
     * Market Data Service 数据未找到异常
     */
    public static class MarketDataNotFoundException extends MarketDataException {
        public MarketDataNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * Market Data Service 请求参数错误异常
     */
    public static class MarketDataBadRequestException extends MarketDataException {
        public MarketDataBadRequestException(String message) {
            super(message);
        }
    }
}