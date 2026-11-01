package com.sweeten.order.service;

import com.sweeten.common.paging.PageResult;
import com.sweeten.order.model.Order;
import com.sweeten.order.model.OrderQueryParam;

public interface OrderService {

    //分页查询订单
    PageResult<Order> page(OrderQueryParam queryParam);

    //根据ID查询订单详情
    Order getById(Integer id);

    //顾客查单：订单号与下单手机号同时匹配才返回，否则返回 null
    Order findByOrderNoAndPhone(String orderNo, String phone);

    //新增订单
    void save(Order order);

    //修改订单
    void update(Order order);

    //订单状态流转（按 OrderStatus 校验流转规则；取消时回补库存；发布 OrderStatusChangedEvent）
    void updateStatus(Integer id, Integer status);

    //删除订单
    void deleteById(Integer id);
}
