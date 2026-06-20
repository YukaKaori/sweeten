package com.itheima.service.impl;

import com.itheima.mapper.DessertMapper;
import com.itheima.pojo.Dessert;
import com.itheima.pojo.DessertQueryParam;
import com.itheima.pojo.PageResult;
import com.itheima.service.DessertService;
import com.itheima.util.PageHelperUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
        dessert.setCreateTime(LocalDateTime.now());
        dessert.setUpdateTime(LocalDateTime.now());
        dessertMapper.insert(dessert);
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    @CacheEvict(value = "dessert:list", allEntries = true)
    public void update(Dessert dessert) {
        dessert.setUpdateTime(LocalDateTime.now());
        dessertMapper.updateById(dessert);
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
}
