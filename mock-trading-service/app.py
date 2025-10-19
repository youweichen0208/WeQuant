#!/usr/bin/env python3
"""
模拟交易服务 - 提供虚拟账户和资金进行交易测试
"""

from flask import Flask, request, jsonify
from flask_cors import CORS
import sqlite3
import uuid
import datetime
import random
from decimal import Decimal
import akshare as ak

app = Flask(__name__)
CORS(app)

# 数据库初始化
def init_db():
    conn = sqlite3.connect('mock_trading.db')
    cursor = conn.cursor()

    # 创建用户表
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS users (
            id TEXT PRIMARY KEY,
            username TEXT UNIQUE NOT NULL,
            email TEXT,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    ''')

    # 创建账户表
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS accounts (
            id TEXT PRIMARY KEY,
            user_id TEXT NOT NULL,
            balance DECIMAL(15,2) DEFAULT 1000000.00,
            total_assets DECIMAL(15,2) DEFAULT 1000000.00,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (user_id) REFERENCES users (id)
        )
    ''')

    # 创建持仓表
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS positions (
            id TEXT PRIMARY KEY,
            account_id TEXT NOT NULL,
            stock_code TEXT NOT NULL,
            stock_name TEXT,
            quantity INTEGER NOT NULL,
            avg_cost DECIMAL(10,2) NOT NULL,
            current_price DECIMAL(10,2),
            market_value DECIMAL(15,2),
            profit_loss DECIMAL(15,2),
            profit_loss_pct DECIMAL(5,2),
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (account_id) REFERENCES accounts (id)
        )
    ''')

    # 创建交易记录表
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS trades (
            id TEXT PRIMARY KEY,
            account_id TEXT NOT NULL,
            stock_code TEXT NOT NULL,
            stock_name TEXT,
            trade_type TEXT NOT NULL, -- buy/sell
            quantity INTEGER NOT NULL,
            price DECIMAL(10,2) NOT NULL,
            amount DECIMAL(15,2) NOT NULL,
            commission DECIMAL(10,2) DEFAULT 0.00,
            trade_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            status TEXT DEFAULT 'completed', -- pending/completed/cancelled
            FOREIGN KEY (account_id) REFERENCES accounts (id)
        )
    ''')

    # 创建市场数据缓存表
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS market_cache (
            stock_code TEXT PRIMARY KEY,
            current_price DECIMAL(10,2),
            change_pct DECIMAL(5,2),
            volume INTEGER,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    ''')

    conn.commit()
    conn.close()

def get_db():
    return sqlite3.connect('mock_trading.db')

# 获取股票实时价格（模拟）
def get_stock_price(stock_code):
    try:
        # 尝试从akshare获取实时数据
        stock_info = ak.stock_zh_a_spot_em()
        stock_data = stock_info[stock_info['代码'] == stock_code.replace('.SZ', '').replace('.SH', '')]

        if not stock_data.empty:
            return {
                'code': stock_code,
                'price': float(stock_data.iloc[0]['最新价']),
                'change_pct': float(stock_data.iloc[0]['涨跌幅']),
                'name': stock_data.iloc[0]['名称']
            }
    except Exception as e:
        print(f"获取实时数据失败: {e}")

    # 模拟价格数据
    base_prices = {
        '000001.SZ': 11.40,  # 平安银行
        '000002.SZ': 18.20,  # 万科A
        '600036.SH': 35.80,  # 招商银行
        '600519.SH': 1680.00, # 贵州茅台
        '000858.SZ': 128.50,  # 五粮液
        '002415.SZ': 32.15    # 海康威视
    }

    base_price = base_prices.get(stock_code, 10.0)
    # 添加随机波动
    fluctuation = random.uniform(-0.05, 0.05)  # ±5%
    current_price = base_price * (1 + fluctuation)

    return {
        'code': stock_code,
        'price': round(current_price, 2),
        'change_pct': round(fluctuation * 100, 2),
        'name': get_stock_name(stock_code)
    }

def get_stock_name(stock_code):
    names = {
        '000001.SZ': '平安银行',
        '000002.SZ': '万科A',
        '600036.SH': '招商银行',
        '600519.SH': '贵州茅台',
        '000858.SZ': '五粮液',
        '002415.SZ': '海康威视'
    }
    return names.get(stock_code, '未知股票')

# API路由

@app.route('/api/health', methods=['GET'])
def health_check():
    return jsonify({
        'status': 'ok',
        'service': 'mock-trading-service',
        'timestamp': datetime.datetime.now().isoformat()
    })

@app.route('/api/register', methods=['POST'])
def register_user():
    """注册新用户并创建模拟账户"""
    data = request.json
    username = data.get('username')
    email = data.get('email', '')

    if not username:
        return jsonify({'error': '用户名不能为空'}), 400

    conn = get_db()
    cursor = conn.cursor()

    try:
        # 创建用户
        user_id = str(uuid.uuid4())
        cursor.execute(
            'INSERT INTO users (id, username, email) VALUES (?, ?, ?)',
            (user_id, username, email)
        )

        # 创建模拟账户（100万虚拟资金）
        account_id = str(uuid.uuid4())
        cursor.execute(
            'INSERT INTO accounts (id, user_id, balance, total_assets) VALUES (?, ?, ?, ?)',
            (account_id, user_id, 1000000.00, 1000000.00)
        )

        conn.commit()

        return jsonify({
            'user_id': user_id,
            'account_id': account_id,
            'username': username,
            'initial_balance': 1000000.00,
            'message': '模拟账户创建成功！已获得100万虚拟资金'
        })

    except sqlite3.IntegrityError:
        return jsonify({'error': '用户名已存在'}), 400
    except Exception as e:
        return jsonify({'error': str(e)}), 500
    finally:
        conn.close()

@app.route('/api/account/<account_id>', methods=['GET'])
def get_account_info(account_id):
    """获取账户信息"""
    conn = get_db()
    cursor = conn.cursor()

    try:
        # 获取账户基本信息
        cursor.execute('''
            SELECT a.balance, a.total_assets, u.username
            FROM accounts a
            JOIN users u ON a.user_id = u.id
            WHERE a.id = ?
        ''', (account_id,))

        account = cursor.fetchone()
        if not account:
            return jsonify({'error': '账户不存在'}), 404

        balance, total_assets, username = account

        # 获取持仓信息
        cursor.execute('''
            SELECT stock_code, stock_name, quantity, avg_cost,
                   current_price, market_value, profit_loss, profit_loss_pct
            FROM positions
            WHERE account_id = ?
        ''', (account_id,))

        positions = []
        for pos in cursor.fetchall():
            positions.append({
                'stock_code': pos[0],
                'stock_name': pos[1],
                'quantity': pos[2],
                'avg_cost': float(pos[3]),
                'current_price': float(pos[4]) if pos[4] else 0,
                'market_value': float(pos[5]) if pos[5] else 0,
                'profit_loss': float(pos[6]) if pos[6] else 0,
                'profit_loss_pct': float(pos[7]) if pos[7] else 0
            })

        # 计算总市值
        total_market_value = sum(pos['market_value'] for pos in positions)
        total_assets = balance + total_market_value

        return jsonify({
            'account_id': account_id,
            'username': username,
            'balance': float(balance),
            'total_assets': float(total_assets),
            'market_value': total_market_value,
            'positions': positions,
            'position_count': len(positions)
        })

    except Exception as e:
        return jsonify({'error': str(e)}), 500
    finally:
        conn.close()

@app.route('/api/trade', methods=['POST'])
def execute_trade():
    """执行交易"""
    data = request.json
    account_id = data.get('account_id')
    stock_code = data.get('stock_code')
    trade_type = data.get('trade_type')  # buy/sell
    quantity = data.get('quantity')

    if not all([account_id, stock_code, trade_type, quantity]):
        return jsonify({'error': '缺少必要参数'}), 400

    if trade_type not in ['buy', 'sell']:
        return jsonify({'error': '交易类型错误'}), 400

    # 获取当前股价
    stock_info = get_stock_price(stock_code)
    current_price = stock_info['price']
    stock_name = stock_info['name']

    # 计算交易金额和手续费
    amount = quantity * current_price
    commission = amount * 0.0003  # 万三手续费
    total_cost = amount + commission if trade_type == 'buy' else amount - commission

    conn = get_db()
    cursor = conn.cursor()

    try:
        # 获取账户余额
        cursor.execute('SELECT balance FROM accounts WHERE id = ?', (account_id,))
        account = cursor.fetchone()
        if not account:
            return jsonify({'error': '账户不存在'}), 404

        balance = float(account[0])

        if trade_type == 'buy':
            # 买入检查
            if balance < total_cost:
                return jsonify({'error': '账户余额不足'}), 400

            # 更新账户余额
            new_balance = balance - total_cost
            cursor.execute('UPDATE accounts SET balance = ? WHERE id = ?', (new_balance, account_id))

            # 更新持仓
            cursor.execute('''
                SELECT quantity, avg_cost FROM positions
                WHERE account_id = ? AND stock_code = ?
            ''', (account_id, stock_code))

            existing_position = cursor.fetchone()

            if existing_position:
                # 更新现有持仓
                old_qty, old_cost = existing_position
                new_qty = old_qty + quantity
                new_avg_cost = (old_qty * old_cost + quantity * current_price) / new_qty

                cursor.execute('''
                    UPDATE positions
                    SET quantity = ?, avg_cost = ?, current_price = ?,
                        market_value = ?, updated_at = CURRENT_TIMESTAMP
                    WHERE account_id = ? AND stock_code = ?
                ''', (new_qty, new_avg_cost, current_price, new_qty * current_price, account_id, stock_code))
            else:
                # 创建新持仓
                position_id = str(uuid.uuid4())
                cursor.execute('''
                    INSERT INTO positions (id, account_id, stock_code, stock_name, quantity,
                                         avg_cost, current_price, market_value)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ''', (position_id, account_id, stock_code, stock_name, quantity,
                      current_price, current_price, quantity * current_price))

        else:  # sell
            # 卖出检查
            cursor.execute('''
                SELECT quantity FROM positions
                WHERE account_id = ? AND stock_code = ?
            ''', (account_id, stock_code))

            position = cursor.fetchone()
            if not position or position[0] < quantity:
                return jsonify({'error': '持仓数量不足'}), 400

            # 更新账户余额
            new_balance = balance + total_cost
            cursor.execute('UPDATE accounts SET balance = ? WHERE id = ?', (new_balance, account_id))

            # 更新持仓
            new_qty = position[0] - quantity
            if new_qty == 0:
                # 清空持仓
                cursor.execute('DELETE FROM positions WHERE account_id = ? AND stock_code = ?',
                             (account_id, stock_code))
            else:
                # 减少持仓
                cursor.execute('''
                    UPDATE positions
                    SET quantity = ?, current_price = ?, market_value = ?, updated_at = CURRENT_TIMESTAMP
                    WHERE account_id = ? AND stock_code = ?
                ''', (new_qty, current_price, new_qty * current_price, account_id, stock_code))

        # 记录交易
        trade_id = str(uuid.uuid4())
        cursor.execute('''
            INSERT INTO trades (id, account_id, stock_code, stock_name, trade_type,
                              quantity, price, amount, commission)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        ''', (trade_id, account_id, stock_code, stock_name, trade_type,
              quantity, current_price, amount, commission))

        conn.commit()

        return jsonify({
            'trade_id': trade_id,
            'success': True,
            'message': f'{"买入" if trade_type == "buy" else "卖出"}成功',
            'stock_code': stock_code,
            'stock_name': stock_name,
            'quantity': quantity,
            'price': current_price,
            'amount': amount,
            'commission': commission,
            'total_cost': total_cost
        })

    except Exception as e:
        conn.rollback()
        return jsonify({'error': str(e)}), 500
    finally:
        conn.close()

@app.route('/api/trades/<account_id>', methods=['GET'])
def get_trade_history(account_id):
    """获取交易历史"""
    conn = get_db()
    cursor = conn.cursor()

    try:
        cursor.execute('''
            SELECT stock_code, stock_name, trade_type, quantity, price,
                   amount, commission, trade_time, status
            FROM trades
            WHERE account_id = ?
            ORDER BY trade_time DESC
            LIMIT 50
        ''', (account_id,))

        trades = []
        for trade in cursor.fetchall():
            trades.append({
                'stock_code': trade[0],
                'stock_name': trade[1],
                'trade_type': trade[2],
                'quantity': trade[3],
                'price': float(trade[4]),
                'amount': float(trade[5]),
                'commission': float(trade[6]),
                'trade_time': trade[7],
                'status': trade[8]
            })

        return jsonify({
            'trades': trades,
            'total': len(trades)
        })

    except Exception as e:
        return jsonify({'error': str(e)}), 500
    finally:
        conn.close()

@app.route('/api/market/<stock_code>', methods=['GET'])
def get_market_data(stock_code):
    """获取市场数据"""
    try:
        stock_info = get_stock_price(stock_code)
        return jsonify(stock_info)
    except Exception as e:
        return jsonify({'error': str(e)}), 500

if __name__ == '__main__':
    init_db()
    print("模拟交易服务启动中...")
    print("提供100万虚拟资金进行交易测试")
    print("支持功能：用户注册、账户管理、买卖交易、持仓查询、交易历史")
    app.run(host='0.0.0.0', port=5002, debug=True)