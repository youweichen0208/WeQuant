package com.quant.userservice.service;

import com.quant.userservice.dto.*;
import com.quant.userservice.entity.User;
import jakarta.servlet.http.HttpServletRequest;

public interface UserService {

    /**
     * 用户注册
     */
    UserProfileResponse registerUser(UserRegistrationRequest request);

    /**
     * 用户登录
     */
    LoginResponse loginUser(LoginRequest request, HttpServletRequest httpRequest);

    /**
     * 刷新令牌
     */
    LoginResponse refreshToken(String refreshToken);

    /**
     * 用户登出
     */
    void logoutUser(String sessionToken);

    /**
     * 获取用户信息
     */
    UserProfileResponse getUserProfile(Long userId);

    /**
     * 更新用户信息
     */
    UserProfileResponse updateUserProfile(Long userId, UserProfileUpdateRequest request);

    /**
     * 验证用户凭证
     */
    boolean validateUserCredentials(String usernameOrEmail, String password);

    /**
     * 检查用户名是否可用
     */
    boolean isUsernameAvailable(String username);

    /**
     * 检查邮箱是否可用
     */
    boolean isEmailAvailable(String email);

    /**
     * 根据令牌获取用户
     */
    User getUserBySessionToken(String sessionToken);

    /**
     * 更新用户最后登录时间
     */
    void updateLastLoginTime(Long userId);
}