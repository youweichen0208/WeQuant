#!/bin/bash

echo "=========================================="
echo "🚀 Starting Market Data API Service"
echo "=========================================="

# 检查Python
if ! command -v python3 &> /dev/null; then
    echo "❌ Python3 not found. Please install Python 3.8+."
    exit 1
fi

# 检查依赖
echo "📦 Checking dependencies..."
python3 -c "import flask" 2>/dev/null || {
    echo "⚠️  Flask not found. Installing dependencies..."
    pip3 install -r requirements.txt
}

# 启动API服务
echo ""
echo "✅ Starting Flask API server on port 5000..."
echo ""
python3 api_server.py
