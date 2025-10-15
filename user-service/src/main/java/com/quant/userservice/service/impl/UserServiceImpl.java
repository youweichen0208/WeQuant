package com.quant.userservice.service.impl;

import com.quant.userservice.dto.*;
import com.quant.userservice.entity.User;
import com.quant.userservice.entity.UserSession;
import com.quant.userservice.repository.UserRepository;
import com.quant.userservice.repository.UserSessionRepository;
import com.quant.userservice.service.UserService;
import com.quant.userservice.util.JwtTokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;

    @Override
    public UserProfileResponse registerUser(UserRegistrationRequest request) {
        log.info("注册新用户: {}", request.getUsername());

        // 验证用户名和邮箱唯一性
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("邮箱已被注册");
        }

        // 验证密码确认
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("两次输入的密码不一致");
        }

        // 创建新用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setStatus(User.UserStatus.ACTIVE);
        user.setRole(User.UserRole.USER);

        User savedUser = userRepository.save(user);
        log.info("用户注册成功: {}", savedUser.getUsername());

        return convertToProfileResponse(savedUser);
    }

    @Override
    public LoginResponse loginUser(LoginRequest request, HttpServletRequest httpRequest) {
        log.info("用户登录: {}", request.getUsernameOrEmail());

        // 查找用户
        Optional<User> userOpt = userRepository.findByUsernameOrEmail(request.getUsernameOrEmail());
        if (userOpt.isEmpty()) {
            throw new RuntimeException("用户名或密码错误");
        }

        User user = userOpt.get();

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 检查用户状态
        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new RuntimeException("账户已被禁用或锁定");
        }

        // 生成JWT令牌
        String accessToken = jwtTokenUtil.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtTokenUtil.generateRefreshToken(user.getId());

        // 创建会话记录
        UserSession session = new UserSession();
        session.setUser(user);
        session.setSessionToken(accessToken);
        session.setRefreshToken(refreshToken);
        session.setIpAddress(getClientIpAddress(httpRequest));
        session.setUserAgent(httpRequest.getHeader("User-Agent"));
        session.setDeviceInfo(request.getDeviceInfo());
        session.setExpiresAt(jwtTokenUtil.getExpirationDateFromToken(accessToken).toInstant()
                .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
        session.setLastAccessedAt(LocalDateTime.now());

        userSessionRepository.save(session);

        // 更新最后登录时间
        updateLastLoginTime(user.getId());

        log.info("用户登录成功: {}", user.getUsername());

        return new LoginResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtTokenUtil.getRemainingValidityInSeconds(accessToken),
                convertToProfileResponse(user)
        );
    }

    @Override
    public LoginResponse refreshToken(String refreshToken) {
        log.info("刷新令牌请求");

        // 验证刷新令牌
        if (!jwtTokenUtil.validateToken(refreshToken) || jwtTokenUtil.isTokenExpired(refreshToken)) {
            throw new RuntimeException("刷新令牌无效或已过期");
        }

        // 检查令牌类型
        if (!"refresh".equals(jwtTokenUtil.getTokenType(refreshToken))) {
            throw new RuntimeException("无效的令牌类型");
        }

        // 查找会话
        Optional<UserSession> sessionOpt = userSessionRepository.findByRefreshToken(refreshToken);
        if (sessionOpt.isEmpty() || !sessionOpt.get().getIsActive()) {
            throw new RuntimeException("会话不存在或已失效");
        }

        UserSession session = sessionOpt.get();
        User user = session.getUser();

        // 生成新的访问令牌
        String newAccessToken = jwtTokenUtil.generateAccessToken(user.getId(), user.getUsername());
        String newRefreshToken = jwtTokenUtil.generateRefreshToken(user.getId());

        // 更新会话
        session.setSessionToken(newAccessToken);
        session.setRefreshToken(newRefreshToken);
        session.setExpiresAt(jwtTokenUtil.getExpirationDateFromToken(newAccessToken).toInstant()
                .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
        session.setLastAccessedAt(LocalDateTime.now());

        userSessionRepository.save(session);

        log.info("令牌刷新成功: {}", user.getUsername());

        return new LoginResponse(
                newAccessToken,
                newRefreshToken,
                "Bearer",
                jwtTokenUtil.getRemainingValidityInSeconds(newAccessToken),
                convertToProfileResponse(user)
        );
    }

    @Override
    public void logoutUser(String sessionToken) {
        log.info("用户登出请求");

        Optional<UserSession> sessionOpt = userSessionRepository.findBySessionTokenAndIsActiveTrue(sessionToken);
        if (sessionOpt.isPresent()) {
            UserSession session = sessionOpt.get();
            session.setIsActive(false);
            userSessionRepository.save(session);
            log.info("用户登出成功: {}", session.getUser().getUsername());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return convertToProfileResponse(user);
    }

    @Override
    public UserProfileResponse updateUserProfile(Long userId, UserProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 更新用户信息
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getRiskLevel() != null) {
            user.setRiskLevel(request.getRiskLevel());
        }
        if (request.getMaxDailyLoss() != null) {
            user.setMaxDailyLoss(request.getMaxDailyLoss());
        }
        if (request.getMaxPositionRatio() != null) {
            user.setMaxPositionRatio(request.getMaxPositionRatio());
        }
        if (request.getEnableTrading() != null) {
            user.setEnableTrading(request.getEnableTrading());
        }
        if (request.getEnableNotifications() != null) {
            user.setEnableNotifications(request.getEnableNotifications());
        }

        User savedUser = userRepository.save(user);
        log.info("用户信息更新成功: {}", savedUser.getUsername());

        return convertToProfileResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validateUserCredentials(String usernameOrEmail, String password) {
        Optional<User> userOpt = userRepository.findByUsernameOrEmail(usernameOrEmail);
        if (userOpt.isEmpty()) {
            return false;
        }
        return passwordEncoder.matches(password, userOpt.get().getPassword());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserBySessionToken(String sessionToken) {
        Optional<UserSession> sessionOpt = userSessionRepository.findBySessionTokenAndIsActiveTrue(sessionToken);
        if (sessionOpt.isEmpty()) {
            throw new RuntimeException("会话不存在或已失效");
        }

        UserSession session = sessionOpt.get();

        // 更新最后访问时间
        session.setLastAccessedAt(LocalDateTime.now());
        userSessionRepository.save(session);

        return session.getUser();
    }

    @Override
    public void updateLastLoginTime(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
    }

    private UserProfileResponse convertToProfileResponse(User user) {
        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setRole(user.getRole());
        response.setStatus(user.getStatus());
        response.setAccountBalance(user.getAccountBalance());
        response.setAvailableBalance(user.getAvailableBalance());
        response.setRiskLevel(user.getRiskLevel());
        response.setEnableTrading(user.getEnableTrading());
        response.setEnableNotifications(user.getEnableNotifications());
        response.setCreatedAt(user.getCreatedAt());
        response.setLastLoginAt(user.getLastLoginAt());
        return response;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}