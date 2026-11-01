package com.sweeten.order.event;

import com.sweeten.order.model.Order;

/**
 * 顾客下单成功（订单已写入、库存已扣减，仍在下单事务内）。
 * <p>
 * 监听方同步执行、与下单处于同一事务：会员注册（customer 模块）在这里完成。
 * 监听方失败不得影响下单——要么像会员注册那样在保存点（Propagation.NESTED）内执行，
 * 要么对外部副作用（通知、配送派单等）使用 {@code @TransactionalEventListener(phase = AFTER_COMMIT)}。
 */
public record OrderPlacedEvent(Order order) {
}
