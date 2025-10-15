from datetime import datetime
from typing import Dict, Any, Optional
from pydantic import BaseModel


class StockData(BaseModel):
    """股票数据模型"""
    ts_code: str  # 股票代码
    trade_date: str  # 交易日期
    open: Optional[float] = None  # 开盘价
    high: Optional[float] = None  # 最高价
    low: Optional[float] = None  # 最低价
    close: Optional[float] = None  # 收盘价
    pre_close: Optional[float] = None  # 昨收价
    change: Optional[float] = None  # 涨跌额
    pct_chg: Optional[float] = None  # 涨跌幅
    vol: Optional[float] = None  # 成交量
    amount: Optional[float] = None  # 成交额
    timestamp: datetime = None  # 数据时间戳

    def __init__(self, **data):
        if 'timestamp' not in data:
            data['timestamp'] = datetime.now()
        super().__init__(**data)


class IndexData(BaseModel):
    """指数数据模型"""
    ts_code: str  # 指数代码
    trade_date: str  # 交易日期
    close: Optional[float] = None  # 收盘点位
    open: Optional[float] = None  # 开盘点位
    high: Optional[float] = None  # 最高点位
    low: Optional[float] = None  # 最低点位
    pre_close: Optional[float] = None  # 昨收盘点位
    change: Optional[float] = None  # 涨跌点
    pct_chg: Optional[float] = None  # 涨跌幅
    vol: Optional[float] = None  # 成交量
    amount: Optional[float] = None  # 成交额
    timestamp: datetime = None

    def __init__(self, **data):
        if 'timestamp' not in data:
            data['timestamp'] = datetime.now()
        super().__init__(**data)


class RealtimeData(BaseModel):
    """实时数据模型"""
    code: str  # 代码
    name: str  # 名称
    price: float  # 当前价
    change: float  # 涨跌额
    pct_change: float  # 涨跌幅
    volume: Optional[float] = None  # 成交量
    amount: Optional[float] = None  # 成交额
    timestamp: datetime = None

    def __init__(self, **data):
        if 'timestamp' not in data:
            data['timestamp'] = datetime.now()
        super().__init__(**data)