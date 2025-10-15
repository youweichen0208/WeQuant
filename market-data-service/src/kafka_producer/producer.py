import json
from typing import Any, Dict
from kafka import KafkaProducer
from loguru import logger

from ..config import settings


class KafkaMessageProducer:
    """Kafka消息生产者"""

    def __init__(self):
        self.producer = None
        self._connect()

    def _connect(self):
        """连接到Kafka"""
        try:
            self.producer = KafkaProducer(
                bootstrap_servers=settings.kafka_bootstrap_servers.split(','),
                value_serializer=lambda v: json.dumps(v, ensure_ascii=False, default=str).encode('utf-8'),
                key_serializer=lambda k: k.encode('utf-8') if k else None,
                acks='all',  # 等待所有副本确认
                retries=3,   # 重试次数
                batch_size=16384,  # 批次大小
                linger_ms=1,       # 等待时间
                buffer_memory=33554432,  # 缓冲区大小
                request_timeout_ms=30000,  # 请求超时30秒
                api_version=(0, 10, 1)  # 指定API版本以加快连接
            )
            logger.info("Kafka producer connected successfully")
        except Exception as e:
            logger.error(f"Failed to connect to Kafka: {e}")
            self.producer = None

    def is_connected(self) -> bool:
        """检查是否连接"""
        return self.producer is not None

    def send_message(self, topic: str, message: Dict[str, Any], key: str = None) -> bool:
        """发送消息到Kafka"""
        if not self.is_connected():
            logger.error("Kafka producer not connected")
            return False

        try:
            future = self.producer.send(topic, value=message, key=key)
            # 可选：等待发送完成
            result = future.get(timeout=10)
            logger.debug(f"Message sent to {topic}: {result}")
            return True

        except Exception as e:
            logger.error(f"Failed to send message to {topic}: {e}")
            return False

    def send_stock_data(self, stock_data: Any, is_realtime: bool = False):
        """发送股票数据"""
        topic = settings.kafka_topic_stock_realtime if is_realtime else settings.kafka_topic_stock_daily

        # 转换为字典
        if hasattr(stock_data, 'dict'):
            message = stock_data.dict()
        else:
            message = stock_data

        # 使用股票代码作为key，确保同一股票的消息有序
        key = message.get('ts_code') or message.get('code')

        return self.send_message(topic, message, key)

    def send_index_data(self, index_data: Any):
        """发送指数数据"""
        topic = settings.kafka_topic_index_data

        if hasattr(index_data, 'dict'):
            message = index_data.dict()
        else:
            message = index_data

        key = message.get('ts_code')
        return self.send_message(topic, message, key)

    def send_batch(self, topic: str, messages: list, key_field: str = None):
        """批量发送消息"""
        if not self.is_connected():
            logger.error("Kafka producer not connected")
            return False

        success_count = 0
        for message in messages:
            key = None
            if key_field and isinstance(message, dict):
                key = message.get(key_field)
            elif hasattr(message, key_field):
                key = getattr(message, key_field, None)

            if self.send_message(topic, message, key):
                success_count += 1

        logger.info(f"Sent {success_count}/{len(messages)} messages to {topic}")
        return success_count == len(messages)

    def close(self):
        """关闭生产者"""
        if self.producer:
            self.producer.close()
            logger.info("Kafka producer closed")