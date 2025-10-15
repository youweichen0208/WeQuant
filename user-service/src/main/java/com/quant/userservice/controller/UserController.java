package com.quant.userservice.controller;

import com.quant.userservice.dto.*;
import com.quant.userservice.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserProfileResponse>> registerUser(
            @Valid @RequestBody UserRegistrationRequest request) {
        log.info("用户注册请求: {}", request.getUsername());

        UserProfileResponse user = userService.registerUser(request);
        return ResponseEntity.ok(ApiResponse.success(user, "注册成功"));
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> loginUser(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        log.info("用户登录请求: {}", request.getUsernameOrEmail());

        LoginResponse response = userService.loginUser(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(response, "登录成功"));
    }

    /**
     * 刷新令牌
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            @RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        log.info("刷新令牌请求");

        LoginResponse response = userService.refreshToken(refreshToken);
        return ResponseEntity.ok(ApiResponse.success(response, "令牌刷新成功"));
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logoutUser(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        log.info("用户登出请求");

        userService.logoutUser(token);
        return ResponseEntity.ok(ApiResponse.success(null, "登出成功"));
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUserProfile(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        var user = userService.getUserBySessionToken(token);

        UserProfileResponse profile = userService.getUserProfile(user.getId());
        return ResponseEntity.ok(ApiResponse.success(profile, "获取用户信息成功"));
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateUserProfile(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody UserProfileUpdateRequest request) {
        String token = authHeader.replace("Bearer ", "");
        var user = userService.getUserBySessionToken(token);

        UserProfileResponse updatedProfile = userService.updateUserProfile(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(updatedProfile, "用户信息更新成功"));
    }

    /**
     * 检查用户名可用性
     */
    @GetMapping("/check-username")
    public ResponseEntity<ApiResponse<Boolean>> checkUsernameAvailability(
            @RequestParam String username) {
        boolean available = userService.isUsernameAvailable(username);
        return ResponseEntity.ok(ApiResponse.success(available,
            available ? "用户名可用" : "用户名已被使用"));
    }

    /**
     * 检查邮箱可用性
     */
    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<Boolean>> checkEmailAvailability(
            @RequestParam String email) {
        boolean available = userService.isEmailAvailable(email);
        log.info("check email is here");
        return ResponseEntity.ok(ApiResponse.success(available,
            available ? "邮箱可用" : "邮箱已被使用"));
    }
}