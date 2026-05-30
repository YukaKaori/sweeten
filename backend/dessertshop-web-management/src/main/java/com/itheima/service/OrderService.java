package com.itheima.service;

import com.itheima.pojo.Order;
import com.itheima.pojo.OrderQueryParam;
import com.itheima.pojo.PageResult;

public interface OrderService {

    //分页查询订单
    PageResult<Order> page(OrderQueryParam queryParam);

    //根据ID查询订单详情
    Order getById(Integer id);

    //新增订单
    void save(Order order);

    //修改订单
    void update(Order order);

    //删除订单
    void deleteById(Integer id);
}
