# Python调用国内股票市场API完整教程

## 目录
- [API选择](#api选择)
- [快速开始](#快速开始)
- [Tushare使用指南](#tushare使用指南)
- [AKShare使用指南](#akshare使用指南)
- [实战示例](#实战示例)
- [常见问题](#常见问题)
- [最佳实践](#最佳实践)

---

## API选择

你的项目已经集成了两个主流的中国股票数据API：

### 1. Tushare（推荐）⭐

**优点：**
- ✅ 数据质量高，专业金融数据
- ✅ 接口稳定，更新及时
- ✅ 支持历史数据回测
- ✅ 数据完整（财务数据、基本面、技术指标等）

**缺点：**
- ❌ 需要注册获取Token
- ❌ 免费版有积分限制

**官网：** https://tushare.pro

### 2. AKShare（备选）

**优点：**
- ✅ 完全免费，无需注册
- ✅ 数据源丰富（东方财富、新浪财经等）
- ✅ 上手简单

**缺点：**
- ❌ 数据质量不如Tushare
- ❌ 稳定性稍差（依赖第三方网站）

**官网：** https://akshare.akfamily.xyz

### 对比表

| 特性 | Tushare | AKShare |
|------|---------|---------|
| **价格** | 免费版限额 | 完全免费 |
| **注册** | 需要Token | 无需注册 |
| **数据质量** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| **稳定性** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |
| **数据完整性** | 极高 | 较高 |
| **更新速度** | 实时 | 准实时 |
| **适用场景** | 专业量化、回测 | 个人项目、快速原型 |

---

## 快速开始

### 1. 安装依赖

```bash
cd /Users/youweichen/quant-trading-platform/market-data-service

# 安装所有依赖
pip3 install -r requirements.txt

# 或单独安装
pip3 install tushare akshare pandas
```

### 2. 配置Tushare Token（推荐）

#### 步骤1: 注册Tushare账号

1. 访问 https://tushare.pro/register
2. 注册账号（手机号或邮箱）
3. 登录后进入个人中心：https://tushare.pro/user/token
4. 复制你的Token（类似：`1234567890abcdef1234567890abcdef`）

#### 步骤2: 配置环境变量

创建 `.env` 文件：

```bash
cd /Users/youweichen/quant-trading-platform/market-data-service

# 创建.env文件
cat > .env << 'EOF'
# Tushare配置
TUSHARE_TOKEN=your_token_here

# AKShare配置
AKSHARE_ENABLED=true

# Kafka配置
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# 股票代码列表（逗号分隔）
STOCK_CODES=000001.SZ,000002.SZ,600000.SH,600519.SH

# 数据采集间隔（秒）
COLLECT_REALTIME_INTERVAL=60
EOF
```

**替换你的Token：**
```bash
# 用你的真实Token替换
vim .env
# 将 your_token_here 改为你从Tushare获取的Token
```

#### 步骤3: 验证配置

```python
# 测试脚本
import tushare as ts

token = 'your_token_here'  # 替换为你的Token
ts.set_token(token)
pro = ts.pro_api()

# 测试获取数据
df = pro.daily(ts_code='000001.SZ', start_date='20240101', end_date='20240115')
print(df.head())
```

### 3. 不使用Token（仅AKShare）

如果你不想注册Tushare，可以只使用AKShare：

```bash
# .env文件
TUSHARE_TOKEN=  # 留空
AKSHARE_ENABLED=true
```

---

## Tushare使用指南

### 1. 初始化

```python
from src.data_providers.tushare_provider import TushareProvider

# 方式1: 从环境变量读取Token
from src.config import settings
provider = TushareProvider(token=settings.tushare_token)

# 方式2: 直接传入Token
provider = TushareProvider(token='your_token_here')

# 检查是否可用
if provider.is_available():
    print("Tushare已准备就绪！")
else:
    print("Tushare未配置或Token无效")
```

### 2. 获取股票日线数据

```python
# 获取平安银行（000001.SZ）的日K线数据
stocks = provider.get_stock_daily(
    ts_code='000001.SZ',
    start_date='20240101',  # 2024-01-01
    end_date='20240115'     # 2024-01-15
)

# 打印数据
for stock in stocks:
    print(f"日期: {stock.trade_date}, "
          f"开盘: {stock.open}, "
          f"收盘: {stock.close}, "
          f"涨跌幅: {stock.pct_chg}%")
```

**输出示例：**
```
日期: 20240115, 开盘: 12.50, 收盘: 12.65, 涨跌幅: 1.20%
日期: 20240112, 开盘: 12.40, 收盘: 12.50, 涨跌幅: 0.81%
...
```

### 3. 获取实时行情

```python
# 获取多只股票的实时行情
realtime_data = provider.get_stock_realtime(
    ts_codes=['000001.SZ', '600519.SH', '000002.SZ']
)

for data in realtime_data:
    print(f"{data.code} {data.name}: "
          f"¥{data.price} ({data.pct_change:+.2f}%)")
```

**输出示例：**
```
000001.SZ 平安银行: ¥12.65 (+1.20%)
600519.SH 贵州茅台: ¥1850.00 (-0.50%)
000002.SZ 万科A: ¥8.30 (+2.10%)
```

### 4. 获取指数数据

```python
# 获取上证指数数据
index_data = provider.get_index_daily(
    ts_code='000001.SH',  # 上证指数
    start_date='20240101',
    end_date='20240115'
)

for index in index_data:
    print(f"日期: {index.trade_date}, "
          f"收盘: {index.close}, "
          f"涨跌幅: {index.pct_chg}%")
```

### 5. 常用股票代码

```python
# 股票代码格式：6位数字 + .交易所
STOCK_CODES = {
    # 沪市（上海证券交易所）- 以6开头
    '600000.SH': '浦发银行',
    '600519.SH': '贵州茅台',
    '600036.SH': '招商银行',
    '601318.SH': '中国平安',

    # 深市（深圳证券交易所）- 以0开头
    '000001.SZ': '平安银行',
    '000002.SZ': '万科A',
    '000858.SZ': '五粮液',

    # 创业板 - 以3开头
    '300750.SZ': '宁德时代',
    '300059.SZ': '东方财富',
}

# 指数代码
INDEX_CODES = {
    '000001.SH': '上证指数',
    '399001.SZ': '深证成指',
    '399006.SZ': '创业板指',
    '000300.SH': '沪深300',
}
```

---

## AKShare使用指南

### 1. 初始化

```python
from src.data_providers.akshare_provider import AKShareProvider

# AKShare无需Token，直接初始化
provider = AKShareProvider()

# 检查是否可用
if provider.is_available():
    print("AKShare已准备就绪！")
```

### 2. 获取股票日线数据

```python
# 获取平安银行的日K线数据
stocks = provider.get_stock_daily(
    ts_code='000001.SZ',
    start_date='2024-01-01',  # AKShare支持横杠格式
    end_date='2024-01-15'
)

for stock in stocks:
    print(f"日期: {stock.trade_date}, "
          f"收盘: {stock.close}")
```

### 3. 获取实时行情

```python
# 获取实时行情（AKShare会获取所有A股，然后筛选）
realtime_data = provider.get_stock_realtime(
    ts_codes=['000001.SZ', '600519.SH']
)

for data in realtime_data:
    print(f"{data.name}: ¥{data.price}")
```

### 4. 获取指数数据

```python
# 获取上证指数
index_data = provider.get_index_daily(
    ts_code='000001.SH',
    start_date='2024-01-01',
    end_date='2024-01-15'
)
```

---

## 实战示例

### 示例1: 获取茅台股价并分析

```python
from src.data_providers.tushare_provider import TushareProvider
from src.config import settings
import pandas as pd

# 初始化
provider = TushareProvider(token=settings.tushare_token)

# 获取贵州茅台近一年数据
stocks = provider.get_stock_daily(
    ts_code='600519.SH',
    start_date='20230101',
    end_date='20231231'
)

# 转换为DataFrame便于分析
data = []
for stock in stocks:
    data.append({
        'date': stock.trade_date,
        'close': stock.close,
        'pct_chg': stock.pct_chg
    })

df = pd.DataFrame(data)

# 统计分析
print(f"总交易日: {len(df)}")
print(f"最高价: ¥{df['close'].max():.2f}")
print(f"最低价: ¥{df['close'].min():.2f}")
print(f"平均价: ¥{df['close'].mean():.2f}")
print(f"年涨跌幅: {df['pct_chg'].sum():.2f}%")
```

### 示例2: 监控多只股票实时涨跌

```python
import time
from datetime import datetime

def monitor_stocks(codes, interval=60):
    """监控股票实时涨跌"""
    provider = TushareProvider(token=settings.tushare_token)

    while True:
        print(f"\n=== {datetime.now().strftime('%Y-%m-%d %H:%M:%S')} ===")

        # 获取实时数据
        realtime_data = provider.get_stock_realtime(ts_codes=codes)

        # 按涨跌幅排序
        sorted_data = sorted(realtime_data,
                           key=lambda x: x.pct_change,
                           reverse=True)

        # 打印
        for data in sorted_data:
            emoji = "📈" if data.pct_change > 0 else "📉"
            print(f"{emoji} {data.code} {data.name}: "
                  f"¥{data.price:.2f} "
                  f"({data.pct_change:+.2f}%)")

        # 等待下次更新
        time.sleep(interval)

# 监控自选股
monitor_stocks([
    '600519.SH',  # 贵州茅台
    '000858.SZ',  # 五粮液
    '601318.SH',  # 中国平安
    '000001.SZ',  # 平安银行
], interval=60)
```

### 示例3: 计算均线策略

```python
def calculate_moving_average(ts_code, days=20):
    """计算移动平均线"""
    provider = TushareProvider(token=settings.tushare_token)

    # 获取最近60天数据
    from datetime import datetime, timedelta
    end_date = datetime.now().strftime('%Y%m%d')
    start_date = (datetime.now() - timedelta(days=days*3)).strftime('%Y%m%d')

    stocks = provider.get_stock_daily(
        ts_code=ts_code,
        start_date=start_date,
        end_date=end_date
    )

    # 提取收盘价
    closes = [s.close for s in stocks if s.close]

    if len(closes) < days:
        print("数据不足")
        return

    # 计算MA
    ma = sum(closes[:days]) / days
    current_price = closes[0]

    print(f"股票代码: {ts_code}")
    print(f"当前价格: ¥{current_price:.2f}")
    print(f"{days}日均线: ¥{ma:.2f}")

    if current_price > ma:
        print("✅ 多头信号（价格在均线之上）")
    else:
        print("❌ 空头信号（价格在均线之下）")

# 测试
calculate_moving_average('600519.SH', days=20)
```

### 示例4: 对比多只股票表现

```python
def compare_stocks(codes, start_date, end_date):
    """对比多只股票表现"""
    provider = TushareProvider(token=settings.tushare_token)

    results = []
    for code in codes:
        stocks = provider.get_stock_daily(code, start_date, end_date)

        if len(stocks) >= 2:
            # 计算区间涨跌幅
            first_close = stocks[-1].close  # 第一天
            last_close = stocks[0].close    # 最后一天

            if first_close and last_close:
                pct_change = ((last_close - first_close) / first_close) * 100

                results.append({
                    'code': code,
                    'start_price': first_close,
                    'end_price': last_close,
                    'pct_change': pct_change
                })

    # 排序
    results.sort(key=lambda x: x['pct_change'], reverse=True)

    # 打印结果
    print(f"\n{'代码':<12} {'起始价':<10} {'结束价':<10} {'涨跌幅':<10}")
    print("-" * 50)
    for r in results:
        emoji = "📈" if r['pct_change'] > 0 else "📉"
        print(f"{r['code']:<12} "
              f"¥{r['start_price']:<9.2f} "
              f"¥{r['end_price']:<9.2f} "
              f"{emoji} {r['pct_change']:+.2f}%")

# 对比银行股
compare_stocks(
    codes=['600000.SH', '601318.SH', '000001.SZ', '600036.SH'],
    start_date='20240101',
    end_date='20240115'
)
```

### 示例5: 集成到Kafka（项目实际用法）

```python
from src.kafka_producer.producer import KafkaProducer
from src.data_providers.tushare_provider import TushareProvider
from src.config import settings
import time
import json

def publish_stock_data():
    """定时采集股票数据并发送到Kafka"""
    # 初始化
    provider = TushareProvider(token=settings.tushare_token)
    producer = KafkaProducer(
        bootstrap_servers=settings.kafka_bootstrap_servers
    )

    # 股票代码列表
    codes = settings.stock_codes.split(',')

    while True:
        # 获取实时数据
        realtime_data = provider.get_stock_realtime(ts_codes=codes)

        # 发送到Kafka
        for data in realtime_data:
            message = {
                'code': data.code,
                'name': data.name,
                'price': data.price,
                'change': data.change,
                'pct_change': data.pct_change,
                'timestamp': time.time()
            }

            producer.send(
                topic=settings.kafka_topic_stock_realtime,
                value=json.dumps(message)
            )

            print(f"发送到Kafka: {data.code} ¥{data.price}")

        # 等待下次采集
        time.sleep(settings.collect_realtime_interval)

# 运行
if __name__ == '__main__':
    publish_stock_data()
```

---

## 常见问题

### Q1: Tushare Token无效或过期？

**错误信息：**
```
抱歉，您没有访问该接口的权限，权限的具体详情访问：https://tushare.pro/document/1?doc_id=108
```

**解决方案：**

1. **检查Token是否正确**
   ```python
   import tushare as ts
   ts.set_token('your_token')
   pro = ts.pro_api()

   # 测试
   df = pro.trade_cal(exchange='SSE', start_date='20240101', end_date='20240115')
   print(df)
   ```

2. **检查积分是否足够**
   - 登录 https://tushare.pro/user/token
   - 查看积分余额
   - 免费用户：120积分/天
   - 某些接口需要更高权限

3. **使用低积分接口**
   ```python
   # 高积分接口（需要2000+积分）
   df = pro.daily_basic(ts_code='000001.SZ', trade_date='20240115')

   # 低积分接口（推荐）
   df = pro.daily(ts_code='000001.SZ', start_date='20240101', end_date='20240115')
   ```

### Q2: AKShare数据获取失败？

**错误信息：**
```
requests.exceptions.ConnectionError: HTTPSConnectionPool...
```

**解决方案：**

1. **检查网络连接**
   ```bash
   ping baidu.com
   ```

2. **重试机制**
   ```python
   import time

   def fetch_with_retry(func, max_retries=3):
       for i in range(max_retries):
           try:
               return func()
           except Exception as e:
               print(f"尝试 {i+1}/{max_retries} 失败: {e}")
               time.sleep(2)
       return None

   # 使用
   data = fetch_with_retry(lambda: ak.stock_zh_a_spot_em())
   ```

3. **使用Tushare作为备选**
   ```python
   def get_stock_data(ts_code, start_date, end_date):
       # 优先使用Tushare
       if tushare_provider.is_available():
           return tushare_provider.get_stock_daily(ts_code, start_date, end_date)
       # 备选AKShare
       else:
           return akshare_provider.get_stock_daily(ts_code, start_date, end_date)
   ```

### Q3: 股票代码格式错误？

**常见错误：**
```python
# ❌ 错误格式
'000001'      # 缺少交易所后缀
'SZ000001'    # 顺序错误
'000001-SZ'   # 分隔符错误

# ✅ 正确格式
'000001.SZ'   # 深市
'600000.SH'   # 沪市
```

**转换函数：**
```python
def normalize_stock_code(code):
    """标准化股票代码"""
    # 去除空格和特殊字符
    code = code.strip().upper()

    # 如果只有6位数字，自动添加后缀
    if len(code) == 6 and code.isdigit():
        if code.startswith('6'):
            return f"{code}.SH"  # 沪市
        elif code.startswith(('0', '3')):
            return f"{code}.SZ"  # 深市

    # 检查格式
    if '.' in code:
        parts = code.split('.')
        if len(parts) == 2 and parts[0].isdigit() and parts[1] in ['SH', 'SZ']:
            return code

    raise ValueError(f"无效的股票代码: {code}")

# 测试
print(normalize_stock_code('000001'))    # 000001.SZ
print(normalize_stock_code('600519'))    # 600519.SH
```

### Q4: 日期格式问题？

**不同API的日期格式：**

```python
# Tushare: YYYYMMDD（无分隔符）
tushare_date = '20240115'

# AKShare: YYYY-MM-DD（横杠分隔）
akshare_date = '2024-01-15'

# 转换函数
def to_tushare_date(date_str):
    """转换为Tushare日期格式"""
    return date_str.replace('-', '')

def to_akshare_date(date_str):
    """转换为AKShare日期格式"""
    if '-' not in date_str:
        # YYYYMMDD -> YYYY-MM-DD
        return f"{date_str[:4]}-{date_str[4:6]}-{date_str[6:]}"
    return date_str

# 使用
print(to_tushare_date('2024-01-15'))  # 20240115
print(to_akshare_date('20240115'))    # 2024-01-15
```

### Q5: 数据更新时间？

**A股交易时间：**
- 开盘：9:30 - 11:30, 13:00 - 15:00
- 数据更新：通常在收盘后30分钟内

**建议采集时间：**
```python
# 日线数据：收盘后采集
DAILY_COLLECT_TIME = "15:30"  # 下午3:30

# 实时数据：交易时段每分钟采集
REALTIME_INTERVAL = 60  # 60秒

# 周末和节假日不交易
import datetime

def is_trading_day():
    """判断是否交易日"""
    today = datetime.date.today()
    # 周末
    if today.weekday() >= 5:
        return False
    # TODO: 添加节假日判断
    return True
```

---

## 最佳实践

### 1. 使用数据缓存

```python
import pickle
from pathlib import Path

class DataCache:
    def __init__(self, cache_dir='cache'):
        self.cache_dir = Path(cache_dir)
        self.cache_dir.mkdir(exist_ok=True)

    def get(self, key):
        """从缓存读取"""
        cache_file = self.cache_dir / f"{key}.pkl"
        if cache_file.exists():
            with open(cache_file, 'rb') as f:
                return pickle.load(f)
        return None

    def set(self, key, value):
        """写入缓存"""
        cache_file = self.cache_dir / f"{key}.pkl"
        with open(cache_file, 'wb') as f:
            pickle.dump(value, f)

# 使用
cache = DataCache()

def get_stock_data_cached(ts_code, start_date, end_date):
    key = f"{ts_code}_{start_date}_{end_date}"

    # 先从缓存读取
    data = cache.get(key)
    if data:
        print("从缓存读取")
        return data

    # 从API获取
    print("从API获取")
    data = provider.get_stock_daily(ts_code, start_date, end_date)

    # 存入缓存
    cache.set(key, data)
    return data
```

### 2. 异常处理和重试

```python
import time
from functools import wraps

def retry_on_error(max_retries=3, delay=2):
    """重试装饰器"""
    def decorator(func):
        @wraps(func)
        def wrapper(*args, **kwargs):
            for attempt in range(max_retries):
                try:
                    return func(*args, **kwargs)
                except Exception as e:
                    if attempt < max_retries - 1:
                        print(f"尝试 {attempt + 1} 失败: {e}，{delay}秒后重试...")
                        time.sleep(delay)
                    else:
                        print(f"达到最大重试次数，失败: {e}")
                        raise
        return wrapper
    return decorator

# 使用
@retry_on_error(max_retries=3, delay=5)
def fetch_data(ts_code):
    return provider.get_stock_daily(ts_code, '20240101', '20240115')
```

### 3. 批量获取优化

```python
def batch_fetch_stocks(codes, start_date, end_date, batch_size=10, delay=1):
    """批量获取股票数据，避免频繁请求"""
    all_data = {}

    for i in range(0, len(codes), batch_size):
        batch = codes[i:i+batch_size]
        print(f"处理批次 {i//batch_size + 1}/{(len(codes)-1)//batch_size + 1}")

        for code in batch:
            try:
                data = provider.get_stock_daily(code, start_date, end_date)
                all_data[code] = data
            except Exception as e:
                print(f"获取 {code} 失败: {e}")

        # 批次间延迟
        if i + batch_size < len(codes):
            time.sleep(delay)

    return all_data

# 使用
codes = ['000001.SZ', '000002.SZ', '600000.SH', '600519.SH']
data = batch_fetch_stocks(codes, '20240101', '20240115')
```

### 4. 日志记录

```python
from loguru import logger

# 配置日志
logger.add(
    "logs/market_data_{time}.log",
    rotation="1 day",
    retention="30 days",
    level="INFO"
)

def fetch_with_log(ts_code, start_date, end_date):
    """带日志的数据获取"""
    logger.info(f"开始获取 {ts_code} 数据: {start_date} ~ {end_date}")

    try:
        data = provider.get_stock_daily(ts_code, start_date, end_date)
        logger.success(f"成功获取 {len(data)} 条记录")
        return data
    except Exception as e:
        logger.error(f"获取失败: {e}")
        raise
```

### 5. 数据验证

```python
def validate_stock_data(data):
    """验证股票数据完整性"""
    if not data:
        return False, "数据为空"

    for stock in data:
        # 检查必要字段
        if not stock.trade_date:
            return False, f"缺少交易日期"

        # 检查价格合理性
        if stock.close and (stock.close <= 0 or stock.close > 10000):
            return False, f"异常价格: {stock.close}"

        # 检查涨跌幅合理性
        if stock.pct_chg and abs(stock.pct_chg) > 20:
            # A股有涨跌停限制（一般±10%，ST股票±5%）
            return False, f"异常涨跌幅: {stock.pct_chg}%"

    return True, "数据验证通过"

# 使用
data = provider.get_stock_daily('000001.SZ', '20240101', '20240115')
is_valid, message = validate_stock_data(data)
if is_valid:
    print(message)
else:
    print(f"数据验证失败: {message}")
```

---

## 总结

### 推荐使用方案

**方案1: Tushare主力 + AKShare备用（推荐）⭐**

```python
from src.config import settings

# 初始化两个Provider
tushare_provider = TushareProvider(token=settings.tushare_token)
akshare_provider = AKShareProvider()

def get_stock_data(ts_code, start_date, end_date):
    """智能选择数据源"""
    # 优先Tushare
    if tushare_provider.is_available():
        return tushare_provider.get_stock_daily(ts_code, start_date, end_date)
    # 备用AKShare
    elif akshare_provider.is_available():
        return akshare_provider.get_stock_daily(ts_code, start_date, end_date)
    else:
        raise Exception("所有数据源不可用")
```

**方案2: 纯AKShare（快速开始）**

适合不想注册Tushare的用户：

```python
provider = AKShareProvider()
data = provider.get_stock_daily('000001.SZ', '2024-01-01', '2024-01-15')
```

### 下一步

1. ✅ 注册Tushare获取Token
2. ✅ 配置 `.env` 文件
3. ✅ 运行测试脚本验证
4. ✅ 集成到Kafka采集服务
5. ✅ 搭建前端展示界面

### 参考资源

- **Tushare文档**: https://tushare.pro/document/2
- **AKShare文档**: https://akshare.akfamily.xyz/data/stock/stock.html
- **项目代码**: `/Users/youweichen/quant-trading-platform/market-data-service`

---

**提示：** 你的项目已经完整实现了这两个API的封装，可以直接使用！
