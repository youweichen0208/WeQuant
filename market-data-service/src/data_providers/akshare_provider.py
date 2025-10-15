from typing import List
import akshare as ak
import pandas as pd
from loguru import logger

from .base import DataProvider
from ..models import StockData, IndexData, RealtimeData


class AKShareProvider(DataProvider):
    """AKShare数据提供者"""

    def __init__(self):
        pass

    def is_available(self) -> bool:
        """检查AKShare是否可用"""
        try:
            # 测试获取股票代码列表（更稳定的接口）
            df = ak.stock_info_a_code_name()
            return not df.empty
        except Exception as e:
            logger.warning(f"AKShare not available: {e}")
            return False

    def get_stock_daily(self, ts_code: str, start_date: str, end_date: str) -> List[StockData]:
        """获取股票日线数据"""
        try:
            # 转换代码格式：000001.SZ -> 000001
            symbol = ts_code.split('.')[0]

            # 获取历史数据
            df = ak.stock_zh_a_hist(symbol=symbol, period="daily",
                                   start_date=start_date.replace('-', ''),
                                   end_date=end_date.replace('-', ''),
                                   adjust="")

            if df.empty:
                return []

            stocks = []
            for _, row in df.iterrows():
                # AKShare返回的列名可能不同，需要映射
                trade_date = row['日期'].strftime('%Y%m%d') if pd.notna(row['日期']) else ''

                stock = StockData(
                    ts_code=ts_code,
                    trade_date=trade_date,
                    open=float(row.get('开盘', 0)) if pd.notna(row.get('开盘')) else None,
                    high=float(row.get('最高', 0)) if pd.notna(row.get('最高')) else None,
                    low=float(row.get('最低', 0)) if pd.notna(row.get('最低')) else None,
                    close=float(row.get('收盘', 0)) if pd.notna(row.get('收盘')) else None,
                    vol=float(row.get('成交量', 0)) if pd.notna(row.get('成交量')) else None,
                    amount=float(row.get('成交额', 0)) if pd.notna(row.get('成交额')) else None,
                    pct_chg=float(row.get('涨跌幅', 0)) if pd.notna(row.get('涨跌幅')) else None
                )
                stocks.append(stock)

            logger.info(f"AKShare fetched {len(stocks)} daily records for {ts_code}")
            return stocks

        except Exception as e:
            logger.error(f"AKShare error fetching daily data for {ts_code}: {e}")
            return []

    def get_stock_realtime(self, ts_codes: List[str]) -> List[RealtimeData]:
        """获取股票实时数据

        注意：AKShare的实时行情接口可能因为频繁调用被限流
        建议使用Tushare作为主要数据源，或增加调用间隔
        """
        try:
            # 获取今日股票数据作为"实时"数据的替代方案
            # 因为 ak.stock_zh_a_spot() 容易被限流，返回HTML而非JSON
            from datetime import datetime
            today = datetime.now().strftime('%Y%m%d')

            realtime_data = []
            for ts_code in ts_codes:
                try:
                    symbol = ts_code.split('.')[0]
                    # 使用历史数据接口获取今日数据（更稳定）
                    df = ak.stock_zh_a_hist(
                        symbol=symbol,
                        period="daily",
                        start_date=today,
                        end_date=today,
                        adjust=""
                    )

                    if not df.empty:
                        row = df.iloc[0]
                        data = RealtimeData(
                            code=ts_code,
                            name=ts_code,  # 历史数据接口不返回名称
                            price=float(row.get('收盘', 0)) if pd.notna(row.get('收盘')) else None,
                            change=float(row.get('涨跌额', 0)) if pd.notna(row.get('涨跌额')) else None,
                            pct_change=float(row.get('涨跌幅', 0)) if pd.notna(row.get('涨跌幅')) else None,
                            volume=float(row.get('成交量', 0)) if pd.notna(row.get('成交量')) else None,
                            amount=float(row.get('成交额', 0)) if pd.notna(row.get('成交额')) else None
                        )
                        realtime_data.append(data)
                except Exception as e:
                    logger.warning(f"Failed to fetch data for {ts_code}: {e}")
                    continue

            if realtime_data:
                logger.info(f"AKShare fetched {len(realtime_data)} stock records using daily API")
            return realtime_data

        except Exception as e:
            logger.error(f"AKShare error fetching realtime data: {e}")
            return []

    def get_index_daily(self, ts_code: str, start_date: str, end_date: str) -> List[IndexData]:
        """获取指数日线数据"""
        try:
            # 指数代码映射
            index_mapping = {
                '000001.SH': 'sh000001',  # 上证指数
                '399001.SZ': 'sz399001',  # 深证成指
                '399006.SZ': 'sz399006',  # 创业板指
            }

            symbol = index_mapping.get(ts_code, ts_code.lower())

            df = ak.stock_zh_index_daily(symbol=symbol)
            if df.empty:
                return []

            # 筛选日期范围
            df['date'] = pd.to_datetime(df['date'])
            start = pd.to_datetime(start_date)
            end = pd.to_datetime(end_date)
            df = df[(df['date'] >= start) & (df['date'] <= end)]

            indices = []
            for _, row in df.iterrows():
                index = IndexData(
                    ts_code=ts_code,
                    trade_date=row['date'].strftime('%Y%m%d'),
                    close=float(row.get('close', 0)),
                    open=float(row.get('open', 0)),
                    high=float(row.get('high', 0)),
                    low=float(row.get('low', 0)),
                    vol=float(row.get('volume', 0)),
                    amount=float(row.get('amount', 0))
                )
                indices.append(index)

            logger.info(f"AKShare fetched {len(indices)} index records for {ts_code}")
            return indices

        except Exception as e:
            logger.error(f"AKShare error fetching index data for {ts_code}: {e}")
            return []