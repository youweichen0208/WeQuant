#!/usr/bin/env python3
"""
Market Data Service 测试脚本
测试AKShare数据获取功能
"""

import sys
import os
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

import akshare as ak
from datetime import datetime

def test_akshare_basic():
    """测试AKShare基本功能"""
    print("=== 测试AKShare基本功能 ===")

    try:
        # 获取A股实时行情（简化版，避免代理问题）
        print("正在获取A股基本信息...")

        # 尝试获取股票基本信息
        stock_info = ak.stock_info_a_code_name()
        print(f"成功获取 {len(stock_info)} 只股票信息")
        print("前5只股票：")
        print(stock_info.head())

        return True

    except Exception as e:
        print(f"AKShare测试失败: {e}")
        return False

def test_specific_stock():
    """测试获取特定股票数据"""
    print("\n=== 测试获取特定股票历史数据 ===")

    try:
        # 获取平安银行历史数据
        symbol = "000001"  # 平安银行
        print(f"正在获取 {symbol} (平安银行) 的历史数据...")

        df = ak.stock_zh_a_hist(
            symbol=symbol,
            period="daily",
            start_date="20240101",
            end_date="20240115",
            adjust=""
        )

        if not df.empty:
            print(f"成功获取 {len(df)} 条历史记录")
            print("最近5天数据：")
            print(df.head())
            return True
        else:
            print("未获取到数据")
            return False

    except Exception as e:
        print(f"获取股票历史数据失败: {e}")
        return False

def test_alternative_api():
    """测试替代的API接口"""
    print("\n=== 测试替代的数据接口 ===")

    try:
        # 使用新浪财经接口
        print("尝试使用新浪财经接口...")
        df = ak.stock_zh_a_spot()

        if not df.empty:
            print(f"成功获取 {len(df)} 只股票实时数据")
            # 显示前几只股票
            print("前5只股票：")
            print(df[['代码', '名称', '最新价', '涨跌幅']].head())
            return True
        else:
            print("未获取到数据")
            return False

    except Exception as e:
        print(f"替代接口测试失败: {e}")
        return False

def main():
    """主测试函数"""
    print(f"开始测试 Market Data Service")
    print(f"时间: {datetime.now()}")
    print(f"AKShare版本: {ak.__version__}")
    print("-" * 50)

    # 运行测试
    tests = [
        ("基本功能测试", test_akshare_basic),
        ("特定股票测试", test_specific_stock),
        ("替代接口测试", test_alternative_api),
    ]

    results = []
    for test_name, test_func in tests:
        try:
            result = test_func()
            results.append((test_name, result))
            print(f"✅ {test_name}: {'通过' if result else '失败'}")
        except Exception as e:
            print(f"❌ {test_name}: 异常 - {e}")
            results.append((test_name, False))

        print("-" * 30)

    # 总结
    print("\n=== 测试总结 ===")
    passed = sum(1 for _, result in results if result)
    total = len(results)

    print(f"通过: {passed}/{total}")

    if passed > 0:
        print("✅ 至少有一个数据源可用，服务可以正常工作！")
        print("\n建议:")
        print("1. 如果网络有代理设置，可能影响数据获取")
        print("2. 可以注册Tushare Token作为备用数据源")
        print("3. 可以在非交易时间测试，避免接口限流")
    else:
        print("❌ 所有数据源都不可用，请检查网络连接")
        print("\n故障排除:")
        print("1. 检查网络连接: ping baidu.com")
        print("2. 检查代理设置")
        print("3. 尝试更换网络环境")

if __name__ == "__main__":
    main()