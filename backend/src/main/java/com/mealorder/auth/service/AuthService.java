package com.mealorder.auth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mealorder.auth.dto.AuthDtos.LoginRequest;
import com.mealorder.auth.dto.AuthDtos.LoginResponse;
import com.mealorder.auth.entity.AdminUser;
import com.mealorder.auth.mapper.AdminUserMapper;
import com.mealorder.common.exception.BusinessException;
import com.mealorder.common.result.ResultCode;
import com.mealorder.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 登录/刷新：BCrypt 校验 + JWT 双 token 签发与轮换
 */
@Slf4j
@Service
public class AuthService {

    private final AdminUserMapper adminUserMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthService(AdminUserMapper adminUserMapper, JwtUtil jwtUtil) {
        this.adminUserMapper = adminUserMapper;
        this.jwtUtil = jwtUtil;
    }

    public LoginResponse login(LoginRequest request) {
        AdminUser user = adminUserMapper.selectOne(
                Wrappers.<AdminUser>lambdaQuery().eq(AdminUser::getUsername, request.username()));
        // 用户不存在与密码错误给同一句提示，不泄露账号存在性
        if (user == null || user.getStatus() == 0 || !encoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "用户名或密码错误");
        }
        log.info("管理端登录成功: username={}, role={}", user.getUsername(), user.getRole());
        return issue(user);
    }

    /**
     * 刷新：校验 refresh token 并双 token 轮换（旧的 access/refresh 同时作废——无状态 JWT
     * 无法真正拉黑，轮换至少保证旧 refresh 不能无限续期，Redis 黑名单留 M2+）
     */
    public LoginResponse refresh(String refreshToken) {
        Claims claims = parseAndCheck(refreshToken, JwtUtil.TYPE_REFRESH);
        AdminUser user = adminUserMapper.selectById(Long.valueOf(claims.getSubject()));
        if (user == null || user.getStatus() == 0) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return issue(user);
    }

    /** 从任意合法 access token 还原当前用户（拦截器用） */
    public Claims parseAccessToken(String token) {
        return parseAndCheck(token, JwtUtil.TYPE_ACCESS);
    }

    private LoginResponse issue(AdminUser user) {
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername(), user.getRole());
        return new LoginResponse(accessToken, refreshToken, user.getUsername(), user.getRole());
    }

    private Claims parseAndCheck(String token, String expectedType) {
        Claims claims;
        try {
            claims = jwtUtil.parse(token);
        } catch (JwtException e) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        if (!expectedType.equals(claims.get("type", String.class))) {
            // refresh token 冒充 access 用（或反过来），一律拒绝
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return claims;
    }
}
