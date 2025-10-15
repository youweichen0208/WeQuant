# 代码覆盖率配置指南

## 覆盖率服务对比

### 1. Codecov（推荐）✅
- **优点**:
  - 免费支持公开/私有仓库
  - GitHub集成完美
  - 可视化最好
  - 支持PR评论
  - 覆盖率徽章
- **缺点**: 需要注册账号
- **官网**: https://codecov.io

### 2. Coveralls
- **优点**: 界面简洁，易用
- **缺点**: 私有仓库收费
- **官网**: https://coveralls.io

### 3. SonarCloud
- **优点**:
  - 代码质量+覆盖率
  - 技术债分析
  - 安全漏洞扫描
- **缺点**: 配置复杂
- **官网**: https://sonarcloud.io

## 快速开始

### 步骤1: 注册Codecov

1. 访问 https://codecov.io
2. 使用GitHub账号登录
3. 授权访问你的仓库
4. 获取 `CODECOV_TOKEN`

### 步骤2: 配置GitHub Secrets

1. 进入仓库 Settings → Secrets and variables → Actions
2. 点击 "New repository secret"
3. 添加:
   - Name: `CODECOV_TOKEN`
   - Value: 从Codecov获取的token

### 步骤3: 推送代码

```bash
git add .
git commit -m "Add coverage reporting"
git push
```

GitHub Actions会自动运行，并上传覆盖率到Codecov。

## 在README中添加徽章

### Java覆盖率徽章
```markdown
[![codecov](https://codecov.io/gh/YOUR_USERNAME/quant-trading-platform/branch/main/graph/badge.svg?flag=unittests)](https://codecov.io/gh/YOUR_USERNAME/quant-trading-platform)
```

### 前端覆盖率徽章
```markdown
[![codecov](https://codecov.io/gh/YOUR_USERNAME/quant-trading-platform/branch/main/graph/badge.svg?flag=frontend)](https://codecov.io/gh/YOUR_USERNAME/quant-trading-platform)
```

### CI状态徽章
```markdown
[![CI](https://github.com/YOUR_USERNAME/quant-trading-platform/actions/workflows/ci.yml/badge.svg)](https://github.com/YOUR_USERNAME/quant-trading-platform/actions/workflows/ci.yml)
```

## 本地生成覆盖率报告

### Java (JaCoCo)
```bash
cd user-service
mvn clean test jacoco:report

# 查看报告
open target/site/jacoco/index.html
```

### 前端 (Vitest/Jest)
```bash
cd web-frontend
npm run test:coverage

# 查看报告
open coverage/index.html
```

## 覆盖率配置说明

### Java (pom.xml中的配置)

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <!-- 检查覆盖率阈值 -->
        <execution>
            <id>check</id>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>PACKAGE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.60</minimum> <!-- 60%最低覆盖率 -->
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

**覆盖率指标说明:**
- `LINE`: 行覆盖率（推荐）
- `BRANCH`: 分支覆盖率（更严格）
- `INSTRUCTION`: 字节码指令覆盖率
- `CLASS`: 类覆盖率
- `METHOD`: 方法覆盖率

**推荐阈值:**
- 新项目: 60-70%
- 成熟项目: 80%+
- 核心模块: 90%+

## PR中查看覆盖率

配置完成后，每个Pull Request都会显示:
- ✅ 覆盖率变化
- 📊 新增代码覆盖率
- 💬 Codecov机器人评论

## 排除文件

### Java - 排除不需要测试的类

在JaCoCo配置中添加:
```xml
<configuration>
    <excludes>
        <exclude>**/config/**</exclude>
        <exclude>**/dto/**</exclude>
        <exclude>**/entity/**</exclude>
        <exclude>**/*Application.class</exclude>
    </excludes>
</configuration>
```

### 前端 - 排除文件 (vitest.config.ts)
```typescript
export default defineConfig({
  test: {
    coverage: {
      exclude: [
        'src/**/*.d.ts',
        'src/**/*.config.ts',
        'src/main.ts',
      ]
    }
  }
})
```

## 其他CI平台

### GitLab CI
```yaml
test:
  script:
    - mvn test jacoco:report
    - bash <(curl -s https://codecov.io/bash)
```

### CircleCI
```yaml
- run:
    name: Upload coverage
    command: bash <(curl -s https://codecov.io/bash)
```

## 常见问题

### Q: 覆盖率上传失败？
A: 检查 `CODECOV_TOKEN` 是否正确配置

### Q: 覆盖率显示0%？
A: 确认测试有运行，检查jacoco.xml路径

### Q: 如何提高覆盖率？
A:
1. 为核心逻辑添加单元测试
2. 使用Mockito模拟依赖
3. 增加边界条件测试
4. 添加集成测试

### Q: 需要100%覆盖率吗？
A: 不需要。70-80%是合理目标，过度追求覆盖率反而降低效率。

## 高级功能

### 1. 覆盖率趋势图
Codecov自动生成覆盖率趋势，可查看代码质量变化

### 2. 文件级覆盖率
点击Codecov报告可查看每个文件的覆盖率

### 3. PR阻塞
配置覆盖率下降超过阈值时阻止合并:
```yaml
# codecov.yml
coverage:
  status:
    project:
      default:
        target: auto
        threshold: 1%
```

## 参考链接
- JaCoCo文档: https://www.jacoco.org/jacoco/
- Codecov文档: https://docs.codecov.com/
- GitHub Actions市场: https://github.com/marketplace/actions/codecov
