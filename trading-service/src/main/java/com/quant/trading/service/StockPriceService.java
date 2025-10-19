package com.quant.trading.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockPriceService {

    private final RestTemplate restTemplate;
    private final Random random = new Random();

    /**
     * 获取股票当前价格
     */
    public BigDecimal getCurrentPrice(String stockCode) {
        try {
            // 尝试从股票服务获取实时价格
            String url = "http://localhost:8082/stock-service/api/v1/stocks/" + stockCode + "/latest";
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("close")) {
                return BigDecimal.valueOf(((Number) response.get("close")).doubleValue());
            }
        } catch (Exception e) {
            log.warn("从股票服务获取价格失败，使用模拟价格: {}", e.getMessage());
        }

        // 如果股票服务不可用，返回模拟价格
        return getSimulatedPrice(stockCode);
    }

    /**
     * 获取模拟股票价格
     */
    private BigDecimal getSimulatedPrice(String stockCode) {
        Map<String, BigDecimal> basePrices = Map.of(
            "000001.SZ", BigDecimal.valueOf(11.40),  // 平安银行
            "000002.SZ", BigDecimal.valueOf(18.20),  // 万科A
            "600036.SH", BigDecimal.valueOf(35.80),  // 招商银行
            "600519.SH", BigDecimal.valueOf(1680.00), // 贵州茅台
            "000858.SZ", BigDecimal.valueOf(128.50),  // 五粮液
            "002415.SZ", BigDecimal.valueOf(32.15)    // 海康威视
        );

        BigDecimal basePrice = basePrices.getOrDefault(stockCode, BigDecimal.valueOf(10.0));

        // 添加随机波动 ±5%
        double fluctuation = (random.nextDouble() - 0.5) * 0.1; // ±5%
        BigDecimal currentPrice = basePrice.multiply(BigDecimal.valueOf(1 + fluctuation));

        return currentPrice.setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * 获取股票信息（包含价格和涨跌幅）
     */
    public Map<String, Object> getStockInfo(String stockCode) {
        BigDecimal currentPrice = getCurrentPrice(stockCode);
        BigDecimal changePercent = BigDecimal.valueOf((random.nextDouble() - 0.5) * 10); // ±5%

        return Map.of(
            "code", stockCode,
            "price", currentPrice,
            "change_pct", changePercent.setScale(2, BigDecimal.ROUND_HALF_UP),
            "name", getStockName(stockCode)
        );
    }

    private String getStockName(String stockCode) {
        return switch (stockCode) {
            case "000001.SZ" -> "平安银行";
            case "000002.SZ" -> "万科A";
            case "600036.SH" -> "招商银行";
            case "600519.SH" -> "贵州茅台";
            case "000858.SZ" -> "五粮液";
            case "002415.SZ" -> "海康威视";
            default -> "未知股票";
        };
    }
}