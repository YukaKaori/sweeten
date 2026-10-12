package com.itheima.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.exception.BusinessException;
import com.itheima.mapper.OrderMapper;
import com.itheima.pojo.Dessert;
import com.itheima.pojo.MobileOrderRequest;
import com.itheima.pojo.Order;
import com.itheima.service.impl.MobileOrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("移动端下单：订单号冲突重试")
class MobileOrderNoRetryTest {

    private OrderMapper orderMapper;
    private MobileOrderServiceImpl service;
    private final List<String> attemptedNos = new ArrayList<>();

    @BeforeEach
    void setUp() {
        orderMapper = mock(OrderMapper.class);
        DessertService dessertService = mock(DessertService.class);
        MemberService memberService = mock(MemberService.class);
        Dessert d = new Dessert();
        d.setId(3);
        d.setName("抹茶拿铁");
        d.setPrice(28.0);
        d.setStatus(1);
        d.setStock(100);
        when(dessertService.getById(3)).thenReturn(d);
        when(dessertService.deductStock(3, 1)).thenReturn(true);
        service = new MobileOrderServiceImpl(orderMapper, new ObjectMapper(), memberService, dessertService);
    }

    private MobileOrderRequest request() {
        MobileOrderRequest req = new MobileOrderRequest();
        req.setCustomerName("测试");
        req.setPhone("13800000000");
        req.setAddress("测试路");
        req.setTotalAmount(28.0);
        MobileOrderRequest.OrderItem item = new MobileOrderRequest.OrderItem();
        item.setProductId(3);
        item.setProductName("抹茶拿铁");
        item.setPrice(28.0);
        item.setQty(1);
        req.setItems(List.of(item));
        return req;
    }

    @Test
    @DisplayName("订单号撞上唯一索引 → 重新生成后成功")
    void retriesOnDuplicateOrderNo() {
        doAnswer(inv -> {
            Order o = inv.getArgument(0);
            attemptedNos.add(o.getOrderNo());
            if (attemptedNos.size() == 1) {
                throw new DuplicateKeyException("uk_order_no");
            }
            return null;
        }).when(orderMapper).insert(any(Order.class));

        Order order = service.createOrder(request());

        verify(orderMapper, times(2)).insert(any(Order.class));
        assertEquals(attemptedNos.get(1), order.getOrderNo());
        assertTrue(order.getOrderNo().matches("DS\\d{18}"));
    }

    @Test
    @DisplayName("连续冲突超过上限 → 友好报错（事务回滚，库存随之恢复）")
    void givesUpAfterMaxAttempts() {
        doThrow(new DuplicateKeyException("uk_order_no")).when(orderMapper).insert(any(Order.class));
        assertThrows(BusinessException.class, () -> service.createOrder(request()));
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderMapper, times(5)).insert(captor.capture());
    }
}
