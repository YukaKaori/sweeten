package com.itheima;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtTest {

    private static final String SIGN_KEY = "DessertShop@2024#SecureKey!@#$%^&*";

    @Test
    public void testGenJwt() {
        SecretKey key = Keys.hmacShaKeyFor(SIGN_KEY.getBytes(StandardCharsets.UTF_8));

        Map<String, Object> claims = new HashMap<>();
        claims.put("id", 10);
        claims.put("username", "itheima");

        String jwt = Jwts.builder()
                .claims(claims)
                .signWith(key)
                .expiration(new Date(System.currentTimeMillis() + 12 * 3600 * 1000))
                .compact();

        System.out.println("生成的JWT: " + jwt);
    }

    @Test
    public void testParseJwt() {
        SecretKey key = Keys.hmacShaKeyFor(SIGN_KEY.getBytes(StandardCharsets.UTF_8));

        Map<String, Object> claims = new HashMap<>();
        claims.put("id", 10);
        claims.put("username", "itheima");

        String jwt = Jwts.builder()
                .claims(claims)
                .signWith(key)
                .expiration(new Date(System.currentTimeMillis() + 12 * 3600 * 1000))
                .compact();

        Claims parsedClaims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(jwt)
                .getPayload();

        System.out.println("解析结果: " + parsedClaims);
        System.out.println("ID: " + parsedClaims.get("id"));
        System.out.println("用户名: " + parsedClaims.get("username"));
    }
}
