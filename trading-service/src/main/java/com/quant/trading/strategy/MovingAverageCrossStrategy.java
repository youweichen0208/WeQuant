package com.quant.trading.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.trading.entity.StrategySignal;
import com.quant.trading.indicator.IndicatorCalculator;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 双均线交叉策略
 *
 * 策略逻辑:
 * - 金叉(Golden Cross): 短期均线向上穿越长期均线 → 买入信号
 * - 死叉(Death Cross): 短期均线向下穿越长期均线 → 卖出信号
 *
 * 默认参数:
 * - shortPeriod: 5 (5日均线)
 * - longPeriod: 20 (20日均线)
 */
@Component
public class MovingAverageCrossStrategy implements TradingStrategy {

    private static final String STRATEGY_TYPE = "MA_CROSS";
    private static final int DEFAULT_SHORT_PERIOD = 5;
    private static final int DEFAULT_LONG_PERIOD = 20;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public StrategySignal generateSignal(String stockCode, List<BigDecimal> prices, String parameters) {
        if (prices == null || prices.size() < DEFAULT_LONG_PERIOD) {
            return createHoldSignal(stockCode, prices, "数据不足，无法计算均线");
        }

        // 解析策略参数
        int shortPeriod = DEFAULT_SHORT_PERIOD;
        int longPeriod = DEFAULT_LONG_PERIOD;

        try {
            if (parameters != null && !parameters.isEmpty()) {
                Map<String, Object> params = objectMapper.readValue(parameters, Map.class);
                if (params.containsKey("shortPeriod")) {
                    shortPeriod = (Integer) params.get("shortPeriod");
                }
                if (params.containsKey("longPeriod")) {
                    longPeriod = (Integer) params.get("longPeriod");
                }
            }
        } catch (Exception e) {
            System.err.println("解析策略参数失败: " + e.getMessage());
        }

        // 计算短期和长期均线
        List<BigDecimal> shortMA = IndicatorCalculator.calculateSMA(prices, shortPeriod);
        List<BigDecimal> longMA = IndicatorCalculator.calculateSMA(prices, longPeriod);

        // 获取当前价格和最新的均线值
        BigDecimal currentPrice = prices.get(prices.size() - 1);
        BigDecimal currentShortMA = shortMA.get(shortMA.size() - 1);
        BigDecimal currentLongMA = longMA.get(longMA.size() - 1);

        // 获取前一天的均线值（用于判断交叉）
        BigDecimal previousShortMA = shortMA.get(shortMA.size() - 2);
        BigDecimal previousLongMA = longMA.get(longMA.size() - 2);

        // 创建信号对象
        StrategySignal signal = new StrategySignal();
        signal.setStockCode(stockCode);
        signal.setPrice(currentPrice);
        signal.setSignalTime(LocalDateTime.now());

        // 判断金叉或死叉
        // 金叉: 前一天短期MA < 长期MA, 今天短期MA > 长期MA
        if (previousShortMA.compareTo(previousLongMA) < 0 &&
            currentShortMA.compareTo(currentLongMA) > 0) {

            signal.setSignalType("BUY");
            signal.setReason(String.format("金叉信号: MA%d(%.2f) 向上穿越 MA%d(%.2f)",
                shortPeriod, currentShortMA, longPeriod, currentLongMA));

            // 计算信号强度：短期MA超过长期MA的百分比
            BigDecimal crossPercent = currentShortMA.subtract(currentLongMA)
                .divide(currentLongMA, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100));
            signal.setSignalStrength(crossPercent.abs());

        }
        // 死叉: 前一天短期MA > 长期MA, 今天短期MA < 长期MA
        else if (previousShortMA.compareTo(previousLongMA) > 0 &&
                 currentShortMA.compareTo(currentLongMA) < 0) {

            signal.setSignalType("SELL");
            signal.setReason(String.format("死叉信号: MA%d(%.2f) 向下穿越 MA%d(%.2f)",
                shortPeriod, currentShortMA, longPeriod, currentLongMA));

            // 计算信号强度
            BigDecimal crossPercent = currentLongMA.subtract(currentShortMA)
                .divide(currentLongMA, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100));
            signal.setSignalStrength(crossPercent.abs());

        } else {
            // 没有交叉，持有
            signal.setSignalType("HOLD");

            String position = currentShortMA.compareTo(currentLongMA) > 0 ? "上方" : "下方";
            signal.setReason(String.format("无交叉信号: MA%d(%.2f) 在 MA%d(%.2f) %s",
                shortPeriod, currentShortMA, longPeriod, currentLongMA, position));
            signal.setSignalStrength(BigDecimal.ZERO);
        }

        signal.setExecuted(false);
        return signal;
    }

    @Override
    public String getStrategyType() {
        return STRATEGY_TYPE;
    }

    @Override
    public String getDescription() {
        return "双均线交叉策略：通过短期均线和长期均线的交叉判断买卖时机。" +
               "金叉时买入，死叉时卖出。";
    }

    @Override
    public boolean validateParameters(String parameters) {
        try {
            if (parameters == null || parameters.isEmpty()) {
                return true; // 使用默认参数
            }

            Map<String, Object> params = objectMapper.readValue(parameters, Map.class);

            if (params.containsKey("shortPeriod")) {
                int shortPeriod = (Integer) params.get("shortPeriod");
                if (shortPeriod < 2 || shortPeriod > 100) {
                    return false;
                }
            }

            if (params.containsKey("longPeriod")) {
                int longPeriod = (Integer) params.get("longPeriod");
                if (longPeriod < 2 || longPeriod > 200) {
                    return false;
                }
            }

            // 确保短期 < 长期
            int shortPeriod = params.containsKey("shortPeriod") ?
                (Integer) params.get("shortPeriod") : DEFAULT_SHORT_PERIOD;
            int longPeriod = params.containsKey("longPeriod") ?
                (Integer) params.get("longPeriod") : DEFAULT_LONG_PERIOD;

            return shortPeriod < longPeriod;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 创建持有信号
     */
    private StrategySignal createHoldSignal(String stockCode, List<BigDecimal> prices, String reason) {
        StrategySignal signal = new StrategySignal();
        signal.setStockCode(stockCode);
        signal.setSignalType("HOLD");
        signal.setReason(reason);
        signal.setSignalTime(LocalDateTime.now());
        signal.setExecuted(false);

        if (prices != null && !prices.isEmpty()) {
            signal.setPrice(prices.get(prices.size() - 1));
        } else {
            signal.setPrice(BigDecimal.ZERO);
        }

        signal.setSignalStrength(BigDecimal.ZERO);
        return signal;
    }
}
