#!/usr/bin/env python3
"""
诊断脚本 - 查看AKShare接口失败的详细原因
"""

import akshare as ak
import traceback
import requests

print("=" * 70)
print("🔍 AKShare 接口诊断")
print("=" * 70)

# 测试1: 检查网络连接
print("\n1️⃣  测试基本网络连接...")
test_urls = [
    ("百度", "https://www.baidu.com"),
    ("东方财富", "https://www.eastmoney.com"),
    ("深交所", "https://www.szse.cn"),
    ("新浪财经", "http://finance.sina.com.cn"),
]

for name, url in test_urls:
    try:
        response = requests.get(url, timeout=5)
        print(f"   ✓ {name}: {response.status_code}")
    except Exception as e:
        print(f"   ✗ {name}: {type(e).__name__} - {str(e)[:50]}")

# 测试2: 尝试不同的AKShare接口
print("\n2️⃣  测试不同的AKShare接口...")

interfaces = [
    ("stock_zh_a_spot_em", "东方财富实时行情", lambda: ak.stock_zh_a_spot_em()),
    ("stock_zh_a_spot", "新浪财经实时行情", lambda: ak.stock_zh_a_spot()),
    ("stock_info_a_code_name", "股票代码列表", lambda: ak.stock_info_a_code_name()),
    ("stock_zh_a_hist", "历史数据(000001)", lambda: ak.stock_zh_a_hist(
        symbol="000001",
        period="daily",
        start_date="20251001",
        end_date="20251010",
        adjust=""
    )),
]

for func_name, desc, func in interfaces:
    print(f"\n   📊 {func_name} ({desc})")
    try:
        result = func()
        if result is not None and not result.empty:
            print(f"      ✓ 成功: 获取 {len(result)} 条数据")
            if 'name' in result.columns or '名称' in result.columns:
                name_col = 'name' if 'name' in result.columns else '名称'
                print(f"      示例: {result[name_col].head(3).tolist()}")
            elif 'code' in result.columns or '代码' in result.columns:
                code_col = 'code' if 'code' in result.columns else '代码'
                print(f"      示例代码: {result[code_col].head(3).tolist()}")
        else:
            print(f"      ⚠️  返回空数据")
    except Exception as e:
        print(f"      ✗ 失败: {type(e).__name__}")
        print(f"      详细: {str(e)[:100]}")
        # 打印完整错误栈
        if "--verbose" in __import__('sys').argv:
            print("\n" + "="*50)
            traceback.print_exc()
            print("="*50)

# 测试3: 检查代理设置
print("\n3️⃣  检查代理设置...")
import os
proxy_vars = ['HTTP_PROXY', 'HTTPS_PROXY', 'http_proxy', 'https_proxy', 'ALL_PROXY']
has_proxy = False
for var in proxy_vars:
    if var in os.environ:
        print(f"   ⚠️  发现代理: {var} = {os.environ[var]}")
        has_proxy = True

if not has_proxy:
    print("   ✓ 未检测到系统代理")

# 测试4: AKShare版本
print("\n4️⃣  AKShare 信息...")
print(f"   版本: {ak.__version__}")
print(f"   安装路径: {ak.__file__}")

print("\n" + "=" * 70)
print("💡 诊断建议:")
print("   1. 如果所有网络连接都失败 → 检查网络连接")
print("   2. 如果只有某些接口失败 → 尝试使用其他接口")
print("   3. 如果有代理 → 可能需要配置代理或关闭代理")
print("   4. 如果stock_zh_a_hist成功 → 说明AKShare工作正常，只是实时接口被限")
print("\n   运行 'python3 debug_akshare.py --verbose' 可查看详细错误栈")
print("=" * 70)
