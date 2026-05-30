package com.itheima.mapper;

import com.itheima.pojo.Order;
import com.itheima.pojo.OrderQueryParam;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface OrderMapper {

    //分页查询订单
    List<Order> list(OrderQueryParam queryParam);

    //根据ID查询订单详情
    @Select("select * from orders where id = #{id}")
    Order getById(Integer id);

    //新增订单
    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert("insert into orders(order_no, customer_name, phone, items, amount, status, address, create_time, update_time) " +
            "values(#{orderNo}, #{customerName}, #{phone}, #{items}, #{amount}, #{status}, #{address}, #{createTime}, #{updateTime})")
    void insert(Order order);

    //修改订单
    void updateById(Order order);

    //删除订单
    @Delete("delete from orders where id = #{id}")
    void deleteById(Integer id);
}
