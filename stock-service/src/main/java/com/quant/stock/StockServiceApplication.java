package com.quant.stock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Stock Service 主应用类
 *
 * 股票数据微服务：
 * - 处理股票历史数据查询
 * - 提供股票实时数据接口
 * - 管理股票基础信息
 * - 支持数据缓存和异步处理
 *
 * @author Quant Trading Platform
 * @version 1.0.0
 */
@SpringBootApplication
@EnableCaching
@EnableRetry
@EnableAsync
@EnableScheduling
public class StockServiceApplication {

    public static void main(String[] args) {
        printBanner();
        SpringApplication.run(StockServiceApplication.class, args);
        printStartupInfo();
    }

    /**
     * 打印启动横幅
     */
    private static void printBanner() {
        System.out.println();
        System.out.println("========================================================================================================");
        System.out.println("  ███████╗████████╗ ██████╗  ██████╗██╗  ██╗    ███████╗███████╗██████╗ ██╗   ██╗██╗ ██████╗███████╗");
        System.out.println("  ██╔════╝╚══██╔══╝██╔═══██╗██╔════╝██║ ██╔╝    ██╔════╝██╔════╝██╔══██╗██║   ██║██║██╔════╝██╔════╝");
        System.out.println("  ███████╗   ██║   ██║   ██║██║     █████╔╝     ███████╗█████╗  ██████╔╝██║   ██║██║██║     █████╗  ");
        System.out.println("  ╚════██║   ██║   ██║   ██║██║     ██╔═██╗     ╚════██║██╔══╝  ██╔══██╗╚██╗ ██╔╝██║██║     ██╔══╝  ");
        System.out.println("  ███████║   ██║   ╚██████╔╝╚██████╗██║  ██╗    ███████║███████╗██║  ██║ ╚████╔╝ ██║╚██████╗███████╗");
        System.out.println("  ╚══════╝   ╚═╝    ╚═════╝  ╚═════╝╚═╝  ╚═╝    ╚══════╝╚══════╝╚═╝  ╚═╝  ╚═══╝  ╚═╝ ╚═════╝╚══════╝");
        System.out.println("========================================================================================================");
        System.out.println("🚀 启动 Stock Service (股票数据微服务)");
        System.out.println("📈 量化交易平台 - 股票数据处理中心");
        System.out.println("========================================================================================================");
        System.out.println();
    }

    /**
     * 打印启动完成信息
     */
    private static void printStartupInfo() {
        System.out.println();
        System.out.println("========================================================================================================");
        System.out.println("✅ Stock Service 启动成功！");
        System.out.println("========================================================================================================");
        System.out.println("🌐 服务地址:");
        System.out.println("   • API Base URL: http://localhost:8082/stock-service");
        System.out.println("   • Health Check: http://localhost:8082/stock-service/actuator/health");
        System.out.println("   • API Documentation: http://localhost:8082/stock-service/swagger-ui.html");
        System.out.println("   • Metrics: http://localhost:8082/stock-service/actuator/metrics");
        System.out.println();
        System.out.println("📊 主要功能:");
        System.out.println("   • 股票历史数据查询");
        System.out.println("   • 股票实时数据获取");
        System.out.println("   • 股票基础信息管理");
        System.out.println("   • 数据缓存和优化");
        System.out.println();
        System.out.println("🔗 外部依赖:");
        System.out.println("   • Market Data Service: http://localhost:5001");
        System.out.println("   • Redis Cache: localhost:6379");
        System.out.println("   • MySQL Database: localhost:3306");
        System.out.println();
        System.out.println("📝 API 示例:");
        System.out.println("   • GET /api/v1/stocks/000001.SZ/history?days=30");
        System.out.println("   • GET /api/v1/stocks/000001.SZ/latest");
        System.out.println("   • GET /api/v1/stocks/000001.SZ/info");
        System.out.println("   • POST /api/v1/stocks/batch/latest");
        System.out.println("========================================================================================================");
        System.out.println("🎯 Ready to serve stock data requests!");
        System.out.println("========================================================================================================");
        System.out.println();
    }
}