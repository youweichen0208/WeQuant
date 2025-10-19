#!/bin/bash
# 启动 SQLite Web 管理界面

DB_PATH="/Users/youweichen/quant-trading-platform/mock-trading-service/mock_trading.db"
PORT=8080

echo "🚀 启动 SQLite Web 管理界面..."
echo "数据库: $DB_PATH"
echo "端口: $PORT"
echo ""
echo "📊 数据库概览:"
echo "用户数: $(sqlite3 $DB_PATH 'SELECT COUNT(*) FROM users;')"
echo "交易数: $(sqlite3 $DB_PATH 'SELECT COUNT(*) FROM trades;')"
echo "持仓数: $(sqlite3 $DB_PATH 'SELECT COUNT(*) FROM positions;')"
echo ""
echo "🌐 Web访问地址: http://localhost:$PORT"
echo "💡 按 Ctrl+C 停止服务"
echo ""

# 切换到数据库目录并启动 sqlite-web
cd /Users/youweichen/quant-trading-platform/mock-trading-service
sqlite_web mock_trading.db --port $PORT --host 0.0.0.0