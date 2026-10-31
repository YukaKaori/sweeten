package com.itheima.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.BaseTest;
import com.itheima.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 订单状态 × 会员积分一致性：
 * 下单只注册会员不发积分；完成时发放且只发一次；完成前取消不涉及积分；
 * 旧规则下单即发放过积分的订单被取消时扣回一次（余额不足扣到 0）；积分兑换校验归属与并发余额。
 */
@AutoConfigureMockMvc
@TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:pointstest;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;NON_KEYWORDS=BEGIN,END")
@DisplayName("订单 × 会员积分一致性集成测试")
class MemberPointsIntegrationTest extends BaseTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private ObjectMapper objectMapper;

    private static int phoneSeq = 0;
    private String phone;

    @BeforeEach
    void setUp() {
        jdbc.update("update dessert set price = 28.00, stock = 100, sales = 200, status = 1 where id = 3");
        phone = String.format("1360000%04d", ++phoneSeq);
    }

    private String admin() {
        return jwtUtils.generateJwt(Map.of("id", 1, "username", "admin", "roles", List.of("super_admin")));
    }

    /** 下 qty 杯抹茶拿铁（28 元/杯），返回订单 id */
    private Integer placeOrder(int qty) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "customerName", "积分测试", "phone", phone, "address", "测试路",
                "totalAmount", 28.0 * qty,
                "items", List.of(Map.of("productId", 3, "productName", "抹茶拿铁", "price", 28.0, "qty", qty))));
        MvcResult r = mockMvc.perform(post("/mobile/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();
        String orderNo = objectMapper.readTree(r.getResponse().getContentAsString()).at("/data/orderNo").asText();
        return jdbc.queryForObject("select id from orders where order_no = ?", Integer.class, orderNo);
    }

    private void moveTo(Integer orderId, int status) throws Exception {
        mockMvc.perform(put("/orders/status").param("id", orderId.toString()).param("status", String.valueOf(status))
                        .header("token", admin()))
                .andExpect(status().isOk());
    }

    private Map<String, Object> member() {
        return jdbc.queryForMap("select * from member where phone = ?", phone);
    }

    private int logCount(Integer orderId, int type) {
        return jdbc.queryForObject("select count(*) from points_log where related_id = ? and type = ?", Integer.class, orderId, type);
    }

    @Test
    @DisplayName("下单：注册会员并发新人券，但不发积分")
    void placingOrderRegistersMemberWithoutPoints() throws Exception {
        Integer orderId = placeOrder(2);
        Map<String, Object> m = member();
        assertEquals(0, ((Number) m.get("points")).intValue());
        assertEquals(0, ((Number) m.get("order_count")).intValue());
        assertEquals(0, logCount(orderId, 1));
        assertEquals(1, jdbc.queryForObject("select count(*) from member_coupon where member_id = ?",
                Integer.class, m.get("id")));
    }

    @Test
    @DisplayName("完成：发放积分/成长值/累计消费/订单数；重复提交完成不重复发放")
    void completingOrderAwardsPointsOnce() throws Exception {
        Integer orderId = placeOrder(2); // 56 元
        moveTo(orderId, 2);
        moveTo(orderId, 3);
        moveTo(orderId, 4);
        moveTo(orderId, 4); // 重复请求：无副作用

        Map<String, Object> m = member();
        assertEquals(56, ((Number) m.get("points")).intValue());
        assertEquals(56, ((Number) m.get("growth")).intValue());
        assertEquals(1, ((Number) m.get("order_count")).intValue());
        assertEquals(0, new java.math.BigDecimal("56.00").compareTo((java.math.BigDecimal) m.get("total_spent")));
        assertEquals(1, logCount(orderId, 1));
    }

    @Test
    @DisplayName("完成前取消：回补库存，不产生任何积分流水")
    void cancellingBeforeCompletionTouchesNoPoints() throws Exception {
        Integer orderId = placeOrder(3);
        assertEquals(97, jdbc.queryForObject("select stock from dessert where id = 3", Integer.class));
        moveTo(orderId, 5);
        moveTo(orderId, 5); // 重复取消：无副作用

        assertEquals(100, jdbc.queryForObject("select stock from dessert where id = 3", Integer.class));
        assertEquals(0, ((Number) member().get("points")).intValue());
        assertEquals(0, jdbc.queryForObject("select count(*) from points_log where related_id = ?", Integer.class, orderId));
    }

    @Test
    @DisplayName("旧规则已发积分的订单被取消：扣回一次；余额不足时扣到 0 并如实记录")
    void cancellingLegacyAwardedOrderRevokesOnce() throws Exception {
        Integer orderId = placeOrder(2);
        Integer memberId = ((Number) member().get("id")).intValue();
        // 模拟旧规则：下单即发 56 积分，之后会员已把其中 46 分花掉，只剩 10 分
        jdbc.update("update member set points = 10, total_points = 56, growth = 56, total_spent = 56, order_count = 1 where id = ?", memberId);
        jdbc.update("insert into points_log(member_id, type, points, balance, reason, related_id, created_at) values (?, 1, 56, 56, '旧规则', ?, now())",
                memberId, orderId);

        moveTo(orderId, 5);
        moveTo(orderId, 5);

        Map<String, Object> m = member();
        assertEquals(0, ((Number) m.get("points")).intValue());
        assertEquals(0, ((Number) m.get("total_points")).intValue());
        assertEquals(0, ((Number) m.get("order_count")).intValue());
        assertEquals(1, logCount(orderId, 7));
        Map<String, Object> revoke = jdbc.queryForMap("select points, reason from points_log where related_id = ? and type = 7", orderId);
        assertEquals(-10, ((Number) revoke.get("points")).intValue());
        assertTrue(((String) revoke.get("reason")).contains("余额不足"));
    }

    @Test
    @DisplayName("旧规则已发积分的订单走到完成：不重复发放")
    void completingLegacyAwardedOrderDoesNotDoubleAward() throws Exception {
        Integer orderId = placeOrder(1);
        Integer memberId = ((Number) member().get("id")).intValue();
        jdbc.update("update member set points = 28 where id = ?", memberId);
        jdbc.update("insert into points_log(member_id, type, points, balance, reason, related_id, created_at) values (?, 1, 28, 28, '旧规则', ?, now())",
                memberId, orderId);

        moveTo(orderId, 2);
        moveTo(orderId, 3);
        moveTo(orderId, 4);

        assertEquals(28, ((Number) member().get("points")).intValue());
        assertEquals(1, logCount(orderId, 1));
    }

    @Test
    @DisplayName("积分兑换：会员 ID 与手机号不匹配 → 拒绝；匹配 → 扣积分发券；余额不足 → 不发券")
    void redeemRequiresOwnershipAndBalance() throws Exception {
        Integer orderId = placeOrder(2);
        moveTo(orderId, 2);
        moveTo(orderId, 3);
        moveTo(orderId, 4); // 56 积分
        Integer memberId = ((Number) member().get("id")).intValue();

        mockMvc.perform(post("/mobile/member/redeem").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("memberId", memberId, "phone", "13999999999", "templateId", 2))))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg", containsString("不匹配")));
        mockMvc.perform(get("/mobile/member/points-log").param("memberId", memberId.toString()).param("phone", "13999999999"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/mobile/member/redeem").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("memberId", memberId, "phone", phone, "templateId", 2))))
                .andExpect(jsonPath("$.code").value(1));
        assertEquals(6, ((Number) member().get("points")).intValue());

        // 再兑换：余额 6 < 50
        int couponsBefore = jdbc.queryForObject("select count(*) from member_coupon where member_id = ?", Integer.class, memberId);
        jdbc.update("update member_coupon set status = 2 where member_id = ?", memberId); // 先"用掉"已领的券，排除重复领取规则
        mockMvc.perform(post("/mobile/member/redeem").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("memberId", memberId, "phone", phone, "templateId", 2))))
                .andExpect(jsonPath("$.code").value(0));
        assertEquals(couponsBefore, jdbc.queryForObject("select count(*) from member_coupon where member_id = ?", Integer.class, memberId));
    }

    @Test
    @DisplayName("删除订单：进行中 → 400（须先取消）；已取消 → 可删除；整单修改不能改状态")
    void deleteAndUpdateRespectStatusRules() throws Exception {
        Integer orderId = placeOrder(1);
        mockMvc.perform(delete("/orders").param("id", orderId.toString()).header("token", admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.msg", containsString("先取消")));

        Map<String, Object> o = jdbc.queryForMap("select * from orders where id = ?", orderId);
        String body = objectMapper.writeValueAsString(Map.of("id", orderId, "orderNo", o.get("order_no"),
                "customerName", "积分测试", "phone", phone, "items", o.get("items"), "amount", 28.0, "status", 4));
        mockMvc.perform(put("/orders").contentType(MediaType.APPLICATION_JSON).content(body).header("token", admin()))
                .andExpect(status().isBadRequest());
        assertEquals(1, jdbc.queryForObject("select status from orders where id = ?", Integer.class, orderId));

        moveTo(orderId, 5);
        mockMvc.perform(delete("/orders").param("id", orderId.toString()).header("token", admin()))
                .andExpect(status().isOk());
        assertEquals(0, jdbc.queryForObject("select count(*) from orders where id = ?", Integer.class, orderId));
    }
}
