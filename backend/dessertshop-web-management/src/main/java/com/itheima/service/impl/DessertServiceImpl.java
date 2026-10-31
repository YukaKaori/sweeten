package com.itheima.service.impl;

import com.itheima.exception.DuplicateException;
import com.itheima.exception.NotFoundException;
import com.itheima.mapper.DessertMapper;
import com.itheima.pojo.Dessert;
import com.itheima.pojo.DessertQueryParam;
import com.itheima.pojo.PageResult;
import com.itheima.service.DessertService;
import com.itheima.util.PageHelperUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DessertServiceImpl implements DessertService {

    private final DessertMapper dessertMapper;

    @Override
    @Cacheable(value = "dessert:list", key = "'page_' + #queryParam.page + '_' + #queryParam.pageSize + '_' + #queryParam.name + '_' + #queryParam.category")
    public PageResult<Dessert> page(DessertQueryParam queryParam) {
        return PageHelperUtils.executePage(queryParam.getPage(), queryParam.getPageSize(),
                () -> dessertMapper.list(queryParam));
    }

    @Override
    public List<Dessert> listByCategory(String category) {
        return dessertMapper.listByCategory(category);
    }

    @Override
    @Cacheable(value = "dessert:list", key = "'detail_' + #id")
    public Dessert getById(Integer id) {
        return dessertMapper.getById(id);
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    @CacheEvict(value = "dessert:list", allEntries = true)
    public void save(Dessert dessert) {
        // 未填写的计数字段按 0 入库（insert 会显式写入 NULL，覆盖表默认值）
        if (dessert.getSales() == null) dessert.setSales(0);
        if (dessert.getStock() == null) dessert.setStock(0);
        if (dessert.getStatus() == null) dessert.setStatus(1);
        dessert.setCreateTime(LocalDateTime.now());
        dessert.setUpdateTime(LocalDateTime.now());
        try {
            dessertMapper.insert(dessert);
        } catch (DuplicateKeyException e) {
            throw new DuplicateException("甜品名称「" + dessert.getName() + "」已存在");
        }
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    @CacheEvict(value = "dessert:list", allEntries = true)
    public void update(Dessert dessert) {
        dessert.setUpdateTime(LocalDateTime.now());
        try {
            dessertMapper.updateById(dessert);
        } catch (DuplicateKeyException e) {
            throw new DuplicateException("甜品名称「" + dessert.getName() + "」已存在");
        }
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    @CacheEvict(value = "dessert:list", allEntries = true)
    public void deleteById(Integer id) {
        dessertMapper.deleteById(id);
    }

    @Override
    public List<Map<String, Object>> countByCategory() {
        return dessertMapper.countByCategory();
    }

    @Override
    public List<Dessert> listOnSale() {
        return dessertMapper.listOnSale();
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    @CacheEvict(value = "dessert:list", allEntries = true)
    public void updatePrice(Integer id, Double price, Double originalPrice) {
        Dessert dessert = new Dessert();
        dessert.setId(id);
        dessert.setPrice(price);
        dessert.setOriginalPrice(originalPrice);
        dessert.setUpdateTime(LocalDateTime.now());
        if (dessertMapper.updatePrice(dessert) == 0) {
            throw new NotFoundException("甜品", id);
        }
    }

    @Override
    @CacheEvict(value = "dessert:list", allEntries = true)
    public boolean deductStock(Integer id, Integer qty) {
        return dessertMapper.deductStock(id, qty) > 0;
    }

    @Override
    @CacheEvict(value = "dessert:list", allEntries = true)
    public void restoreStock(Integer id, Integer qty) {
        dessertMapper.restoreStock(id, qty);
    }
}
