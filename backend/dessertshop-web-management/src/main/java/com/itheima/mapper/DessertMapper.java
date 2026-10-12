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
    @Insert("insert into dessert(name, category, price, original_price, image, description, icon, sales, stock, status, create_time, update_time) " +
            "values(#{name}, #{category}, #{price}, #{originalPrice}, #{image}, #{description}, #{icon}, #{sales}, #{stock}, #{status}, #{createTime}, #{updateTime})")
    void insert(Dessert dessert);

    //修改甜品
    void updateById(Dessert dessert);

    //删除甜品
    @Delete("delete from dessert where id = #{id}")
    void deleteById(Integer id);

    //查询各分类数量
    @Select("select category, count(*) as count from dessert group by category")
    List<java.util.Map<String, Object>> countByCategory();

    //热销排行TOP N
    @Select("select name, sales from dessert where status = 1 order by sales desc limit #{limit}")
    List<java.util.Map<String, Object>> selectTopSalesRanking(@Param("limit") Integer limit);

    //查询全部上架甜品（移动端"全部"分类）
    @Select("select * from dessert where status = 1 order by sales desc")
    List<Dessert> listOnSale();

    //仅调整价格（窄更新，避免整对象覆写其它字段）
    @Update("update dessert set price = #{price}, original_price = coalesce(#{originalPrice}, original_price), " +
            "update_time = #{updateTime} where id = #{id}")
    int updatePrice(Dessert dessert);

    //下单扣库存 + 记销量（条件更新保证原子性：仅上架且库存充足时成功，返回影响行数）
    @Update("update dessert set stock = stock - #{qty}, sales = coalesce(sales, 0) + #{qty}, update_time = now() " +
            "where id = #{id} and status = 1 and stock >= #{qty}")
    int deductStock(@Param("id") Integer id, @Param("qty") Integer qty);

    //取消订单回补库存 + 回退销量
    @Update("update dessert set stock = coalesce(stock, 0) + #{qty}, sales = greatest(coalesce(sales, 0) - #{qty}, 0), " +
            "update_time = now() where id = #{id}")
    int restoreStock(@Param("id") Integer id, @Param("qty") Integer qty);

    //库存预警（库存低于阈值的上架甜品）
    @Select("select name, stock from dessert where stock < #{threshold} and status = 1 order by stock asc")
    List<java.util.Map<String, Object>> selectLowStockAlerts(@Param("threshold") Integer threshold);
}
