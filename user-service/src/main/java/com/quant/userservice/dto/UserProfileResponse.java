package com.quant.userservice.dto;

import lombok.Data;
import com.quant.userservice.entity.User.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
public class UserProfileResponse {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String phoneNumber;
    private UserRole role;
    private UserStatus status;
    private BigDecimal accountBalance;
    private BigDecimal availableBalance;
    private RiskLevel riskLevel;
    private Boolean enableTrading;
    private Boolean enableNotifications;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}