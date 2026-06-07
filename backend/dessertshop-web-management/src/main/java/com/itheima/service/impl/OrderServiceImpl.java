package com.itheima.service.impl;

import com.itheima.mapper.OrderMapper;
import com.itheima.pojo.Order;
import com.itheima.pojo.OrderQueryParam;
import com.itheima.pojo.PageResult;
import com.itheima.service.OrderService;
import com.itheima.util.PageHelperUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;

    @Override
    public PageResult<Order> page(OrderQueryParam queryParam) {
        return PageHelperUtils.executePage(queryParam.getPage(), queryParam.getPageSize(),
                () -> orderMapper.list(queryParam));
    }

    @Override
    public Order getById(Integer id) {
        return orderMapper.getById(id);
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public void save(Order order) {
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.insert(order);
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public void update(Order order) {
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public void deleteById(Integer id) {
        orderMapper.deleteById(id);
    }
}
