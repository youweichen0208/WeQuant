# Spring Security 完整教程 - User Service

## 目录
- [Spring Security简介](#spring-security简介)
- [核心概念](#核心概念)
- [项目中的实现](#项目中的实现)
- [认证流程详解](#认证流程详解)
- [授权机制](#授权机制)
- [JWT集成](#jwt集成)
- [实战示例](#实战示例)
- [常见问题](#常见问题)
- [最佳实践](#最佳实践)

---

## Spring Security简介

### 什么是Spring Security？

Spring Security是一个功能强大且高度可定制的**身份验证**和**访问控制**框架。它是保护基于Spring的应用程序的事实标准。

### 核心功能

1. **认证（Authentication）** - 验证用户身份
2. **授权（Authorization）** - 控制用户访问权限
3. **防护攻击** - CSRF、会话固定、点击劫持等
4. **集成支持** - OAuth2、LDAP、JWT等

### 为什么需要Spring Security？

```java
// ❌ 没有Spring Security
@PostMapping("/transfer")
public void transfer(TransferRequest request) {
    // 任何人都可以转账！危险！
    accountService.transfer(request);
}

// ✅ 使用Spring Security
@PostMapping("/transfer")
@PreAuthorize("hasRole('USER')")  // 必须登录
public void transfer(TransferRequest request, @AuthenticationPrincipal User user) {
    // 只有认证用户可以转账
    accountService.transfer(user.getId(), request);
}
```

---

## 核心概念

### 1. Authentication（认证）

**认证对象包含：**
- `Principal` - 用户标识（通常是用户名）
- `Credentials` - 凭证（通常是密码）
- `Authorities` - 权限列表

```java
// 认证对象示例
public interface Authentication extends Principal, Serializable {
    Collection<? extends GrantedAuthority> getAuthorities();  // 权限
    Object getCredentials();                                  // 凭证
    Object getDetails();                                      // 详细信息
    Object getPrincipal();                                    // 用户主体
    boolean isAuthenticated();                                // 是否已认证
    void setAuthenticated(boolean isAuthenticated);
}
```

### 2. SecurityContext（安全上下文）

保存当前认证用户的信息。

```java
// 获取当前用户
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
String username = auth.getName();
Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
```

### 3. UserDetails（用户详情）

Spring Security对用户信息的抽象。

```java
public interface UserDetails extends Serializable {
    String getUsername();                                     // 用户名
    String getPassword();                                     // 密码
    Collection<? extends GrantedAuthority> getAuthorities(); // 权限
    boolean isAccountNonExpired();                           // 账户未过期
    boolean isAccountNonLocked();                            // 账户未锁定
    boolean isCredentialsNonExpired();                       // 凭证未过期
    boolean isEnabled();                                     // 账户启用
}
```

### 4. GrantedAuthority（授权）

表示用户的权限。

```java
public interface GrantedAuthority extends Serializable {
    String getAuthority();  // 返回权限字符串，如 "ROLE_ADMIN"
}
```

### 5. SecurityFilterChain（安全过滤链）

Spring Security的核心，处理所有HTTP请求。

```
HTTP请求 → SecurityFilterChain → Controller
          ↓
    认证过滤器
    授权过滤器
    CSRF过滤器
    会话管理过滤器
    异常处理过滤器
```

---

## 项目中的实现

### 1. 依赖配置

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

### 2. SecurityConfig配置

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用CSRF（使用JWT时）
            .csrf(csrf -> csrf.disable())

            // 配置CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // 配置会话策略
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 配置授权规则
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/v1/users/register", "/api/v1/users/login")
                    .permitAll()  // 公开端点
                .anyRequest()
                    .authenticated()  // 其他需认证
            );

        return http.build();
    }
}
```

### 3. 配置解析

#### 3.1 禁用CSRF
```java
.csrf(csrf -> csrf.disable())
```

**为什么禁用？**
- 使用JWT认证（无状态）
- 不使用Cookie存储会话
- CSRF主要防护Cookie-based认证

**什么时候需要CSRF？**
```java
// 传统Session认证
.csrf(csrf -> csrf
    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
)
```

#### 3.2 CORS配置
```java
.cors(cors -> cors.configurationSource(corsConfigurationSource()))

@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    // 允许的源（开发环境用*，生产环境指定域名）
    configuration.setAllowedOriginPatterns(Arrays.asList("*"));

    // 允许的HTTP方法
    configuration.setAllowedMethods(Arrays.asList(
        "GET", "POST", "PUT", "DELETE", "OPTIONS"
    ));

    // 允许的请求头
    configuration.setAllowedHeaders(Arrays.asList("*"));

    // 允许携带凭证（Cookie、Authorization header）
    configuration.setAllowCredentials(true);

    // 预检请求缓存时间（1小时）
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

**CORS工作流程：**
```
前端(localhost:3000) → OPTIONS /api/users → 预检请求
                                          ← Access-Control-Allow-Origin: *
前端 → GET /api/users → 实际请求
                      ← 数据 + CORS头
```

#### 3.3 会话策略
```java
.sessionManagement(session ->
    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
```

**会话策略对比：**

| 策略 | 说明 | 使用场景 |
|------|------|----------|
| `ALWAYS` | 总是创建Session | 传统Web应用 |
| `IF_REQUIRED` | 需要时创建 | 默认策略 |
| `NEVER` | 不创建，但会使用已有Session | 混合认证 |
| `STATELESS` | 完全无状态，不创建Session | **JWT、API** |

**无状态认证优势：**
```
传统Session:
客户端 → 服务器 → 查询Session存储 → 验证用户
                 ↓
              Redis/内存

JWT无状态:
客户端 → 服务器 → 验证JWT签名 → 解析用户信息
                 ↓
              无需存储！
```

#### 3.4 授权规则
```java
.authorizeHttpRequests(authz -> authz
    .requestMatchers("/api/v1/users/register", "/api/v1/users/login")
        .permitAll()        // 公开访问
    .requestMatchers("/api/v1/admin/**")
        .hasRole("ADMIN")   // 需要ADMIN角色
    .anyRequest()
        .authenticated()    // 需要认证
)
```

**匹配器方法：**

```java
// 1. URL匹配
.requestMatchers("/public/**").permitAll()
.requestMatchers("/admin/**").hasRole("ADMIN")

// 2. HTTP方法匹配
.requestMatchers(HttpMethod.GET, "/api/users").permitAll()
.requestMatchers(HttpMethod.POST, "/api/users").hasRole("ADMIN")

// 3. 正则匹配
.requestMatchers(RegexRequestMatcher.regexMatcher("/api/v\\d+/.*")).permitAll()

// 4. Ant风格匹配
.requestMatchers(AntPathRequestMatcher.antMatcher("/api/**/public")).permitAll()
```

**权限表达式：**

```java
.permitAll()                          // 所有人
.authenticated()                      // 已认证用户
.hasRole("ADMIN")                     // 有ADMIN角色
.hasAnyRole("ADMIN", "USER")          // 有任一角色
.hasAuthority("READ_PRIVILEGE")       // 有指定权限
.hasAnyAuthority("READ", "WRITE")     // 有任一权限
.access("hasRole('ADMIN') and hasIpAddress('192.168.1.0/24')")  // SpEL表达式
```

---

## 认证流程详解

### 1. 传统用户名密码认证

```java
@Service
public class UserServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        // 1. 从数据库查询用户
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));

        // 2. 转换为Spring Security的UserDetails
        return org.springframework.security.core.userdetails.User
            .withUsername(user.getUsername())
            .password(user.getPassword())  // 已加密的密码
            .authorities("ROLE_USER")       // 用户权限
            .accountExpired(false)
            .accountLocked(false)
            .credentialsExpired(false)
            .disabled(!user.isEnabled())
            .build();
    }
}
```

### 2. 认证流程图

```
用户登录请求
    ↓
UsernamePasswordAuthenticationFilter (过滤器)
    ↓
AuthenticationManager (认证管理器)
    ↓
AuthenticationProvider (认证提供者)
    ↓
UserDetailsService.loadUserByUsername() (加载用户)
    ↓
PasswordEncoder.matches() (验证密码)
    ↓
认证成功 → SecurityContext (保存认证信息)
    ↓
返回JWT Token
```

### 3. 登录实现示例

```java
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        try {
            // 1. 创建认证令牌
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                request.getUsername(),
                request.getPassword()
            );

            // 2. 执行认证
            Authentication authenticated = authenticationManager.authenticate(authentication);

            // 3. 生成JWT
            String token = jwtUtil.generateToken(authenticated.getName());

            // 4. 返回token
            return ResponseEntity.ok(new LoginResponse(token));

        } catch (AuthenticationException e) {
            throw new BadCredentialsException("用户名或密码错误");
        }
    }
}
```

### 4. 密码加密

```java
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

// 注册时加密密码
@Service
public class UserService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    public void registerUser(RegisterRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());

        // 加密密码
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        user.setPassword(encodedPassword);

        userRepository.save(user);
    }
}
```

**BCrypt特点：**
- 自动加盐（每次结果不同）
- 慢速算法（防暴力破解）
- 单向加密（不可逆）

```java
// 同一密码，多次加密结果不同
String password = "123456";
String hash1 = encoder.encode(password);
// $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH

String hash2 = encoder.encode(password);
// $2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG

// 但都能验证成功
encoder.matches(password, hash1); // true
encoder.matches(password, hash2); // true
```

---

## 授权机制

### 1. 方法级授权

```java
@Configuration
@EnableMethodSecurity  // Spring Security 6.x
// @EnableGlobalMethodSecurity(prePostEnabled = true)  // Spring Security 5.x
public class SecurityConfig {
}

@Service
public class OrderService {

    // 前置授权 - 方法执行前检查
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteOrder(Long orderId) {
        // 只有ADMIN可以删除订单
    }

    // 后置授权 - 方法执行后检查
    @PostAuthorize("returnObject.userId == authentication.principal.id")
    public Order getOrder(Long orderId) {
        // 只能查看自己的订单
        return orderRepository.findById(orderId);
    }

    // 过滤集合
    @PreFilter("filterObject.userId == authentication.principal.id")
    public void processOrders(List<Order> orders) {
        // 过滤掉不属于当前用户的订单
    }

    @PostFilter("filterObject.userId == authentication.principal.id")
    public List<Order> getAllOrders() {
        // 返回时只包含当前用户的订单
        return orderRepository.findAll();
    }

    // 安全参数
    @PreAuthorize("#userId == authentication.principal.id")
    public void updateProfile(Long userId, ProfileDTO profile) {
        // 只能更新自己的资料
    }
}
```

### 2. SpEL表达式

```java
// 角色检查
@PreAuthorize("hasRole('ADMIN')")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")

// 权限检查
@PreAuthorize("hasAuthority('READ_PRIVILEGE')")
@PreAuthorize("hasAnyAuthority('READ', 'WRITE')")

// 用户检查
@PreAuthorize("authentication.principal.username == #username")
@PreAuthorize("authentication.principal.id == #userId")

// 组合条件
@PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
@PreAuthorize("hasRole('ADMIN') and hasAuthority('DELETE')")

// 复杂逻辑
@PreAuthorize("@orderService.isOwner(#orderId, authentication.principal.id)")
```

### 3. 自定义权限评估器

```java
@Component("orderSecurity")
public class OrderSecurityService {

    @Autowired
    private OrderRepository orderRepository;

    public boolean isOwner(Long orderId, Long userId) {
        return orderRepository.findById(orderId)
            .map(order -> order.getUserId().equals(userId))
            .orElse(false);
    }

    public boolean canAccess(Long orderId, Authentication authentication) {
        User user = (User) authentication.getPrincipal();

        // 管理员可以访问所有订单
        if (user.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            return true;
        }

        // 普通用户只能访问自己的订单
        return isOwner(orderId, user.getId());
    }
}

// 使用自定义评估器
@PreAuthorize("@orderSecurity.canAccess(#orderId, authentication)")
public Order getOrder(Long orderId) {
    return orderRepository.findById(orderId).orElseThrow();
}
```

---

## JWT集成

### 1. JWT工具类

```java
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

    // 生成Token
    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", username);
        claims.put("created", new Date());

        return Jwts.builder()
            .setClaims(claims)
            .setSubject(username)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(signatureAlgorithm, secret)
            .compact();
    }

    // 从Token获取用户名
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    // 从Token获取过期时间
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    // 获取Claims
    private <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    // 解析Token
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
            .setSigningKey(secret)
            .parseClaimsJws(token)
            .getBody();
    }

    // 验证Token是否过期
    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    // 验证Token
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = getUsernameFromToken(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
}
```

### 2. JWT认证过滤器

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // 1. 从请求头获取Token
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 2. 提取Token
            final String token = authHeader.substring(7);
            final String username = jwtUtil.getUsernameFromToken(token);

            // 3. 验证Token并设置认证信息
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtUtil.validateToken(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());

                    authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));

                    // 设置到Security上下文
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception e) {
            logger.error("JWT认证失败: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
```

### 3. 注册JWT过滤器

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/v1/users/login", "/api/v1/users/register")
                    .permitAll()
                .anyRequest()
                    .authenticated()
            )
            // 添加JWT过滤器
            .addFilterBefore(jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

### 4. JWT Token刷新

```java
@PostMapping("/refresh")
public ResponseEntity<TokenResponse> refreshToken(
        @RequestHeader("Authorization") String authHeader) {

    String oldToken = authHeader.substring(7);

    // 验证旧Token
    if (jwtUtil.isTokenExpired(oldToken)) {
        throw new BadCredentialsException("Token已过期");
    }

    String username = jwtUtil.getUsernameFromToken(oldToken);

    // 生成新Token
    String newToken = jwtUtil.generateToken(username);
    String newRefreshToken = jwtUtil.generateRefreshToken(username);

    return ResponseEntity.ok(new TokenResponse(newToken, newRefreshToken));
}
```

---

## 实战示例

### 示例1: 用户注册

```java
@PostMapping("/register")
public ResponseEntity<ApiResponse> register(@Valid @RequestBody RegisterRequest request) {

    // 1. 验证用户名是否存在
    if (userRepository.existsByUsername(request.getUsername())) {
        throw new DuplicateResourceException("用户名已存在");
    }

    // 2. 验证邮箱是否存在
    if (userRepository.existsByEmail(request.getEmail())) {
        throw new DuplicateResourceException("邮箱已被使用");
    }

    // 3. 创建用户
    User user = new User();
    user.setUsername(request.getUsername());
    user.setEmail(request.getEmail());

    // 4. 加密密码
    String encodedPassword = passwordEncoder.encode(request.getPassword());
    user.setPassword(encodedPassword);

    // 5. 设置默认角色
    user.setRoles(Set.of("ROLE_USER"));
    user.setEnabled(true);

    // 6. 保存用户
    userRepository.save(user);

    return ResponseEntity.ok(ApiResponse.success("注册成功"));
}
```

### 示例2: 用户登录

```java
@PostMapping("/login")
public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {

    // 1. 认证用户
    Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            request.getUsernameOrEmail(),
            request.getPassword()
        )
    );

    // 2. 设置认证上下文
    SecurityContextHolder.getContext().setAuthentication(authentication);

    // 3. 生成Token
    String accessToken = jwtUtil.generateToken(authentication.getName());
    String refreshToken = jwtUtil.generateRefreshToken(authentication.getName());

    // 4. 保存会话信息（可选）
    UserSession session = new UserSession();
    session.setSessionToken(accessToken);
    session.setRefreshToken(refreshToken);
    session.setExpiresAt(LocalDateTime.now().plusDays(1));
    sessionRepository.save(session);

    // 5. 返回响应
    return ResponseEntity.ok(new LoginResponse(accessToken, refreshToken));
}
```

### 示例3: 获取当前用户

```java
@GetMapping("/profile")
public ResponseEntity<UserProfileResponse> getProfile(
        @AuthenticationPrincipal UserDetails currentUser) {

    // 从认证对象获取用户信息
    User user = userRepository.findByUsername(currentUser.getUsername())
        .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

    UserProfileResponse response = UserProfileResponse.builder()
        .id(user.getId())
        .username(user.getUsername())
        .email(user.getEmail())
        .roles(user.getRoles())
        .build();

    return ResponseEntity.ok(response);
}

// 或者使用SecurityContextHolder
@GetMapping("/me")
public ResponseEntity<UserProfileResponse> getCurrentUser() {

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();

    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

    return ResponseEntity.ok(toResponse(user));
}
```

### 示例4: 更新用户资料

```java
@PutMapping("/profile")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<UserProfileResponse> updateProfile(
        @Valid @RequestBody UpdateProfileRequest request,
        @AuthenticationPrincipal UserDetails currentUser) {

    User user = userRepository.findByUsername(currentUser.getUsername())
        .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

    // 更新允许修改的字段
    if (request.getEmail() != null) {
        // 验证邮箱唯一性
        if (!user.getEmail().equals(request.getEmail()) &&
            userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("邮箱已被使用");
        }
        user.setEmail(request.getEmail());
    }

    if (request.getPhone() != null) {
        user.setPhone(request.getPhone());
    }

    userRepository.save(user);

    return ResponseEntity.ok(toResponse(user));
}
```

### 示例5: 修改密码

```java
@PostMapping("/change-password")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<ApiResponse> changePassword(
        @Valid @RequestBody ChangePasswordRequest request,
        @AuthenticationPrincipal UserDetails currentUser) {

    User user = userRepository.findByUsername(currentUser.getUsername())
        .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

    // 验证旧密码
    if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
        throw new BadCredentialsException("旧密码错误");
    }

    // 验证新密码和确认密码
    if (!request.getNewPassword().equals(request.getConfirmPassword())) {
        throw new ValidationException("两次输入的密码不一致");
    }

    // 更新密码
    String encodedPassword = passwordEncoder.encode(request.getNewPassword());
    user.setPassword(encodedPassword);
    userRepository.save(user);

    // 使所有会话失效（可选）
    sessionRepository.deleteByUserId(user.getId());

    return ResponseEntity.ok(ApiResponse.success("密码修改成功，请重新登录"));
}
```

### 示例6: 管理员操作

```java
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")  // 整个Controller需要ADMIN权限
public class AdminController {

    // 获取所有用户
    @GetMapping("/users")
    public ResponseEntity<Page<UserDTO>> getAllUsers(Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);
        return ResponseEntity.ok(users.map(this::toDTO));
    }

    // 禁用用户
    @PostMapping("/users/{userId}/disable")
    public ResponseEntity<ApiResponse> disableUser(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        user.setEnabled(false);
        userRepository.save(user);

        // 使该用户的所有会话失效
        sessionRepository.deleteByUserId(userId);

        return ResponseEntity.ok(ApiResponse.success("用户已禁用"));
    }

    // 分配角色
    @PostMapping("/users/{userId}/roles")
    public ResponseEntity<ApiResponse> assignRole(
            @PathVariable Long userId,
            @RequestBody AssignRoleRequest request) {

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        user.getRoles().add(request.getRole());
        userRepository.save(user);

        return ResponseEntity.ok(ApiResponse.success("角色分配成功"));
    }
}
```

---

## 常见问题

### Q1: 如何获取当前登录用户？

**方法1: 使用@AuthenticationPrincipal**
```java
@GetMapping("/profile")
public User getProfile(@AuthenticationPrincipal UserDetails user) {
    return userRepository.findByUsername(user.getUsername());
}
```

**方法2: 使用SecurityContextHolder**
```java
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
String username = auth.getName();
```

**方法3: 使用Principal**
```java
@GetMapping("/profile")
public User getProfile(Principal principal) {
    return userRepository.findByUsername(principal.getName());
}
```

### Q2: 如何处理认证失败？

```java
@Component
public class AuthenticationFailureHandler
        implements org.springframework.security.web.authentication.AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> data = new HashMap<>();
        data.put("success", false);
        data.put("message", "认证失败: " + exception.getMessage());

        ObjectMapper mapper = new ObjectMapper();
        response.getWriter().write(mapper.writeValueAsString(data));
    }
}
```

### Q3: 如何实现Remember Me？

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .rememberMe(remember -> remember
            .key("uniqueAndSecret")
            .tokenValiditySeconds(86400 * 7)  // 7天
            .userDetailsService(userDetailsService)
        );
    return http.build();
}
```

### Q4: 如何实现登出？

```java
@PostMapping("/logout")
public ResponseEntity<ApiResponse> logout(
        @RequestHeader("Authorization") String authHeader) {

    String token = authHeader.substring(7);

    // 1. 从数据库删除会话
    sessionRepository.deleteBySessionToken(token);

    // 2. 清除Security上下文
    SecurityContextHolder.clearContext();

    // 3. 可选：将Token加入黑名单
    tokenBlacklistService.addToBlacklist(token);

    return ResponseEntity.ok(ApiResponse.success("登出成功"));
}
```

### Q5: 如何防止并发登录？

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .sessionManagement(session -> session
            .maximumSessions(1)  // 最多1个会话
            .maxSessionsPreventsLogin(true)  // 拒绝新登录
            .expiredUrl("/login?expired")
        );
    return http.build();
}
```

### Q6: 如何自定义登录页面？

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .formLogin(form -> form
            .loginPage("/login")
            .loginProcessingUrl("/perform_login")
            .defaultSuccessUrl("/dashboard", true)
            .failureUrl("/login?error=true")
            .permitAll()
        );
    return http.build();
}
```

### Q7: 如何测试Security配置？

```java
@SpringBootTest
@AutoConfigureMockMvc
public class SecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testPublicEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/users/login"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    public void testProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/users/profile"))
            .andExpect(status().isOk());
    }

    @Test
    public void testUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users/profile"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    public void testAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
            .andExpect(status().isOk());
    }
}
```

---

## 最佳实践

### 1. 密码安全

```java
// ✅ 使用BCrypt
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);  // 增加强度
}

// ❌ 不要使用弱加密
// new MessageDigestPasswordEncoder("MD5")  // 不安全！
// NoOpPasswordEncoder.getInstance()       // 明文！
```

### 2. Token管理

```java
// ✅ 短期访问Token + 长期刷新Token
String accessToken = generateToken(username, 15 * 60);       // 15分钟
String refreshToken = generateRefreshToken(username, 30 * 24 * 60 * 60);  // 30天

// ✅ Token黑名单
public class TokenBlacklistService {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public void addToBlacklist(String token) {
        long expiration = jwtUtil.getExpirationDateFromToken(token).getTime();
        redisTemplate.opsForValue().set(
            "blacklist:" + token,
            "revoked",
            expiration - System.currentTimeMillis(),
            TimeUnit.MILLISECONDS
        );
    }

    public boolean isBlacklisted(String token) {
        return redisTemplate.hasKey("blacklist:" + token);
    }
}
```

### 3. 生产环境配置

```yaml
# application-prod.yml
jwt:
  secret: ${JWT_SECRET}  # 从环境变量读取
  expiration: 900000     # 15分钟

spring:
  security:
    # 启用HTTPS
    require-ssl: true

cors:
  # 指定允许的域名
  allowed-origins: https://yourdomain.com
```

### 4. 审计日志

```java
@Aspect
@Component
public class SecurityAuditAspect {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @AfterReturning("@annotation(PreAuthorize)")
    public void logSecurityEvent(JoinPoint joinPoint) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        AuditLog log = new AuditLog();
        log.setUsername(auth.getName());
        log.setAction(joinPoint.getSignature().getName());
        log.setTimestamp(LocalDateTime.now());
        log.setIpAddress(getClientIP());

        auditLogRepository.save(log);
    }
}
```

### 5. 角色层次结构

```java
@Bean
public RoleHierarchy roleHierarchy() {
    RoleHierarchyImpl hierarchy = new RoleHierarchyImpl();
    hierarchy.setHierarchy(
        "ROLE_ADMIN > ROLE_MANAGER > ROLE_USER"
    );
    return hierarchy;
}

// ADMIN拥有MANAGER和USER的所有权限
// MANAGER拥有USER的权限
```

### 6. 多因素认证(MFA)

```java
@Service
public class MFAService {

    public String generateTOTPSecret() {
        return Base32.random();
    }

    public boolean validateTOTP(String secret, String code) {
        TimeBasedOneTimePasswordGenerator totp =
            new TimeBasedOneTimePasswordGenerator();

        String generatedCode = totp.generate(secret, Instant.now());
        return code.equals(generatedCode);
    }
}

@PostMapping("/verify-mfa")
public ResponseEntity<?> verifyMFA(
        @RequestBody MFAVerifyRequest request,
        @AuthenticationPrincipal User user) {

    if (mfaService.validateTOTP(user.getMfaSecret(), request.getCode())) {
        // 生成完整访问Token
        String token = jwtUtil.generateToken(user.getUsername());
        return ResponseEntity.ok(new TokenResponse(token));
    }

    throw new BadCredentialsException("MFA验证失败");
}
```

### 7. IP白名单

```java
@Component
public class IpAddressAuthenticationFilter extends OncePerRequestFilter {

    @Value("#{'${security.allowed-ips}'.split(',')}")
    private List<String> allowedIps;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String clientIp = getClientIP(request);

        if (request.getRequestURI().startsWith("/api/v1/admin/") &&
            !allowedIps.contains(clientIp)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                "Access denied from IP: " + clientIp);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
```

### 8. 速率限制

```java
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    @Autowired
    private RedisTemplate<String, Integer> redisTemplate;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String key = "rate_limit:" + getClientIP(request);

        Integer requests = redisTemplate.opsForValue().get(key);
        if (requests == null) {
            redisTemplate.opsForValue().set(key, 1, 1, TimeUnit.MINUTES);
        } else if (requests >= 100) {  // 每分钟100次
            response.sendError(HttpServletResponse.SC_TOO_MANY_REQUESTS,
                "Rate limit exceeded");
            return;
        } else {
            redisTemplate.opsForValue().increment(key);
        }

        filterChain.doFilter(request, response);
    }
}
```

---

## 总结

### Spring Security核心要点

1. **认证 (Authentication)** - 证明你是谁
2. **授权 (Authorization)** - 决定你能做什么
3. **过滤器链** - 处理所有安全相关逻辑
4. **无状态JWT** - 适合现代API和微服务
5. **方法级安全** - 细粒度权限控制

### 推荐学习路径

1. ✅ 理解核心概念
2. ✅ 配置基本认证和授权
3. ✅ 集成JWT
4. ✅ 实现方法级安全
5. ✅ 添加审计和监控
6. ✅ 优化生产环境配置

### 参考资源

- 官方文档: https://spring.io/projects/spring-security
- JWT官网: https://jwt.io
- OWASP安全指南: https://owasp.org
- Baeldung教程: https://www.baeldung.com/spring-security

---

**记住**: 安全是一个持续的过程，不是一次性的配置！
