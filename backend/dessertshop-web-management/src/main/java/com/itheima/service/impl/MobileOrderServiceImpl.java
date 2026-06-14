package com.itheima.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.mapper.OrderMapper;
import com.itheima.pojo.MobileOrderRequest;
import com.itheima.pojo.Order;
import com.itheima.service.MobileOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 移动端订单服务实现 — 将移动端请求转为系统 Order 模型并保存
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MobileOrderServiceImpl implements MobileOrderService {

    private final OrderMapper orderMapper;
    private final ObjectMapper objectMapper;

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public String createOrder(MobileOrderRequest request) {
        // 1. 生成订单号：DS + yyyyMMddHHmmss + 4位随机数
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String suffix = String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
        String orderNo = "DS" + now + suffix;

        // 2. 序列化商品明细为 JSON 字符串
        String itemsJson;
        try {
            itemsJson = objectMapper.writeValueAsString(request.getItems());
        } catch (JsonProcessingException e) {
            log.error("序列化订单明细失败", e);
            throw new RuntimeException("订单数据格式错误");
        }

        // 3. 构建 Order 对象并保存
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setCustomerName(request.getCustomerName());
        order.setPhone(request.getPhone());
        order.setAddress(request.getAddress());
        order.setItems(itemsJson);
        order.setAmount(request.getTotalAmount());
        order.setStatus(1); // 1 = 待处理
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        orderMapper.insert(order);
        log.info("移动端订单创建成功: orderNo={}, amount={}", orderNo, request.getTotalAmount());

        return orderNo;
    }
}
