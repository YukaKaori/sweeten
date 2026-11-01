package com.sweeten.order.model;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 订单生命周期（单一事实来源）。
 * <p>
 * 状态码就是 orders.status 列与接口里的整数值，<b>不可改动</b>；前端 utils/constants.js 的 ORDER_STATUS /
 * ORDER_STATUS_FLOW 与本枚举一一对应。
 * <pre>
 *   待支付(0) ──支付成功──▶ 待接单(1) ──接单──▶ 制作中(2) ──出餐──▶ 配送中(3) ──送达──▶ 已完成(4)
 *       └───────────取消───────────┴──取消──▶ 已取消(5)
 * </pre>
 * 线上商城目前没有接入支付，顾客下单直接进入「待接单」；接入支付后，下单进入「待支付」，
 * 由支付确认推进到「待接单」（流转表已允许 0 → 1）。制作中之后不可取消（原料与产能已投入）。
 */
public enum OrderStatus {

    /** 已下单，等待付款 */
    PENDING_PAYMENT(0, "待支付"),
    /** 已付款（或无需在线支付），等待门店接单 */
    AWAITING_ACCEPTANCE(1, "待接单"),
    /** 门店已接单，正在制作 */
    IN_PRODUCTION(2, "制作中"),
    /** 已出餐，配送中 */
    OUT_FOR_DELIVERY(3, "配送中"),
    /** 已送达，订单完成（发放会员积分） */
    COMPLETED(4, "已完成"),
    /** 已取消（回补库存，扣回已发积分） */
    CANCELLED(5, "已取消");

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
            PENDING_PAYMENT, EnumSet.of(AWAITING_ACCEPTANCE, CANCELLED),
            AWAITING_ACCEPTANCE, EnumSet.of(IN_PRODUCTION, CANCELLED),
            IN_PRODUCTION, EnumSet.of(OUT_FOR_DELIVERY),
            OUT_FOR_DELIVERY, EnumSet.of(COMPLETED),
            COMPLETED, EnumSet.noneOf(OrderStatus.class),
            CANCELLED, EnumSet.noneOf(OrderStatus.class)
    );

    private final int code;
    private final String label;

    OrderStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    /** 存库 / 接口使用的整数状态码 */
    public int code() {
        return code;
    }

    public String label() {
        return label;
    }

    public boolean canTransitionTo(OrderStatus next) {
        return TRANSITIONS.get(this).contains(next);
    }

    /** 未知状态码返回 empty（历史脏数据不会让调用方抛出意外异常） */
    public static Optional<OrderStatus> fromCode(Integer code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values()).filter(s -> s.code == code).findFirst();
    }

    /** 状态名称；未知状态码原样显示 */
    public static String labelOf(Integer code) {
        return fromCode(code).map(OrderStatus::label).orElse(String.valueOf(code));
    }
}
