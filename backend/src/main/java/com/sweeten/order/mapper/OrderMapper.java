package com.sweeten.order.mapper;

import com.sweeten.order.model.Order;
import com.sweeten.order.model.OrderQueryParam;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 订单读写。经营统计（营收、订单量、趋势）见 analytics 模块的 AnalyticsMapper。
 */
@Mapper
public interface OrderMapper {

    //分页查询订单
    List<Order> list(OrderQueryParam queryParam);

    //根据ID查询订单详情
    @Select("select * from orders where id = #{id}")
    Order getById(Integer id);

    //按订单号 + 手机号查询（顾客商城查单：两者都匹配才返回）
    @Select("select * from orders where order_no = #{orderNo} and phone = #{phone}")
    Order getByOrderNoAndPhone(@Param("orderNo") String orderNo, @Param("phone") String phone);

    //新增订单
    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert("insert into orders(order_no, customer_name, phone, items, amount, status, address, create_time, update_time) " +
            "values(#{orderNo}, #{customerName}, #{phone}, #{items}, #{amount}, #{status}, #{address}, #{createTime}, #{updateTime})")
    void insert(Order order);

    //修改订单
    void updateById(Order order);

    //状态流转（带原状态条件，防止并发下重复流转/重复回补库存）
    @Update("update orders set status = #{toStatus}, update_time = now() where id = #{id} and status = #{fromStatus}")
    int updateStatus(@Param("id") Integer id, @Param("fromStatus") Integer fromStatus, @Param("toStatus") Integer toStatus);

    //删除订单
    @Delete("delete from orders where id = #{id}")
    void deleteById(Integer id);
}
