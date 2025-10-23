package com.quant.trading.repository;

import com.quant.trading.entity.StrategySignal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StrategySignalRepository extends JpaRepository<StrategySignal, Long> {

    /**
     * 根据策略ID查询信号
     */
    List<StrategySignal> findByStrategyId(Long strategyId);

    /**
     * 根据策略ID和信号类型查询
     */
    List<StrategySignal> findByStrategyIdAndSignalType(Long strategyId, String signalType);

    /**
     * 查询未执行的信号
     */
    List<StrategySignal> findByExecutedFalse();

    /**
     * 根据策略ID查询最近的信号
     */
    List<StrategySignal> findByStrategyIdOrderBySignalTimeDesc(Long strategyId);

    /**
     * 根据时间范围查询信号
     */
    List<StrategySignal> findBySignalTimeBetween(LocalDateTime startTime, LocalDateTime endTime);
}
