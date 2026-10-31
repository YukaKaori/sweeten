package com.itheima.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.pojo.Dessert;
import com.itheima.pojo.PageResult;
import com.itheima.service.DessertService;
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

@WebMvcTest(DessertController.class)
@DisplayName("甜品控制器测试")
class DessertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DessertService dessertService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("分页查询甜品 - 返回成功")
    void shouldReturnPageResult() throws Exception {
        PageResult<Dessert> emptyPage = new PageResult<>();
        emptyPage.setTotal(0L);
        emptyPage.setRows(Collections.emptyList());
        when(dessertService.page(any())).thenReturn(emptyPage);

        mockMvc.perform(get("/desserts?page=1&pageSize=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    @Test
    @DisplayName("根据ID查询甜品 - 存在时返回成功")
    void shouldGetById() throws Exception {
        Dessert dessert = new Dessert();
        dessert.setId(1);
        dessert.setName("草莓慕斯");
        dessert.setCategory("cake");
        dessert.setPrice(38.00);

        when(dessertService.getById(1)).thenReturn(dessert);

        mockMvc.perform(get("/desserts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.name").value("草莓慕斯"));
    }

    @Test
    @DisplayName("根据分类查询甜品列表")
    void shouldListByCategory() throws Exception {
        when(dessertService.listByCategory("cake")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/desserts/category/cake"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    @Test
    @DisplayName("新增甜品 - 成功")
    void shouldSaveDessert() throws Exception {
        Dessert newDessert = new Dessert();
        newDessert.setName("新甜品");
        newDessert.setCategory("cake");
        newDessert.setPrice(28.00);

        mockMvc.perform(post("/desserts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newDessert)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }
}
