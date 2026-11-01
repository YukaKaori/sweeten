package com.sweeten.order.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("订单生命周期")
class OrderStatusTest {

    @Test
    @DisplayName("状态码是存库与接口契约：0..5 不可改动")
    void codesAreStable() {
        assertEquals(List.of(0, 1, 2, 3, 4, 5), Arrays.stream(OrderStatus.values()).map(OrderStatus::code).toList());
        assertEquals(List.of("待支付", "待接单", "制作中", "配送中", "已完成", "已取消"),
                Arrays.stream(OrderStatus.values()).map(OrderStatus::label).toList());
    }

    @Test
    @DisplayName("流转表与前端 ORDER_STATUS_FLOW 一致：0→1/5，1→2/5，2→3，3→4，4、5 为终态")
    void transitionsMatchFrontendFlow() {
        Map<Integer, List<Integer>> expected = Map.of(
                0, List.of(1, 5), 1, List.of(2, 5), 2, List.of(3), 3, List.of(4), 4, List.of(), 5, List.of());
        for (OrderStatus from : OrderStatus.values()) {
            List<Integer> allowed = Arrays.stream(OrderStatus.values())
                    .filter(from::canTransitionTo).map(OrderStatus::code).toList();
            assertEquals(expected.get(from.code()), allowed, "from " + from);
        }
    }

    @Test
    @DisplayName("制作中之后不可取消")
    void cannotCancelAfterProductionStarts() {
        assertFalse(OrderStatus.IN_PRODUCTION.canTransitionTo(OrderStatus.CANCELLED));
        assertFalse(OrderStatus.OUT_FOR_DELIVERY.canTransitionTo(OrderStatus.CANCELLED));
    }

    @Test
    @DisplayName("未知状态码不会抛出异常：fromCode 为空，名称原样显示")
    void unknownCodesAreTolerated() {
        assertTrue(OrderStatus.fromCode(9).isEmpty());
        assertTrue(OrderStatus.fromCode(null).isEmpty());
        assertEquals("9", OrderStatus.labelOf(9));
        assertEquals("已完成", OrderStatus.labelOf(4));
    }
}
