from typing import Optional
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    # Tushare配置
    tushare_token: Optional[str] = None
    akshare_enabled: bool = True

    # Kafka配置
    kafka_bootstrap_servers: str = "localhost:9092"
    kafka_topic_stock_realtime: str = "stock_realtime"
    kafka_topic_stock_daily: str = "stock_daily"
    kafka_topic_index_data: str = "index_data"

    # 数据收集配置
    collect_realtime_interval: int = 60
    collect_daily_time: str = "18:00"
    stock_codes: str = "000001.SZ,000002.SZ,600000.SH"

    # 日志配置
    log_level: str = "INFO"
    log_file: str = "logs/market_data_service.log"

    # 服务配置
    service_name: str = "quant-market-data-service"
    service_port: int = 8888

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


# 全局配置实例
settings = Settings()