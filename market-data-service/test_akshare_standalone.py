#!/usr/bin/env python3
"""
独立测试脚本 - 可直接在terminal运行
测试AKShare数据提供者功能
"""

import sys
import os

# 添加项目路径
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import akshare as ak
import pandas as pd
from datetime import datetime, timedelta

def test_akshare_directly():
    """直接测试AKShare API"""
    print("=" * 70)
    print("📊 AKShare 数据提供者测试")
    print("=" * 70)

    # 1. 获取最近一个交易日的数据（而非实时数据）
    print("\n1️⃣  获取最近交易日的股票数据...")
    try:
        # 获取最近10天，确保能覆盖到最后一个交易日
        today = datetime.now()
        dates = []
        for i in range(10):
            date = today - timedelta(days=i)
            dates.append(date.strftime('%Y%m%d'))

        start_date = dates[-1]  # 10天前
        end_date = dates[0]      # 今天

        # 测试几只代表性股票
        sample_stocks = ['000001', '600519', '000002']
        latest_data = []

        for symbol in sample_stocks:
            try:
                df = ak.stock_zh_a_hist(
                    symbol=symbol,
                    period="daily",
                    start_date=start_date,
                    end_date=end_date,
                    adjust=""
                )
                if not df.empty:
                    latest = df.iloc[0]  # 最新的一天
                    latest_data.append({
                        '代码': symbol,
                        '名称': latest.get('股票代码', symbol),
                        '日期': latest['日期'].strftime('%Y-%m-%d'),
                        '收盘': f"¥{latest['收盘']:.2f}",
                        '涨跌幅': f"{latest['涨跌幅']:.2f}%"
                    })
            except:
                continue

        if latest_data:
            print(f"   ✓ 成功获取 {len(latest_data)} 只股票最近交易日数据")
            print("\n   最近交易日数据:")
            for stock in latest_data:
                print(f"      {stock['代码']:>6} | {stock['日期']} | {stock['收盘']:>10} | {stock['涨跌幅']:>8}")
        else:
            print("   ⚠️  未获取到数据")
    except Exception as e:
        print(f"   ⚠️  获取失败: {str(e)[:80]}...")

    # 2. 测试获取历史数据
    print("\n2️⃣  测试获取股票历史数据...")
    test_stocks = [
        ('000001', '平安银行'),
        ('600519', '贵州茅台'),
    ]

    # 获取最近5个交易日
    today = datetime.now()
    dates_to_try = []
    for i in range(10):
        date = today - timedelta(days=i)
        dates_to_try.append(date.strftime('%Y%m%d'))

    start_date = dates_to_try[-1]
    end_date = dates_to_try[0]

    for symbol, name in test_stocks:
        print(f"\n   📈 {symbol} ({name})")
        try:
            df = ak.stock_zh_a_hist(
                symbol=symbol,
                period="daily",
                start_date=start_date,
                end_date=end_date,
                adjust=""
            )

            if not df.empty:
                print(f"      ✓ 获取 {len(df)} 条记录")

                # 显示最新数据
                latest = df.iloc[0]
                print(f"      最新: {latest['日期'].strftime('%Y-%m-%d')}")
                print(f"      开盘: ¥{latest['开盘']:.2f}")
                print(f"      收盘: ¥{latest['收盘']:.2f}")
                print(f"      涨跌: {latest['涨跌幅']:.2f}%")
            else:
                print(f"      ⚠️  期间无数据")

        except Exception as e:
            print(f"      ✗ 失败: {e}")

    # 3. 测试获取指数数据
    print("\n3️⃣  测试获取指数数据...")
    indices = [
        ('sh000001', '上证指数'),
        ('sz399001', '深证成指'),
    ]

    for symbol, name in indices:
        print(f"\n   📊 {name}")
        try:
            df = ak.stock_zh_index_daily(symbol=symbol)
            if not df.empty:
                # 筛选日期
                df['date'] = pd.to_datetime(df['date'])
                start = pd.to_datetime(start_date)
                end = pd.to_datetime(end_date)
                df_filtered = df[(df['date'] >= start) & (df['date'] <= end)]

                if not df_filtered.empty:
                    print(f"      ✓ 获取 {len(df_filtered)} 条记录")
                    latest = df_filtered.iloc[0]
                    print(f"      最新: {latest['date'].strftime('%Y-%m-%d')}")
                    print(f"      收盘: {latest['close']:.2f}")
                else:
                    print(f"      ⚠️  期间无数据")
            else:
                print(f"      ⚠️  无数据")
        except Exception as e:
            print(f"      ✗ 失败: {e}")

    print("\n" + "=" * 70)
    print("✅ 测试完成！")
    print("\n💡 提示:")
    print("   - 如果看到数据，说明AKShare工作正常")
    print("   - 周末/节假日会显示最近交易日的数据")
    print("   - 可以修改test_stocks列表测试更多股票")
    print("=" * 70)

if __name__ == "__main__":
    test_akshare_directly()
