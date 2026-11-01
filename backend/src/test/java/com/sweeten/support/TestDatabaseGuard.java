package com.sweeten.support;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 测试安全闸：任何测试上下文创建的数据源都必须是 H2 内存库。
 * <p>
 * 经 src/test/resources/META-INF/spring.factories 注册，只存在于测试 classpath，
 * 对 Maven 与 IDE 中运行的每个 Spring 测试都生效。忘记加 {@code @ActiveProfiles("test")}
 * （或不继承 {@link BaseTest}）的测试会在建数据源时立即失败，而不是悄悄连上开发机的真实 MySQL
 * ——曾经就有测试直连真实库并改写员工密码。
 */
public class TestDatabaseGuard implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    static final String ALLOWED_URL_PREFIX = "jdbc:h2:mem:";

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        context.getBeanFactory().addBeanPostProcessor(new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                // Hikari 连接池在首次 getConnection 时才建立连接，此处校验早于任何连接
                if (bean instanceof HikariDataSource dataSource) {
                    check(dataSource.getJdbcUrl());
                }
                return bean;
            }
        });
    }

    static void check(String jdbcUrl) {
        if (jdbcUrl == null || !jdbcUrl.startsWith(ALLOWED_URL_PREFIX)) {
            throw new IllegalStateException("测试只允许使用 H2 内存库，当前数据源为 " + jdbcUrl
                    + "。请给测试类加 @ActiveProfiles(\"test\")（集成测试继承 BaseTest），不要连接真实数据库。");
        }
    }
}
