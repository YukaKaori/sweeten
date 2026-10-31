package com.itheima.mapper;

import com.itheima.pojo.Dessert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@DisplayName("甜品 Mapper 集成测试")
class DessertMapperTest {

    @Autowired
    private DessertMapper dessertMapper;

    @Test
    @DisplayName("根据ID查询甜品 - 存在时应返回甜品")
    void shouldGetById_WhenExists() {
        Dessert result = dessertMapper.getById(1);
        assertNotNull(result);
        assertEquals("草莓慕斯", result.getName());
        assertEquals("cake", result.getCategory());
    }

    @Test
    @DisplayName("根据ID查询甜品 - 不存在时应返回null")
    void shouldReturnNull_WhenNotFound() {
        Dessert result = dessertMapper.getById(99999);
        assertNull(result);
    }

    @Test
    @DisplayName("根据分类查询甜品列表")
    void shouldListByCategory() {
        List<Dessert> result = dessertMapper.listByCategory("cake");
        assertNotNull(result);
        assertTrue(result.size() >= 2);
        assertTrue(result.stream().allMatch(d -> "cake".equals(d.getCategory())));
    }

    @Test
    @DisplayName("新增甜品 - 应成功并生成ID")
    void shouldInsertAndGenerateId() {
        Dessert d = new Dessert();
        d.setName("测试甜品-" + System.nanoTime());
        d.setCategory("drink");
        d.setPrice(20.00);
        d.setOriginalPrice(25.00);
        d.setStock(100);
        d.setStatus(1);
        d.setSales(0);
        d.setCreateTime(LocalDateTime.now());
        d.setUpdateTime(LocalDateTime.now());
        dessertMapper.insert(d);
        assertNotNull(d.getId());
        assertTrue(d.getId() > 0);
        Dessert inserted = dessertMapper.getById(d.getId());
        assertNotNull(inserted);
        assertEquals(d.getName(), inserted.getName());
    }

    @Test
    @DisplayName("修改甜品 - 应更新字段")
    void shouldUpdateById() {
        Dessert d = dessertMapper.getById(1);
        assertNotNull(d);
        d.setPrice(99.99);
        d.setUpdateTime(LocalDateTime.now());
        dessertMapper.updateById(d);
        Dessert updated = dessertMapper.getById(1);
        assertEquals(99.99, updated.getPrice(), 0.01);
    }

    @Test
    @DisplayName("删除甜品 - 应成功删除")
    void shouldDeleteById() {
        Dessert d = new Dessert();
        d.setName("待删除甜品-" + System.nanoTime());
        d.setCategory("snack");
        d.setPrice(10.00);
        d.setStock(50);
        d.setStatus(1);
        d.setSales(0);
        d.setCreateTime(LocalDateTime.now());
        d.setUpdateTime(LocalDateTime.now());
        dessertMapper.insert(d);
        Integer id = d.getId();
        assertNotNull(id);
        dessertMapper.deleteById(id);
        assertNull(dessertMapper.getById(id));
    }

    @Test
    @DisplayName("热销排行 - 应返回指定数量")
    void shouldSelectTopSalesRanking() {
        List<java.util.Map<String, Object>> result = dessertMapper.selectTopSalesRanking(3);
        assertNotNull(result);
        assertTrue(result.size() <= 3);
    }
}
