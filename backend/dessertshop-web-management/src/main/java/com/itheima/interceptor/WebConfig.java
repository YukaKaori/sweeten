package com.itheima.interceptor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * Web配置类 — CORS + 拦截器注册
 * <p>
 * 前端经同源反向代理（开发：Vite proxy；生产：Nginx）访问接口时不需要跨域。
 * 只有前端与接口分属不同域名时才在 app.cors.allowed-origins 中列出前端域名（逗号分隔），
 * 不再对任意来源开放。认证用的是请求头 token 而非 Cookie，因此不开启 allowCredentials。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;
    private final String[] allowedOrigins;

    public WebConfig(RateLimitInterceptor rateLimitInterceptor,
                     @Value("${app.cors.allowed-origins:}") String allowedOrigins) {
        this.rateLimitInterceptor = rateLimitInterceptor;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }

    /**
     * 配置跨域请求（未配置任何来源时不注册，即只允许同源）
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (allowedOrigins.length == 0) {
            return;
        }
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type", "token")
                .maxAge(3600);
    }

    /**
     * 注册登录限流拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/login");
    }
}
