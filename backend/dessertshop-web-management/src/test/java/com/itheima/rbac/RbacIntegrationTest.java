package com.itheima.rbac;

import com.itheima.BaseTest;
import com.itheima.mapper.EmpMapper;
import com.itheima.pojo.Emp;
import com.itheima.utils.BCryptUtils;
import com.itheima.utils.JwtUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * RBAC 集成测试 — 走真实 TokenFilter + PermissionAspect + H2
 * <p>
 * 使用独立 H2 库 (rbactest), 避免与其他测试上下文共享的 testdb 重复执行种子脚本冲突。
 */
@AutoConfigureMockMvc
// NON_KEYWORDS=BEGIN,END: EmpMapper.getById 的 SQL 引用 emp_expr 的 begin/end 列 (MySQL 合法, H2 中是保留字)
@TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:rbactest;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;NON_KEYWORDS=BEGIN,END")
@DisplayName("RBAC 权限体系集成测试")
class RbacIntegrationTest extends BaseTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private EmpMapper empMapper;

    private String token(Integer id, String username, List<String> roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", id);
        claims.put("username", username);
        if (roles != null) {
            claims.put("roles", roles);
        }
        return jwtUtils.generateJwt(claims);
    }

    // ==================== 纵向越权 ====================

    @Test
    @DisplayName("收银员调 DELETE /emps → 403")
    void cashierDeleteEmpForbidden() throws Exception {
        mockMvc.perform(delete("/emps").param("ids", "999")
                        .header("token", token(2, "chef01", List.of("cashier"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @DisplayName("店长(super_admin)调 DELETE /emps → 200")
    void superAdminDeleteEmpOk() throws Exception {
        mockMvc.perform(delete("/emps").param("ids", "999")
                        .header("token", token(1, "admin", List.of("super_admin"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    @Test
    @DisplayName("收银员访问有权限的读接口 GET /orders → 200")
    void cashierListOrdersOk() throws Exception {
        mockMvc.perform(get("/orders?page=1&pageSize=10")
                        .header("token", token(2, "chef01", List.of("cashier"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    @Test
    @DisplayName("收银员访问无权限的读接口 GET /emps → 403")
    void cashierListEmpsForbidden() throws Exception {
        mockMvc.perform(get("/emps?page=1&pageSize=10")
                        .header("token", token(2, "chef01", List.of("cashier"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("未绑定角色的用户访问受控接口 → 403")
    void emptyRolesForbidden() throws Exception {
        mockMvc.perform(get("/orders?page=1&pageSize=10")
                        .header("token", token(3, "nobody", List.of())))
                .andExpect(status().isForbidden());
    }

    // ==================== 令牌与登录保底 ====================

    @Test
    @DisplayName("旧版 token(无 roles 字段) → 401 要求重新登录")
    void legacyTokenWithoutRolesUnauthorized() throws Exception {
        mockMvc.perform(get("/orders?page=1&pageSize=10")
                        .header("token", token(1, "admin", null)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("无 token 访问受控接口 → 401")
    void noTokenUnauthorized() throws Exception {
        mockMvc.perform(get("/orders?page=1&pageSize=10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("未打注解的接口(如 /depts)仅要求登录, 任意角色可访问")
    void unannotatedEndpointRequiresLoginOnly() throws Exception {
        // /upload 之外, 目前登录保底的业务接口是 profile/password; 用 profile 验证登录态即可访问
        mockMvc.perform(get("/emps/profile/1")
                        .header("token", token(1, "admin", List.of("finance"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    @Test
    @DisplayName("登录响应带 roles/permissions, token 含 roles claim")
    void loginReturnsRolesAndPermissions() throws Exception {
        empMapper.updatePassword(1, BCryptUtils.hashPassword("test-pass-1"));

        String body = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"test-pass-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.roles[0]").value("super_admin"))
                .andExpect(jsonPath("$.data.permissions").isArray())
                .andReturn().getResponse().getContentAsString();

        String jwt = body.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
        Object roles = jwtUtils.parseJWT(jwt).get("roles");
        assertInstanceOf(List.class, roles);
        assertTrue(((List<?>) roles).contains("super_admin"));
    }

    // ==================== IDOR 修复 ====================

    @Test
    @DisplayName("IDOR: A 用户传 B 的 id 查 profile, 返回的是 A 自己的数据")
    void profileIgnoresPathId() throws Exception {
        mockMvc.perform(get("/emps/profile/2")
                        .header("token", token(1, "admin", List.of("super_admin"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("admin"));
    }

    @Test
    @DisplayName("IDOR: A 用户传 B 的 id 改密码, 实际改的是 A 自己的密码")
    void updatePasswordIgnoresBodyId() throws Exception {
        empMapper.updatePassword(1, BCryptUtils.hashPassword("old-pass-a"));
        Emp victimBefore = empMapper.getByUsername("chef01");

        mockMvc.perform(put("/emps/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":2,\"oldPassword\":\"old-pass-a\",\"newPassword\":\"new-pass-a\"}")
                        .header("token", token(1, "admin", List.of("super_admin"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        // A(1) 的密码被改为新密码
        assertTrue(BCryptUtils.verifyPassword("new-pass-a", empMapper.getByUsername("admin").getPassword()));
        // B(2) 的密码未被动过
        assertEquals(victimBefore.getPassword(), empMapper.getByUsername("chef01").getPassword());
    }
}
