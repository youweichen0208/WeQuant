package com.quant.trading.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 策略信号实体类
 * 记录策略生成的买卖信号
 */
@Entity
@Table(name = "strategy_signals")
@Data
public class StrategySignal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联的策略ID
     */
    @Column(nullable = false)
    private Long strategyId;

    /**
     * 股票代码
     */
    @Column(nullable = false, length = 20)
    private String stockCode;

    /**
     * 股票名称
     */
    @Column(length = 50)
    private String stockName;

    /**
     * 信号类型: BUY, SELL, HOLD
     */
    @Column(nullable = false, length = 10)
    private String signalType;

    /**
     * 信号产生时的股票价格
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * 信号强度(0-100)，可选
     */
    @Column(precision = 5, scale = 2)
    private BigDecimal signalStrength;

    /**
     * 信号原因/详情
     */
    @Column(columnDefinition = "TEXT")
    private String reason;

    /**
     * 信号生成时间
     */
    @Column(nullable = false)
    private LocalDateTime signalTime;

    /**
     * 是否已执行交易
     */
    @Column(nullable = false)
    private Boolean executed;

    /**
     * 执行时间
     */
    private LocalDateTime executedAt;

    @PrePersist
    protected void onCreate() {
        signalTime = LocalDateTime.now();
        if (executed == null) {
            executed = false;
        }
    }
}
