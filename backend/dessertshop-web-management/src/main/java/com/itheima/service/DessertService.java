package com.itheima.service;

import com.itheima.pojo.Dessert;
import com.itheima.pojo.DessertQueryParam;
import com.itheima.pojo.PageResult;

import java.util.List;
import java.util.Map;

public interface DessertService {

    //分页查询甜品
    PageResult<Dessert> page(DessertQueryParam queryParam);

    //根据分类查询甜品列表
    List<Dessert> listByCategory(String category);

    //根据ID查询甜品
    Dessert getById(Integer id);

    //新增甜品
    void save(Dessert dessert);

    //修改甜品
    void update(Dessert dessert);

    //删除甜品
    void deleteById(Integer id);

    //查询各分类数量
    List<Map<String, Object>> countByCategory();
}
