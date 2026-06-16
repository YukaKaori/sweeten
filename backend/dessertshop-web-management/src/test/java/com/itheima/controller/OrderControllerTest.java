package com.itheima.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.pojo.Order;
import com.itheima.pojo.PageResult;
import com.itheima.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;


import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@DisplayName("订单控制器测试")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("分页查询订单 - 返回成功")
    void shouldReturnPageResult() throws Exception {
        PageResult<Order> emptyPage = new PageResult<>();
        emptyPage.setTotal(0L);
        emptyPage.setRows(Collections.emptyList());
        when(orderService.page(any())).thenReturn(emptyPage);

        mockMvc.perform(get("/orders?page=1&pageSize=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    @Test
    @DisplayName("根据ID查询订单 - 存在时返回成功")
    void shouldGetById() throws Exception {
        Order order = new Order();
        order.setId(1);
        order.setOrderNo("DS20240601001");
        order.setCustomerName("测试用户");
        order.setItems("草莓慕斯 x1");
        order.setAmount(38.00);
        order.setStatus(1);

        when(orderService.getById(1)).thenReturn(order);

        mockMvc.perform(get("/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.orderNo").value("DS20240601001"));
    }
}
