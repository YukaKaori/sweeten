package com.itheima.service;

import com.itheima.pojo.MobileOrderRequest;

/**
 * 移动端订单服务
 */
public interface MobileOrderService {

    /**
     * 移动端创建订单
     * @param request 下单请求
     * @return 生成的订单号
     */
    String createOrder(MobileOrderRequest request);
}
