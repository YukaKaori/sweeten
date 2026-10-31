package com.itheima.service;

import com.itheima.pojo.MobileOrderRequest;
import com.itheima.pojo.Order;

/**
 * 移动端订单服务
 */
public interface MobileOrderService {

    /**
     * 移动端创建订单：按商品库当前价格重算金额，并在同一事务内扣减库存、累加销量
     * @param request 下单请求
     * @return 已保存的订单（含订单号与服务端核算金额）
     */
    Order createOrder(MobileOrderRequest request);

    /**
     * 顾客查单：订单号与下单手机号同时匹配才返回
     * @return 订单；不匹配时返回 null
     */
    Order findOrder(String orderNo, String phone);
}
