package com.itheima.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.nio.charset.StandardCharsets;

/**
 * 生产环境启动自检：发现明显不安全的配置时直接拒绝启动。
 * （缺失的必填环境变量由 application-prod.yml 中无默认值的占位符负责报错）
 */
@Slf4j
@Configuration
@Profile("prod")
public class ProductionSafetyCheck {

    /** 仓库 application.yml 中公开的开发默认密钥，生产绝不能使用 */
    private static final String DEV_DEFAULT_JWT_SECRET = "DessertShop@2024#SecureKey!@#$%^&*";

    @Value("${jwt.secret}")
    private String jwtSecret;

    @PostConstruct
    void verify() {
        if (DEV_DEFAULT_JWT_SECRET.equals(jwtSecret)) {
            throw new IllegalStateException("生产环境禁止使用仓库内置的默认 JWT 密钥，请通过 JWT_SECRET 配置随机密钥");
        }
        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET 长度至少 32 字节");
        }
        log.info("生产环境配置自检通过");
    }
}
