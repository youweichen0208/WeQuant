package com.quant.userservice.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class UserRegistrationRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    private String username;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 100, message = "密码长度必须在8-100字符之间")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
             message = "密码必须包含至少一个小写字母、一个大写字母和一个数字")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    @Size(max = 100, message = "姓名长度不能超过100字符")
    private String fullName;

    @Pattern(regexp = "^[1-9]\\d{10}$", message = "手机号格式不正确")
    private String phoneNumber;
}