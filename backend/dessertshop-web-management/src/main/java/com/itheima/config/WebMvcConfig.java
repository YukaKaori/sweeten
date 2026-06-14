package com.itheima.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS 配置 — 允许移动端 HTML 跨域访问 API
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/mobile/**")  // 仅移动端接口开放跨域
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
