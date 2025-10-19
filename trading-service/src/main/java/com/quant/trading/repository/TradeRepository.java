package com.quant.trading.repository;

import com.quant.trading.entity.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TradeRepository extends JpaRepository<Trade, Long> {
    List<Trade> findByAccountIdOrderByTradeTimeDesc(String accountId);
    List<Trade> findByAccountIdAndStockCodeOrderByTradeTimeDesc(String accountId, String stockCode);
}