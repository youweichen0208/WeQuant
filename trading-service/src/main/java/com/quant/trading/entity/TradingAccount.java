package com.quant.trading.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "trading_accounts")
public class TradingAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String accountId;

    @Column(nullable = false)
    private String userId;

    @Column(precision = 15, scale = 2)
    private BigDecimal balance = BigDecimal.valueOf(1000000.00); // 100万虚拟资金

    @Column(precision = 15, scale = 2)
    private BigDecimal totalAssets = BigDecimal.valueOf(1000000.00);

    @Column(precision = 15, scale = 2)
    private BigDecimal frozenAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    private AccountStatus status = AccountStatus.ACTIVE;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum AccountStatus {
        ACTIVE, FROZEN, CLOSED
    }
}