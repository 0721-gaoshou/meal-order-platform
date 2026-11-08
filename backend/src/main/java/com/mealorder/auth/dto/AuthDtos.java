package com.mealorder.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 认证模块 DTO 集中定义（都是无行为数据体，不值得拆文件）
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank(message = "用户名不能为空") String username,
            @NotBlank(message = "密码不能为空") String password) {
    }

    public record RefreshRequest(
            @NotBlank(message = "refreshToken 不能为空") String refreshToken) {
    }

    /**
     * 登录/刷新成功响应：双 token 轮换，前端整体覆盖旧凭证
     */
    public record LoginResponse(
            String accessToken,
            String refreshToken,
            String username,
            String role) {
    }
}
