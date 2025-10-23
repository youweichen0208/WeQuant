package com.quant.trading.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 策略实体类
 */
@Entity
@Table(name = "strategies")
@Data
public class Strategy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 策略名称
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 策略类型: MA_CROSS(双均线), MACD, RSI, CUSTOM(自定义)
     */
    @Column(nullable = false, length = 50)
    private String type;

    /**
     * 策略参数(JSON格式)
     * 例如: {"shortPeriod": 5, "longPeriod": 20}
     */
    @Column(columnDefinition = "TEXT")
    private String parameters;

    /**
     * 策略描述
     */
    @Column(length = 500)
    private String description;

    /**
     * 策略状态: STOPPED, RUNNING, PAUSED
     */
    @Column(nullable = false, length = 20)
    private String status;

    /**
     * 用户ID
     */
    @Column(nullable = false)
    private Long userId;

    /**
     * 创建时间
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = "STOPPED";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
