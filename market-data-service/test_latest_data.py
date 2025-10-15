#!/usr/bin/env python3
"""测试获取最新可用数据"""

import sys
import os
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from datetime import datetime, timedelta
from src.data_providers.akshare_provider import AKShareProvider

def test_latest_data():
    """测试获取最新数据"""
    provider = AKShareProvider()

    # 获取最近10天的日期（确保能覆盖到最后一个交易日）
    today = datetime.now()
    dates = []
    for i in range(10):
        date = today - timedelta(days=i)
        dates.append(date.strftime('%Y%m%d'))

    print(f"当前时间: {today.strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"星期: {['一', '二', '三', '四', '五', '六', '日'][today.weekday()]}")
    print("=" * 60)

    # 测试获取平安银行最近的数据
    test_code = '000001.SZ'
    start_date = dates[-1]  # 10天前
    end_date = dates[0]      # 今天

    print(f"\n正在获取 {test_code} 最近10天的数据...")
    print(f"日期范围: {start_date} ~ {end_date}")
    print("-" * 60)

    daily_data = provider.get_stock_daily(test_code, start_date, end_date)

    if daily_data:
        print(f"\n✓ 成功获取 {len(daily_data)} 条记录")
        print("\n最新的5条数据:")
        print(f"{'日期':<12} {'开盘':>8} {'收盘':>8} {'涨跌幅':>8} {'成交量(手)':>12}")
        print("-" * 60)

        # 按日期倒序显示（最新的在前）
        for data in daily_data[:5]:
            date_str = f"{data.trade_date[:4]}-{data.trade_date[4:6]}-{data.trade_date[6:]}"
            print(f"{date_str:<12} {data.open:>8.2f} {data.close:>8.2f} "
                  f"{data.pct_chg:>7.2f}% {data.vol:>12.0f}")

        # 显示最后一个交易日
        latest = daily_data[0]
        latest_date = datetime.strptime(latest.trade_date, '%Y%m%d')
        days_ago = (today - latest_date).days

        print("\n" + "=" * 60)
        print(f"📊 最后交易日: {latest_date.strftime('%Y-%m-%d')} ({days_ago}天前)")
        print(f"   收盘价: ¥{latest.close:.2f}")
        print(f"   涨跌幅: {latest.pct_chg:+.2f}%")

        if days_ago == 0:
            print("\n✅ 这是今天的数据！")
        elif days_ago == 1:
            print("\n⚠️  这是昨天的数据（今天可能还未开盘）")
        elif days_ago <= 3:
            print(f"\n⚠️  最新数据是{days_ago}天前的（可能是周末或节假日）")
        else:
            print(f"\n❌ 数据已经{days_ago}天没有更新了")

    else:
        print("\n✗ 未获取到数据")

    print("\n" + "=" * 60)

if __name__ == "__main__":
    test_latest_data()
