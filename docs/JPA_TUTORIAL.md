# Spring Data JPA 完整教程

## 目录
- [JPA简介](#jpa简介)
- [核心概念](#核心概念)
- [项目配置](#项目配置)
  - [JPA如何知道使用哪个数据库和表](#5-jpa如何知道使用哪个数据库和表)
- [JpaRepository详解](#jparepository详解)
- [实体映射](#实体映射)
- [查询方法](#查询方法)
- [事务管理](#事务管理)
- [性能优化](#性能优化)
- [测试策略](#测试策略)
- [常见问题](#常见问题)

---

## JPA简介

### 什么是JPA？

**JPA (Java Persistence API)** 是Java的ORM（对象关系映射）规范，用于将Java对象映射到数据库表。

```
Java对象 ←→ JPA ←→ 数据库表
  User          ↕      users
  Order         ↕      orders
```

### JPA vs Hibernate vs Spring Data JPA

```
┌─────────────────────────────────┐
│    Spring Data JPA              │  ← 封装层，提供Repository
│    (更高层抽象)                   │
├─────────────────────────────────┤
│    Hibernate                    │  ← JPA实现，具体ORM框架
│    (JPA实现)                     │
├─────────────────────────────────┤
│    JPA                          │  ← 规范/接口
│    (规范)                        │
├─────────────────────────────────┤
│    JDBC                         │  ← 底层数据库驱动
└─────────────────────────────────┘
```

**关系说明：**
- **JPA** = 接口规范（类似JDBC是规范）
- **Hibernate** = JPA的实现（最流行的ORM框架）
- **Spring Data JPA** = 在Hibernate上封装，提供Repository模式

### JpaRepository不是内存数据库！

```java
// ❌ 误解：JpaRepository是内存数据库
// ✅ 真相：JpaRepository是数据访问接口，连接真实数据库

// 开发环境 → MySQL
// 测试环境 → H2内存数据库（可选）
// 生产环境 → MySQL/PostgreSQL
```

---

## 核心概念

### 1. Entity（实体）

Java对象，映射到数据库表。

```java
@Entity                          // 标记为JPA实体
@Table(name = "users")          // 指定表名（可选）
public class User {

    @Id                         // 主键
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // 自增
    private Long id;

    @Column(name = "username", nullable = false, unique = true)
    private String username;    // 映射到username列

    private String email;       // 默认列名与字段名相同

    @CreatedDate                // 自动设置创建时间
    private LocalDateTime createdAt;

    @LastModifiedDate          // 自动设置更新时间
    private LocalDateTime updatedAt;
}
```

### 2. Repository（仓储）

数据访问接口，无需实现，Spring自动生成。

```java
public interface UserRepository extends JpaRepository<User, Long> {
    // 空接口！Spring自动实现常用方法
    // - save()
    // - findById()
    // - findAll()
    // - delete()
    // - count()
    // ...
}
```

### 3. EntityManager

JPA的核心API，管理实体的生命周期。

```java
@PersistenceContext
private EntityManager entityManager;

// 直接使用EntityManager（高级场景）
User user = entityManager.find(User.class, 1L);
entityManager.persist(user);
```

---

## 项目配置

### 1. Maven依赖

```xml
<!-- pom.xml -->
<dependencies>
    <!-- Spring Data JPA -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <!-- MySQL驱动（生产环境） -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- H2数据库（测试环境，可选） -->
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- Lombok（简化代码） -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

### 2. 数据库配置

```yaml
# application.yml
spring:
  datasource:
    # MySQL配置（开发/生产环境）
    url: jdbc:mysql://localhost:3306/quant_trading?useSSL=false&serverTimezone=UTC
    username: root
    password: root123456
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    # Hibernate配置
    hibernate:
      ddl-auto: update        # 启动时更新表结构
      # 可选值：
      # - none: 不做任何操作（生产环境推荐）
      # - validate: 验证表结构
      # - update: 更新表结构（开发环境）
      # - create: 启动时创建表
      # - create-drop: 启动时创建，关闭时删除

    show-sql: true           # 显示SQL语句
    properties:
      hibernate:
        format_sql: true     # 格式化SQL
        dialect: org.hibernate.dialect.MySQLDialect  # MySQL方言
        use_sql_comments: true  # SQL中显示注释

    # 数据库平台
    database-platform: org.hibernate.dialect.MySQLDialect

  # SQL初始化（可选）
  sql:
    init:
      mode: always          # 总是执行初始化脚本
      schema-locations: classpath:schema.sql
      data-locations: classpath:data.sql
```

### 3. 测试环境配置（H2内存数据库）

```yaml
# application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb    # H2内存数据库
    driver-class-name: org.h2.Driver
    username: sa
    password:

  jpa:
    hibernate:
      ddl-auto: create-drop   # 测试完自动删除
    show-sql: true
    database-platform: org.hibernate.dialect.H2Dialect

  h2:
    console:
      enabled: true           # 启用H2控制台
      path: /h2-console
```

### 4. 启用JPA审计

```java
@Configuration
@EnableJpaAuditing
public class JpaConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.of("system");
            }

            return Optional.of(authentication.getName());
        };
    }
}
```

### 5. JPA如何知道使用哪个数据库和表？

这是初学者经常困惑的问题。JPA通过多层配置来确定数据库和表。

#### 5.1 数据库配置（application.yml决定）

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/quant_trading
    #              ↑           ↑       ↑
    #         数据库类型      主机端口   数据库名
    username: root
    password: root123456
    driver-class-name: com.mysql.cj.jdbc.Driver
```

**URL格式解析：**
```
jdbc:mysql://localhost:3306/quant_trading?useSSL=false&serverTimezone=UTC
└─┬─┘ └──┬──┘ └────┬─────┘└─┬─┘ └──────┬───────┘ └─────────┬──────────┘
协议   数据库   主机地址    端口   数据库名称           连接参数
      类型
```

**不同数据库的URL格式：**
```yaml
# MySQL
url: jdbc:mysql://localhost:3306/quant_trading

# PostgreSQL
url: jdbc:postgresql://localhost:5432/quant_trading

# Oracle
url: jdbc:oracle:thin:@localhost:1521:XE

# H2内存数据库（测试用）
url: jdbc:h2:mem:testdb
```

#### 5.2 表名配置（Entity注解决定）

```java
@Entity                        // 1. 标记为JPA实体
@Table(name = "users")         // 2. 指定数据库表名
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username")  // 3. 指定列名
    private String username;

    private String email;       // 4. 未指定，默认列名 = 字段名
}
```

**表名映射规则：**

```java
// 1. 显式指定表名（推荐）
@Entity
@Table(name = "users")
public class User { }
// → 映射到 users 表

// 2. 不指定表名，使用默认规则
@Entity
public class User { }
// → 默认映射到 user 表（类名小写）

@Entity
public class OrderDetail { }
// → 默认映射到 order_detail 表（驼峰转下划线）
```

**列名映射规则：**

```java
public class User {

    // 显式指定列名
    @Column(name = "user_name")
    private String userName;
    // → 映射到 user_name 列

    // 不指定，使用默认规则
    private String email;
    // → 映射到 email 列

    private String phoneNumber;
    // → 映射到 phone_number 列（驼峰转下划线）
}
```

#### 5.3 完整配置链路

```
1. DataSource（数据源） - 决定连接哪个数据库
   ↓
   application.yml → DataSourceProperties → DataSource

2. EntityManagerFactory（实体管理器工厂） - 扫描和管理实体
   ↓
   EntityManager扫描 → @Entity注解 → 元数据

3. Entity Mapping（实体映射） - 决定映射哪个表
   ↓
   @Table(name = "users") → 表名
   @Column(name = "username") → 列名

4. SQL执行
   ↓
   DataSource连接到quant_trading数据库
   执行SQL: SELECT * FROM users WHERE username = ?
```

#### 5.4 配置流程图

```
┌─────────────────────────────────────────────┐
│  application.yml                            │
│  ┌────────────────────────────────────┐    │
│  │ spring.datasource.url =            │    │
│  │ jdbc:mysql://localhost:3306/       │    │
│  │         quant_trading              │    │
│  └────────────────────────────────────┘    │
└─────────────────┬───────────────────────────┘
                  │
                  ↓ Spring Boot自动配置
         ┌─────────────────┐
         │   DataSource    │ ← 数据库连接池
         │  (HikariCP)     │
         └────────┬────────┘
                  │
                  ↓
    ┌──────────────────────────┐
    │ EntityManagerFactory     │ ← Hibernate实现
    │ (LocalContainerEntityM...│
    └────────┬─────────────────┘
             │
             ↓ 扫描@Entity
    ┌─────────────────┐
    │ EntityManager   │
    └────────┬────────┘
             │
             ↓ 读取元数据
    ┌─────────────────────────┐
    │  @Entity                │
    │  @Table(name = "users") │  ← 表名
    │  class User {           │
    │    @Column("username")  │  ← 列名
    │    String username;     │
    │  }                      │
    └────────┬────────────────┘
             │
             ↓ 生成SQL
    ┌─────────────────────────────────┐
    │ SELECT * FROM users             │
    │ WHERE username = ?              │
    └─────────────────────────────────┘
             │
             ↓ 通过DataSource执行
    ┌─────────────────────────────────┐
    │ MySQL: quant_trading.users      │
    └─────────────────────────────────┘
```

#### 5.5 多数据源配置

如果项目需要连接多个数据库：

```java
@Configuration
public class DataSourceConfig {

    // 主数据源
    @Primary
    @Bean(name = "primaryDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.primary")
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create().build();
    }

    // 主EntityManagerFactory
    @Primary
    @Bean(name = "primaryEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean primaryEntityManagerFactory(
            @Qualifier("primaryDataSource") DataSource dataSource) {

        LocalContainerEntityManagerFactoryBean em =
            new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.quant.user.entity");  // 扫描User等实体

        return em;
    }

    // 次数据源（如：报表数据库）
    @Bean(name = "secondaryDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.secondary")
    public DataSource secondaryDataSource() {
        return DataSourceBuilder.create().build();
    }

    // 次EntityManagerFactory
    @Bean(name = "secondaryEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean secondaryEntityManagerFactory(
            @Qualifier("secondaryDataSource") DataSource dataSource) {

        LocalContainerEntityManagerFactoryBean em =
            new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.quant.report.entity");  // 扫描Report等实体

        return em;
    }
}
```

**配置文件：**

```yaml
spring:
  datasource:
    # 主数据库（用户服务）
    primary:
      url: jdbc:mysql://localhost:3306/quant_trading
      username: root
      password: root123456

    # 次数据库（报表系统）
    secondary:
      url: jdbc:mysql://localhost:3306/quant_report
      username: report_user
      password: report123
```

**使用示例：**

```java
// UserRepository使用主数据源
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // 连接到 quant_trading.users
}

// ReportRepository使用次数据源
@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    // 连接到 quant_report.reports
}
```

#### 5.6 实体扫描机制

Spring Boot如何找到Entity类？

```java
// 方式1: 主类的@SpringBootApplication会扫描同包及子包
@SpringBootApplication  // 等价于 @EnableAutoConfiguration + @ComponentScan
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}

// 项目结构：
// com.quant.user
//   ├── UserServiceApplication.java  ← @SpringBootApplication
//   ├── controller/
//   ├── service/
//   └── entity/
//       ├── User.java        ← @Entity，会被扫描到
//       └── UserSession.java ← @Entity，会被扫描到

// 方式2: 手动指定扫描包
@Configuration
@EnableJpaRepositories(basePackages = "com.quant.user.repository")
@EntityScan(basePackages = "com.quant.user.entity")
public class JpaConfig {
}
```

#### 5.7 验证配置

**测试数据库连接：**

```java
@SpringBootTest
public class DataSourceTest {

    @Autowired
    private DataSource dataSource;

    @Test
    public void testConnection() throws SQLException {
        // 获取连接
        Connection connection = dataSource.getConnection();

        // 验证数据库名称
        String catalog = connection.getCatalog();
        System.out.println("Connected to database: " + catalog);
        // 输出: Connected to database: quant_trading

        // 验证数据库类型
        DatabaseMetaData metaData = connection.getMetaData();
        System.out.println("Database: " + metaData.getDatabaseProductName());
        // 输出: Database: MySQL

        connection.close();
    }
}
```

**测试表映射：**

```java
@SpringBootTest
public class EntityMappingTest {

    @Autowired
    private EntityManager entityManager;

    @Test
    public void testTableMapping() {
        // 获取User实体的元数据
        EntityType<User> entityType =
            entityManager.getMetamodel().entity(User.class);

        // 输出表名
        Table table = User.class.getAnnotation(Table.class);
        System.out.println("Entity User maps to table: " + table.name());
        // 输出: Entity User maps to table: users

        // 输出所有列
        entityType.getAttributes().forEach(attr -> {
            System.out.println("Field: " + attr.getName());
        });
    }
}
```

#### 5.8 常见配置问题

**Q1: 连接错误的数据库？**

```java
// 检查application.yml中的URL
spring.datasource.url: jdbc:mysql://localhost:3306/wrong_database
//                                                  ↑
//                                        确保数据库名称正确
```

**Q2: 找不到表？**

```java
// 1. 检查@Table注解
@Table(name = "user")  // 表名拼写错误
// 应该是: @Table(name = "users")

// 2. 检查数据库是否存在该表
// MySQL: SHOW TABLES;
// 查看: users表是否存在

// 3. 检查schema是否正确
@Table(name = "users", schema = "quant_trading")
```

**Q3: 字段类型不匹配？**

```java
// Java实体
@Column(name = "balance")
private BigDecimal balance;

// 数据库表
// balance DECIMAL(20, 2)  ✅ 正确
// balance VARCHAR(50)     ❌ 类型不匹配
```

**Q4: 多数据源时使用错误的库？**

```java
// 确保Repository使用正确的EntityManager
@Repository
@Qualifier("primaryEntityManagerFactory")  // 指定使用哪个EntityManagerFactory
public interface UserRepository extends JpaRepository<User, Long> {
}
```

#### 5.9 调试技巧

**1. 查看实际执行的SQL：**

```yaml
spring:
  jpa:
    show-sql: true                    # 显示SQL
    properties:
      hibernate:
        format_sql: true              # 格式化SQL
        use_sql_comments: true        # 显示注释
```

**2. 查看数据库连接信息：**

```java
@Component
public class DataSourceLogger {

    @Autowired
    private DataSource dataSource;

    @PostConstruct
    public void logDataSourceInfo() throws SQLException {
        Connection conn = dataSource.getConnection();
        DatabaseMetaData metaData = conn.getMetaData();

        System.out.println("=== DataSource Info ===");
        System.out.println("Database: " + metaData.getDatabaseProductName());
        System.out.println("Version: " + metaData.getDatabaseProductVersion());
        System.out.println("URL: " + metaData.getURL());
        System.out.println("User: " + metaData.getUserName());
        System.out.println("Catalog: " + conn.getCatalog());  // 数据库名

        conn.close();
    }
}
```

输出示例：
```
=== DataSource Info ===
Database: MySQL
Version: 8.0.33
URL: jdbc:mysql://localhost:3306/quant_trading
User: root@localhost
Catalog: quant_trading
```

**3. 验证表映射：**

```java
@SpringBootTest
public class MappingDebugTest {

    @Autowired
    private EntityManager entityManager;

    @Test
    public void printAllMappings() {
        Metamodel metamodel = entityManager.getMetamodel();

        System.out.println("=== Entity Mappings ===");
        metamodel.getEntities().forEach(entity -> {
            Class<?> javaType = entity.getJavaType();
            Table table = javaType.getAnnotation(Table.class);

            String tableName = (table != null) ? table.name() : javaType.getSimpleName();
            System.out.println(javaType.getSimpleName() + " → " + tableName);
        });
    }
}
```

输出示例：
```
=== Entity Mappings ===
User → users
UserSession → user_sessions
Role → roles
```

---

## JpaRepository详解

### 1. Repository接口层次结构

```
Repository (标记接口)
    ↓
CrudRepository (增删改查)
    ↓
PagingAndSortingRepository (分页排序)
    ↓
JpaRepository (JPA特定功能)
```

### 2. JpaRepository继承的方法

```java
public interface JpaRepository<T, ID> extends PagingAndSortingRepository<T, ID> {

    // === 保存/更新 ===
    <S extends T> S save(S entity);                  // 保存或更新单个
    <S extends T> List<S> saveAll(Iterable<S> entities);  // 批量保存

    // === 查询 ===
    Optional<T> findById(ID id);                     // 根据ID查询
    boolean existsById(ID id);                       // 判断是否存在
    List<T> findAll();                               // 查询所有
    List<T> findAllById(Iterable<ID> ids);           // 根据ID列表查询
    long count();                                    // 统计数量

    // === 删除 ===
    void deleteById(ID id);                          // 根据ID删除
    void delete(T entity);                           // 删除实体
    void deleteAll(Iterable<? extends T> entities);  // 批量删除
    void deleteAll();                                // 删除所有

    // === JPA特有方法 ===
    void flush();                                    // 立即同步到数据库
    <S extends T> S saveAndFlush(S entity);          // 保存并立即同步
    void deleteInBatch(Iterable<T> entities);        // 批量删除（一条SQL）
    T getOne(ID id);                                 // 获取引用（懒加载）
    T getReferenceById(ID id);                       // 获取引用
}
```

### 3. 基本使用示例

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // 1. 保存
    public User createUser(User user) {
        return userRepository.save(user);  // 自动判断新增或更新
    }

    // 2. 查询单个
    public User getUser(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
    }

    // 3. 查询所有
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // 4. 分页查询
    public Page<User> getUsersPage(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return userRepository.findAll(pageable);
    }

    // 5. 更新
    public User updateUser(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());

        return userRepository.save(user);  // save自动识别为更新
    }

    // 6. 删除
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    // 7. 批量操作
    public void deleteUsers(List<Long> ids) {
        userRepository.deleteAllById(ids);
    }

    // 8. 判断存在
    public boolean userExists(String username) {
        return userRepository.existsByUsername(username);
    }

    // 9. 统计
    public long countActiveUsers() {
        return userRepository.countByStatus(UserStatus.ACTIVE);
    }
}
```

---

## 实体映射

### 1. 基础实体

```java
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_username", columnList = "username"),
    @Index(name = "idx_email", columnList = "email")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "email", unique = true, length = 100)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)  // 存储枚举的名称（推荐）
    @Column(name = "status", nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "balance", precision = 20, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "is_enabled", nullable = false)
    private Boolean enabled = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

### 2. 公共基类

```java
@MappedSuperclass  // 不是实体，只是基类
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
public abstract class BaseEntity implements Serializable {

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;

    @Version  // 乐观锁
    private Long version;
}
```

### 3. 关联关系

#### 一对多（One-to-Many）

```java
// User实体（一方）
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 一个用户有多个订单
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Order> orders = new ArrayList<>();

    // 辅助方法
    public void addOrder(Order order) {
        orders.add(order);
        order.setUser(this);
    }

    public void removeOrder(Order order) {
        orders.remove(order);
        order.setUser(null);
    }
}

// Order实体（多方）
@Entity
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 多个订单属于一个用户
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
```

#### 多对多（Many-to-Many）

```java
// User实体
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 一个用户有多个角色
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",                      // 中间表名
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();
}

// Role实体
@Entity
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    // 一个角色被多个用户拥有（可选，双向关系）
    @ManyToMany(mappedBy = "roles")
    private Set<User> users = new HashSet<>();
}
```

#### 一对一（One-to-One）

```java
// User实体
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 一个用户有一个配置文件
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "profile_id")
    private UserProfile profile;
}

// UserProfile实体
@Entity
public class UserProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String bio;
    private String avatar;

    // 双向关系（可选）
    @OneToOne(mappedBy = "profile")
    private User user;
}
```

### 4. 常用注解说明

| 注解 | 说明 | 示例 |
|------|------|------|
| `@Entity` | 标记为JPA实体 | `@Entity` |
| `@Table` | 指定表名 | `@Table(name = "users")` |
| `@Id` | 主键 | `@Id` |
| `@GeneratedValue` | 主键生成策略 | `@GeneratedValue(strategy = IDENTITY)` |
| `@Column` | 列属性 | `@Column(unique = true, length = 50)` |
| `@Temporal` | 日期/时间类型 | `@Temporal(TemporalType.TIMESTAMP)` |
| `@Enumerated` | 枚举类型 | `@Enumerated(EnumType.STRING)` |
| `@Transient` | 不持久化 | `@Transient` |
| `@Embedded` | 嵌入对象 | `@Embedded` |
| `@Lob` | 大对象 | `@Lob` |

---

## 查询方法

### 1. 方法命名查询（Query Method）

Spring Data JPA根据方法名自动生成SQL。

```java
public interface UserRepository extends JpaRepository<User, Long> {

    // 1. 简单查询
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    List<User> findByStatus(UserStatus status);

    // 2. 条件组合（And）
    Optional<User> findByUsernameAndPassword(String username, String password);
    List<User> findByUsernameAndEmail(String username, String email);

    // 3. 条件组合（Or）
    List<User> findByUsernameOrEmail(String username, String email);

    // 4. 比较操作
    List<User> findByBalanceGreaterThan(BigDecimal amount);
    List<User> findByBalanceLessThan(BigDecimal amount);
    List<User> findByBalanceBetween(BigDecimal start, BigDecimal end);
    List<User> findByCreatedAtAfter(LocalDateTime date);
    List<User> findByCreatedAtBefore(LocalDateTime date);

    // 5. 模糊查询
    List<User> findByUsernameLike(String pattern);         // %pattern%
    List<User> findByUsernameStartingWith(String prefix);  // prefix%
    List<User> findByUsernameEndingWith(String suffix);    // %suffix
    List<User> findByUsernameContaining(String keyword);   // %keyword%

    // 6. 空值判断
    List<User> findByEmailIsNull();
    List<User> findByEmailIsNotNull();

    // 7. 集合操作
    List<User> findByIdIn(List<Long> ids);
    List<User> findByStatusIn(List<UserStatus> statuses);
    List<User> findByIdNotIn(List<Long> excludeIds);

    // 8. 布尔值
    List<User> findByEnabledTrue();
    List<User> findByEnabledFalse();
    List<User> findByEnabled(Boolean enabled);

    // 9. 排序
    List<User> findByStatusOrderByCreatedAtDesc(UserStatus status);
    List<User> findByStatusOrderByBalanceAscUsernameDesc(UserStatus status);

    // 10. 分页
    Page<User> findByStatus(UserStatus status, Pageable pageable);
    Slice<User> findByEnabled(Boolean enabled, Pageable pageable);

    // 11. Top/First
    User findFirstByOrderByCreatedAtDesc();
    List<User> findTop10ByOrderByBalanceDesc();
    User findTopByOrderByBalanceDesc();

    // 12. Distinct
    List<User> findDistinctByStatus(UserStatus status);

    // 13. Count
    long countByStatus(UserStatus status);
    long countByEnabled(Boolean enabled);

    // 14. Exists
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    // 15. Delete
    void deleteByStatus(UserStatus status);
    long deleteByCreatedAtBefore(LocalDateTime date);
}
```

**方法命名规则：**

```
find + By + 属性名 + 条件关键字 + (And/Or + 属性名 + 条件关键字)* + OrderBy + 属性名 + Asc/Desc
```

### 2. @Query自定义查询

#### JPQL查询

```java
public interface UserRepository extends JpaRepository<User, Long> {

    // 1. 基本JPQL
    @Query("SELECT u FROM User u WHERE u.status = :status")
    List<User> findUsersByStatus(@Param("status") UserStatus status);

    // 2. 投影查询（只查询部分字段）
    @Query("SELECT u.username, u.email FROM User u WHERE u.enabled = true")
    List<Object[]> findActiveUsersInfo();

    // 3. JOIN查询
    @Query("SELECT u FROM User u JOIN FETCH u.orders WHERE u.id = :id")
    User findUserWithOrders(@Param("id") Long id);

    // 4. 聚合查询
    @Query("SELECT u.status, COUNT(u) FROM User u GROUP BY u.status")
    List<Object[]> countByStatus();

    @Query("SELECT SUM(u.balance) FROM User u WHERE u.enabled = true")
    BigDecimal getTotalBalance();

    // 5. 子查询
    @Query("SELECT u FROM User u WHERE u.balance > (SELECT AVG(u2.balance) FROM User u2)")
    List<User> findUsersAboveAverageBalance();

    // 6. 模糊查询
    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<User> searchByUsername(@Param("keyword") String keyword);

    // 7. 排序（动态）
    @Query("SELECT u FROM User u WHERE u.status = :status")
    List<User> findByStatus(@Param("status") UserStatus status, Sort sort);

    // 8. 分页
    @Query("SELECT u FROM User u WHERE u.enabled = :enabled")
    Page<User> findByEnabled(@Param("enabled") Boolean enabled, Pageable pageable);
}
```

#### 原生SQL查询

```java
public interface UserRepository extends JpaRepository<User, Long> {

    // 1. 基本SQL
    @Query(value = "SELECT * FROM users WHERE status = :status", nativeQuery = true)
    List<User> findByStatusNative(@Param("status") String status);

    // 2. 复杂SQL
    @Query(value = """
        SELECT u.*, COUNT(o.id) as order_count
        FROM users u
        LEFT JOIN orders o ON u.id = o.user_id
        GROUP BY u.id
        HAVING COUNT(o.id) > :minOrders
        """, nativeQuery = true)
    List<Object[]> findUsersWithMinOrders(@Param("minOrders") int minOrders);

    // 3. 更新操作
    @Modifying
    @Query("UPDATE User u SET u.status = :status WHERE u.lastAccessedAt < :date")
    int updateInactiveUsers(@Param("status") UserStatus status, @Param("date") LocalDateTime date);

    // 4. 删除操作
    @Modifying
    @Query("DELETE FROM User u WHERE u.createdAt < :date AND u.enabled = false")
    int deleteOldDisabledUsers(@Param("date") LocalDateTime date);

    // 5. 批量更新
    @Modifying
    @Query(value = "UPDATE users SET balance = balance * 1.1 WHERE status = 'VIP'", nativeQuery = true)
    int applyVipBonus();
}
```

### 3. Specification动态查询

```java
// 1. 定义Specification
public class UserSpecifications {

    public static Specification<User> hasUsername(String username) {
        return (root, query, cb) ->
            username == null ? null : cb.equal(root.get("username"), username);
    }

    public static Specification<User> hasStatus(UserStatus status) {
        return (root, query, cb) ->
            status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<User> balanceGreaterThan(BigDecimal amount) {
        return (root, query, cb) ->
            amount == null ? null : cb.greaterThan(root.get("balance"), amount);
    }

    public static Specification<User> createdBetween(LocalDateTime start, LocalDateTime end) {
        return (root, query, cb) -> {
            if (start == null || end == null) return null;
            return cb.between(root.get("createdAt"), start, end);
        };
    }
}

// 2. Repository继承JpaSpecificationExecutor
public interface UserRepository extends JpaRepository<User, Long>,
                                         JpaSpecificationExecutor<User> {
}

// 3. Service中使用
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<User> searchUsers(UserSearchCriteria criteria) {
        Specification<User> spec = Specification.where(null);

        if (criteria.getUsername() != null) {
            spec = spec.and(UserSpecifications.hasUsername(criteria.getUsername()));
        }

        if (criteria.getStatus() != null) {
            spec = spec.and(UserSpecifications.hasStatus(criteria.getStatus()));
        }

        if (criteria.getMinBalance() != null) {
            spec = spec.and(UserSpecifications.balanceGreaterThan(criteria.getMinBalance()));
        }

        return userRepository.findAll(spec);
    }
}
```

### 4. QueryDSL（类型安全）

```java
// 1. 添加依赖
<dependency>
    <groupId>com.querydsl</groupId>
    <artifactId>querydsl-jpa</artifactId>
</dependency>

// 2. Repository
public interface UserRepository extends JpaRepository<User, Long>,
                                         QuerydslPredicateExecutor<User> {
}

// 3. 使用
@Service
public class UserService {

    public List<User> searchUsers(String username, UserStatus status) {
        QUser user = QUser.user;

        BooleanBuilder builder = new BooleanBuilder();

        if (username != null) {
            builder.and(user.username.containsIgnoreCase(username));
        }

        if (status != null) {
            builder.and(user.status.eq(status));
        }

        return (List<User>) userRepository.findAll(builder);
    }
}
```

---

## 事务管理

### 1. @Transactional基础

```java
@Service
@Transactional  // 类级别：所有方法都是事务
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    // 1. 默认事务（读写）
    public User createUser(User user) {
        // 保存用户
        User savedUser = userRepository.save(user);

        // 创建账户
        Account account = new Account();
        account.setUserId(savedUser.getId());
        account.setBalance(BigDecimal.ZERO);
        accountRepository.save(account);

        // 如果任何操作失败，全部回滚
        return savedUser;
    }

    // 2. 只读事务（性能优化）
    @Transactional(readOnly = true)
    public User getUser(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
    }

    // 3. 指定传播行为
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logUserActivity(Long userId, String activity) {
        // 新事务，独立于父事务
        // 即使父事务回滚，日志也会保存
    }

    // 4. 指定隔离级别
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void transferMoney(Long fromId, Long toId, BigDecimal amount) {
        // 串行化隔离，防止并发问题
    }

    // 5. 指定回滚规则
    @Transactional(rollbackFor = Exception.class)
    public void riskyOperation() {
        // 任何Exception都回滚（默认只有RuntimeException）
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public void partialOperation() {
        // BusinessException不回滚
    }

    // 6. 超时设置
    @Transactional(timeout = 30)  // 30秒超时
    public void longRunningOperation() {
        // 长时间操作
    }
}
```

### 2. 事务传播行为

| 传播行为 | 说明 | 使用场景 |
|---------|------|----------|
| `REQUIRED` | 有事务就用，没有就创建（默认） | 大多数业务方法 |
| `REQUIRES_NEW` | 总是创建新事务 | 日志记录、审计 |
| `SUPPORTS` | 有事务就用，没有就非事务执行 | 查询方法 |
| `NOT_SUPPORTED` | 以非事务方式执行 | 性能要求高的只读操作 |
| `MANDATORY` | 必须在事务中执行 | 核心业务逻辑 |
| `NEVER` | 不能在事务中执行 | 特殊场景 |
| `NESTED` | 嵌套事务 | 复杂业务流程 |

### 3. 声明式事务示例

```java
@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private PaymentService paymentService;

    @Transactional
    public Order createOrder(OrderRequest request) {
        // 1. 创建订单
        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setAmount(request.getAmount());
        order.setStatus(OrderStatus.PENDING);
        order = orderRepository.save(order);

        try {
            // 2. 扣减库存
            inventoryService.reduceStock(request.getProductId(), request.getQuantity());

            // 3. 处理支付
            paymentService.processPayment(order.getId(), request.getAmount());

            // 4. 更新订单状态
            order.setStatus(OrderStatus.COMPLETED);
            return orderRepository.save(order);

        } catch (Exception e) {
            // 5. 出错回滚
            order.setStatus(OrderStatus.FAILED);
            orderRepository.save(order);
            throw e;  // 触发事务回滚
        }
    }
}
```

### 4. 编程式事务

```java
@Service
public class UserService {

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    // 方式1: TransactionTemplate
    public User createUserProgrammatic(User user) {
        return transactionTemplate.execute(status -> {
            try {
                User saved = userRepository.save(user);
                accountRepository.save(new Account(saved.getId()));
                return saved;
            } catch (Exception e) {
                status.setRollbackOnly();  // 标记回滚
                throw e;
            }
        });
    }

    // 方式2: TransactionManager
    public void createUserWithManager(User user) {
        TransactionDefinition def = new DefaultTransactionDefinition();
        TransactionStatus status = transactionManager.getTransaction(def);

        try {
            userRepository.save(user);
            transactionManager.commit(status);
        } catch (Exception e) {
            transactionManager.rollback(status);
            throw e;
        }
    }
}
```

---

## 性能优化

### 1. N+1问题解决

```java
// ❌ N+1问题
@Transactional(readOnly = true)
public List<Order> getOrders() {
    List<Order> orders = orderRepository.findAll();  // 1次查询
    for (Order order : orders) {
        User user = order.getUser();  // N次查询！
        System.out.println(user.getUsername());
    }
    return orders;
}

// ✅ 解决方案1: JOIN FETCH
@Query("SELECT o FROM Order o JOIN FETCH o.user")
List<Order> findAllWithUser();

// ✅ 解决方案2: EntityGraph
@EntityGraph(attributePaths = {"user", "items"})
List<Order> findAll();

// ✅ 解决方案3: 分页 + EntityGraph
@EntityGraph(attributePaths = "user")
Page<Order> findAll(Pageable pageable);
```

### 2. 批量操作

```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ❌ 效率低
    default void saveAllOneByOne(List<User> users) {
        for (User user : users) {
            save(user);  // 每次一条SQL
        }
    }

    // ✅ 批量保存
    // 配置：spring.jpa.properties.hibernate.jdbc.batch_size=20
}

@Service
public class UserService {

    // 批量插入
    @Transactional
    public void batchInsert(List<User> users) {
        int batchSize = 20;
        for (int i = 0; i < users.size(); i++) {
            userRepository.save(users.get(i));

            if (i % batchSize == 0 && i > 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
    }

    // 批量更新
    @Modifying
    @Query("UPDATE User u SET u.status = :status WHERE u.id IN :ids")
    int batchUpdateStatus(@Param("ids") List<Long> ids, @Param("status") UserStatus status);
}
```

### 3. 懒加载与急加载

```java
@Entity
public class User {

    // 懒加载（默认）- 用到时才查询
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Order> orders;

    // 急加载 - 立即查询
    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Role> roles;
}

// 动态控制加载
@EntityGraph(attributePaths = {"orders", "orders.items"})
Optional<User> findById(Long id);
```

### 4. 投影（Projection）

```java
// 接口投影
public interface UserSummary {
    String getUsername();
    String getEmail();
    LocalDateTime getCreatedAt();
}

public interface UserRepository extends JpaRepository<User, Long> {
    // 只查询需要的字段
    List<UserSummary> findAllProjectedBy();

    UserSummary findProjectedById(Long id);
}

// DTO投影
@Query("SELECT new com.quant.dto.UserDTO(u.id, u.username, u.email) FROM User u")
List<UserDTO> findAllDTO();
```

### 5. 二级缓存

```yaml
# application.yml
spring:
  jpa:
    properties:
      hibernate:
        cache:
          use_second_level_cache: true
          use_query_cache: true
          region:
            factory_class: org.hibernate.cache.jcache.JCacheRegionFactory
```

```java
@Entity
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class User {
    // ...
}
```

---

## 测试策略

### 1. 单元测试（H2内存数据库）

```java
@DataJpaTest  // 只加载JPA组件
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void testSaveUser() {
        // 准备数据
        User user = User.builder()
            .username("testuser")
            .email("test@example.com")
            .password("password123")
            .build();

        // 执行保存
        User saved = userRepository.save(user);

        // 验证
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUsername()).isEqualTo("testuser");

        // 清除缓存，确保从数据库读取
        entityManager.flush();
        entityManager.clear();

        // 验证数据库
        User found = userRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getUsername()).isEqualTo("testuser");
    }

    @Test
    void testFindByUsername() {
        // 准备测试数据
        User user = new User();
        user.setUsername("john");
        user.setEmail("john@example.com");
        entityManager.persist(user);
        entityManager.flush();

        // 执行查询
        Optional<User> found = userRepository.findByUsername("john");

        // 验证
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("john@example.com");
    }

    @Test
    void testCustomQuery() {
        // 批量插入测试数据
        for (int i = 0; i < 5; i++) {
            User user = new User();
            user.setUsername("user" + i);
            user.setEmail("user" + i + "@example.com");
            user.setStatus(i % 2 == 0 ? UserStatus.ACTIVE : UserStatus.INACTIVE);
            entityManager.persist(user);
        }
        entityManager.flush();

        // 测试查询
        List<User> activeUsers = userRepository.findByStatus(UserStatus.ACTIVE);

        // 验证
        assertThat(activeUsers).hasSize(3);
    }
}
```

### 2. 集成测试（真实数据库）

```java
@SpringBootTest
@Transactional  // 测试后自动回滚
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testCreateUser() {
        // 创建用户
        User user = new User();
        user.setUsername("integration_test");
        user.setEmail("test@example.com");
        user.setPassword("password");

        User created = userService.createUser(user);

        // 验证
        assertThat(created.getId()).isNotNull();

        // 从数据库验证
        User found = userRepository.findById(created.getId()).orElseThrow();
        assertThat(found.getUsername()).isEqualTo("integration_test");
    }

    @Test
    void testTransactionRollback() {
        // 触发异常，验证事务回滚
        assertThrows(Exception.class, () -> {
            userService.createUserWithError();
        });

        // 验证数据未保存
        long count = userRepository.count();
        assertThat(count).isEqualTo(0);
    }
}
```

### 3. TestContainers（Docker测试）

```java
@SpringBootTest
@Testcontainers
class UserRepositoryTestContainersTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    private UserRepository userRepository;

    @Test
    void testWithRealDatabase() {
        User user = new User();
        user.setUsername("testcontainer");
        user.setEmail("test@container.com");

        User saved = userRepository.save(user);

        assertThat(saved.getId()).isNotNull();
    }
}
```

---

## 常见问题

### Q1: save()如何判断新增还是更新？

```java
// 判断逻辑
@Transactional
public <S extends T> S save(S entity) {
    if (entityInformation.isNew(entity)) {
        em.persist(entity);  // 新增
        return entity;
    } else {
        return em.merge(entity);  // 更新
    }
}

// 判断标准：
// 1. ID为null → 新增
// 2. ID不为null → 更新
// 3. 使用@Version → 检查版本号
```

### Q2: 如何避免LazyInitializationException？

```java
// ❌ 错误：在事务外访问懒加载属性
@Transactional(readOnly = true)
public User getUser(Long id) {
    return userRepository.findById(id).orElseThrow();
}

// 在Controller中访问
public void test() {
    User user = userService.getUser(1L);
    user.getOrders().size();  // LazyInitializationException!
}

// ✅ 解决方案1: 在事务内初始化
@Transactional(readOnly = true)
public User getUser(Long id) {
    User user = userRepository.findById(id).orElseThrow();
    Hibernate.initialize(user.getOrders());  // 强制初始化
    return user;
}

// ✅ 解决方案2: JOIN FETCH
@Query("SELECT u FROM User u JOIN FETCH u.orders WHERE u.id = :id")
Optional<User> findByIdWithOrders(@Param("id") Long id);

// ✅ 解决方案3: @EntityGraph
@EntityGraph(attributePaths = "orders")
Optional<User> findById(Long id);

// ✅ 解决方案4: Open Session In View（不推荐）
spring.jpa.open-in-view=true  # 保持Session打开到View层
```

### Q3: 如何处理并发更新？

```java
// 乐观锁
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version  // 版本号
    private Long version;

    private BigDecimal balance;
}

@Service
public class UserService {

    @Transactional
    public void updateBalance(Long userId, BigDecimal amount) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setBalance(user.getBalance().add(amount));
        userRepository.save(user);  // 如果version不匹配，抛出OptimisticLockingFailureException
    }

    // 重试机制
    @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3)
    public void updateBalanceWithRetry(Long userId, BigDecimal amount) {
        updateBalance(userId, amount);
    }
}
```

### Q4: 如何处理大数据量查询？

```java
// ❌ 一次性加载所有数据（内存溢出）
List<User> users = userRepository.findAll();  // 100万条数据！

// ✅ 解决方案1: 分页
Page<User> page = userRepository.findAll(PageRequest.of(0, 1000));

// ✅ 解决方案2: Stream
@Query("SELECT u FROM User u")
Stream<User> streamAll();

@Transactional(readOnly = true)
public void processAllUsers() {
    try (Stream<User> stream = userRepository.streamAll()) {
        stream.forEach(user -> {
            // 处理每个用户
            processUser(user);
        });
    }
}

// ✅ 解决方案3: 批量处理
@Transactional(readOnly = true)
public void processBatch() {
    int pageSize = 1000;
    int pageNumber = 0;

    Page<User> page;
    do {
        page = userRepository.findAll(PageRequest.of(pageNumber++, pageSize));
        page.forEach(this::processUser);
        entityManager.clear();  // 清除缓存
    } while (page.hasNext());
}
```

---

## 总结

### JPA vs JDBC vs MyBatis

| 特性 | JPA | MyBatis | JDBC |
|------|-----|---------|------|
| 学习曲线 | 中等 | 简单 | 简单 |
| 开发效率 | 高 | 中 | 低 |
| SQL控制 | 低 | 高 | 高 |
| 性能 | 中 | 高 | 最高 |
| 适用场景 | CRUD为主 | 复杂SQL | 特殊需求 |

### 何时使用JPA？

✅ **适合：**
- 业务模型清晰
- CRUD操作为主
- 需要快速开发
- 多数据库支持

❌ **不适合：**
- 复杂SQL报表
- 性能极致优化
- 动态SQL频繁
- 遗留数据库

### 学习建议

1. ✅ 理解JPA核心概念
2. ✅ 掌握Entity映射
3. ✅ 熟练使用Repository
4. ✅ 了解事务管理
5. ✅ 关注性能优化
6. ✅ 编写测试用例

### 参考资源

- Spring Data JPA文档: https://spring.io/projects/spring-data-jpa
- Hibernate文档: https://hibernate.org/orm/documentation/
- Baeldung JPA教程: https://www.baeldung.com/the-persistence-layer-with-spring-data-jpa

---

**记住：JpaRepository不是内存数据库，它是连接真实数据库的Repository接口！**
