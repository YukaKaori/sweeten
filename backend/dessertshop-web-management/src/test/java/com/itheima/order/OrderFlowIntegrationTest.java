package com.itheima.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.BaseTest;
import com.itheima.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
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
 * 订单链路集成测试 — 移动端下单(服务端核价 + 扣库存 + 记销量) → 后台状态流转 → 取消回补库存，
 * 以及调价窄接口、移动端商品详情。走真实 Controller + Service + MyBatis + H2。
 * <p>
 * 种子数据(data.sql)：1 草莓慕斯 38.00 库存50 销量100；2 提拉米苏 32.00 库存30 销量80；
 * 3 抹茶拿铁 28.00 库存100 销量200；4 红丝绒蛋糕 已下架。
 */
@AutoConfigureMockMvc
@TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:ordertest;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;NON_KEYWORDS=BEGIN,END")
@DisplayName("订单链路集成测试")
class OrderFlowIntegrationTest extends BaseTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void resetDesserts() {
        jdbcTemplate.update("update dessert set price = 38.00, stock = 50, sales = 100, status = 1 where id = 1");
        jdbcTemplate.update("update dessert set price = 32.00, stock = 30, sales = 80, status = 1 where id = 2");
        jdbcTemplate.update("update dessert set price = 28.00, stock = 100, sales = 200, status = 1 where id = 3");
        jdbcTemplate.update("update dessert set stock = 0, status = 0 where id = 4");
        Cache cache = cacheManager.getCache("dessert:list");
        try {
            if (cache != null) {
                cache.clear();
            }
        } catch (RuntimeException e) {
            // Redis 不可用时业务侧本就降级直查数据库（RedisConfig.errorHandler），这里同样忽略
        }
    }

    private String adminToken() {
        return jwtUtils.generateJwt(Map.of("id", 1, "username", "admin", "roles", List.of("super_admin")));
    }

    private String orderBody(double totalAmount, Object... idQtyPairs) throws Exception {
        List<Map<String, Object>> items = new java.util.ArrayList<>();
        for (int i = 0; i < idQtyPairs.length; i += 2) {
            // 客户端单价/名称故意给错：服务端必须以商品库为准
            items.add(Map.of("productId", idQtyPairs[i], "productName", "客户端名称", "price", 0.01, "qty", idQtyPairs[i + 1]));
        }
        return objectMapper.writeValueAsString(Map.of(
                "customerName", "集成测试", "phone", "13912345678", "address", "测试路 1 号",
                "totalAmount", totalAmount, "items", items));
    }

    private int stockOf(int id) {
        return jdbcTemplate.queryForObject("select stock from dessert where id = ?", Integer.class, id);
    }

    private int salesOf(int id) {
        return jdbcTemplate.queryForObject("select sales from dessert where id = ?", Integer.class, id);
    }

    private Integer placeOrder(double total, Object... idQtyPairs) throws Exception {
        MvcResult result = mockMvc.perform(post("/mobile/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(total, idQtyPairs)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andReturn();
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        return jdbcTemplate.queryForObject("select id from orders where order_no = ?", Integer.class,
                data.get("orderNo").asText());
    }

    // ==================== 移动端下单 ====================

    @Test
    @DisplayName("下单：金额按库内价格核算，扣库存、记销量，明细取库内名称与单价")
    void mobileOrderDeductsStockAndUsesServerPrice() throws Exception {
        mockMvc.perform(post("/mobile/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(132.00, 1, 2, 3, 2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.amount").value(132.0))
                .andExpect(jsonPath("$.data.orderNo").isNotEmpty());

        assertEquals(48, stockOf(1));
        assertEquals(102, salesOf(1));
        assertEquals(98, stockOf(3));
        assertEquals(202, salesOf(3));

        Map<String, Object> order = jdbcTemplate.queryForMap(
                "select items, amount, status from orders where customer_name = '集成测试' order by id desc limit 1");
        String items = (String) order.get("items");
        assertTrue(items.contains("草莓慕斯") && items.contains("38.0"), items);
        assertFalse(items.contains("客户端名称"), items);
        assertEquals(1, ((Number) order.get("status")).intValue());
    }

    @Test
    @DisplayName("下单：客户端金额与服务端核算不一致 → 400，库存不变")
    void mobileOrderRejectsTamperedTotal() throws Exception {
        mockMvc.perform(post("/mobile/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(0.02, 1, 2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.msg", containsString("价格已更新")));

        assertEquals(50, stockOf(1));
        assertEquals(100, salesOf(1));
    }

    @Test
    @DisplayName("下单：任一商品库存不足 → 整单回滚，已扣的其它商品库存恢复")
    void mobileOrderInsufficientStockRollsBack() throws Exception {
        mockMvc.perform(post("/mobile/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(38.00 + 32.00 * 31, 1, 1, 2, 31)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.msg", containsString("库存不足")));

        assertEquals(50, stockOf(1));
        assertEquals(30, stockOf(2));
    }

    @Test
    @DisplayName("下单：同一商品拆成多行提交时合并计数，不能绕过库存校验")
    void mobileOrderMergesDuplicateLines() throws Exception {
        mockMvc.perform(post("/mobile/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(32.00 * 40, 2, 20, 2, 20)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.msg", containsString("库存不足")));

        assertEquals(30, stockOf(2));
    }

    @Test
    @DisplayName("下单：已下架商品 → 400")
    void mobileOrderRejectsOffSaleProduct() throws Exception {
        mockMvc.perform(post("/mobile/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(108.00, 4, 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.msg", containsString("已下架")));
    }

    @Test
    @DisplayName("顾客查单：订单号 + 手机号都匹配才返回，手机号不符 → 404")
    void mobileOrderLookupRequiresMatchingPhone() throws Exception {
        Integer id = placeOrder(28.00, 3, 1);
        String orderNo = jdbcTemplate.queryForObject("select order_no from orders where id = ?", String.class, id);

        mockMvc.perform(get("/mobile/orders/" + orderNo).param("phone", "13912345678"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderNo").value(orderNo))
                .andExpect(jsonPath("$.data.status").value(1))
                .andExpect(jsonPath("$.data.address").doesNotExist());

        mockMvc.perform(get("/mobile/orders/" + orderNo).param("phone", "13900000000"))
                .andExpect(status().isNotFound());
    }

    // ==================== 状态流转 ====================

    @Test
    @DisplayName("状态流转：按规则前进；不允许的跳转 → 400")
    void orderStatusFollowsFlow() throws Exception {
        Integer id = placeOrder(56.00, 3, 2);

        mockMvc.perform(put("/orders/status").param("id", id.toString()).param("status", "2")
                        .header("token", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        // 制作中不允许直接完成，也不允许取消
        mockMvc.perform(put("/orders/status").param("id", id.toString()).param("status", "4")
                        .header("token", adminToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.msg", containsString("不能变更为")));
        mockMvc.perform(put("/orders/status").param("id", id.toString()).param("status", "5")
                        .header("token", adminToken()))
                .andExpect(status().isBadRequest());

        assertEquals(2, jdbcTemplate.queryForObject("select status from orders where id = ?", Integer.class, id));
    }

    @Test
    @DisplayName("取消移动端订单：回补库存、回退销量")
    void cancelMobileOrderRestoresStock() throws Exception {
        Integer id = placeOrder(96.00, 2, 3);
        assertEquals(27, stockOf(2));
        assertEquals(83, salesOf(2));

        mockMvc.perform(put("/orders/status").param("id", id.toString()).param("status", "5")
                        .header("token", adminToken()))
                .andExpect(status().isOk());

        assertEquals(30, stockOf(2));
        assertEquals(80, salesOf(2));
    }

    @Test
    @DisplayName("取消历史纯文本明细订单：只改状态，不动库存")
    void cancelLegacyOrderLeavesStockUntouched() throws Exception {
        jdbcTemplate.update("update orders set status = 0 where id = 3");

        mockMvc.perform(put("/orders/status").param("id", "3").param("status", "5")
                        .header("token", adminToken()))
                .andExpect(status().isOk());

        assertEquals(5, jdbcTemplate.queryForObject("select status from orders where id = 3", Integer.class));
        assertEquals(50, stockOf(1));
        assertEquals(100, stockOf(3));
    }

    @Test
    @DisplayName("状态流转：订单不存在 → 404；状态越界 → 400")
    void orderStatusValidation() throws Exception {
        mockMvc.perform(put("/orders/status").param("id", "99999").param("status", "1")
                        .header("token", adminToken()))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/orders/status").param("id", "1").param("status", "9")
                        .header("token", adminToken()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("订单查询：结束日期当天下的订单要包含在内")
    void orderDateRangeIncludesEndDay() throws Exception {
        placeOrder(28.00, 3, 1);
        String today = java.time.LocalDate.now().toString();

        mockMvc.perform(get("/orders").param("begin", today).param("end", today)
                        .param("customerName", "集成测试")
                        .header("token", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    // ==================== 调价窄接口 ====================

    @Test
    @DisplayName("调价：只改价格字段，名称/库存/状态不受影响；价格非法 → 400")
    void updatePriceOnlyTouchesPrice() throws Exception {
        mockMvc.perform(put("/desserts/price")
                        .header("token", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":3,\"price\":26.5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        Map<String, Object> row = jdbcTemplate.queryForMap("select name, price, original_price, stock, status from dessert where id = 3");
        assertEquals(26.5, ((Number) row.get("price")).doubleValue());
        assertEquals(32.0, ((Number) row.get("original_price")).doubleValue());
        assertEquals("抹茶拿铁", row.get("name"));
        assertEquals(100, ((Number) row.get("stock")).intValue());

        mockMvc.perform(put("/desserts/price")
                        .header("token", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":3,\"price\":0}"))
                .andExpect(status().isBadRequest());
    }

    // ==================== 移动端商品 ====================

    @Test
    @DisplayName("移动端商品：详情可查在售商品，已下架返回 404；列表不含下架商品")
    void mobileProductEndpoints() throws Exception {
        mockMvc.perform(get("/mobile/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("草莓慕斯"))
                .andExpect(jsonPath("$.data.stock").value(50));

        mockMvc.perform(get("/mobile/products/4"))
                .andExpect(status().isNotFound());

        MvcResult all = mockMvc.perform(get("/mobile/products").param("category", "all"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode list = objectMapper.readTree(all.getResponse().getContentAsString()).get("data");
        assertEquals(3, list.size());
        list.forEach(p -> assertNotEquals(4, p.get("id").asInt()));
    }
}
