# DataGrip 连接数据库完整教程

## 目录
- [什么是DataGrip](#什么是datagrip)
- [安装DataGrip](#安装datagrip)
- [连接MySQL数据库](#连接mysql数据库)
- [基本操作](#基本操作)
- [高级功能](#高级功能)
- [常用快捷键](#常用快捷键)
- [常见问题](#常见问题)

---

## 什么是DataGrip？

**DataGrip** 是JetBrains出品的专业数据库管理工具，支持几乎所有主流数据库。

### 主要优势
- ✅ 智能SQL代码补全
- ✅ 强大的查询编辑器
- ✅ 数据库对象浏览
- ✅ 数据导入导出
- ✅ ER图自动生成
- ✅ 版本控制集成
- ✅ 多数据库支持（MySQL, PostgreSQL, Oracle等）

### 替代工具
- **MySQL Workbench** - MySQL官方工具，免费
- **Navicat** - 功能强大，付费
- **DBeaver** - 开源免费，功能全面
- **命令行** - 最轻量，适合简单操作

---

## 安装DataGrip

### 方法1: 官网下载（推荐）

1. 访问官网：https://www.jetbrains.com/datagrip/
2. 点击 "Download"
3. 选择你的操作系统（macOS/Windows/Linux）
4. 下载并安装

### 方法2: 使用Homebrew（macOS）

```bash
# 安装DataGrip
brew install --cask datagrip

# 或使用JetBrains Toolbox管理所有JetBrains工具
brew install --cask jetbrains-toolbox
```

### 激活许可

**方式1: 学生/教师免费许可**
- 访问：https://www.jetbrains.com/community/education/
- 使用学校邮箱申请免费许可

**方式2: 30天试用**
- 首次启动选择 "Start Trial"

**方式3: 购买许可**
- 个人版：$89/year（第一年）
- 团队版：$199/year

---

## 连接MySQL数据库

### 步骤1: 确认数据库运行中

在连接之前，确保MySQL已启动：

```bash
# 检查Docker容器是否运行
docker ps | grep mysql

# 或检查MySQL服务状态
brew services list | grep mysql

# 或使用Docker Compose
cd /Users/youweichen/quant-trading-platform
docker-compose ps
```

### 步骤2: 打开DataGrip

启动DataGrip应用程序。

### 步骤3: 创建新的数据库连接

**图文步骤：**

1. **点击 "+" 按钮添加数据源**
   ```
   左侧边栏 → Database → 点击 "+" 或 "New" → Data Source → MySQL
   ```

2. **或使用菜单**
   ```
   File → New → Data Source → MySQL
   ```

### 步骤4: 配置连接参数

在弹出的配置窗口中填写以下信息：

#### 基本配置（General选项卡）

```
Name: quant_trading (连接名称，自定义)

Host: localhost
Port: 3306
Database: quant_trading

User: root
Password: root123456

URL: jdbc:mysql://localhost:3306/quant_trading
```

**详细说明：**

| 字段 | 值 | 说明 |
|------|-----|------|
| **Name** | `quant_trading` | 在DataGrip中显示的连接名称 |
| **Host** | `localhost` | 数据库服务器地址（本地） |
| **Port** | `3306` | MySQL默认端口 |
| **Database** | `quant_trading` | 要连接的数据库名 |
| **User** | `root` | MySQL用户名 |
| **Password** | `root123456` | MySQL密码 |

#### 高级配置（Advanced选项卡）

**推荐添加的连接参数：**

```
useSSL=false
serverTimezone=UTC
allowPublicKeyRetrieval=true
characterEncoding=utf8mb4
```

**完整URL示例：**
```
jdbc:mysql://localhost:3306/quant_trading?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&characterEncoding=utf8mb4
```

### 步骤5: 下载MySQL驱动

首次连接MySQL时，DataGrip会提示下载驱动：

1. 点击 "Download missing driver files"
2. 等待下载完成（自动下载MySQL Connector/J）

**如果下载失败：**
- 点击 "Download" 按钮旁边的 "Driver"
- 选择 "MySQL Connector/J" 版本（推荐8.0.33）
- 手动下载驱动jar文件并添加

### 步骤6: 测试连接

1. 点击 "Test Connection" 按钮
2. 成功会显示：✅ "Succeeded"
3. 失败会显示错误信息（参考常见问题部分）

### 步骤7: 应用并连接

1. 点击 "OK" 保存配置
2. 在左侧Database面板双击连接名
3. 或右键点击 → "Connect"

**成功标志：**
- 连接名前的图标变为绿色 🟢
- 可以展开查看数据库、表、视图等对象

---

## 基本操作

### 1. 浏览数据库对象

```
quant_trading (数据库)
├── schemas
│   └── quant_trading
│       ├── tables
│       │   ├── users
│       │   ├── user_sessions
│       │   └── roles
│       ├── views
│       ├── procedures
│       └── functions
```

**操作：**
- 展开/折叠：点击箭头图标
- 查看表结构：双击表名
- 刷新：右键 → Refresh

### 2. 查看表数据

**方法1: 双击表名**
- 在左侧Database面板双击 `users` 表
- 会打开表数据编辑器

**方法2: 右键菜单**
- 右键点击表 → "Jump to Query Console"
- 会生成 `SELECT * FROM users` 查询

**方法3: 快捷键**
- 选中表名 → `F4`（查看表结构）
- 选中表名 → `Ctrl+B`（跳转到定义）

### 3. 执行SQL查询

**创建查询控制台：**

1. **新建Query Console**
   ```
   右键数据库连接 → New → Query Console
   或快捷键: Cmd+Shift+L (macOS) / Ctrl+Shift+L (Windows)
   ```

2. **编写SQL**
   ```sql
   -- 查询所有用户
   SELECT * FROM users;

   -- 条件查询
   SELECT * FROM users WHERE username = 'admin';

   -- 联表查询
   SELECT u.username, s.session_token
   FROM users u
   LEFT JOIN user_sessions s ON u.id = s.user_id
   WHERE u.enabled = TRUE;
   ```

3. **执行查询**
   - 执行当前语句：`Cmd+Enter` (macOS) / `Ctrl+Enter` (Windows)
   - 执行全部：`Cmd+Shift+Enter` / `Ctrl+Shift+Enter`
   - 执行选中：选中SQL → `Cmd+Enter`

### 4. 编辑数据

**在表数据视图中：**

1. **添加行**
   - 点击 "+" 按钮
   - 或快捷键：`Cmd+N` / `Ctrl+N`

2. **编辑单元格**
   - 双击单元格
   - 或按 `F2`

3. **删除行**
   - 选中行 → 点击 "-" 按钮
   - 或快捷键：`Cmd+Delete` / `Ctrl+Delete`

4. **提交更改**
   - 点击 "Submit" 按钮（绿色勾号）
   - 或快捷键：`Cmd+Enter` / `Ctrl+Enter`

5. **回滚更改**
   - 点击 "Rollback" 按钮（红色X）
   - 或快捷键：`Cmd+Z` / `Ctrl+Z`

### 5. 导出数据

**导出表数据：**

1. 右键点击表 → "Export Data"
2. 选择格式：
   - **CSV** - Excel兼容
   - **JSON** - 适合API
   - **SQL** - 可导入其他数据库
   - **Excel** - .xlsx格式
   - **HTML** - 网页格式

3. 配置导出选项
4. 点击 "Export"

**导出查询结果：**

1. 执行查询
2. 在结果面板右键 → "Export Data"
3. 选择格式和路径

### 6. 导入数据

**从CSV导入：**

1. 右键点击表 → "Import Data from File"
2. 选择CSV文件
3. 映射列（自动匹配或手动调整）
4. 点击 "Import"

**从SQL脚本导入：**

1. 打开SQL文件：`File → Open`
2. 执行脚本：`Cmd+Shift+Enter`

---

## 高级功能

### 1. 生成ER图（Entity-Relationship Diagram）

**查看数据库关系图：**

1. 右键点击数据库 → "Diagrams" → "Show Visualization"
2. 或快捷键：`Cmd+Alt+Shift+U` (macOS)

**功能：**
- 自动显示表之间的外键关系
- 拖拽调整布局
- 导出为图片（PNG/SVG）

### 2. 数据库比较（Database Diff）

**比较两个数据库结构：**

1. `Tools → Comparison → Compare With...`
2. 选择目标数据库
3. 查看差异报告
4. 生成迁移脚本

**使用场景：**
- 开发环境 vs 生产环境
- 本地数据库 vs 远程数据库

### 3. SQL代码格式化

**格式化SQL：**

1. 编写SQL代码
2. 快捷键：`Cmd+Alt+L` (macOS) / `Ctrl+Alt+L` (Windows)

**示例：**

```sql
-- 格式化前
select u.id,u.username,s.session_token from users u left join user_sessions s on u.id=s.user_id where u.enabled=true;

-- 格式化后
SELECT u.id,
       u.username,
       s.session_token
FROM users u
         LEFT JOIN user_sessions s ON u.id = s.user_id
WHERE u.enabled = TRUE;
```

### 4. 版本控制集成

**将SQL文件加入Git：**

1. `VCS → Enable Version Control Integration`
2. 选择 Git
3. 可以查看文件历史、diff、commit等

### 5. 数据源同步

**同步多个环境：**

```
本地开发 (localhost:3306)
    ↓ 同步结构
测试环境 (test-db:3306)
    ↓ 同步结构
生产环境 (prod-db:3306)
```

1. 右键数据库 → "SQL Scripts" → "SQL Generator"
2. 选择要同步的对象
3. 生成DDL脚本
4. 在目标数据库执行

### 6. 数据库监控

**查看连接会话：**

1. `Tools → Database → Monitor Sessions`
2. 查看活动查询
3. 终止长时间运行的查询

---

## 常用快捷键

### 通用操作

| 快捷键 (macOS) | 快捷键 (Windows) | 功能 |
|---------------|-----------------|------|
| `Cmd+N` | `Ctrl+N` | 新建查询/表/行 |
| `Cmd+O` | `Ctrl+O` | 打开文件 |
| `Cmd+S` | `Ctrl+S` | 保存 |
| `Cmd+W` | `Ctrl+W` | 关闭当前标签 |
| `Cmd+Q` | `Alt+F4` | 退出应用 |

### SQL编辑

| 快捷键 (macOS) | 快捷键 (Windows) | 功能 |
|---------------|-----------------|------|
| `Cmd+Enter` | `Ctrl+Enter` | 执行当前语句 |
| `Cmd+Shift+Enter` | `Ctrl+Shift+Enter` | 执行全部 |
| `Cmd+/` | `Ctrl+/` | 注释/取消注释 |
| `Cmd+D` | `Ctrl+D` | 复制当前行 |
| `Cmd+Alt+L` | `Ctrl+Alt+L` | 格式化代码 |
| `Cmd+Space` | `Ctrl+Space` | 代码补全 |

### 数据库导航

| 快捷键 (macOS) | 快捷键 (Windows) | 功能 |
|---------------|-----------------|------|
| `Cmd+Shift+L` | `Ctrl+Shift+L` | 新建Query Console |
| `F4` | `F4` | 查看表结构 |
| `Cmd+B` | `Ctrl+B` | 跳转到定义 |
| `Cmd+F` | `Ctrl+F` | 在当前文件查找 |
| `Cmd+Shift+F` | `Ctrl+Shift+F` | 全局查找 |

### 数据编辑

| 快捷键 (macOS) | 快捷键 (Windows) | 功能 |
|---------------|-----------------|------|
| `Cmd+N` | `Ctrl+N` | 添加新行 |
| `F2` | `F2` | 编辑单元格 |
| `Cmd+Delete` | `Ctrl+Delete` | 删除行 |
| `Cmd+Enter` | `Ctrl+Enter` | 提交更改 |
| `Cmd+Z` | `Ctrl+Z` | 撤销 |

---

## 常见问题

### Q1: 无法连接数据库 - "Communications link failure"

**错误信息：**
```
Communications link failure
The last packet sent successfully to the server was 0 milliseconds ago.
```

**解决方案：**

1. **检查MySQL是否运行**
   ```bash
   docker ps | grep mysql
   # 或
   brew services list | grep mysql
   ```

2. **检查端口是否正确**
   - 默认：3306
   - 确认：`docker port <container_name>`

3. **检查防火墙**
   ```bash
   # macOS关闭防火墙测试
   sudo /usr/libexec/ApplicationFirewall/socketfilterfw --setglobalstate off
   ```

4. **检查Docker网络**
   ```bash
   docker network ls
   docker network inspect bridge
   ```

### Q2: 认证失败 - "Access denied for user 'root'@'localhost'"

**错误信息：**
```
Access denied for user 'root'@'localhost' (using password: YES)
```

**解决方案：**

1. **检查密码是否正确**
   - 从 `application.properties` 确认密码
   - 密码：`root123456`

2. **尝试其他认证方式**
   - Advanced → serverTimezone: `UTC`
   - Advanced → allowPublicKeyRetrieval: `true`

3. **重置MySQL密码**
   ```bash
   docker exec -it quant-mysql mysql -uroot -p
   # 输入旧密码后执行
   ALTER USER 'root'@'%' IDENTIFIED BY 'root123456';
   FLUSH PRIVILEGES;
   ```

### Q3: 时区错误 - "The server time zone value 'CST' is unrecognized"

**解决方案：**

在URL中添加时区参数：
```
jdbc:mysql://localhost:3306/quant_trading?serverTimezone=UTC
```

### Q4: SSL警告

**错误信息：**
```
Establishing SSL connection without server's identity verification is not recommended.
```

**解决方案：**

在URL中禁用SSL：
```
jdbc:mysql://localhost:3306/quant_trading?useSSL=false
```

### Q5: 驱动下载失败

**解决方案：**

**方法1: 手动下载驱动**

1. 访问：https://dev.mysql.com/downloads/connector/j/
2. 下载 MySQL Connector/J（8.0.33）
3. 在DataGrip中：
   - Database → Driver → MySQL
   - 点击 "+" → 添加下载的jar文件

**方法2: 使用Maven仓库**

1. 访问：https://repo1.maven.org/maven2/mysql/mysql-connector-java/
2. 下载对应版本的jar

### Q6: 中文乱码

**解决方案：**

1. **连接URL添加编码**
   ```
   characterEncoding=utf8mb4
   ```

2. **检查数据库字符集**
   ```sql
   SHOW VARIABLES LIKE 'character%';

   -- 修改数据库字符集
   ALTER DATABASE quant_trading CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

3. **检查表字符集**
   ```sql
   -- 查看表字符集
   SHOW CREATE TABLE users;

   -- 修改表字符集
   ALTER TABLE users CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

### Q7: 连接太慢

**优化方案：**

1. **禁用DNS反向解析**
   - MySQL配置：`skip-name-resolve`

2. **增加连接超时**
   ```
   connectTimeout=10000
   socketTimeout=30000
   ```

3. **使用连接池**
   - DataGrip会自动管理连接池

### Q8: 无法看到某些数据库

**解决方案：**

1. **检查用户权限**
   ```sql
   SHOW GRANTS FOR 'root'@'localhost';
   ```

2. **授予权限**
   ```sql
   GRANT ALL PRIVILEGES ON quant_trading.* TO 'root'@'%';
   FLUSH PRIVILEGES;
   ```

3. **刷新DataGrip**
   - 右键数据库连接 → Refresh

---

## 实战示例

### 示例1: 查看用户表数据

```sql
-- 查询所有用户
SELECT * FROM users;

-- 查询特定用户
SELECT * FROM users WHERE username = 'admin';

-- 查询用户数量
SELECT COUNT(*) as total_users FROM users;

-- 查询启用的用户
SELECT id, username, email, created_at
FROM users
WHERE enabled = TRUE
ORDER BY created_at DESC;
```

### 示例2: 用户和会话联表查询

```sql
-- 查看有活跃会话的用户
SELECT
    u.id,
    u.username,
    u.email,
    s.session_token,
    s.created_at as session_created,
    s.expires_at
FROM users u
INNER JOIN user_sessions s ON u.id = s.user_id
WHERE s.expires_at > NOW()
ORDER BY s.created_at DESC;
```

### 示例3: 数据统计

```sql
-- 用户注册趋势
SELECT
    DATE(created_at) as date,
    COUNT(*) as new_users
FROM users
GROUP BY DATE(created_at)
ORDER BY date DESC
LIMIT 30;

-- 活跃会话统计
SELECT
    COUNT(*) as active_sessions,
    COUNT(DISTINCT user_id) as unique_users
FROM user_sessions
WHERE expires_at > NOW();
```

### 示例4: 数据清理

```sql
-- 删除过期会话（谨慎操作！）
DELETE FROM user_sessions
WHERE expires_at < NOW();

-- 或者标记为已删除（更安全）
UPDATE user_sessions
SET is_active = FALSE
WHERE expires_at < NOW();
```

### 示例5: 创建测试数据

```sql
-- 插入测试用户
INSERT INTO users (username, email, password, enabled, created_at)
VALUES
    ('test_user1', 'test1@example.com', '$2a$10$encoded_password', TRUE, NOW()),
    ('test_user2', 'test2@example.com', '$2a$10$encoded_password', TRUE, NOW()),
    ('test_user3', 'test3@example.com', '$2a$10$encoded_password', FALSE, NOW());

-- 批量更新
UPDATE users
SET enabled = TRUE
WHERE username LIKE 'test_%';
```

---

## 连接配置快速参考

### 本地开发环境配置

```
Name: quant_trading_local
Host: localhost
Port: 3306
Database: quant_trading
User: root
Password: root123456
URL: jdbc:mysql://localhost:3306/quant_trading?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
```

### Docker环境配置

```
Name: quant_trading_docker
Host: localhost (如果在宿主机访问)
     或 quant-mysql (如果在Docker网络内访问)
Port: 3306
Database: quant_trading
User: root
Password: root123456
```

### 远程服务器配置

```
Name: quant_trading_production
Host: your-server-ip
Port: 3306
Database: quant_trading
User: prod_user
Password: your_secure_password
SSH Tunnel: (推荐使用SSH隧道增加安全性)
```

---

## 安全建议

### 1. 不要在生产环境使用root账户

```sql
-- 创建专用数据库用户
CREATE USER 'quant_app'@'%' IDENTIFIED BY 'secure_password_here';

-- 授予必要权限
GRANT SELECT, INSERT, UPDATE, DELETE ON quant_trading.* TO 'quant_app'@'%';

-- 禁止DROP等危险操作
-- REVOKE DROP, CREATE, ALTER ON quant_trading.* FROM 'quant_app'@'%';

FLUSH PRIVILEGES;
```

### 2. 使用SSH隧道连接远程数据库

**配置SSH隧道：**

1. 在DataGrip连接配置中选择 "SSH/SSL" 选项卡
2. 勾选 "Use SSH tunnel"
3. 填写SSH配置：
   ```
   Host: your-server-ip
   Port: 22
   User: your-ssh-username
   Auth type: Key pair (推荐) 或 Password
   Private key: ~/.ssh/id_rsa
   ```

### 3. 只读连接

**创建只读用户：**

```sql
CREATE USER 'readonly_user'@'%' IDENTIFIED BY 'readonly_password';
GRANT SELECT ON quant_trading.* TO 'readonly_user'@'%';
FLUSH PRIVILEGES;
```

### 4. 敏感数据脱敏

**查询时隐藏敏感信息：**

```sql
-- 隐藏部分邮箱
SELECT
    username,
    CONCAT(LEFT(email, 3), '***@', SUBSTRING_INDEX(email, '@', -1)) as masked_email
FROM users;

-- 隐藏部分密码哈希
SELECT
    username,
    CONCAT(LEFT(password, 10), '...') as masked_password
FROM users;
```

---

## 总结

### DataGrip vs 其他工具

| 工具 | 优点 | 缺点 | 推荐场景 |
|------|------|------|----------|
| **DataGrip** | 智能补全、多DB支持、强大功能 | 收费 | 专业开发 |
| **MySQL Workbench** | 免费、MySQL官方 | 仅支持MySQL | MySQL专用 |
| **DBeaver** | 免费开源、功能全面 | UI稍逊 | 预算有限 |
| **命令行** | 轻量、脚本化 | 不直观 | 运维任务 |

### 学习路径

1. ✅ 完成首次连接
2. ✅ 掌握基本CRUD操作
3. ✅ 学习SQL查询和优化
4. ✅ 使用高级功能（ER图、数据比较）
5. ✅ 集成到开发流程

### 参考资源

- DataGrip官方文档: https://www.jetbrains.com/help/datagrip/
- MySQL文档: https://dev.mysql.com/doc/
- SQL教程: https://www.w3schools.com/sql/

---

**提示：** 本教程基于 DataGrip 2023.3 版本，不同版本界面可能略有差异。
