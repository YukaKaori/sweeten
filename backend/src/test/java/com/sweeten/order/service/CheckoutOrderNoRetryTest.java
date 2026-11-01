package com.sweeten.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sweeten.catalog.model.Dessert;
import com.sweeten.catalog.service.DessertService;
import com.sweeten.common.exception.BusinessException;
import com.sweeten.order.event.OrderPlacedEvent;
import com.sweeten.order.mapper.OrderMapper;
import com.sweeten.order.model.CheckoutRequest;
import com.sweeten.order.model.Order;
import com.sweeten.order.model.OrderLine;
import com.sweeten.order.model.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("商城下单：订单号冲突重试与下单事件")
class CheckoutOrderNoRetryTest {

    private OrderMapper orderMapper;
    private ApplicationEventPublisher eventPublisher;
    private CheckoutServiceImpl service;
    private final List<String> attemptedNos = new ArrayList<>();

    @BeforeEach
    void setUp() {
        orderMapper = mock(OrderMapper.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        DessertService dessertService = mock(DessertService.class);
        Dessert d = new Dessert();
        d.setId(3);
        d.setName("抹茶拿铁");
        d.setPrice(28.0);
        d.setStatus(1);
        d.setStock(100);
        when(dessertService.getById(3)).thenReturn(d);
        when(dessertService.deductStock(3, 1)).thenReturn(true);
        service = new CheckoutServiceImpl(orderMapper, new ObjectMapper(), dessertService, eventPublisher);
    }

    private CheckoutRequest request() {
        CheckoutRequest req = new CheckoutRequest();
        req.setCustomerName("测试");
        req.setPhone("13800000000");
        req.setAddress("测试路");
        req.setTotalAmount(28.0);
        OrderLine item = new OrderLine();
        item.setProductId(3);
        item.setProductName("抹茶拿铁");
        item.setPrice(28.0);
        item.setQty(1);
        req.setItems(List.of(item));
        return req;
    }

    @Test
    @DisplayName("订单号撞上唯一索引 → 重新生成后成功，并发布一次下单事件")
    void retriesOnDuplicateOrderNo() {
        doAnswer(inv -> {
            Order o = inv.getArgument(0);
            attemptedNos.add(o.getOrderNo());
            if (attemptedNos.size() == 1) {
                throw new DuplicateKeyException("uk_order_no");
            }
            return null;
        }).when(orderMapper).insert(any(Order.class));

        Order order = service.placeOrder(request());

        verify(orderMapper, times(2)).insert(any(Order.class));
        assertEquals(attemptedNos.get(1), order.getOrderNo());
        assertTrue(order.getOrderNo().matches("DS\\d{18}"));
        assertEquals(OrderStatus.AWAITING_ACCEPTANCE.code(), order.getStatus());
        verify(eventPublisher, times(1)).publishEvent(new OrderPlacedEvent(order));
    }

    @Test
    @DisplayName("连续冲突超过上限 → 友好报错（事务回滚，库存随之恢复），不发布下单事件")
    void givesUpAfterMaxAttempts() {
        doThrow(new DuplicateKeyException("uk_order_no")).when(orderMapper).insert(any(Order.class));
        assertThrows(BusinessException.class, () -> service.placeOrder(request()));
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderMapper, times(5)).insert(captor.capture());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("下单后续处理（会员注册）失败不影响下单")
    void listenerFailureDoesNotFailCheckout() {
        doThrow(new IllegalStateException("member service down")).when(eventPublisher).publishEvent(any(Object.class));
        Order order = service.placeOrder(request());
        assertNotNull(order.getOrderNo());
    }
}
