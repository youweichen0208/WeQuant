package com.quant.trading.service;

import com.quant.trading.entity.TradingAccount;
import com.quant.trading.entity.Position;
import com.quant.trading.entity.Trade;
import com.quant.trading.repository.TradingAccountRepository;
import com.quant.trading.repository.PositionRepository;
import com.quant.trading.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradingService {

    private final TradingAccountRepository accountRepository;
    private final PositionRepository positionRepository;
    private final TradeRepository tradeRepository;
    private final StockPriceService stockPriceService;

    /**
     * 创建模拟交易账户
     */
    @Transactional
    public TradingAccount createAccount(String userId) {
        // 检查用户是否已有账户
        Optional<TradingAccount> existingAccount = accountRepository.findByUserId(userId);
        if (existingAccount.isPresent()) {
            throw new RuntimeException("用户已存在交易账户");
        }

        TradingAccount account = new TradingAccount();
        account.setAccountId(UUID.randomUUID().toString());
        account.setUserId(userId);
        account.setBalance(BigDecimal.valueOf(1000000.00)); // 100万虚拟资金
        account.setTotalAssets(BigDecimal.valueOf(1000000.00));

        return accountRepository.save(account);
    }

    /**
     * 获取账户信息
     */
    public TradingAccount getAccount(String userId) {
        Optional<TradingAccount> account = accountRepository.findByUserId(userId);
        if (account.isEmpty()) {
            throw new RuntimeException("交易账户不存在");
        }

        TradingAccount tradingAccount = account.get();
        updateAccountAssets(tradingAccount);
        return tradingAccount;
    }

    /**
     * 执行交易
     */
    @Transactional
    public Trade executeTrade(String userId, String stockCode, Trade.TradeType tradeType,
                            Integer quantity, BigDecimal price) {

        TradingAccount account = getAccount(userId);

        // 计算交易金额和手续费
        BigDecimal amount = price.multiply(BigDecimal.valueOf(quantity));
        BigDecimal commission = amount.multiply(BigDecimal.valueOf(0.0003)); // 万三手续费
        BigDecimal totalCost = tradeType == Trade.TradeType.BUY ?
            amount.add(commission) : amount.subtract(commission);

        // 执行交易逻辑
        if (tradeType == Trade.TradeType.BUY) {
            executeBuyOrder(account, stockCode, quantity, price, amount, commission);
        } else {
            executeSellOrder(account, stockCode, quantity, price, amount, commission);
        }

        // 创建交易记录
        Trade trade = new Trade();
        trade.setTradeId(UUID.randomUUID().toString());
        trade.setAccountId(account.getAccountId());
        trade.setStockCode(stockCode);
        trade.setStockName(getStockName(stockCode));
        trade.setTradeType(tradeType);
        trade.setQuantity(quantity);
        trade.setPrice(price);
        trade.setAmount(amount);
        trade.setCommission(commission);
        trade.setStatus(Trade.TradeStatus.COMPLETED);

        return tradeRepository.save(trade);
    }

    /**
     * 执行买入订单
     */
    private void executeBuyOrder(TradingAccount account, String stockCode, Integer quantity,
                               BigDecimal price, BigDecimal amount, BigDecimal commission) {

        BigDecimal totalCost = amount.add(commission);

        // 检查余额
        if (account.getBalance().compareTo(totalCost) < 0) {
            throw new RuntimeException("账户余额不足");
        }

        // 更新账户余额
        account.setBalance(account.getBalance().subtract(totalCost));
        accountRepository.save(account);

        // 更新持仓
        Optional<Position> existingPosition = positionRepository.findByAccountIdAndStockCode(
            account.getAccountId(), stockCode);

        if (existingPosition.isPresent()) {
            // 更新现有持仓
            Position position = existingPosition.get();
            int newQuantity = position.getQuantity() + quantity;
            BigDecimal newAvgCost = position.getAvgCost()
                .multiply(BigDecimal.valueOf(position.getQuantity()))
                .add(price.multiply(BigDecimal.valueOf(quantity)))
                .divide(BigDecimal.valueOf(newQuantity), 2, RoundingMode.HALF_UP);

            position.setQuantity(newQuantity);
            position.setAvgCost(newAvgCost);
            position.setCurrentPrice(price);
            position.setMarketValue(price.multiply(BigDecimal.valueOf(newQuantity)));

            positionRepository.save(position);
        } else {
            // 创建新持仓
            Position position = new Position();
            position.setAccountId(account.getAccountId());
            position.setStockCode(stockCode);
            position.setStockName(getStockName(stockCode));
            position.setQuantity(quantity);
            position.setAvgCost(price);
            position.setCurrentPrice(price);
            position.setMarketValue(price.multiply(BigDecimal.valueOf(quantity)));

            positionRepository.save(position);
        }
    }

    /**
     * 执行卖出订单
     */
    private void executeSellOrder(TradingAccount account, String stockCode, Integer quantity,
                                BigDecimal price, BigDecimal amount, BigDecimal commission) {

        // 检查持仓
        Optional<Position> position = positionRepository.findByAccountIdAndStockCode(
            account.getAccountId(), stockCode);

        if (position.isEmpty() || position.get().getQuantity() < quantity) {
            throw new RuntimeException("持仓数量不足");
        }

        BigDecimal totalReceived = amount.subtract(commission);

        // 更新账户余额
        account.setBalance(account.getBalance().add(totalReceived));
        accountRepository.save(account);

        // 更新持仓
        Position pos = position.get();
        int newQuantity = pos.getQuantity() - quantity;

        if (newQuantity == 0) {
            // 清空持仓
            positionRepository.delete(pos);
        } else {
            // 减少持仓
            pos.setQuantity(newQuantity);
            pos.setCurrentPrice(price);
            pos.setMarketValue(price.multiply(BigDecimal.valueOf(newQuantity)));
            positionRepository.save(pos);
        }
    }

    /**
     * 获取持仓列表
     */
    public List<Position> getPositions(String userId) {
        TradingAccount account = getAccount(userId);
        List<Position> positions = positionRepository.findByAccountId(account.getAccountId());

        // 更新持仓的当前价格和盈亏
        positions.forEach(this::updatePositionProfitLoss);

        return positions;
    }

    /**
     * 获取交易历史
     */
    public List<Trade> getTradeHistory(String userId, int limit) {
        TradingAccount account = getAccount(userId);
        return tradeRepository.findByAccountIdOrderByTradeTimeDesc(account.getAccountId())
            .stream()
            .limit(limit)
            .toList();
    }

    /**
     * 更新账户总资产
     */
    private void updateAccountAssets(TradingAccount account) {
        List<Position> positions = positionRepository.findByAccountId(account.getAccountId());

        BigDecimal totalMarketValue = positions.stream()
            .map(pos -> {
                updatePositionProfitLoss(pos);
                return pos.getMarketValue();
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        account.setTotalAssets(account.getBalance().add(totalMarketValue));
        accountRepository.save(account);
    }

    /**
     * 更新持仓盈亏
     */
    private void updatePositionProfitLoss(Position position) {
        try {
            BigDecimal currentPrice = stockPriceService.getCurrentPrice(position.getStockCode());
            position.setCurrentPrice(currentPrice);

            BigDecimal marketValue = currentPrice.multiply(BigDecimal.valueOf(position.getQuantity()));
            position.setMarketValue(marketValue);

            BigDecimal costValue = position.getAvgCost().multiply(BigDecimal.valueOf(position.getQuantity()));
            BigDecimal profitLoss = marketValue.subtract(costValue);
            position.setProfitLoss(profitLoss);

            if (costValue.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal profitLossPct = profitLoss.divide(costValue, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
                position.setProfitLossPct(profitLossPct);
            }

            positionRepository.save(position);
        } catch (Exception e) {
            log.warn("更新持仓价格失败: {}", e.getMessage());
        }
    }

    /**
     * 获取股票名称
     */
    private String getStockName(String stockCode) {
        // 这里可以调用股票服务获取股票名称
        // 暂时返回简单映射
        return switch (stockCode) {
            case "000001.SZ" -> "平安银行";
            case "000002.SZ" -> "万科A";
            case "600036.SH" -> "招商银行";
            case "600519.SH" -> "贵州茅台";
            case "000858.SZ" -> "五粮液";
            case "002415.SZ" -> "海康威视";
            default -> "未知股票";
        };
    }

    /**
     * 重置账户数据
     * 将用户账户恢复到初始状态：100万资金，无持仓
     */
    private void resetAccount() {
        // TODO: 实现账户重置功能
        // 1. 清空用户持仓
        // 2. 重置资金为1,000,000
        // 3. 清除交易历史（可选）
    }
}