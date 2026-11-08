package com.mealorder.auth.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mealorder.auth.service.AuthService;
import com.mealorder.common.exception.BusinessException;
import com.mealorder.common.result.R;
import com.mealorder.common.result.ResultCode;
import com.mealorder.common.util.UserContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 登录态拦截：白名单外全部要求合法 access token，解析结果写入 UserContext
 * 响应保持 HTTP 200 + 业务码（与前端 request.ts 的处理约定一致）
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final AuthService authService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return reject(response, ResultCode.UNAUTHORIZED);
        }
        Claims claims;
        try {
            claims = authService.parseAccessToken(auth.substring(7));
        } catch (BusinessException e) {
            return reject(response, ResultCode.UNAUTHORIZED, e.getMessage());
        }
        UserContext.set(new UserContext.CurrentUser(
                Long.valueOf(claims.getSubject()),
                claims.get("username", String.class),
                claims.get("role", String.class)));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) {
        UserContext.clear();
    }

    private boolean reject(HttpServletResponse response, ResultCode code) throws Exception {
        return reject(response, code, null);
    }

    private boolean reject(HttpServletResponse response, ResultCode code, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        R<Void> body = message == null ? R.fail(code) : R.fail(code, message);
        response.getWriter().write(objectMapper.writeValueAsString(body));
        return false;
    }
}
