package com.quant.trading.dto;

import com.quant.trading.entity.Position;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class AccountInfoResponse {
    private String accountId;
    private String userId;
    private BigDecimal balance;
    private BigDecimal totalAssets;
    private BigDecimal marketValue;
    private List<Position> positions;
    private Integer positionCount;

    public BigDecimal getMarketValue() {
        if (positions == null) return BigDecimal.ZERO;
        return positions.stream()
            .map(pos -> pos.getMarketValue() != null ? pos.getMarketValue() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}