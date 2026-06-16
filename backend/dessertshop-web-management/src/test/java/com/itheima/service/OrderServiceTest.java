package com.itheima.service;

import com.itheima.mapper.OrderMapper;
import com.itheima.pojo.Order;
import com.itheima.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("订单服务单元测试")
class OrderServiceTest {

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order testOrder;

    @BeforeEach
    void setUp() {
        testOrder = new Order();
        testOrder.setId(1);
        testOrder.setOrderNo("DS20240601001");
        testOrder.setCustomerName("测试用户");
        testOrder.setPhone("13800000001");
        testOrder.setItems("草莓慕斯 x1");
        testOrder.setAmount(38.00);
        testOrder.setStatus(1);
        testOrder.setAddress("北京市");
        testOrder.setCreateTime(LocalDateTime.now());
        testOrder.setUpdateTime(LocalDateTime.now());
    }

    @Test
    @DisplayName("根据ID查询订单 - 存在时应返回订单对象")
    void shouldGetById_WhenExists() {
        when(orderMapper.getById(1)).thenReturn(testOrder);
        Order result = orderService.getById(1);
        assertNotNull(result);
        assertEquals("DS20240601001", result.getOrderNo());
        verify(orderMapper).getById(1);
    }

    @Test
    @DisplayName("根据ID查询订单 - 不存在时应返回null")
    void shouldReturnNull_WhenNotExists() {
        when(orderMapper.getById(999)).thenReturn(null);
        Order result = orderService.getById(999);
        assertNull(result);
    }

    @Test
    @DisplayName("新增订单 - 应自动设置时间戳")
    void shouldSetTimestamps_WhenSaving() {
        Order newOrder = new Order();
        newOrder.setOrderNo("DS20240601099");
        newOrder.setCustomerName("新客户");
        newOrder.setItems("测试商品");
        newOrder.setAmount(100.00);
        orderService.save(newOrder);
        assertNotNull(newOrder.getCreateTime());
        assertNotNull(newOrder.getUpdateTime());
        verify(orderMapper).insert(newOrder);
    }

    @Test
    @DisplayName("修改订单 - 应更新修改时间")
    void shouldUpdateTimestamp_WhenUpdating() {
        Order toUpdate = new Order();
        toUpdate.setId(1);
        toUpdate.setStatus(2);
        orderService.update(toUpdate);
        assertNotNull(toUpdate.getUpdateTime());
        verify(orderMapper).updateById(toUpdate);
    }

    @Test
    @DisplayName("删除订单 - 应调用 Mapper 的 deleteById")
    void shouldCallDeleteById() {
        orderService.deleteById(1);
        verify(orderMapper).deleteById(1);
    }
}
