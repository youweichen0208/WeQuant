package com.quant.trading.controller;

import com.quant.trading.dto.TradeRequest;
import com.quant.trading.dto.AccountInfoResponse;
import com.quant.trading.entity.TradingAccount;
import com.quant.trading.entity.Position;
import com.quant.trading.entity.Trade;
import com.quant.trading.service.TradingService;
import com.quant.trading.service.StockPriceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/trading")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TradingController {

    private final TradingService tradingService;
    private final StockPriceService stockPriceService;

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "ok",
            "service", "trading-service",
            "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * 创建模拟交易账户
     */
    @PostMapping("/account")
    public ResponseEntity<Map<String, Object>> createAccount(@RequestParam String userId) {
        try {
            TradingAccount account = tradingService.createAccount(userId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "模拟交易账户创建成功",
                "accountId", account.getAccountId(),
                "initialBalance", account.getBalance(),
                "data", account
            ));
        } catch (Exception e) {
            log.error("创建交易账户失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * 获取账户信息
     */
    @GetMapping("/account")
    public ResponseEntity<AccountInfoResponse> getAccount(@RequestParam String userId) {
        try {
            TradingAccount account = tradingService.getAccount(userId);
            List<Position> positions = tradingService.getPositions(userId);

            AccountInfoResponse response = AccountInfoResponse.builder()
                .accountId(account.getAccountId())
                .userId(account.getUserId())
                .balance(account.getBalance())
                .totalAssets(account.getTotalAssets())
                .positions(positions)
                .positionCount(positions.size())
                .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取账户信息失败", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 执行交易
     */
    @PostMapping("/trade")
    public ResponseEntity<Map<String, Object>> executeTrade(@RequestBody TradeRequest request) {
        try {
            // 获取当前股价
            Map<String, Object> stockInfo = stockPriceService.getStockInfo(request.getStockCode());
            java.math.BigDecimal currentPrice = (java.math.BigDecimal) stockInfo.get("price");

            Trade trade = tradingService.executeTrade(
                request.getUserId(),
                request.getStockCode(),
                request.getTradeType(),
                request.getQuantity(),
                currentPrice
            );

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", request.getTradeType() == Trade.TradeType.BUY ? "买入成功" : "卖出成功",
                "tradeId", trade.getTradeId(),
                "data", trade
            ));

        } catch (Exception e) {
            log.error("交易执行失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * 获取持仓列表
     */
    @GetMapping("/positions")
    public ResponseEntity<List<Position>> getPositions(@RequestParam String userId) {
        try {
            List<Position> positions = tradingService.getPositions(userId);
            return ResponseEntity.ok(positions);
        } catch (Exception e) {
            log.error("获取持仓失败", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 获取交易历史
     */
    @GetMapping("/trades")
    public ResponseEntity<Map<String, Object>> getTradeHistory(
            @RequestParam String userId,
            @RequestParam(defaultValue = "50") int limit) {
        try {
            List<Trade> trades = tradingService.getTradeHistory(userId, limit);
            return ResponseEntity.ok(Map.of(
                "trades", trades,
                "total", trades.size()
            ));
        } catch (Exception e) {
            log.error("获取交易历史失败", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 获取股票市场数据
     */
    @GetMapping("/market/{stockCode}")
    public ResponseEntity<Map<String, Object>> getMarketData(@PathVariable String stockCode) {
        try {
            Map<String, Object> stockInfo = stockPriceService.getStockInfo(stockCode);
            return ResponseEntity.ok(stockInfo);
        } catch (Exception e) {
            log.error("获取市场数据失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        }
    }
}