package com.sweeten;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 应用入口冒烟测试：完整上下文能在 test profile 下启动。
 * <p>
 * 曾经这里是不带 profile 的 @SpringBootTest，会直连开发机的真实 MySQL；现在与其它集成测试一样只用 H2，
 * 且使用独立库名（共享 testdb 的种子脚本不可重复执行）。
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:apptest;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;NON_KEYWORDS=BEGIN,END")
@DisplayName("应用入口")
class SweetenApplicationTests {

    @Autowired
    private ApplicationContext context;

    @Test
    @DisplayName("应用上下文以 sweeten-server 身份启动")
    void contextLoads() {
        assertEquals("sweeten-server", context.getEnvironment().getProperty("spring.application.name"));
        assertTrue(context.containsBean("sweetenApplication"));
    }
}
