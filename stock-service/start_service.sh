#!/bin/bash

# Stock Service 启动脚本
echo "🚀 启动 Stock Service..."

# 设置工作目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# 检查Java环境
if ! command -v java &> /dev/null; then
    echo "❌ Java未安装或不在PATH中"
    exit 1
fi

echo "☕ Java版本:"
java -version

# 检查依赖服务
echo "🔍 检查依赖服务..."
if ! curl -s http://localhost:5001/api/health > /dev/null; then
    echo "❌ Market Data Service不可用 (http://localhost:5001)"
    echo "请先启动Market Data Service"
    exit 1
fi

echo "✅ Market Data Service运行正常"

# 检查Redis（可选）
if ! nc -z localhost 6379 2>/dev/null; then
    echo "⚠️  Redis不可用，缓存功能将被禁用"
fi

# 如果有Maven，使用Maven运行
if command -v mvn &> /dev/null; then
    echo "📦 使用Maven启动..."
    mvn spring-boot:run -Dspring-boot.run.profiles=dev
else
    echo "❌ Maven未找到"
    echo "请安装Maven或使用IDE运行StockServiceApplication"
    echo ""
    echo "使用IDE运行步骤："
    echo "1. 在IDE中打开项目"
    echo "2. 找到 StockServiceApplication.java"
    echo "3. 右键点击并选择 'Run' 或 'Debug'"
    echo "4. 或使用快捷键运行主类"
    echo ""
    echo "运行成功后访问："
    echo "• Health Check: http://localhost:8082/stock-service/api/health"
    echo "• API Documentation: http://localhost:8082/stock-service/swagger-ui.html"
    exit 1
fi