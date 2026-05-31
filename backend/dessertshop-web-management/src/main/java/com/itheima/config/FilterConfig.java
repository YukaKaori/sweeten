package com.itheima.config;

import com.itheima.filter.TokenFilter;
import com.itheima.utils.JwtUtils;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Filter配置类
 */
@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<TokenFilter> tokenFilterRegistration(JwtUtils jwtUtils) {
        FilterRegistrationBean<TokenFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TokenFilter(jwtUtils));
        registration.addUrlPatterns("/*");
        registration.setName("tokenFilter");
        registration.setOrder(1);
        return registration;
    }
}
