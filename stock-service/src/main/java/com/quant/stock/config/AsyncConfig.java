package com.quant.stock.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务配置
 * 配置异步执行器用于股票数据异步查询
 *
 * @author Quant Trading Platform
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    /**
     * 股票数据查询异步执行器
     */
    @Bean("taskExecutor")
    @Primary
    public Executor taskExecutor() {
        log.info("初始化异步任务执行器");

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数
        executor.setCorePoolSize(5);

        // 最大线程数
        executor.setMaxPoolSize(20);

        // 队列大小
        executor.setQueueCapacity(100);

        // 线程名前缀
        executor.setThreadNamePrefix("stock-async-");

        // 拒绝策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 线程空闲时间
        executor.setKeepAliveSeconds(60);

        // 允许核心线程超时
        executor.setAllowCoreThreadTimeOut(true);

        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 等待时间
        executor.setAwaitTerminationSeconds(60);

        // 初始化
        executor.initialize();

        log.info("异步任务执行器初始化完成 - 核心线程数: {}, 最大线程数: {}, 队列大小: {}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
    }
}