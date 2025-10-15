#!/bin/bash

# 市场数据服务启动脚本

# 检查Python环境
if ! command -v python3 &> /dev/null; then
    echo "❌ Python3 not found"
    exit 1
fi

# 检查依赖
echo "📋 Checking dependencies..."
pip3 install -r requirements.txt

# 创建必要目录
mkdir -p logs

# 检查配置文件
if [ ! -f ".env" ]; then
    echo "⚠️ .env file not found, copying from .env.example"
    cp .env.example .env
    echo "📝 Please edit .env with your configuration"
fi

# 运行测试
echo "🧪 Running tests..."
python3 src/test_service.py

if [ $? -eq 0 ]; then
    echo "✅ Tests passed"
    echo "🚀 Starting market data service..."
    python3 src/main.py
else
    echo "❌ Tests failed"
    exit 1
fi