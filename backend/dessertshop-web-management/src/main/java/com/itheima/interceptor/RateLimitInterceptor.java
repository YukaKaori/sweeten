package com.itheima.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 登录限流拦截器 — 1分钟内同一IP最多10次登录尝试
 * <p>
 * 客户端 IP 默认取 TCP 连接地址；只有部署在会覆盖 X-Forwarded-For 的反向代理之后时，
 * 才应开启 app.security.trust-forwarded-for，否则攻击者每次换一个伪造的 XFF 即可绕过限流。
 */
@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final int MAX_ATTEMPTS = 10;
    private static final long WINDOW_MS = 60_000;
    /** 计数表超过该规模时清理过期窗口，防止大量不同 IP 撑爆内存 */
    private static final int CLEANUP_THRESHOLD = 10_000;

    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();
    private final boolean trustForwardedFor;

    public RateLimitInterceptor(@Value("${app.security.trust-forwarded-for:false}") boolean trustForwardedFor) {
        this.trustForwardedFor = trustForwardedFor;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        String ip = getClientIp(request);
        long now = System.currentTimeMillis();

        if (counters.size() > CLEANUP_THRESHOLD) {
            counters.values().removeIf(c -> now - c.windowStart > WINDOW_MS);
        }

        WindowCounter counter = counters.compute(ip, (k, v) -> {
            if (v == null || now - v.windowStart > WINDOW_MS) {
                return new WindowCounter(now);
            }
            return v;
        });

        int count = counter.attempts.incrementAndGet();

        if (count > MAX_ATTEMPTS) {
            log.warn("登录限流触发, IP: {}, 尝试次数: {}", ip, count);
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":0,\"msg\":\"请求过于频繁，请稍后重试\"}");
            return false;
        }

        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        if (trustForwardedFor) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isBlank()) {
                return xForwardedFor.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    private static class WindowCounter {
        final long windowStart;
        final AtomicInteger attempts = new AtomicInteger(0);

        WindowCounter(long windowStart) {
            this.windowStart = windowStart;
        }
    }
}
