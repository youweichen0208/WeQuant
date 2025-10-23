package com.quant.trading.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.trading.entity.Strategy;
import com.quant.trading.entity.StrategySignal;
import com.quant.trading.repository.StrategyRepository;
import com.quant.trading.repository.StrategySignalRepository;
import com.quant.trading.strategy.MovingAverageCrossStrategy;
import com.quant.trading.strategy.TradingStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 策略服务类
 * 负责策略的创建、执行、信号生成等核心功能
 */
@Service
public class StrategyService {

    @Autowired
    private StrategyRepository strategyRepository;

    @Autowired
    private StrategySignalRepository strategySignalRepository;

    @Autowired
    private MovingAverageCrossStrategy movingAverageCrossStrategy;

    @Autowired
    private StockPriceService stockPriceService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 创建新策略
     */
    public Strategy createStrategy(String name, String type, String parameters, String description, Long userId) {
        // 验证策略类型
        TradingStrategy strategy = getStrategyByType(type);
        if (strategy == null) {
            throw new IllegalArgumentException("不支持的策略类型: " + type);
        }

        // 验证参数
        if (!strategy.validateParameters(parameters)) {
            throw new IllegalArgumentException("策略参数无效");
        }

        Strategy newStrategy = new Strategy();
        newStrategy.setName(name);
        newStrategy.setType(type);
        newStrategy.setParameters(parameters);
        newStrategy.setDescription(description);
        newStrategy.setUserId(userId);
        newStrategy.setStatus("STOPPED");

        return strategyRepository.save(newStrategy);
    }

    /**
     * 为指定股票生成交易信号
     */
    public StrategySignal generateSignal(Long strategyId, String stockCode) {
        // 获取策略配置
        Strategy strategy = strategyRepository.findById(strategyId)
            .orElseThrow(() -> new IllegalArgumentException("策略不存在: " + strategyId));

        // 获取策略实现
        TradingStrategy tradingStrategy = getStrategyByType(strategy.getType());
        if (tradingStrategy == null) {
            throw new IllegalArgumentException("不支持的策略类型: " + strategy.getType());
        }

        // 获取历史价格数据
        List<BigDecimal> prices = getHistoricalPrices(stockCode, 60); // 获取60天历史数据

        // 生成信号
        StrategySignal signal = tradingStrategy.generateSignal(stockCode, prices, strategy.getParameters());
        signal.setStrategyId(strategyId);

        // 获取股票名称
        try {
            String stockName = getStockName(stockCode);
            signal.setStockName(stockName);
        } catch (Exception e) {
            signal.setStockName(stockCode);
        }

        // 保存信号
        return strategySignalRepository.save(signal);
    }

    /**
     * 批量生成信号（为策略的所有关注股票）
     */
    public List<StrategySignal> generateSignalsForStrategy(Long strategyId, List<String> stockCodes) {
        List<StrategySignal> signals = new ArrayList<>();

        for (String stockCode : stockCodes) {
            try {
                StrategySignal signal = generateSignal(strategyId, stockCode);
                signals.add(signal);
            } catch (Exception e) {
                System.err.println("为股票 " + stockCode + " 生成信号失败: " + e.getMessage());
            }
        }

        return signals;
    }

    /**
     * 获取策略的所有信号
     */
    public List<StrategySignal> getStrategySignals(Long strategyId) {
        return strategySignalRepository.findByStrategyIdOrderBySignalTimeDesc(strategyId);
    }

    /**
     * 获取用户的所有策略
     */
    public List<Strategy> getUserStrategies(Long userId) {
        return strategyRepository.findByUserId(userId);
    }

    /**
     * 启动策略
     */
    public void startStrategy(Long strategyId) {
        Strategy strategy = strategyRepository.findById(strategyId)
            .orElseThrow(() -> new IllegalArgumentException("策略不存在"));

        strategy.setStatus("RUNNING");
        strategyRepository.save(strategy);
    }

    /**
     * 停止策略
     */
    public void stopStrategy(Long strategyId) {
        Strategy strategy = strategyRepository.findById(strategyId)
            .orElseThrow(() -> new IllegalArgumentException("策略不存在"));

        strategy.setStatus("STOPPED");
        strategyRepository.save(strategy);
    }

    /**
     * 删除策略
     */
    public void deleteStrategy(Long strategyId) {
        // 先删除相关信号
        List<StrategySignal> signals = strategySignalRepository.findByStrategyId(strategyId);
        strategySignalRepository.deleteAll(signals);

        // 删除策略
        strategyRepository.deleteById(strategyId);
    }

    /**
     * 根据策略类型获取策略实现
     */
    private TradingStrategy getStrategyByType(String type) {
        switch (type) {
            case "MA_CROSS":
                return movingAverageCrossStrategy;
            // 后续可以添加更多策略类型
            default:
                return null;
        }
    }

    /**
     * 获取历史价格数据
     */
    private List<BigDecimal> getHistoricalPrices(String stockCode, int days) {
        try {
            // 这里应该调用实际的历史数据API
            // 暂时使用模拟数据进行演示
            return generateMockPrices(stockCode, days);
        } catch (Exception e) {
            throw new RuntimeException("获取历史价格数据失败: " + e.getMessage());
        }
    }

    /**
     * 生成模拟价格数据（用于演示）
     */
    private List<BigDecimal> generateMockPrices(String stockCode, int days) {
        List<BigDecimal> prices = new ArrayList<>();
        Random random = new Random();

        // 基础价格（根据股票代码生成不同的基础价格）
        double basePrice = 10.0 + (stockCode.hashCode() % 50);

        for (int i = 0; i < days; i++) {
            // 生成随机波动（-5% 到 +5%）
            double change = (random.nextDouble() - 0.5) * 0.1;
            basePrice = basePrice * (1 + change);

            // 确保价格在合理范围内
            if (basePrice < 1.0) basePrice = 1.0;
            if (basePrice > 1000.0) basePrice = 1000.0;

            prices.add(BigDecimal.valueOf(basePrice).setScale(2, BigDecimal.ROUND_HALF_UP));
        }

        return prices;
    }

    /**
     * 获取股票名称
     */
    private String getStockName(String stockCode) {
        // 这里应该调用股票信息API获取股票名称
        // 暂时返回股票代码
        Map<String, String> stockNames = new HashMap<>();
        stockNames.put("000001.SZ", "平安银行");
        stockNames.put("000002.SZ", "万科A");
        stockNames.put("600036.SH", "招商银行");
        stockNames.put("600519.SH", "贵州茅台");

        return stockNames.getOrDefault(stockCode, stockCode);
    }

    /**
     * 获取支持的策略类型列表
     */
    public List<Map<String, Object>> getSupportedStrategies() {
        List<Map<String, Object>> strategies = new ArrayList<>();

        Map<String, Object> maCross = new HashMap<>();
        maCross.put("type", "MA_CROSS");
        maCross.put("name", "双均线交叉策略");
        maCross.put("description", "通过短期均线和长期均线的交叉判断买卖时机");
        maCross.put("parameters", Map.of(
            "shortPeriod", Map.of("type", "number", "default", 5, "description", "短期均线周期"),
            "longPeriod", Map.of("type", "number", "default", 20, "description", "长期均线周期")
        ));
        strategies.add(maCross);

        return strategies;
    }
}