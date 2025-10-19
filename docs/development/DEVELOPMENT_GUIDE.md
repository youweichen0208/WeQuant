# 开发指南

## 概述

WeQuant 量化交易平台开发指南，涵盖开发环境搭建、代码规范、开发流程、测试策略等完整开发流程。

## 🛠️ 开发环境搭建

### 前置要求

| 工具 | 版本 | 用途 |
|------|------|------|
| Java JDK | 17+ | Java 微服务开发 |
| Python | 3.9+ | Python 服务开发 |
| Node.js | 18+ | 前端开发 |
| Maven | 3.8+ | Java 项目构建 |
| Docker | 20.10+ | 容器化开发 |
| Git | 2.30+ | 版本控制 |
| IDE | IntelliJ IDEA / VS Code | 开发工具 |

### 1. 克隆项目

```bash
git clone https://github.com/youweichen0208/WeQuant.git
cd WeQuant
```

### 2. 基础设施启动

```bash
# 启动 Docker 基础服务
docker-compose up -d mysql redis kafka zookeeper

# 等待服务启动
sleep 30

# 创建 Kafka Topic
docker exec quant-kafka kafka-topics --bootstrap-server localhost:9092 --create --topic stock_realtime --partitions 3 --replication-factor 1 --if-not-exists
docker exec quant-kafka kafka-topics --bootstrap-server localhost:9092 --create --topic stock_daily --partitions 3 --replication-factor 1 --if-not-exists
```

### 3. 后端服务启动

#### Python 服务
```bash
# 市场数据服务
cd market-data-service
pip3 install -r requirements.txt
python3 app.py &

# 模拟交易服务
cd ../mock-trading-service
pip3 install -r requirements.txt
python3 app.py &
```

#### Java 服务
```bash
# 股票服务
cd stock-service
mvn spring-boot:run &

# 用户服务
cd ../user-service
mvn spring-boot:run &

# 交易服务
cd ../trading-service
mvn spring-boot:run &
```

### 4. 前端服务启动

```bash
cd web-frontend
npm install
npm run dev
```

### 5. 验证环境

```bash
# 检查服务状态
curl http://localhost:5001/api/health    # 市场数据服务
curl http://localhost:5002/api/health    # 模拟交易服务
curl http://localhost:8082/stock-service/api/health  # 股票服务
curl http://localhost:3003               # 前端服务
```

---

## 📁 项目结构

```
WeQuant/
├── docs/                           # 文档
│   ├── api/                        # API 文档
│   ├── architecture/               # 架构文档
│   ├── deployment/                 # 部署文档
│   └── development/                # 开发文档
├── web-frontend/                   # Vue.js 前端
│   ├── src/
│   │   ├── components/             # 公共组件
│   │   ├── views/                  # 页面视图
│   │   ├── store/                  # 状态管理
│   │   ├── api/                    # API 调用层
│   │   └── router/                 # 路由配置
│   ├── public/                     # 静态资源
│   └── tests/                      # 前端测试
├── user-service/                   # Java 用户服务
│   ├── src/main/java/com/quant/user/
│   │   ├── controller/             # 控制器
│   │   ├── service/                # 业务服务
│   │   ├── entity/                 # 实体类
│   │   ├── repository/             # 数据访问
│   │   ├── dto/                    # 数据传输对象
│   │   └── config/                 # 配置类
│   └── src/test/                   # 单元测试
├── stock-service/                  # Java 股票服务
├── trading-service/                # Java 交易服务
├── market-data-service/            # Python 市场数据服务
│   ├── app.py                      # 应用入口
│   ├── services/                   # 业务服务
│   ├── models/                     # 数据模型
│   ├── config/                     # 配置文件
│   └── tests/                      # 单元测试
├── mock-trading-service/           # Python 模拟交易服务
├── database/                       # 数据库脚本
│   ├── migrations/                 # 数据库迁移
│   └── seeds/                      # 测试数据
├── scripts/                        # 构建和部署脚本
├── .github/workflows/              # CI/CD 配置
├── docker-compose.yml              # Docker 编排
└── README.md                       # 项目说明
```

---

## 🎨 代码规范

### Java 代码规范

#### 1. 命名规范

```java
// 类名：大驼峰命名
public class StockService {

    // 常量：全大写，下划线分隔
    private static final String DEFAULT_MARKET = "SZ";

    // 变量和方法：小驼峰命名
    private String stockCode;

    public StockData getLatestPrice(String stockCode) {
        return null;
    }
}
```

#### 2. 注释规范

```java
/**
 * 股票数据服务
 *
 * @author WeQuant Team
 * @version 1.0
 * @since 2025-10-17
 */
@Service
public class StockService {

    /**
     * 获取股票最新价格
     *
     * @param stockCode 股票代码，格式：000001.SZ
     * @return 股票实时数据
     * @throws StockNotFoundException 当股票不存在时抛出
     */
    public StockData getLatestPrice(String stockCode) {
        // 参数验证
        if (StringUtils.isBlank(stockCode)) {
            throw new IllegalArgumentException("股票代码不能为空");
        }

        // 业务逻辑
        return stockRepository.findByCode(stockCode);
    }
}
```

#### 3. 异常处理

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(StockNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleStockNotFound(StockNotFoundException e) {
        ErrorResponse error = ErrorResponse.builder()
            .code(ErrorCode.STOCK_NOT_FOUND.getCode())
            .message(e.getMessage())
            .timestamp(LocalDateTime.now())
            .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
}
```

### Python 代码规范

#### 1. 命名规范

```python
# 常量：全大写，下划线分隔
DEFAULT_CACHE_TTL = 300

# 类名：大驼峰命名
class MarketDataService:

    # 方法和变量：下划线命名
    def get_stock_price(self, stock_code: str) -> dict:
        """获取股票价格"""
        return {}

# 私有方法：下划线开头
def _validate_stock_code(stock_code: str) -> bool:
    """验证股票代码格式"""
    return True
```

#### 2. 类型注解

```python
from typing import Dict, List, Optional
from datetime import datetime

class StockData:
    def __init__(self, code: str, name: str, price: float):
        self.code = code
        self.name = name
        self.price = price

def get_stock_history(
    stock_code: str,
    days: int = 30
) -> List[Dict[str, any]]:
    """
    获取股票历史数据

    Args:
        stock_code: 股票代码
        days: 天数，默认30天

    Returns:
        历史数据列表

    Raises:
        ValueError: 当股票代码格式错误时
    """
    pass
```

### Vue.js 代码规范

#### 1. 组件命名

```vue
<!-- StockHistoryChart.vue -->
<template>
  <div class="stock-history-chart">
    <!-- 组件内容 -->
  </div>
</template>

<script setup>
// 使用 Composition API
import { ref, computed, onMounted } from 'vue'

// 响应式数据
const stockData = ref([])
const isLoading = ref(false)

// 计算属性
const chartOptions = computed(() => {
  return {
    // ECharts 配置
  }
})

// 生命周期
onMounted(() => {
  loadStockData()
})

// 方法
const loadStockData = async () => {
  isLoading.value = true
  try {
    // 数据加载逻辑
  } catch (error) {
    console.error('加载数据失败:', error)
  } finally {
    isLoading.value = false
  }
}
</script>

<style scoped>
.stock-history-chart {
  /* 样式定义 */
}
</style>
```

#### 2. API 调用

```javascript
// api/stock.js
import axios from 'axios'

const stockApi = {
  // 获取股票最新价格
  async getLatestPrice(stockCode) {
    try {
      const response = await axios.get(`/api/stocks/${stockCode}/latest`)
      return response.data
    } catch (error) {
      console.error('获取股价失败:', error)
      throw error
    }
  },

  // 获取历史数据
  async getHistory(stockCode, days = 30) {
    try {
      const response = await axios.get(`/api/stocks/${stockCode}/history`, {
        params: { days }
      })
      return response.data
    } catch (error) {
      console.error('获取历史数据失败:', error)
      throw error
    }
  }
}

export default stockApi
```

---

## 🔄 开发流程

### Git 工作流

#### 1. 分支策略

```bash
# 主分支
main          # 生产环境代码
develop       # 开发分支

# 功能分支
feature/user-authentication
feature/stock-chart-enhancement
feature/trading-simulation

# 修复分支
hotfix/critical-bug-fix
bugfix/minor-issue-fix

# 发布分支
release/v1.0.0
```

#### 2. 提交规范

```bash
# 提交类型
feat:     新功能
fix:      修复 bug
docs:     文档更新
style:    代码格式调整
refactor: 代码重构
test:     测试相关
chore:    构建过程或辅助工具变动

# 提交示例
git commit -m "feat: 添加股票搜索自动补全功能

- 实现股票代码和名称的模糊搜索
- 添加热门股票快捷选择
- 优化搜索性能，添加防抖处理

Closes #123"
```

#### 3. 代码审查

```bash
# 创建 Pull Request
1. 推送功能分支到远程仓库
2. 在 GitHub 创建 Pull Request
3. 填写详细的变更说明
4. 指定审查者
5. 通过 CI 检查
6. 获得批准后合并
```

### 开发流程

#### 1. 需求分析

```markdown
## 功能需求：股票搜索优化

### 背景
当前股票搜索功能体验不佳，用户需要输入完整的股票代码才能查找。

### 需求描述
- 支持股票代码和名称的模糊搜索
- 提供搜索建议和自动补全
- 添加热门股票快速选择
- 优化搜索性能

### 验收标准
- [ ] 输入2个字符即开始搜索建议
- [ ] 搜索结果在500ms内返回
- [ ] 支持拼音首字母搜索
- [ ] 移动端友好的交互体验
```

#### 2. 技术设计

```markdown
## 技术设计：股票搜索优化

### 架构设计
- 前端：Vue 组件 + 防抖处理
- 后端：股票服务增加搜索 API
- 缓存：Redis 缓存热门搜索结果

### API 设计
GET /api/v1/stocks/search?q={keyword}&limit={limit}

### 数据库设计
- 新增股票搜索索引表
- 支持拼音搜索字段

### 性能优化
- 客户端防抖 300ms
- 服务端缓存热门结果
- 数据库全文索引
```

#### 3. 开发实现

```bash
# 1. 创建功能分支
git checkout -b feature/stock-search-enhancement

# 2. 后端开发
cd stock-service
# 实现搜索 API

# 3. 前端开发
cd web-frontend
# 实现搜索组件

# 4. 测试验证
npm run test
mvn test

# 5. 提交代码
git add .
git commit -m "feat: 优化股票搜索功能"
git push origin feature/stock-search-enhancement
```

---

## 🧪 测试策略

### 单元测试

#### Java 单元测试

```java
@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private MarketDataService marketDataService;

    @InjectMocks
    private StockService stockService;

    @Test
    @DisplayName("获取股票最新价格 - 成功场景")
    void testGetLatestPrice_Success() {
        // Given
        String stockCode = "000001.SZ";
        StockData expectedData = StockData.builder()
            .code(stockCode)
            .name("平安银行")
            .price(BigDecimal.valueOf(11.40))
            .build();

        when(marketDataService.getLatestPrice(stockCode))
            .thenReturn(expectedData);

        // When
        StockData result = stockService.getLatestPrice(stockCode);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo(stockCode);
        assertThat(result.getPrice()).isEqualTo(BigDecimal.valueOf(11.40));
    }

    @Test
    @DisplayName("获取股票最新价格 - 股票不存在")
    void testGetLatestPrice_StockNotFound() {
        // Given
        String stockCode = "999999.SZ";
        when(marketDataService.getLatestPrice(stockCode))
            .thenThrow(new StockNotFoundException("股票不存在"));

        // When & Then
        assertThatThrownBy(() -> stockService.getLatestPrice(stockCode))
            .isInstanceOf(StockNotFoundException.class)
            .hasMessage("股票不存在");
    }
}
```

#### Python 单元测试

```python
import pytest
from unittest.mock import Mock, patch
from services.market_data_service import MarketDataService

class TestMarketDataService:

    @pytest.fixture
    def service(self):
        return MarketDataService()

    @patch('services.akshare_service.get_real_time_data')
    def test_get_stock_price_success(self, mock_akshare, service):
        # Given
        stock_code = "000001.SZ"
        mock_data = {
            'code': stock_code,
            'name': '平安银行',
            'price': 11.40,
            'change_pct': 0.53
        }
        mock_akshare.return_value = mock_data

        # When
        result = service.get_stock_price(stock_code)

        # Then
        assert result is not None
        assert result['code'] == stock_code
        assert result['price'] == 11.40
        mock_akshare.assert_called_once_with(stock_code)

    def test_get_stock_price_invalid_code(self, service):
        # Given
        invalid_code = "invalid"

        # When & Then
        with pytest.raises(ValueError, match="无效的股票代码"):
            service.get_stock_price(invalid_code)
```

#### Vue 组件测试

```javascript
import { mount } from '@vue/test-utils'
import { describe, it, expect, vi } from 'vitest'
import StockSearch from '@/components/StockSearch.vue'

describe('StockSearch', () => {
  it('渲染搜索输入框', () => {
    const wrapper = mount(StockSearch)

    expect(wrapper.find('input[type="text"]').exists()).toBe(true)
    expect(wrapper.find('.search-suggestions').exists()).toBe(false)
  })

  it('输入关键词显示搜索建议', async () => {
    const mockSearchApi = vi.fn().mockResolvedValue([
      { code: '000001.SZ', name: '平安银行' }
    ])

    const wrapper = mount(StockSearch, {
      global: {
        mocks: {
          $api: { searchStocks: mockSearchApi }
        }
      }
    })

    const input = wrapper.find('input')
    await input.setValue('平安')
    await wrapper.vm.$nextTick()

    expect(mockSearchApi).toHaveBeenCalledWith('平安')
    expect(wrapper.find('.search-suggestions').exists()).toBe(true)
  })
})
```

### 集成测试

#### API 集成测试

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class StockControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("获取股票最新价格 API 测试")
    void testGetLatestPriceApi() throws Exception {
        String stockCode = "000001.SZ";

        mockMvc.perform(get("/api/v1/stocks/{code}/latest", stockCode))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.code").value(stockCode))
            .andExpect(jsonPath("$.data.price").exists());
    }
}
```

### 端到端测试

```javascript
// e2e/stock-search.spec.js
import { test, expect } from '@playwright/test'

test.describe('股票搜索功能', () => {
  test('用户可以搜索并选择股票', async ({ page }) => {
    // 访问首页
    await page.goto('http://localhost:3003')

    // 点击搜索框
    await page.click('[data-testid="stock-search-input"]')

    // 输入搜索关键词
    await page.fill('[data-testid="stock-search-input"]', '平安')

    // 等待搜索建议出现
    await page.waitForSelector('[data-testid="search-suggestions"]')

    // 选择第一个建议
    await page.click('[data-testid="suggestion-item"]:first-child')

    // 验证股票被选中
    await expect(page.locator('[data-testid="selected-stock"]')).toContainText('000001.SZ')
  })
})
```

---

## 🔧 开发工具配置

### IDE 配置

#### IntelliJ IDEA

```xml
<!-- .idea/checkstyle-idea.xml -->
<CheckStyle-IDEA>
  <option name="configuration">
    <map>
      <entry key="checkstyle-version" value="8.45" />
      <entry key="copy-libs" value="true" />
      <entry key="location-0" value="BUNDLED:(bundled):Sun Checks" />
    </map>
  </option>
</CheckStyle-IDEA>
```

#### VS Code

```json
// .vscode/settings.json
{
  "editor.formatOnSave": true,
  "editor.codeActionsOnSave": {
    "source.fixAll.eslint": true
  },
  "eslint.validate": [
    "javascript",
    "javascriptreact",
    "typescript",
    "vue"
  ],
  "vetur.validation.template": false,
  "java.configuration.updateBuildConfiguration": "interactive",
  "java.saveActions.organizeImports": true
}
```

### 代码质量工具

#### ESLint 配置

```javascript
// .eslintrc.js
module.exports = {
  env: {
    browser: true,
    es2021: true,
    node: true
  },
  extends: [
    'eslint:recommended',
    '@vue/typescript/recommended',
    'prettier'
  ],
  parserOptions: {
    ecmaVersion: 'latest',
    parser: '@typescript-eslint/parser',
    sourceType: 'module'
  },
  plugins: [
    'vue',
    '@typescript-eslint'
  ],
  rules: {
    'vue/multi-word-component-names': 'off',
    '@typescript-eslint/no-unused-vars': 'error',
    'prefer-const': 'error',
    'no-console': process.env.NODE_ENV === 'production' ? 'warn' : 'off'
  }
}
```

#### Prettier 配置

```json
// .prettierrc
{
  "semi": false,
  "singleQuote": true,
  "tabWidth": 2,
  "trailingComma": "es5",
  "printWidth": 100,
  "bracketSpacing": true,
  "arrowParens": "avoid"
}
```

#### Checkstyle 配置

```xml
<!-- checkstyle.xml -->
<!DOCTYPE module PUBLIC
    "-//Puppy Crawl//DTD Check Configuration 1.3//EN"
    "http://www.puppycrawl.com/dtds/configuration_1_3.dtd">

<module name="Checker">
    <property name="charset" value="UTF-8"/>
    <property name="severity" value="warning"/>

    <module name="TreeWalker">
        <module name="NeedBraces"/>
        <module name="LeftCurly"/>
        <module name="RightCurly"/>
        <module name="WhitespaceAfter"/>
        <module name="WhitespaceAround"/>
    </module>
</module>
```

---

## 📚 开发最佳实践

### 1. 性能优化

#### 前端优化

```javascript
// 使用 computed 而不是 methods
const expensiveValue = computed(() => {
  return heavyCalculation(props.data)
})

// 使用 v-memo 优化列表渲染
<div v-for="item in list" :key="item.id" v-memo="[item.id, item.name]">
  {{ item.name }}
</div>

// 组件懒加载
const StockChart = defineAsyncComponent(() => import('./StockChart.vue'))
```

#### 后端优化

```java
// 使用缓存
@Cacheable(value = "stockPrice", key = "#stockCode")
public StockData getLatestPrice(String stockCode) {
    return marketDataService.getLatestPrice(stockCode);
}

// 批量操作
@Transactional
public void updatePositions(List<Position> positions) {
    positionRepository.saveAll(positions);
}

// 异步处理
@Async
public CompletableFuture<Void> processLargeDataSet(List<StockData> data) {
    // 处理逻辑
    return CompletableFuture.completedFuture(null);
}
```

### 2. 安全最佳实践

```java
// 输入验证
@Valid
public ResponseEntity<StockData> getStock(@PathVariable @Pattern(regexp = "\\d{6}\\.(SZ|SH)") String stockCode) {
    return ResponseEntity.ok(stockService.getLatestPrice(stockCode));
}

// SQL 注入防护
@Query("SELECT s FROM Stock s WHERE s.code = :code")
Stock findByCode(@Param("code") String code);

// 敏感信息脱敏
@JsonIgnore
private String password;
```

### 3. 错误处理

```javascript
// 前端错误处理
const handleApiError = (error) => {
  if (error.response?.status === 401) {
    // 重定向到登录页
    router.push('/login')
  } else if (error.response?.status >= 500) {
    // 显示系统错误
    ElMessage.error('系统错误，请稍后重试')
  } else {
    // 显示具体错误信息
    ElMessage.error(error.response?.data?.message || '操作失败')
  }
}
```

### 4. 日志记录

```java
// 结构化日志
@Slf4j
@Service
public class StockService {

    public StockData getLatestPrice(String stockCode) {
        log.info("获取股票价格开始, stockCode={}", stockCode);

        try {
            StockData result = marketDataService.getLatestPrice(stockCode);
            log.info("获取股票价格成功, stockCode={}, price={}", stockCode, result.getPrice());
            return result;
        } catch (Exception e) {
            log.error("获取股票价格失败, stockCode={}, error={}", stockCode, e.getMessage(), e);
            throw e;
        }
    }
}
```

---

## 🚀 部署和发布

### 本地构建

```bash
# 前端构建
cd web-frontend
npm run build

# Java 服务构建
cd stock-service
mvn clean package -DskipTests

# Docker 镜像构建
docker build -t stock-service:latest .
```

### CI/CD 流水线

```yaml
# .github/workflows/ci.yml
name: CI/CD Pipeline

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v3

    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'

    - name: Set up Node.js
      uses: actions/setup-node@v3
      with:
        node-version: '18'

    - name: Run Java tests
      run: |
        cd stock-service
        mvn test

    - name: Run frontend tests
      run: |
        cd web-frontend
        npm ci
        npm run test

  deploy:
    needs: test
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'

    steps:
    - name: Deploy to production
      run: |
        # 部署脚本
        echo "Deploying to production..."
```

---

## 📋 开发清单

### 功能开发清单

- [ ] 需求分析和技术设计
- [ ] 创建功能分支
- [ ] 后端 API 开发
- [ ] 前端界面开发
- [ ] 单元测试编写
- [ ] 集成测试验证
- [ ] 代码审查
- [ ] 文档更新
- [ ] 部署测试环境
- [ ] 用户验收测试

### 代码质量清单

- [ ] 代码符合团队规范
- [ ] 测试覆盖率 > 80%
- [ ] 无严重安全漏洞
- [ ] 性能满足要求
- [ ] 日志记录完整
- [ ] 错误处理健壮
- [ ] 文档齐全

---

*最后更新: 2025-10-17*
*开发指南版本: v1.0*