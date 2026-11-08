package com.mealorder.common.config;

import com.mealorder.auth.interceptor.AuthInterceptor;
import com.mealorder.auth.interceptor.RoleInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 拦截器注册与鉴权白名单（路径相对 context-path /api）
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final RoleInterceptor roleInterceptor;

    public WebMvcConfig(AuthInterceptor authInterceptor, RoleInterceptor roleInterceptor) {
        this.authInterceptor = authInterceptor;
        this.roleInterceptor = roleInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        String[] whitelist = {
                "/auth/login", "/auth/refresh", "/auth/logout",
                "/health",
                // Knife4j / springdoc 文档资源
                "/doc.html", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/webjars/**",
                "/favicon.ico"
        };
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(whitelist);
        // 角色校验只对标注 @RequireRole 的方法生效，无需白名单
        registry.addInterceptor(roleInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(whitelist);
    }
}
