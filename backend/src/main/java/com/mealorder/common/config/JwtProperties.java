package com.mealorder.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置项（application.yml jwt.*）
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** HMAC-SHA256 密钥，至少 32 字节；开发期写 yml，M9 部署改 .env 注入 */
    private String secret;
    private String issuer;
    /** access token 有效期（小时） */
    private long accessTtlHours;
    /** refresh token 有效期（天） */
    private long refreshTtlDays;
}
