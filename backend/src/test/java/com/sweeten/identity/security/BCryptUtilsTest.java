package com.sweeten.identity.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 密码哈希（替代原 PasswordMigrationTest#testBCrypt：那个类会连接开发机的真实数据库）
 */
@DisplayName("BCrypt 密码哈希")
class BCryptUtilsTest {

    @Test
    @DisplayName("哈希后能验证原密码，错误密码验证失败")
    void hashesAndVerifies() {
        String hash = BCryptUtils.hashPassword("123456");
        assertTrue(hash.startsWith("$2a$12$"), "BCrypt cost 12");
        assertTrue(BCryptUtils.verifyPassword("123456", hash));
        assertFalse(BCryptUtils.verifyPassword("654321", hash));
    }

    @Test
    @DisplayName("同一密码每次哈希结果不同（随机盐）")
    void saltsEveryHash() {
        assertNotEquals(BCryptUtils.hashPassword("same"), BCryptUtils.hashPassword("same"));
    }
}
