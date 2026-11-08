package com.mealorder.auth.controller;

import com.mealorder.auth.dto.AuthDtos.LoginRequest;
import com.mealorder.auth.dto.AuthDtos.LoginResponse;
import com.mealorder.auth.dto.AuthDtos.RefreshRequest;
import com.mealorder.auth.service.AuthService;
import com.mealorder.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端认证接口（login/refresh 在拦截器白名单内，logout 前端清凭证即可）
 */
@Tag(name = "认证")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "登录（签发双 token）")
    @PostMapping("/login")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return R.ok(authService.login(request));
    }

    @Operation(summary = "刷新（校验 refresh token 并轮换双 token）")
    @PostMapping("/refresh")
    public R<LoginResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return R.ok(authService.refresh(request.refreshToken()));
    }

    @Operation(summary = "登出（无状态 JWT，前端清凭证即可，接口仅作语义占位）")
    @PostMapping("/logout")
    public R<Void> logout() {
        return R.ok();
    }
}
