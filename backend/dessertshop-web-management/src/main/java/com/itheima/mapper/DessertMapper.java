package com.itheima.mapper;

import com.itheima.pojo.Dessert;
import com.itheima.pojo.DessertQueryParam;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DessertMapper {

    //分页查询甜品
    List<Dessert> list(DessertQueryParam queryParam);

    //查询全部甜品（按分类）
    @Select("select * from dessert where category = #{category} order by sales desc")
    List<Dessert> listByCategory(String category);

    //根据ID查询甜品
    @Select("select * from dessert where id = #{id}")
    Dessert getById(Integer id);

    //新增甜品
    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert("insert into dessert(name, category, price, original_price, image, sales, status, create_time, update_time) " +
            "values(#{name}, #{category}, #{price}, #{originalPrice}, #{image}, #{sales}, #{status}, #{createTime}, #{updateTime})")
    void insert(Dessert dessert);

    //修改甜品
    void updateById(Dessert dessert);

    //删除甜品
    @Delete("delete from dessert where id = #{id}")
    void deleteById(Integer id);

    //查询各分类数量
    @Select("select category, count(*) as count from dessert group by category")
    List<java.util.Map<String, Object>> countByCategory();
}
