package com.sweeten.identity.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JWT 签发与校验（纯单元测试，不启动 Spring、不连任何外部服务）
 */
@DisplayName("JWT 签发与校验")
class JwtUtilsTest {

    private static final String KEY = "SWEETEN-test-only-jwt-signing-key-2026";

    @Test
    @DisplayName("签发的令牌能解析出原始声明（id / username / roles）")
    void roundTripsClaims() {
        JwtUtils jwt = new JwtUtils(KEY, 60_000L);
        String token = jwt.generateJwt(Map.of("id", 10, "username", "sweeten", "roles", List.of("cashier")));

        Claims claims = jwt.parseJWT(token);
        assertEquals(10, claims.get("id"));
        assertEquals("sweeten", claims.get("username"));
        assertEquals(List.of("cashier"), claims.get("roles"));
        assertNotNull(claims.getExpiration());
    }

    @Test
    @DisplayName("其它密钥签发的令牌被拒绝")
    void rejectsTokenSignedWithAnotherKey() {
        String foreign = new JwtUtils("another-signing-key-of-at-least-32-bytes", 60_000L)
                .generateJwt(Map.of("id", 1));
        assertThrows(JwtException.class, () -> new JwtUtils(KEY, 60_000L).parseJWT(foreign));
    }

    @Test
    @DisplayName("篡改载荷后签名校验失败")
    void rejectsTamperedToken() {
        JwtUtils jwt = new JwtUtils(KEY, 60_000L);
        String[] parts = jwt.generateJwt(Map.of("id", 2, "roles", List.of("cashier"))).split("\\.");
        String forgedPayload = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"id\":2,\"roles\":[\"super_admin\"]}".getBytes());
        String tampered = parts[0] + "." + forgedPayload + "." + parts[2];
        assertThrows(JwtException.class, () -> jwt.parseJWT(tampered));
    }

    @Test
    @DisplayName("过期令牌被拒绝")
    void rejectsExpiredToken() {
        JwtUtils jwt = new JwtUtils(KEY, -1_000L);
        String token = jwt.generateJwt(Map.of("id", 3));
        assertThrows(ExpiredJwtException.class, () -> jwt.parseJWT(token));
    }

    @Test
    @DisplayName("少于 256 位的密钥无法用于 HS256，启动即失败")
    void rejectsWeakKey() {
        assertThrows(WeakKeyException.class, () -> new JwtUtils("too-short", 60_000L));
    }
}
