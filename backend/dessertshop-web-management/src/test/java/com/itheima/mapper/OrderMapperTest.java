package com.itheima.mapper;

import com.itheima.pojo.Order;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@DisplayName("订单 Mapper 集成测试")
class OrderMapperTest {

    @Autowired
    private OrderMapper orderMapper;

    @Test
    @DisplayName("根据ID查询订单 - 存在时应返回订单")
    void shouldGetById_WhenExists() {
        Order result = orderMapper.getById(1);
        assertNotNull(result);
        assertEquals("DS20240601001", result.getOrderNo());
    }

    @Test
    @DisplayName("根据ID查询订单 - 不存在时应返回null")
    void shouldReturnNull_WhenNotFound() {
        Order result = orderMapper.getById(99999);
        assertNull(result);
    }

    @Test
    @DisplayName("新增订单 - 应成功并生成ID")
    void shouldInsertAndGenerateId() {
        Order o = new Order();
        o.setOrderNo("DS" + System.nanoTime());
        o.setCustomerName("集成测试用户");
        o.setPhone("13800000099");
        o.setItems("测试甜品 x2");
        o.setAmount(76.00);
        o.setStatus(1);
        o.setAddress("测试地址");
        o.setCreateTime(LocalDateTime.now());
        o.setUpdateTime(LocalDateTime.now());
        orderMapper.insert(o);
        assertNotNull(o.getId());
        assertTrue(o.getId() > 0);
    }

    @Test
    @DisplayName("修改订单 - 应更新字段")
    void shouldUpdateById() {
        Order o = orderMapper.getById(1);
        assertNotNull(o);
        o.setStatus(2);
        o.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(o);
        Order updated = orderMapper.getById(1);
        assertEquals(2, (int) updated.getStatus());
    }

    @Test
    @DisplayName("删除订单 - 应成功删除")
    void shouldDeleteById() {
        Order o = new Order();
        o.setOrderNo("DS" + System.nanoTime());
        o.setCustomerName("待删除用户");
        o.setItems("测试 x1");
        o.setAmount(10.00);
        o.setStatus(0);
        o.setCreateTime(LocalDateTime.now());
        o.setUpdateTime(LocalDateTime.now());
        orderMapper.insert(o);
        Integer id = o.getId();
        assertNotNull(id);
        orderMapper.deleteById(id);
        assertNull(orderMapper.getById(id));
    }
}
