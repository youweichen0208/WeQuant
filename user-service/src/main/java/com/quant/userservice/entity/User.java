package com.quant.userservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    // 交易相关字段
    @Column(name = "account_balance", precision = 15, scale = 2)
    private BigDecimal accountBalance = BigDecimal.ZERO;

    @Column(name = "available_balance", precision = 15, scale = 2)
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Column(name = "frozen_balance", precision = 15, scale = 2)
    private BigDecimal frozenBalance = BigDecimal.ZERO;

    // 风险等级
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level")
    private RiskLevel riskLevel = RiskLevel.CONSERVATIVE;

    // 账户配置
    @Column(name = "max_daily_loss", precision = 10, scale = 2)
    private BigDecimal maxDailyLoss;

    @Column(name = "max_position_ratio", precision = 3, scale = 2)
    private BigDecimal maxPositionRatio = new BigDecimal("0.20"); // 默认最大单仓位比例20%

    @Column(name = "enable_trading")
    private Boolean enableTrading = true;

    @Column(name = "enable_notifications")
    private Boolean enableNotifications = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    // 枚举定义
    public enum UserRole {
        USER, ADMIN, VIP
    }

    public enum UserStatus {
        ACTIVE, INACTIVE, SUSPENDED, LOCKED
    }

    public enum RiskLevel {
        CONSERVATIVE, MODERATE, AGGRESSIVE
    }
}