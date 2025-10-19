package com.quant.trading.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "positions")
public class Position {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false)
    private String stockCode;

    @Column(nullable = false)
    private String stockName;

    @Column(nullable = false)
    private Integer quantity;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal avgCost;

    @Column(precision = 10, scale = 2)
    private BigDecimal currentPrice;

    @Column(precision = 15, scale = 2)
    private BigDecimal marketValue;

    @Column(precision = 15, scale = 2)
    private BigDecimal profitLoss;

    @Column(precision = 5, scale = 2)
    private BigDecimal profitLossPct;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}