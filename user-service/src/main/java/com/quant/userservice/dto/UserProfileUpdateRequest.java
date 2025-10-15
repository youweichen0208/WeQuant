package com.quant.userservice.dto;

import lombok.Data;
import com.quant.userservice.entity.User.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Data
public class UserProfileUpdateRequest {

    @Size(max = 100, message = "姓名长度不能超过100字符")
    private String fullName;

    @Size(max = 20, message = "手机号长度不能超过20字符")
    private String phoneNumber;

    private RiskLevel riskLevel;

    @DecimalMin(value = "0.0", message = "最大日损失必须大于等于0")
    private BigDecimal maxDailyLoss;

    @DecimalMin(value = "0.01", message = "最大仓位比例必须大于0.01")
    @DecimalMax(value = "1.00", message = "最大仓位比例不能超过1.00")
    private BigDecimal maxPositionRatio;

    private Boolean enableTrading;

    private Boolean enableNotifications;
}