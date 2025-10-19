#!/bin/bash
# SQLite 数据库查询脚本

DB_PATH="/Users/youweichen/quant-trading-platform/mock-trading-service/mock_trading.db"

echo "🔍 WeQuant SQLite 数据库查询工具"
echo "=================================="

echo -e "\n📊 数据库概览:"
echo "表数量: $(sqlite3 $DB_PATH '.tables' | wc -w)"
echo "用户数量: $(sqlite3 $DB_PATH 'SELECT COUNT(*) FROM users;')"
echo "账户数量: $(sqlite3 $DB_PATH 'SELECT COUNT(*) FROM accounts;')"
echo "交易记录: $(sqlite3 $DB_PATH 'SELECT COUNT(*) FROM trades;')"
echo "持仓数量: $(sqlite3 $DB_PATH 'SELECT COUNT(*) FROM positions;')"

echo -e "\n👥 用户列表:"
sqlite3 $DB_PATH -header -column "SELECT username, created_at FROM users;"

echo -e "\n💰 账户余额:"
sqlite3 $DB_PATH -header -column "
SELECT u.username, a.balance, a.total_assets
FROM users u
JOIN accounts a ON u.id = a.user_id;
"

echo -e "\n📈 持仓信息:"
sqlite3 $DB_PATH -header -column "
SELECT stock_code, stock_name, quantity, avg_cost, market_value
FROM positions
ORDER BY market_value DESC;
"

echo -e "\n🔄 最近交易:"
sqlite3 $DB_PATH -header -column "
SELECT stock_name, trade_type, quantity, price, trade_time
FROM trades
ORDER BY trade_time DESC
LIMIT 5;
"