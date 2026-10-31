package com.itheima.service;

import com.itheima.mapper.DessertMapper;
import com.itheima.pojo.Dessert;
import com.itheima.service.impl.DessertServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("甜品服务单元测试")
class DessertServiceTest {

    @Mock
    private DessertMapper dessertMapper;

    @InjectMocks
    private DessertServiceImpl dessertService;

    private Dessert testDessert;

    @BeforeEach
    void setUp() {
        testDessert = new Dessert();
        testDessert.setId(1);
        testDessert.setName("草莓慕斯");
        testDessert.setCategory("cake");
        testDessert.setPrice(38.00);
        testDessert.setStock(50);
        testDessert.setStatus(1);
        testDessert.setCreateTime(LocalDateTime.now());
        testDessert.setUpdateTime(LocalDateTime.now());
    }

    @Test
    @DisplayName("根据ID查询甜品 - 存在时应返回甜品对象")
    void shouldGetById_WhenExists() {
        when(dessertMapper.getById(1)).thenReturn(testDessert);
        Dessert result = dessertService.getById(1);
        assertNotNull(result);
        assertEquals("草莓慕斯", result.getName());
        assertEquals("cake", result.getCategory());
        verify(dessertMapper).getById(1);
    }

    @Test
    @DisplayName("根据ID查询甜品 - 不存在时应返回null")
    void shouldReturnNull_WhenNotExists() {
        when(dessertMapper.getById(999)).thenReturn(null);
        Dessert result = dessertService.getById(999);
        assertNull(result);
    }

    @Test
    @DisplayName("新增甜品 - 应自动设置创建时间和更新时间")
    void shouldSetTimestamps_WhenSaving() {
        Dessert newDessert = new Dessert();
        newDessert.setName("新甜品");
        newDessert.setCategory("cake");
        newDessert.setPrice(28.00);
        dessertService.save(newDessert);
        assertNotNull(newDessert.getCreateTime());
        assertNotNull(newDessert.getUpdateTime());
        verify(dessertMapper).insert(newDessert);
    }

    @Test
    @DisplayName("修改甜品 - 应自动更新修改时间")
    void shouldUpdateTimestamp_WhenUpdating() {
        Dessert toUpdate = new Dessert();
        toUpdate.setId(1);
        toUpdate.setName("更新后的甜品");
        dessertService.update(toUpdate);
        assertNotNull(toUpdate.getUpdateTime());
        verify(dessertMapper).updateById(toUpdate);
    }

    @Test
    @DisplayName("删除甜品 - 应调用 Mapper 的 deleteById")
    void shouldCallDeleteById() {
        dessertService.deleteById(1);
        verify(dessertMapper).deleteById(1);
    }

    @Test
    @DisplayName("根据分类查询甜品列表")
    void shouldListByCategory() {
        when(dessertMapper.listByCategory("cake")).thenReturn(Arrays.asList(testDessert));
        List<Dessert> result = dessertService.listByCategory("cake");
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(dessertMapper).listByCategory("cake");
    }
}
