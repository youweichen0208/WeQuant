import sys
import os
from datetime import datetime, time
from typing import List
import schedule
import time as time_module
from loguru import logger

# 添加项目根目录到Python路径
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from .config import settings
from .data_providers import TushareProvider, AKShareProvider
from .kafka_producer import KafkaMessageProducer


class DataService:
    """数据服务主类"""

    def __init__(self):
        self.setup_logging()
        self.kafka_producer = KafkaMessageProducer()
        self.setup_data_providers()
        self.stock_codes = self._parse_stock_codes()

    def setup_logging(self):
        """设置日志"""
        # 创建日志目录
        os.makedirs('logs', exist_ok=True)

        # 配置日志
        logger.remove()  # 移除默认配置
        logger.add(
            sys.stdout,
            level=settings.log_level,
            format="<green>{time:YYYY-MM-DD HH:mm:ss}</green> | <level>{level: <8}</level> | <cyan>{name}</cyan>:<cyan>{function}</cyan>:<cyan>{line}</cyan> - <level>{message}</level>"
        )
        logger.add(
            settings.log_file,
            level=settings.log_level,
            format="{time:YYYY-MM-DD HH:mm:ss} | {level: <8} | {name}:{function}:{line} - {message}",
            rotation="10 MB",
            retention="7 days"
        )

    def setup_data_providers(self):
        """设置数据提供者"""
        self.providers = []

        # Tushare提供者
        if settings.tushare_token:
            tushare_provider = TushareProvider(settings.tushare_token)
            if tushare_provider.is_available():
                self.providers.append(tushare_provider)
                logger.info("Tushare provider initialized")
            else:
                logger.warning("Tushare provider not available")
        else:
            logger.warning("Tushare token not configured")

        # AKShare提供者
        if settings.akshare_enabled:
            akshare_provider = AKShareProvider()
            if akshare_provider.is_available():
                self.providers.append(akshare_provider)
                logger.info("AKShare provider initialized")
            else:
                logger.warning("AKShare provider not available")

        if not self.providers:
            logger.error("No data providers available!")

    def _parse_stock_codes(self) -> List[str]:
        """解析股票代码列表"""
        return [code.strip() for code in settings.stock_codes.split(',') if code.strip()]

    def collect_realtime_data(self):
        """收集实时数据"""
        logger.info("Starting realtime data collection")

        for provider in self.providers:
            try:
                realtime_data = provider.get_stock_realtime(self.stock_codes)

                if realtime_data:
                    # 发送到Kafka
                    for data in realtime_data:
                        success = self.kafka_producer.send_stock_data(data, is_realtime=True)
                        if not success:
                            logger.warning(f"Failed to send realtime data for {data.code}")

                    logger.info(f"Collected {len(realtime_data)} realtime records via {provider.__class__.__name__}")
                else:
                    logger.warning(f"No realtime data from {provider.__class__.__name__}")

            except Exception as e:
                logger.error(f"Error collecting realtime data from {provider.__class__.__name__}: {e}")

    def collect_daily_data(self):
        """收集日线数据"""
        logger.info("Starting daily data collection")
        today = datetime.now().strftime('%Y%m%d')

        for provider in self.providers:
            try:
                for stock_code in self.stock_codes:
                    daily_data = provider.get_stock_daily(stock_code, today, today)

                    if daily_data:
                        # 发送到Kafka
                        for data in daily_data:
                            success = self.kafka_producer.send_stock_data(data, is_realtime=False)
                            if not success:
                                logger.warning(f"Failed to send daily data for {data.ts_code}")

                        logger.info(f"Collected {len(daily_data)} daily records for {stock_code}")

                # 收集主要指数数据
                index_codes = ['000001.SH', '399001.SZ', '399006.SZ']  # 上证指数、深证成指、创业板指
                for index_code in index_codes:
                    index_data = provider.get_index_daily(index_code, today, today)

                    if index_data:
                        for data in index_data:
                            success = self.kafka_producer.send_index_data(data)
                            if not success:
                                logger.warning(f"Failed to send index data for {data.ts_code}")

                        logger.info(f"Collected {len(index_data)} index records for {index_code}")

                # 只用第一个可用的提供者
                break

            except Exception as e:
                logger.error(f"Error collecting daily data from {provider.__class__.__name__}: {e}")

    def schedule_tasks(self):
        """调度任务"""
        # 实时数据收集
        schedule.every(settings.collect_realtime_interval).seconds.do(self.collect_realtime_data)

        # 日线数据收集（每日指定时间）
        schedule.every().day.at(settings.collect_daily_time).do(self.collect_daily_data)

        logger.info(f"Scheduled realtime collection every {settings.collect_realtime_interval} seconds")
        logger.info(f"Scheduled daily collection at {settings.collect_daily_time}")

    def run(self):
        """运行服务"""
        logger.info(f"Starting {settings.service_name}")
        logger.info(f"Stock codes: {self.stock_codes}")
        logger.info(f"Available providers: {[p.__class__.__name__ for p in self.providers]}")

        # 检查Kafka连接
        if not self.kafka_producer.is_connected():
            logger.error("Kafka producer not connected. Exiting...")
            return

        # 调度任务
        self.schedule_tasks()

        # 立即执行一次数据收集（用于测试）
        logger.info("Performing initial data collection...")
        self.collect_realtime_data()

        # 主循环
        logger.info("Service started. Press Ctrl+C to stop.")
        try:
            while True:
                schedule.run_pending()
                time_module.sleep(1)
        except KeyboardInterrupt:
            logger.info("Stopping service...")
        finally:
            self.kafka_producer.close()
            logger.info("Service stopped")


def main():
    """主函数"""
    service = DataService()
    service.run()


if __name__ == "__main__":
    main()