package com.itheima.filter;

import com.itheima.context.CurrentUser;
import com.itheima.context.CurrentUserHolder;
import com.itheima.context.UserRoleLoader;
import com.itheima.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Token认证过滤器
 * <p>
 * 校验 JWT 有效性, 并将 {id, username, roles} 写入 CurrentUserHolder 供下游
 * (PermissionAspect / Controller) 使用, 请求结束后清理。
 * 旧版 token (无 roles 字段) 一律返回 401, 要求重新登录。
 */
@Slf4j
public class TokenFilter implements Filter {

    private final JwtUtils jwtUtils;
    private final UserRoleLoader userRoleLoader;

    public TokenFilter(JwtUtils jwtUtils, UserRoleLoader userRoleLoader) {
        this.jwtUtils = jwtUtils;
        this.userRoleLoader = userRoleLoader;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        //1. CORS 预检请求直接放行 (OPTIONS 不携带自定义 Header)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        //2. 拒绝带路径参数(;)的请求：/upload;.png 这类 URI 的原始串以 .png 结尾，
        //   但 Spring 路由时会忽略 ;... 仍命中 /upload —— 曾导致匿名访问上传接口。本应用不使用矩阵变量。
        String rawUri = request.getRequestURI();
        if (rawUri.indexOf(';') >= 0) {
            log.warn("拒绝带路径参数的请求: {}", rawUri);
            response.setStatus(HttpStatus.SC_BAD_REQUEST);
            return;
        }

        //3. 用容器解码、规范化后的路径做白名单匹配（与 Spring 路由所用路径一致）
        String path = request.getServletPath() + (request.getPathInfo() == null ? "" : request.getPathInfo());
        if (path.isEmpty()) {
            // 非 Servlet 容器环境（如 MockMvc）不填 servletPath；此时 URI 已排除 ';'，去掉 contextPath 即可
            path = rawUri.substring(request.getContextPath().length());
        }
        if (isPublic(path, request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        //4. 获取请求头中的令牌（token）
        String jwt = request.getHeader("token");

        //5. 判断令牌是否存在，如果不存在，返回错误结果（未登录）
        if (!StringUtils.hasLength(jwt)) {
            log.info("获取到jwt令牌为空,返回错误结果");
            response.setStatus(HttpStatus.SC_UNAUTHORIZED);
            return;
        }

        //6. 解析token，如果解析失败，返回错误结果（未登录）
        Claims claims;
        try {
            claims = jwtUtils.parseJWT(jwt);
        } catch (Exception e) {
            log.info("解析令牌失败, 返回错误结果");
            response.setStatus(HttpStatus.SC_UNAUTHORIZED);
            return;
        }

        //7. 旧版 token 无 roles 字段 → 强制重新登录（不做 super_admin 兜底）
        Object rolesClaim = claims.get("roles");
        if (!(rolesClaim instanceof List<?>)) {
            log.info("令牌缺少 roles 字段(旧版token), 要求重新登录");
            response.setStatus(HttpStatus.SC_UNAUTHORIZED);
            return;
        }

        //8. 以数据库为准校验账号仍存在并取当前角色（token 里的 roles 可能已过时：员工被删除/角色被调整）
        Integer userId = toInteger(claims.get("id"));
        Optional<List<String>> currentRoles = userRoleLoader.currentRoles(userId);
        if (currentRoles.isEmpty()) {
            log.info("令牌对应的账号已不存在, 要求重新登录: userId={}", userId);
            response.setStatus(HttpStatus.SC_UNAUTHORIZED);
            return;
        }

        //9. 构建当前用户上下文, 放行, 请求结束后清理
        try {
            CurrentUserHolder.set(new CurrentUser(userId, (String) claims.get("username"), currentRoles.get()));
            log.info("令牌合法, 放行");
            filterChain.doFilter(request, response);
        } finally {
            CurrentUserHolder.clear();
        }
    }

    /** 静态资源后缀：只对 GET/HEAD 放行，任何写操作都必须带令牌 */
    private static final List<String> STATIC_SUFFIXES = List.of(".html", ".css", ".js", ".ico", ".png", ".svg");

    /**
     * 公开资源：登录、移动端 H5 接口（/mobile/**）、接口文档、后端自带静态页
     */
    static boolean isPublic(String path, String method) {
        if ("/login".equals(path) || "/mobile".equals(path) || path.startsWith("/mobile/")) {
            return true;
        }
        boolean readOnly = "GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method);
        if (!readOnly) {
            return false;
        }
        return "/".equals(path)
                || path.startsWith("/doc.html")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/webjars/")
                || STATIC_SUFFIXES.stream().anyMatch(path::endsWith);
    }

    private static Integer toInteger(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }
}
