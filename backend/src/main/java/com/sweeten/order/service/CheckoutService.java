package com.sweeten.order.service;

import com.sweeten.order.model.CheckoutRequest;
import com.sweeten.order.model.Order;

/**
 * 顾客商城结算：把购物车转换为订单。
 */
public interface CheckoutService {

    /**
     * 下单：按商品库当前价格重算金额，并在同一事务内扣减库存、累加销量，成功后发布 OrderPlacedEvent
     *
     * @param request 结算请求
     * @return 已保存的订单（含订单号与服务端核算金额）
     */
    Order placeOrder(CheckoutRequest request);
}
