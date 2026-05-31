package com.itheima.mapper;

import com.itheima.pojo.Order;
import com.itheima.pojo.OrderQueryParam;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    //统计时间范围内已完成订单数
    @Select("select count(*) from orders where status = 4 and create_time between #{start} and #{end}")
    Long countCompletedOrders(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    //统计时间范围内总订单数
    @Select("select count(*) from orders where create_time between #{start} and #{end}")
    Long countOrders(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    //统计时间范围内营收（已完成+配送中）
    @Select("select coalesce(sum(amount), 0) from orders where status in (3, 4) and create_time between #{start} and #{end}")
    Double sumRevenue(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    //按日分组统计营收和订单数
    @Select("select date(create_time) as d, coalesce(sum(amount), 0) as revenue, count(*) as orders " +
            "from orders where status in (3, 4) and create_time between #{start} and #{end} " +
            "group by date(create_time) order by d")
    List<Map<String, Object>> selectDailyRevenue(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
