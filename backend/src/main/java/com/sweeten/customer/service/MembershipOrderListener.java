package com.sweeten.customer.service;

import com.sweeten.order.event.OrderPlacedEvent;
import com.sweeten.order.event.OrderStatusChangedEvent;
import com.sweeten.order.model.Order;
import com.sweeten.order.model.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 会员体系对订单生命周期的响应。
 * <p>
 * 订单模块只发布事件、不依赖会员模块；这里同步监听，与下单 / 流转处于同一事务，规则与改造前完全一致：
 * <ul>
 *   <li>下单 → 按手机号查找或注册会员（发新人券）。在保存点内执行（MemberService#ensureMember 为 NESTED），失败不影响下单</li>
 *   <li>→ 已完成 → 发放积分、成长值、累计消费（每单一次）</li>
 *   <li>→ 已取消 → 若该单发过积分则扣回</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class MembershipOrderListener {

    private final MemberService memberService;

    @EventListener
    public void onOrderPlaced(OrderPlacedEvent event) {
        Order order = event.order();
        memberService.ensureMember(order.getCustomerName(), order.getPhone());
    }

    @EventListener
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        if (event.to() == OrderStatus.COMPLETED) {
            memberService.awardOrderPoints(event.order());
        } else if (event.to() == OrderStatus.CANCELLED) {
            memberService.revokeOrderPoints(event.order());
        }
    }
}
