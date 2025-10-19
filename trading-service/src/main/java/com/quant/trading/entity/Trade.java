package com.quant.trading.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "trades")
public class Trade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String tradeId;

    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false)
    private String stockCode;

    @Column(nullable = false)
    private String stockName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradeType tradeType;

    @Column(nullable = false)
    private Integer quantity;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal price;

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(precision = 10, scale = 2)
    private BigDecimal commission = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    private TradeStatus status = TradeStatus.PENDING;

    @CreationTimestamp
    private LocalDateTime tradeTime;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum TradeType {
        BUY, SELL
    }

    public enum TradeStatus {
        PENDING, COMPLETED, CANCELLED, FAILED
    }
}