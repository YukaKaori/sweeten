package com.sweeten.analytics.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 经营分析只读模型（H2 testdb 种子数据：订单 3 笔——已完成 38 元、制作中 64 元、待支付 66 元；
 * 甜品 4 款——在售 3 款、下架 1 款；员工 3 名，职位 1/2/3）。
 * 与 DessertMapperTest / OrderMapperTest 共用同一测试上下文，其它测试均回滚，种子数据保持不变。
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@DisplayName("经营分析 Mapper 集成测试")
class AnalyticsMapperTest {

    private static final LocalDateTime FROM = LocalDateTime.of(2000, 1, 1, 0, 0);

    @Autowired
    private AnalyticsMapper analyticsMapper;

    private static LocalDateTime until() {
        return LocalDateTime.now().plusDays(1);
    }

    @Test
    @DisplayName("营收只计配送中 + 已完成；订单量含全部状态")
    void revenueCountsOnlyDeliveredOrders() {
        assertEquals(38.0, analyticsMapper.sumRevenue(FROM, until()), 0.001);
        assertEquals(3L, analyticsMapper.countOrders(FROM, until()));
        assertEquals(1L, analyticsMapper.countCompletedOrders(FROM, until()));
    }

    @Test
    @DisplayName("热销排行只含在售商品，按销量降序")
    void topSalesRanking() {
        List<Map<String, Object>> top = analyticsMapper.selectTopSalesRanking(2);
        assertEquals(List.of("抹茶拿铁", "草莓慕斯"), top.stream().map(r -> r.get("name")).toList());
    }

    @Test
    @DisplayName("成品库存预警忽略已下架商品")
    void lowStockAlertsIgnoreOffShelfProducts() {
        List<Map<String, Object>> alerts = analyticsMapper.selectLowStockAlerts(40);
        assertEquals(List.of("提拉米苏"), alerts.stream().map(r -> r.get("name")).toList());
    }

    @Test
    @DisplayName("分类商品款数")
    void productsByCategory() {
        Map<Object, Object> counts = analyticsMapper.countProductsByCategory().stream()
                .collect(Collectors.toMap(r -> r.get("category"), r -> ((Number) r.get("count")).longValue()));
        assertEquals(Map.of("cake", 3L, "drink", 1L), counts);
    }

    @Test
    @DisplayName("职位统计使用门店职位名称（与前端 JOB_LIST 一致）")
    void staffByJobUsesShopJobTitles() {
        List<Object> titles = analyticsMapper.countStaffByJob().stream().map(r -> r.get("pos")).toList();
        assertTrue(titles.containsAll(List.of("经理", "副经理", "实习生")), titles.toString());
    }
}
