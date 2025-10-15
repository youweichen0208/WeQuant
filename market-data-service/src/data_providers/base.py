from abc import ABC, abstractmethod
from typing import List, Optional
import pandas as pd
from ..models import StockData, IndexData, RealtimeData


class DataProvider(ABC):
    """数据提供者抽象基类"""

    @abstractmethod
    def get_stock_daily(self, ts_code: str, start_date: str, end_date: str) -> List[StockData]:
        """获取股票日线数据"""
        pass

    @abstractmethod
    def get_stock_realtime(self, ts_codes: List[str]) -> List[RealtimeData]:
        """获取股票实时数据"""
        pass

    @abstractmethod
    def get_index_daily(self, ts_code: str, start_date: str, end_date: str) -> List[IndexData]:
        """获取指数日线数据"""
        pass

    @abstractmethod
    def is_available(self) -> bool:
        """检查数据源是否可用"""
        pass