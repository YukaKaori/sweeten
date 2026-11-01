package com.sweeten.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 集成测试基类：完整 Spring 上下文 + test profile（H2 内存库、缓存关闭、测试 JWT 密钥）。
 * <p>
 * 约定：
 * <ul>
 *   <li>需要独立数据的测试类用 {@code @TestPropertySource} 指定自己的 H2 库名
 *       （共享 testdb 的 data.sql 使用固定主键，不能在同一个库上重复执行）；</li>
 *   <li>需要登录态时注入 {@code JwtUtils} 签发令牌，令牌里的 id 必须是 data.sql 中存在的员工
 *       （TokenFilter 以数据库为准校验账号与角色）。</li>
 * </ul>
 * 任何测试的数据源都必须是 H2 内存库，由 {@link TestDatabaseGuard} 强制。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
public abstract class BaseTest {
}
