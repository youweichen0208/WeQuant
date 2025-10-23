package com.quant.trading.indicator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 技术指标计算工具类
 * 用于计算MA、MACD、RSI、BOLL等常用技术指标
 */
public class IndicatorCalculator {

    /**
     * 计算简单移动平均线 (Simple Moving Average - SMA)
     *
     * @param prices 价格数据列表（按时间顺序，最新的在最后）
     * @param period 周期（如5日、10日、20日）
     * @return MA值列表（长度与prices相同，前period-1个为null）
     */
    public static List<BigDecimal> calculateSMA(List<BigDecimal> prices, int period) {
        List<BigDecimal> result = new ArrayList<>();

        if (prices == null || prices.size() < period) {
            return result;
        }

        // 前period-1个数据不足，填充null
        for (int i = 0; i < period - 1; i++) {
            result.add(null);
        }

        // 从第period个开始计算MA
        for (int i = period - 1; i < prices.size(); i++) {
            BigDecimal sum = BigDecimal.ZERO;
            for (int j = 0; j < period; j++) {
                sum = sum.add(prices.get(i - j));
            }
            BigDecimal ma = sum.divide(BigDecimal.valueOf(period), 4, RoundingMode.HALF_UP);
            result.add(ma);
        }

        return result;
    }

    /**
     * 计算指数移动平均线 (Exponential Moving Average - EMA)
     *
     * @param prices 价格数据列表
     * @param period 周期
     * @return EMA值列表
     */
    public static List<BigDecimal> calculateEMA(List<BigDecimal> prices, int period) {
        List<BigDecimal> result = new ArrayList<>();

        if (prices == null || prices.isEmpty()) {
            return result;
        }

        // EMA的平滑系数
        BigDecimal multiplier = BigDecimal.valueOf(2.0 / (period + 1));

        // 第一个EMA值使用SMA
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < Math.min(period, prices.size()); i++) {
            sum = sum.add(prices.get(i));
        }
        BigDecimal ema = sum.divide(BigDecimal.valueOf(Math.min(period, prices.size())), 4, RoundingMode.HALF_UP);
        result.add(ema);

        // 后续使用EMA公式: EMA(t) = Price(t) * multiplier + EMA(t-1) * (1 - multiplier)
        for (int i = 1; i < prices.size(); i++) {
            ema = prices.get(i).multiply(multiplier)
                    .add(ema.multiply(BigDecimal.ONE.subtract(multiplier)));
            result.add(ema);
        }

        return result;
    }

    /**
     * 计算MACD指标 (Moving Average Convergence Divergence)
     *
     * @param prices 价格数据列表
     * @param fastPeriod 快线周期（默认12）
     * @param slowPeriod 慢线周期（默认26）
     * @param signalPeriod 信号线周期（默认9）
     * @return MACD结果对象
     */
    public static MACDResult calculateMACD(List<BigDecimal> prices, int fastPeriod, int slowPeriod, int signalPeriod) {
        // 计算快线EMA和慢线EMA
        List<BigDecimal> emaFast = calculateEMA(prices, fastPeriod);
        List<BigDecimal> emaSlow = calculateEMA(prices, slowPeriod);

        // 计算DIF（差离值）= 快线 - 慢线
        List<BigDecimal> dif = new ArrayList<>();
        for (int i = 0; i < prices.size(); i++) {
            BigDecimal difValue = emaFast.get(i).subtract(emaSlow.get(i));
            dif.add(difValue);
        }

        // 计算DEA（信号线）= DIF的EMA
        List<BigDecimal> dea = calculateEMA(dif, signalPeriod);

        // 计算MACD柱状图 = (DIF - DEA) * 2
        List<BigDecimal> histogram = new ArrayList<>();
        for (int i = 0; i < dif.size(); i++) {
            BigDecimal histValue = dif.get(i).subtract(dea.get(i)).multiply(BigDecimal.valueOf(2));
            histogram.add(histValue);
        }

        return new MACDResult(dif, dea, histogram);
    }

    /**
     * 计算RSI指标 (Relative Strength Index - 相对强弱指标)
     *
     * @param prices 价格数据列表
     * @param period 周期（默认14）
     * @return RSI值列表（范围0-100）
     */
    public static List<BigDecimal> calculateRSI(List<BigDecimal> prices, int period) {
        List<BigDecimal> result = new ArrayList<>();

        if (prices == null || prices.size() < period + 1) {
            return result;
        }

        // 计算价格变化
        List<BigDecimal> gains = new ArrayList<>();
        List<BigDecimal> losses = new ArrayList<>();

        for (int i = 1; i < prices.size(); i++) {
            BigDecimal change = prices.get(i).subtract(prices.get(i - 1));
            if (change.compareTo(BigDecimal.ZERO) > 0) {
                gains.add(change);
                losses.add(BigDecimal.ZERO);
            } else {
                gains.add(BigDecimal.ZERO);
                losses.add(change.abs());
            }
        }

        // 计算第一个RSI
        result.add(null); // 第一个数据无法计算

        BigDecimal avgGain = BigDecimal.ZERO;
        BigDecimal avgLoss = BigDecimal.ZERO;

        for (int i = 0; i < period; i++) {
            avgGain = avgGain.add(gains.get(i));
            avgLoss = avgLoss.add(losses.get(i));
        }

        avgGain = avgGain.divide(BigDecimal.valueOf(period), 4, RoundingMode.HALF_UP);
        avgLoss = avgLoss.divide(BigDecimal.valueOf(period), 4, RoundingMode.HALF_UP);

        // 计算RSI
        for (int i = period; i < gains.size(); i++) {
            if (avgLoss.compareTo(BigDecimal.ZERO) == 0) {
                result.add(BigDecimal.valueOf(100));
            } else {
                BigDecimal rs = avgGain.divide(avgLoss, 4, RoundingMode.HALF_UP);
                BigDecimal rsi = BigDecimal.valueOf(100).subtract(
                    BigDecimal.valueOf(100).divide(BigDecimal.ONE.add(rs), 2, RoundingMode.HALF_UP)
                );
                result.add(rsi);
            }

            // 更新平均涨跌幅（使用平滑方法）
            avgGain = avgGain.multiply(BigDecimal.valueOf(period - 1))
                    .add(gains.get(i))
                    .divide(BigDecimal.valueOf(period), 4, RoundingMode.HALF_UP);
            avgLoss = avgLoss.multiply(BigDecimal.valueOf(period - 1))
                    .add(losses.get(i))
                    .divide(BigDecimal.valueOf(period), 4, RoundingMode.HALF_UP);
        }

        return result;
    }

    /**
     * 计算布林带指标 (Bollinger Bands)
     *
     * @param prices 价格数据列表
     * @param period 周期（默认20）
     * @param stdDevMultiplier 标准差倍数（默认2）
     * @return 布林带结果对象
     */
    public static BollingerBandsResult calculateBollingerBands(List<BigDecimal> prices, int period, double stdDevMultiplier) {
        List<BigDecimal> middle = calculateSMA(prices, period);
        List<BigDecimal> upper = new ArrayList<>();
        List<BigDecimal> lower = new ArrayList<>();

        // 计算标准差
        for (int i = 0; i < prices.size(); i++) {
            if (i < period - 1) {
                upper.add(null);
                lower.add(null);
                continue;
            }

            // 计算period周期内的标准差
            BigDecimal mean = middle.get(i);
            BigDecimal variance = BigDecimal.ZERO;

            for (int j = 0; j < period; j++) {
                BigDecimal diff = prices.get(i - j).subtract(mean);
                variance = variance.add(diff.multiply(diff));
            }

            variance = variance.divide(BigDecimal.valueOf(period), 4, RoundingMode.HALF_UP);
            double stdDev = Math.sqrt(variance.doubleValue());
            BigDecimal stdDevBD = BigDecimal.valueOf(stdDev * stdDevMultiplier);

            upper.add(mean.add(stdDevBD));
            lower.add(mean.subtract(stdDevBD));
        }

        return new BollingerBandsResult(upper, middle, lower);
    }

    // ==================== 辅助类 ====================

    /**
     * MACD计算结果
     */
    public static class MACDResult {
        private List<BigDecimal> dif;    // 差离值（快线-慢线）
        private List<BigDecimal> dea;    // 信号线（DIF的EMA）
        private List<BigDecimal> histogram;  // MACD柱状图

        public MACDResult(List<BigDecimal> dif, List<BigDecimal> dea, List<BigDecimal> histogram) {
            this.dif = dif;
            this.dea = dea;
            this.histogram = histogram;
        }

        public List<BigDecimal> getDif() {
            return dif;
        }

        public List<BigDecimal> getDea() {
            return dea;
        }

        public List<BigDecimal> getHistogram() {
            return histogram;
        }
    }

    /**
     * 布林带计算结果
     */
    public static class BollingerBandsResult {
        private List<BigDecimal> upper;   // 上轨
        private List<BigDecimal> middle;  // 中轨（MA）
        private List<BigDecimal> lower;   // 下轨

        public BollingerBandsResult(List<BigDecimal> upper, List<BigDecimal> middle, List<BigDecimal> lower) {
            this.upper = upper;
            this.middle = middle;
            this.lower = lower;
        }

        public List<BigDecimal> getUpper() {
            return upper;
        }

        public List<BigDecimal> getMiddle() {
            return middle;
        }

        public List<BigDecimal> getLower() {
            return lower;
        }
    }
}
