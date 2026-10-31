package com.itheima.rbac;

import com.itheima.BaseTest;
import com.itheima.utils.JwtUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 安全加固回归：TokenFilter 路径参数绕过、上传接口鉴权与类型识别、密码哈希不外泄、登录限流不信任伪造 XFF。
 */
@AutoConfigureMockMvc
@TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:sectest;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;NON_KEYWORDS=BEGIN,END")
@DisplayName("安全加固回归测试")
class SecurityHardeningTest extends BaseTest {

    /** 最小的合法 PNG 文件头（魔数 + IHDR 起始），足以通过类型识别 */
    private static final byte[] PNG_BYTES = {
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D, 'I', 'H', 'D', 'R'};

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtUtils jwtUtils;

    /** 角色以数据库为准：data.sql 中 1=admin(super_admin)、2=chef01(cashier) */
    private String token(String role) {
        return "super_admin".equals(role)
                ? jwtUtils.generateJwt(Map.of("id", 1, "username", "admin", "roles", List.of(role)))
                : jwtUtils.generateJwt(Map.of("id", 2, "username", "chef01", "roles", List.of(role)));
    }

    @Test
    @DisplayName("权限以数据库为准：token 里声称 super_admin 的收银员仍是收银员；账号不存在 → 401")
    void rolesComeFromDatabaseNotToken() throws Exception {
        String inflated = jwtUtils.generateJwt(Map.of("id", 2, "username", "chef01", "roles", List.of("super_admin")));
        mockMvc.perform(delete("/emps").param("ids", "999").header("token", inflated))
                .andExpect(status().isForbidden());

        String ghost = jwtUtils.generateJwt(Map.of("id", 4242, "username", "ghost", "roles", List.of("super_admin")));
        mockMvc.perform(get("/orders").param("page", "1").param("pageSize", "1").header("token", ghost))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("路径参数绕过：/upload;.png 不再被当作静态资源放行 → 400")
    void semicolonSuffixIsRejected() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", PNG_BYTES);
        mockMvc.perform(multipart("/upload;.png").file(file))
                .andExpect(status().isBadRequest());
        mockMvc.perform(multipart("/upload").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("静态后缀白名单只对 GET 生效：POST /x.png 仍需登录")
    void staticSuffixOnlyForReads() throws Exception {
        mockMvc.perform(post("/emps.png")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("上传：无甜品/员工编辑权限的角色（收银）→ 403")
    void uploadRequiresEditPermission() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", PNG_BYTES);
        mockMvc.perform(multipart("/upload").file(file).header("token", token("cashier")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("上传：Content-Type 声称 image/png 但内容是 HTML → 按魔数拒绝")
    void uploadDetectsSpoofedContentType() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "evil.html", "image/png",
                "<html><script>alert(1)</script></html>".getBytes());
        mockMvc.perform(multipart("/upload").file(file).header("token", token("super_admin")))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg", containsString("不支持的文件类型")));
    }

    @Test
    @DisplayName("上传：OSS 不可用（测试环境未配置）→ 可读的错误提示，不是 500")
    void uploadHandlesOssFailure() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "application/octet-stream", PNG_BYTES);
        mockMvc.perform(multipart("/upload").file(file).header("token", token("super_admin")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.msg", containsString("图片服务暂不可用")));
    }

    @Test
    @DisplayName("错误请求返回 400 而非 500：请求体非法 UTF-8 / 删除员工 ids 为空")
    void malformedRequestsAreClientErrors() throws Exception {
        byte[] badUtf8 = {'{', '"', 'n', 'a', 'm', 'e', '"', ':', '"', (byte) 0xB2, (byte) 0xE2, '"', '}'};
        mockMvc.perform(post("/emps").contentType(MediaType.APPLICATION_JSON).content(badUtf8)
                        .header("token", token("super_admin")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(delete("/emps").param("ids", "").header("token", token("super_admin")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("员工列表/详情不返回密码哈希")
    void employeePasswordNotExposed() throws Exception {
        mockMvc.perform(get("/emps").param("page", "1").param("pageSize", "10").header("token", token("super_admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rows[0].username").exists())
                .andExpect(jsonPath("$.data.rows[0].password").doesNotExist());
    }

    @Test
    @DisplayName("登录限流：伪造不同的 X-Forwarded-For 也绕不过（默认不信任 XFF）")
    void loginRateLimitIgnoresSpoofedForwardedFor() throws Exception {
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
                            .header("X-Forwarded-For", "10.0.0." + i)
                            .content("{\"username\":\"nobody\",\"password\":\"wrong-pass\"}"))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", "10.0.0.99")
                        .content("{\"username\":\"nobody\",\"password\":\"wrong-pass\"}"))
                .andExpect(status().isTooManyRequests());
    }
}
