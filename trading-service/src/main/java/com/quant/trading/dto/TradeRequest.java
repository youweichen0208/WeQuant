package com.quant.trading.dto;

import com.quant.trading.entity.Trade;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TradeRequest {
    private String userId;
    private String stockCode;
    private Trade.TradeType tradeType;
    private Integer quantity;
}