package com.mealorder.common.util;

import com.mealorder.common.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

/**
 * JWT 双 token 工具（jjwt 0.12 API）
 * claim 约定：uid / username / role / type(access|refresh)
 */
@Component
public class JwtUtil {

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final JwtProperties props;
    private final SecretKey key;

    public JwtUtil(JwtProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /** 签发 access token（接口鉴权用，短效） */
    public String generateAccessToken(Long userId, String username, String role) {
        return generate(userId, username, role, TYPE_ACCESS,
                Duration.ofHours(props.getAccessTtlHours()));
    }

    /** 签发 refresh token（换新 token 用，长效；refresh 时双 token 轮换） */
    public String generateRefreshToken(Long userId, String username, String role) {
        return generate(userId, username, role, TYPE_REFRESH,
                Duration.ofDays(props.getRefreshTtlDays()));
    }

    /**
     * 解析并校验签名/过期/签发方，非法抛 JwtException（由调用方转成 B0001）
     */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(props.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private String generate(Long userId, String username, String role, String type, Duration ttl) {
        Date now = new Date();
        return Jwts.builder()
                .issuer(props.getIssuer())
                .subject(String.valueOf(userId))
                .claim("uid", userId)
                .claim("username", username)
                .claim("role", role)
                .claim("type", type)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttl.toMillis()))
                .signWith(key)
                .compact();
    }
}
