package com.quant.trading.repository;

import com.quant.trading.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {
    List<Position> findByAccountId(String accountId);
    Optional<Position> findByAccountIdAndStockCode(String accountId, String stockCode);
}