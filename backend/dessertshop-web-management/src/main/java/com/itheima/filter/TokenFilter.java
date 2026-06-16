package com.itheima.filter;

import com.itheima.utils.JwtUtils;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * Token认证过滤器
 */
@Slf4j
public class TokenFilter implements Filter {

    private final JwtUtils jwtUtils;

    /**
     * 构造器注入JwtUtils
     */
    public TokenFilter(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
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

        //2. 获取请求路径
        String uri = request.getRequestURI();

        //3. 判断是否为公开资源，直接放行（登录、移动端API、静态文件）
        if ("/login".equals(uri)
                || uri.startsWith("/mobile")
                || uri.startsWith("/doc.html")
                || uri.startsWith("/swagger-ui")
                || uri.startsWith("/v3/api-docs")
                || uri.startsWith("/webjars")
                || uri.equals("/")
                || uri.endsWith(".html")
                || uri.endsWith(".css")
                || uri.endsWith(".js")
                || uri.endsWith(".ico")
                || uri.endsWith(".png")
                || uri.endsWith(".svg")) {
            log.info("公开接口/资源，直接放行: {}", uri);
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
        try {
            jwtUtils.parseJWT(jwt);
        } catch (Exception e) {
            log.info("解析令牌失败, 返回错误结果");
            response.setStatus(HttpStatus.SC_UNAUTHORIZED);
            return;
        }

        //7. 放行
        log.info("令牌合法, 放行");
        filterChain.doFilter(request, response);
    }
}
