#!/bin/bash
# H2 数据库访问脚本 (通过Java API)

BASE_URL="http://localhost:8083/trading-service"

echo "🔍 WeQuant H2 数据库查询工具"
echo "=================================="

echo -e "\n📊 交易服务状态:"
curl -s "$BASE_URL/actuator/health" | python3 -c "
import json, sys
data = json.load(sys.stdin)
print(f'服务状态: {data[\"status\"]}')
print(f'数据库: {data[\"components\"][\"db\"][\"details\"][\"database\"]}')
print(f'数据库状态: {data[\"components\"][\"db\"][\"status\"]}')
"

echo -e "\n🏥 健康检查详情:"
curl -s "$BASE_URL/actuator/health" | python3 -m json.tool

echo -e "\n📈 JVM 内存信息:"
curl -s "$BASE_URL/actuator/metrics/jvm.memory.used" | python3 -c "
import json, sys
data = json.load(sys.stdin)
memory_mb = data['measurements'][0]['value'] / 1024 / 1024
print(f'JVM内存使用: {memory_mb:.1f} MB')
"

echo -e "\n🔗 H2 Web 控制台访问:"
echo "URL: http://localhost:8083/trading-service/h2-console"
echo "JDBC URL: jdbc:h2:mem:trading_db"
echo "用户名: sa"
echo "密码: (空)"

echo -e "\n💡 注意: H2是内存数据库，服务重启后数据会丢失"