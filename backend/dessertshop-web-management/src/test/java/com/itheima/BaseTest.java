package com.itheima;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 测试基类
 * 激活 test profile，使用 H2 内存数据库
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
public abstract class BaseTest {

    /**
     * 获取测试 JWT token
     * 用于需要认证的测试场景
     */
    protected String getTestToken() {
        // 测试 token，claims: {id: 1, username: "admin"}
        return "eyJhbGciOiJIUzI1NiJ9.eyJpZCI6MSwidXNlcm5hbWUiOiJhZG1pbiJ9.test";
    }
}
