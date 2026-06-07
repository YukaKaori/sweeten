package com.itheima.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT工具类 (基于 jjwt 0.12.x)
 */
@Component
public class JwtUtils {

    private final SecretKey key;
    private final Long expire;

    /**
     * 构造器注入 — 密钥通过环境变量或配置文件注入
     */
    public JwtUtils(@Value("${jwt.secret}") String signKey,
                    @Value("${jwt.expire:43200000}") Long expire) {
        this.key = Keys.hmacShaKeyFor(signKey.getBytes(StandardCharsets.UTF_8));
        this.expire = expire;
    }

    /**
     * 生成JWT令牌
     * @param claims 负载数据
     * @return JWT令牌
     */
    public String generateJwt(Map<String, Object> claims) {
        return Jwts.builder()
                .claims(claims)
                .signWith(key)
                .expiration(new Date(System.currentTimeMillis() + expire))
                .compact();
    }

    /**
     * 解析JWT令牌
     * @param jwt JWT令牌
     * @return JWT负载 Claims
     */
    public Claims parseJWT(String jwt) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(jwt)
                .getPayload();
    }
}
