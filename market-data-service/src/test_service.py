import os
import sys
from datetime import datetime
from pathlib import Path

# 添加项目根目录到Python路径
project_root = Path(__file__).parent
sys.path.insert(0, str(project_root))

from loguru import logger
from config import settings
from data_providers import TushareProvider, AKShareProvider
from kafka_producer import KafkaMessageProducer


def test_data_providers():
    """测试数据提供者"""
    logger.info("Testing data providers...")

    # 测试Tushare
    if settings.tushare_token:
        logger.info("Testing Tushare provider...")
        tushare = TushareProvider(settings.tushare_token)
        if tushare.is_available():
            logger.success("✅ Tushare provider available")
        else:
            logger.warning("❌ Tushare provider not available")
    else:
        logger.warning("⚠️ Tushare token not configured")

    # 测试AKShare
    logger.info("Testing AKShare provider...")
    akshare = AKShareProvider()
    if akshare.is_available():
        logger.success("✅ AKShare provider available")

        # 测试获取数据
        try:
            today = datetime.now().strftime('%Y-%m-%d')
            yesterday = (datetime.now() - pd.Timedelta(days=1)).strftime('%Y-%m-%d')

            data = akshare.get_stock_daily('000001.SZ', yesterday, today)
            if data:
                logger.success(f"✅ Successfully fetched {len(data)} records")
            else:
                logger.warning("⚠️ No data returned")

        except Exception as e:
            logger.error(f"❌ Error testing AKShare: {e}")
    else:
        logger.warning("❌ AKShare provider not available")


def test_kafka_connection():
    """测试Kafka连接"""
    logger.info("Testing Kafka connection...")

    producer = KafkaMessageProducer()
    if producer.is_connected():
        logger.success("✅ Kafka producer connected")

        # 测试发送消息
        test_message = {
            "test": True,
            "timestamp": datetime.now().isoformat(),
            "service": "market-data-service-test"
        }

        success = producer.send_message("test_topic", test_message)
        if success:
            logger.success("✅ Test message sent successfully")
        else:
            logger.warning("⚠️ Failed to send test message")

        producer.close()
    else:
        logger.error("❌ Kafka producer connection failed")


def main():
    """主测试函数"""
    logger.remove()
    logger.add(sys.stdout, level="INFO")

    logger.info("🧪 Starting market data service tests...")

    # 创建必要的目录
    os.makedirs('logs', exist_ok=True)

    # 测试配置
    logger.info(f"Service: {settings.service_name}")
    logger.info(f"Kafka servers: {settings.kafka_bootstrap_servers}")
    logger.info(f"Stock codes: {settings.stock_codes}")

    # 测试数据提供者
    test_data_providers()

    # 测试Kafka连接
    test_kafka_connection()

    logger.info("🎉 Tests completed!")


if __name__ == "__main__":
    # 确保pandas可用于测试
    try:
        import pandas as pd
    except ImportError:
        logger.error("pandas not found. Please install requirements.txt")
        sys.exit(1)

    main()