package com.quant.stock.service;

import com.quant.stock.config.MarketDataResponseErrorHandler.*;
import com.quant.stock.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * 股票数据业务服务类
 * 负责调用Market Data Service获取股票数据，并提供缓存和业务逻辑处理
 *
 * @author Quant Trading Platform
 */
@Service
@Slf4j
public class StockService {

    private final RestTemplate marketDataRestTemplate;
    private final Executor taskExecutor;

    @Value("${market-data.service.url}")
    private String marketDataServiceUrl;

    @Value("${market-data.service.retry.max-attempts:3}")
    private int maxRetryAttempts;

    public StockService(@Qualifier("marketDataRestTemplate") RestTemplate marketDataRestTemplate,
                        Executor taskExecutor) {
        this.marketDataRestTemplate = marketDataRestTemplate;
        this.taskExecutor = taskExecutor;
    }

    /**
     * 获取股票历史数据（带缓存）
     *
     * @param stockCode 股票代码
     * @param days      天数
     * @return 历史数据响应
     */
    @Cacheable(value = "stockHistory", key = "#stockCode + '_' + #days")
    @Retryable(value = {MarketDataServiceException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public StockHistoryResponse getStockHistory(String stockCode, Integer days) {
        log.info("获取股票历史数据: stockCode={}, days={}", stockCode, days);

        try {
            String url = String.format("%s/api/stocks/%s/history?days=%d",
                    marketDataServiceUrl, stockCode, days);

            log.debug("调用Market Data Service: {}", url);

            ResponseEntity<StockHistoryResponse> response = marketDataRestTemplate.getForEntity(
                    url, StockHistoryResponse.class);

            StockHistoryResponse result = response.getBody();
            if (result == null) {
                throw new MarketDataServiceException("Market Data Service返回空响应");
            }

            // 数据处理和验证
            processHistoryData(result);

            log.info("成功获取股票历史数据: stockCode={}, count={}", stockCode, result.getCount());
            return result;

        } catch (MarketDataException e) {
            log.error("Market Data Service异常: stockCode={}, error={}", stockCode, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("获取股票历史数据异常: stockCode={}", stockCode, e);
            throw new MarketDataServiceException("获取股票历史数据失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取股票最新数据（带缓存）
     *
     * @param stockCode 股票代码
     * @return 最新数据响应
     */
    @Cacheable(value = "stockLatest", key = "#stockCode")
    @Retryable(value = {MarketDataServiceException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public StockLatestResponse getStockLatest(String stockCode) {
        log.info("获取股票最新数据: stockCode={}", stockCode);

        try {
            String url = String.format("%s/api/stocks/%s/latest", marketDataServiceUrl, stockCode);

            log.debug("调用Market Data Service: {}", url);

            ResponseEntity<StockLatestResponse> response = marketDataRestTemplate.getForEntity(
                    url, StockLatestResponse.class);

            StockLatestResponse result = response.getBody();
            if (result == null) {
                throw new MarketDataServiceException("Market Data Service返回空响应");
            }

            // 数据处理和验证
            processLatestData(result);

            log.info("成功获取股票最新数据: stockCode={}, tradeDate={}, close={}",
                    stockCode, result.getTradeDate(), result.getClose());
            return result;

        } catch (MarketDataException e) {
            log.error("Market Data Service异常: stockCode={}, error={}", stockCode, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("获取股票最新数据异常: stockCode={}", stockCode, e);
            throw new MarketDataServiceException("获取股票最新数据失败: " + e.getMessage(), e);
        }
    }

    /**
     * 批量获取股票最新数据
     *
     * @param request 批量查询请求
     * @return 批量查询响应
     */
    public BatchStockResponse getBatchStockData(BatchStockRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("批量获取股票数据: queryType={}, stockCodes={}", request.getQueryType(), request.getStockCodes());

        BatchStockResponse response = new BatchStockResponse();
        response.setQueryType(request.getQueryType());

        // 根据查询类型分别处理
        if (request.isLatestQuery()) {
            processBatchLatestQuery(request, response);
        } else if (request.isHistoryQuery()) {
            processBatchHistoryQuery(request, response);
        }

        response.setResponseTime(startTime);
        response.setDefaults();

        log.info("批量查询完成: successCount={}, failedCount={}, responseTime={}ms",
                response.getSuccessCount(), response.getFailedCount(), response.getResponseTimeMs());

        return response;
    }

    /**
     * 异步获取股票历史数据
     *
     * @param stockCode 股票代码
     * @param days      天数
     * @return 异步结果
     */
    public CompletableFuture<StockHistoryResponse> getStockHistoryAsync(String stockCode, Integer days) {
        return CompletableFuture.supplyAsync(() -> getStockHistory(stockCode, days), taskExecutor);
    }

    /**
     * 异步获取股票最新数据
     *
     * @param stockCode 股票代码
     * @return 异步结果
     */
    public CompletableFuture<StockLatestResponse> getStockLatestAsync(String stockCode) {
        return CompletableFuture.supplyAsync(() -> getStockLatest(stockCode), taskExecutor);
    }

    /**
     * 处理历史数据
     */
    private void processHistoryData(StockHistoryResponse response) {
        if (response.getData() != null) {
            // 数据验证和清洗
            List<StockDataPoint> validData = response.getData().stream()
                    .filter(StockDataPoint::isValid)
                    .peek(point -> {
                        // 计算衍生数据
                        point.calculateChangeAmount();
                    })
                    .collect(Collectors.toList());

            response.setData(validData);
            response.setCount(validData.size());
        }
        response.setDefaults();
    }

    /**
     * 处理最新数据
     */
    private void processLatestData(StockLatestResponse response) {
        if (!response.isValid()) {
            throw new MarketDataServiceException("股票最新数据不完整: " + response.getStockCode());
        }
        response.setDefaults();
    }

    /**
     * 处理批量最新数据查询
     */
    private void processBatchLatestQuery(BatchStockRequest request, BatchStockResponse response) {
        for (String stockCode : request.getStockCodes()) {
            try {
                StockLatestResponse latestData = getStockLatest(stockCode);
                response.addSuccessLatest(stockCode, latestData);
            } catch (Exception e) {
                log.warn("批量查询失败 - stockCode: {}, error: {}", stockCode, e.getMessage());
                response.addFailure(stockCode, e.getMessage());
            }
        }
    }

    /**
     * 处理批量历史数据查询
     */
    private void processBatchHistoryQuery(BatchStockRequest request, BatchStockResponse response) {
        int days = request.getValidDays();

        for (String stockCode : request.getStockCodes()) {
            try {
                StockHistoryResponse historyData = getStockHistory(stockCode, days);
                response.addSuccessHistory(stockCode, historyData);
            } catch (Exception e) {
                log.warn("批量查询失败 - stockCode: {}, error: {}", stockCode, e.getMessage());
                response.addFailure(stockCode, e.getMessage());
            }
        }
    }

    /**
     * 验证股票代码格式
     *
     * @param stockCode 股票代码
     * @return 是否有效
     */
    public boolean isValidStockCode(String stockCode) {
        if (stockCode == null || stockCode.trim().isEmpty()) {
            return false;
        }
        return stockCode.matches("^\\d{6}\\.(SZ|SH)$");
    }

    /**
     * 获取股票市场类型
     *
     * @param stockCode 股票代码
     * @return 市场类型
     */
    public String getMarketType(String stockCode) {
        if (!isValidStockCode(stockCode)) {
            return "UNKNOWN";
        }
        return stockCode.endsWith(".SH") ? "上交所" : "深交所";
    }

    /**
     * 计算股票收益率
     *
     * @param historyData 历史数据
     * @return 收益率（百分比）
     */
    public BigDecimal calculateReturn(StockHistoryResponse historyData) {
        if (historyData == null || historyData.getData() == null || historyData.getData().size() < 2) {
            return BigDecimal.ZERO;
        }

        List<StockDataPoint> data = historyData.getData();
        StockDataPoint latest = data.get(0);  // 最新数据
        StockDataPoint earliest = data.get(data.size() - 1);  // 最早数据

        if (latest.getClose() == null || earliest.getClose() == null ||
            earliest.getClose().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return latest.getClose().subtract(earliest.getClose())
                .divide(earliest.getClose(), 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
}