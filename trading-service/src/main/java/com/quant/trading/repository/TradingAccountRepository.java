package com.quant.trading.repository;

import com.quant.trading.entity.TradingAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TradingAccountRepository extends JpaRepository<TradingAccount, Long> {
    Optional<TradingAccount> findByUserId(String userId);
    Optional<TradingAccount> findByAccountId(String accountId);
}