package com.quant.trading.repository;

import com.quant.trading.entity.Strategy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StrategyRepository extends JpaRepository<Strategy, Long> {

    /**
     * 根据用户ID查询所有策略
     */
    List<Strategy> findByUserId(Long userId);

    /**
     * 根据用户ID和状态查询策略
     */
    List<Strategy> findByUserIdAndStatus(Long userId, String status);

    /**
     * 根据策略类型查询
     */
    List<Strategy> findByType(String type);
}
