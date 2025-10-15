from typing import List, Optional
import tushare as ts
import pandas as pd
from loguru import logger

from .base import DataProvider
from ..models import StockData, IndexData, RealtimeData


class TushareProvider(DataProvider):
    """Tushare数据提供者"""

    def __init__(self, token: str):
        self.token = token
        if token:
            ts.set_token(token)
            self.pro = ts.pro_api()
        else:
            self.pro = None
            logger.warning("Tushare token not provided")

    def is_available(self) -> bool:
        """检查Tushare是否可用"""
        return self.pro is not None

    def get_stock_daily(self, ts_code: str, start_date: str, end_date: str) -> List[StockData]:
        """获取股票日线数据"""
        if not self.is_available():
            logger.error("Tushare not available")
            return []

        try:
            df = self.pro.daily(ts_code=ts_code, start_date=start_date, end_date=end_date)
            if df.empty:
                return []

            stocks = []
            for _, row in df.iterrows():
                stock = StockData(
                    ts_code=row['ts_code'],
                    trade_date=row['trade_date'],
                    open=row.get('open'),
                    high=row.get('high'),
                    low=row.get('low'),
                    close=row.get('close'),
                    pre_close=row.get('pre_close'),
                    change=row.get('change'),
                    pct_chg=row.get('pct_chg'),
                    vol=row.get('vol'),
                    amount=row.get('amount')
                )
                stocks.append(stock)

            logger.info(f"Fetched {len(stocks)} daily records for {ts_code}")
            return stocks

        except Exception as e:
            logger.error(f"Error fetching daily data for {ts_code}: {e}")
            return []

    def get_stock_realtime(self, ts_codes: List[str]) -> List[RealtimeData]:
        """获取股票实时数据"""
        if not self.is_available():
            logger.error("Tushare not available")
            return []

        try:
            # Tushare实时数据需要特定接口
            realtime_data = []
            for code in ts_codes:
                # 这里使用Tushare的实时数据接口
                # 注意：实际使用时需要根据Tushare的具体API调整
                df = ts.get_realtime_quotes(code)
                if not df.empty:
                    row = df.iloc[0]
                    data = RealtimeData(
                        code=code,
                        name=row.get('name', ''),
                        price=float(row.get('price', 0)),
                        change=float(row.get('change', 0)),
                        pct_change=float(row.get('changepercent', 0))
                    )
                    realtime_data.append(data)

            logger.info(f"Fetched realtime data for {len(realtime_data)} stocks")
            return realtime_data

        except Exception as e:
            logger.error(f"Error fetching realtime data: {e}")
            return []

    def get_index_daily(self, ts_code: str, start_date: str, end_date: str) -> List[IndexData]:
        """获取指数日线数据"""
        if not self.is_available():
            logger.error("Tushare not available")
            return []

        try:
            df = self.pro.index_daily(ts_code=ts_code, start_date=start_date, end_date=end_date)
            if df.empty:
                return []

            indices = []
            for _, row in df.iterrows():
                index = IndexData(
                    ts_code=row['ts_code'],
                    trade_date=row['trade_date'],
                    close=row.get('close'),
                    open=row.get('open'),
                    high=row.get('high'),
                    low=row.get('low'),
                    pre_close=row.get('pre_close'),
                    change=row.get('change'),
                    pct_chg=row.get('pct_chg'),
                    vol=row.get('vol'),
                    amount=row.get('amount')
                )
                indices.append(index)

            logger.info(f"Fetched {len(indices)} index records for {ts_code}")
            return indices

        except Exception as e:
            logger.error(f"Error fetching index data for {ts_code}: {e}")
            return []