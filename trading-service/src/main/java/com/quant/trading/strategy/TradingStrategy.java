package com.quant.trading.strategy;

import com.quant.trading.entity.StrategySignal;
import java.math.BigDecimal;
import java.util.List;

/**
 * 策略接口
 * 所有交易策略都需要实现这个接口
 */
public interface TradingStrategy {

    /**
     * 生成交易信号
     *
     * @param stockCode 股票代码
     * @param prices 价格数据列表（按时间顺序，最新的在最后）
     * @param parameters 策略参数（JSON格式）
     * @return 交易信号
     */
    StrategySignal generateSignal(String stockCode, List<BigDecimal> prices, String parameters);

    /**
     * 获取策略类型
     */
    String getStrategyType();

    /**
     * 获取策略描述
     */
    String getDescription();

    /**
     * 验证策略参数是否有效
     */
    boolean validateParameters(String parameters);
}
