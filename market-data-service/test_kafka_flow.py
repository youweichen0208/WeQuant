#!/usr/bin/env python3
"""Test market data collection with historical data"""

import sys
import os
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from src.data_providers.akshare_provider import AKShareProvider
from src.kafka_producer import KafkaMessageProducer

def test_historical_data():
    """Test with historical data from a known trading day"""
    provider = AKShareProvider()
    producer = KafkaMessageProducer()

    # Use Jan 15, 2024 - a known trading day
    test_codes = ['000001.SZ', '600519.SH']
    start_date = '20240115'
    end_date = '20240115'

    print(f"Testing daily data collection for {start_date}")
    print("=" * 60)

    for code in test_codes:
        print(f"\n Fetching {code}...")
        daily_data = provider.get_stock_daily(code, start_date, end_date)

        if daily_data:
            for data in daily_data:
                print(f"  ✓ {data.ts_code}: {data.trade_date}")
                print(f"    Open: {data.open}, Close: {data.close}")
                print(f"    Volume: {data.vol}, Change: {data.pct_chg}%")

                # Try to send to Kafka
                success = producer.send_stock_data(data, is_realtime=False)
                print(f"    Kafka: {'✓ Sent' if success else '✗ Failed'}")
        else:
            print(f"  ✗ No data for {code}")

    print("\n" + "=" * 60)
    print("Checking Kafka connection...")
    print(f"Connected: {producer.is_connected()}")

    producer.close()

if __name__ == "__main__":
    test_historical_data()
