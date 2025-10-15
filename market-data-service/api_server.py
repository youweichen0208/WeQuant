#!/usr/bin/env python3
"""
Market Data API Service
提供历史股票数据的REST API，供前端可视化使用
"""

from flask import Flask, jsonify, request
from flask_cors import CORS
from datetime import datetime, timedelta
import sys
import os

# 添加项目路径
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from src.data_providers.akshare_provider import AKShareProvider
from src.config import settings

app = Flask(__name__)
CORS(app)  # 允许前端跨域访问

# 初始化数据提供者
provider = AKShareProvider()

@app.route('/api/health', methods=['GET'])
def health_check():
    """健康检查"""
    return jsonify({
        'status': 'ok',
        'service': 'market-data-api',
        'timestamp': datetime.now().isoformat()
    })

@app.route('/api/stocks/<stock_code>/history', methods=['GET'])
def get_stock_history(stock_code):
    """
    获取股票历史数据

    参数:
        stock_code: 股票代码，如 000001.SZ
        days: 获取天数，默认30天

    示例:
        GET /api/stocks/000001.SZ/history?days=30
    """
    try:
        # 获取参数
        days = request.args.get('days', default=30, type=int)

        # 限制最大天数
        if days > 365:
            days = 365

        # 计算日期范围
        end_date = datetime.now()
        start_date = end_date - timedelta(days=days)

        # 获取数据
        stocks = provider.get_stock_daily(
            ts_code=stock_code,
            start_date=start_date.strftime('%Y%m%d'),
            end_date=end_date.strftime('%Y%m%d')
        )

        if not stocks:
            return jsonify({
                'error': 'No data found',
                'stock_code': stock_code
            }), 404

        # 转换为JSON格式
        data = []
        for stock in reversed(stocks):  # 按日期正序
            data.append({
                'date': stock.trade_date,
                'open': stock.open,
                'high': stock.high,
                'low': stock.low,
                'close': stock.close,
                'volume': stock.vol,
                'amount': stock.amount,
                'pct_change': stock.pct_chg
            })

        return jsonify({
            'stock_code': stock_code,
            'count': len(data),
            'data': data
        })

    except Exception as e:
        return jsonify({
            'error': str(e),
            'stock_code': stock_code
        }), 500

@app.route('/api/stocks/<stock_code>/latest', methods=['GET'])
def get_stock_latest(stock_code):
    """
    获取股票最新数据（最近一个交易日）

    示例:
        GET /api/stocks/000001.SZ/latest
    """
    try:
        # 获取最近10天的数据，确保能获取到最后一个交易日
        end_date = datetime.now()
        start_date = end_date - timedelta(days=10)

        stocks = provider.get_stock_daily(
            ts_code=stock_code,
            start_date=start_date.strftime('%Y%m%d'),
            end_date=end_date.strftime('%Y%m%d')
        )

        if not stocks:
            return jsonify({
                'error': 'No data found',
                'stock_code': stock_code
            }), 404

        # 返回最新的一条
        latest = stocks[0]
        return jsonify({
            'stock_code': stock_code,
            'trade_date': latest.trade_date,
            'open': latest.open,
            'high': latest.high,
            'low': latest.low,
            'close': latest.close,
            'volume': latest.vol,
            'amount': latest.amount,
            'pct_change': latest.pct_chg
        })

    except Exception as e:
        return jsonify({
            'error': str(e),
            'stock_code': stock_code
        }), 500

@app.route('/api/stocks/batch', methods=['POST'])
def get_stocks_batch():
    """
    批量获取多只股票的最新数据

    请求体:
        {
            "stock_codes": ["000001.SZ", "600519.SH"],
            "days": 30
        }

    示例:
        POST /api/stocks/batch
        Content-Type: application/json
        {"stock_codes": ["000001.SZ", "600519.SH"], "days": 30}
    """
    try:
        data = request.get_json()
        stock_codes = data.get('stock_codes', [])
        days = data.get('days', 30)

        if not stock_codes:
            return jsonify({'error': 'stock_codes is required'}), 400

        # 限制批量查询数量
        if len(stock_codes) > 50:
            return jsonify({'error': 'Maximum 50 stocks per request'}), 400

        result = {}
        for stock_code in stock_codes:
            try:
                end_date = datetime.now()
                start_date = end_date - timedelta(days=days)

                stocks = provider.get_stock_daily(
                    ts_code=stock_code,
                    start_date=start_date.strftime('%Y%m%d'),
                    end_date=end_date.strftime('%Y%m%d')
                )

                if stocks:
                    result[stock_code] = {
                        'latest': {
                            'date': stocks[0].trade_date,
                            'close': stocks[0].close,
                            'pct_change': stocks[0].pct_chg
                        },
                        'count': len(stocks)
                    }
                else:
                    result[stock_code] = None

            except Exception as e:
                result[stock_code] = {'error': str(e)}

        return jsonify(result)

    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/api/index/<index_code>/history', methods=['GET'])
def get_index_history(index_code):
    """
    获取指数历史数据

    参数:
        index_code: 指数代码，如 000001.SH (上证指数)
        days: 获取天数，默认30天

    示例:
        GET /api/index/000001.SH/history?days=30
    """
    try:
        days = request.args.get('days', default=30, type=int)

        if days > 365:
            days = 365

        end_date = datetime.now()
        start_date = end_date - timedelta(days=days)

        indices = provider.get_index_daily(
            ts_code=index_code,
            start_date=start_date.strftime('%Y%m%d'),
            end_date=end_date.strftime('%Y%m%d')
        )

        if not indices:
            return jsonify({
                'error': 'No data found',
                'index_code': index_code
            }), 404

        data = []
        for index in reversed(indices):
            data.append({
                'date': index.trade_date,
                'open': index.open,
                'high': index.high,
                'low': index.low,
                'close': index.close,
                'volume': index.vol,
                'amount': index.amount
            })

        return jsonify({
            'index_code': index_code,
            'count': len(data),
            'data': data
        })

    except Exception as e:
        return jsonify({
            'error': str(e),
            'index_code': index_code
        }), 500

if __name__ == '__main__':
    print("=" * 70)
    print("🚀 Market Data API Service")
    print("=" * 70)
    print(f"\n📍 服务地址: http://localhost:5001")
    print(f"📊 数据提供者: AKShare")
    print(f"\n可用API端点:")
    print(f"  GET  /api/health")
    print(f"  GET  /api/stocks/<code>/history?days=30")
    print(f"  GET  /api/stocks/<code>/latest")
    print(f"  POST /api/stocks/batch")
    print(f"  GET  /api/index/<code>/history?days=30")
    print(f"\n示例:")
    print(f"  curl http://localhost:5001/api/stocks/000001.SZ/history?days=7")
    print(f"  curl http://localhost:5001/api/stocks/000001.SZ/latest")
    print("=" * 70)
    print()

    app.run(host='0.0.0.0', port=5001, debug=True)
