package com.itheima.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.itheima.mapper.OrderMapper;
import com.itheima.pojo.Order;
import com.itheima.pojo.OrderQueryParam;
import com.itheima.pojo.PageResult;
import com.itheima.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Override
    public PageResult<Order> page(OrderQueryParam queryParam) {
        //1. 设置分页参数
        PageHelper.startPage(queryParam.getPage(), queryParam.getPageSize());
        //2. 执行查询
        List<Order> orderList = orderMapper.list(queryParam);
        Page<Order> p = (Page<Order>) orderList;
        //3. 封装结果
        return new PageResult(p.getTotal(), p.getResult());
    }

    @Override
    public Order getById(Integer id) {
        return orderMapper.getById(id);
    }

    @Override
    public void save(Order order) {
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.insert(order);
    }

    @Override
    public void update(Order order) {
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    @Override
    public void deleteById(Integer id) {
        orderMapper.deleteById(id);
    }
}
