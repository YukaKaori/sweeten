package com.itheima.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.itheima.mapper.DessertMapper;
import com.itheima.pojo.Dessert;
import com.itheima.pojo.DessertQueryParam;
import com.itheima.pojo.PageResult;
import com.itheima.service.DessertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class DessertServiceImpl implements DessertService {

    @Autowired
    private DessertMapper dessertMapper;

    @Override
    public PageResult<Dessert> page(DessertQueryParam queryParam) {
        //1. 设置分页参数
        PageHelper.startPage(queryParam.getPage(), queryParam.getPageSize());
        //2. 执行查询
        List<Dessert> dessertList = dessertMapper.list(queryParam);
        Page<Dessert> p = (Page<Dessert>) dessertList;
        //3. 封装结果
        return new PageResult(p.getTotal(), p.getResult());
    }

    @Override
    public List<Dessert> listByCategory(String category) {
        return dessertMapper.listByCategory(category);
    }

    @Override
    public Dessert getById(Integer id) {
        return dessertMapper.getById(id);
    }

    @Override
    public void save(Dessert dessert) {
        dessert.setCreateTime(LocalDateTime.now());
        dessert.setUpdateTime(LocalDateTime.now());
        dessertMapper.insert(dessert);
    }

    @Override
    public void update(Dessert dessert) {
        dessert.setUpdateTime(LocalDateTime.now());
        dessertMapper.updateById(dessert);
    }

    @Override
    public void deleteById(Integer id) {
        dessertMapper.deleteById(id);
    }

    @Override
    public List<Map<String, Object>> countByCategory() {
        return dessertMapper.countByCategory();
    }
}
