package com.sweeten.customer.service;

import com.sweeten.order.event.OrderPlacedEvent;
import com.sweeten.order.event.OrderStatusChangedEvent;
import com.sweeten.order.model.Order;
import com.sweeten.order.model.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

/**
 * 订单事件 → 会员积分的路由（端到端效果由 customer.MemberPointsIntegrationTest 覆盖）
 */
@DisplayName("会员对订单事件的响应")
class MembershipOrderListenerTest {

    private MemberService memberService;
    private MembershipOrderListener listener;
    private Order order;

    @BeforeEach
    void setUp() {
        memberService = mock(MemberService.class);
        listener = new MembershipOrderListener(memberService);
        order = new Order();
        order.setId(7);
        order.setCustomerName("小甜");
        order.setPhone("13900000007");
    }

    @Test
    @DisplayName("下单 → 查找或注册会员")
    void placedOrderEnsuresMember() {
        listener.onOrderPlaced(new OrderPlacedEvent(order));
        verify(memberService).ensureMember("小甜", "13900000007");
        verifyNoMoreInteractions(memberService);
    }

    @Test
    @DisplayName("已完成 → 发放积分；已取消 → 扣回积分")
    void completionAwardsAndCancellationRevokes() {
        listener.onOrderStatusChanged(new OrderStatusChangedEvent(order, OrderStatus.OUT_FOR_DELIVERY, OrderStatus.COMPLETED));
        verify(memberService).awardOrderPoints(order);

        listener.onOrderStatusChanged(new OrderStatusChangedEvent(order, OrderStatus.AWAITING_ACCEPTANCE, OrderStatus.CANCELLED));
        verify(memberService).revokeOrderPoints(order);
        verifyNoMoreInteractions(memberService);
    }

    @Test
    @DisplayName("履约中间状态不影响会员")
    void intermediateTransitionsAreIgnored() {
        listener.onOrderStatusChanged(new OrderStatusChangedEvent(order, OrderStatus.AWAITING_ACCEPTANCE, OrderStatus.IN_PRODUCTION));
        listener.onOrderStatusChanged(new OrderStatusChangedEvent(order, OrderStatus.IN_PRODUCTION, OrderStatus.OUT_FOR_DELIVERY));
        verifyNoInteractions(memberService);
    }
}
