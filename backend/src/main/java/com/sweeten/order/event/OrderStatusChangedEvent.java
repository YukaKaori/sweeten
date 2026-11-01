package com.sweeten.order.event;

import com.sweeten.order.model.Order;
import com.sweeten.order.model.OrderStatus;

/**
 * 订单状态已按流转规则变更（仍在流转事务内，取消时库存已回补）。
 * <p>
 * 同步监听方与流转同属一个事务：监听方抛出异常会使整次流转回滚，可安全重试。
 * 当前监听方：会员积分（完成发放、取消扣回）。履约、配送、通知等后续能力从这里接入；
 * 对外部系统的调用请用 {@code @TransactionalEventListener(phase = AFTER_COMMIT)}，避免事务回滚后已对外生效。
 *
 * @param order 流转前读取的订单（order.getStatus() 为原状态）
 */
public record OrderStatusChangedEvent(Order order, OrderStatus from, OrderStatus to) {
}
