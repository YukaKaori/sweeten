package com.sweeten.analytics.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 经营分析只读模型：跨 orders / dessert / emp 表的统计查询集中在这里。
 * <p>
 * 统计口径属于 analytics 模块；订单、商品、员工模块的 Mapper 只保留各自的业务读写，
 * 报表需求变化不需要改动这些业务模块。
 * <p>
 * 订单状态码与 order 模块的 OrderStatus 一致：3=配送中、4=已完成；营收按"配送中 + 已完成"计。
 */
@Mapper
public interface AnalyticsMapper {

    // ------------------------------------------------------------------ 销售

    @Select("select coalesce(sum(amount), 0) from orders where status in (3, 4) and create_time between #{start} and #{end}")
    Double sumRevenue(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("select count(*) from orders where create_time between #{start} and #{end}")
    Long countOrders(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("select count(*) from orders where status = 4 and create_time between #{start} and #{end}")
    Long countCompletedOrders(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("select date(create_time) as d, coalesce(sum(amount), 0) as revenue, count(*) as orders " +
            "from orders where status in (3, 4) and create_time between #{start} and #{end} " +
            "group by date(create_time) order by d")
    List<Map<String, Object>> selectDailyRevenue(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("select dayofweek(create_time) as wd, count(*) as cnt " +
            "from orders where create_time between #{start} and #{end} " +
            "group by dayofweek(create_time) order by wd")
    List<Map<String, Object>> selectWeekdayOrders(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ------------------------------------------------------------------ 商品

    /** 热销排行（在售商品按累计销量） */
    @Select("select name, sales from dessert where status = 1 order by sales desc limit #{limit}")
    List<Map<String, Object>> selectTopSalesRanking(@Param("limit") Integer limit);

    /** 成品库存预警（在售商品库存低于阈值） */
    @Select("select name, stock from dessert where stock < #{threshold} and status = 1 order by stock asc")
    List<Map<String, Object>> selectLowStockAlerts(@Param("threshold") Integer threshold);

    /** 各分类商品款数 */
    @Select("select category, count(*) as count from dessert group by category")
    List<Map<String, Object>> countProductsByCategory();

    // ------------------------------------------------------------------ 员工

    /** 各职位人数（职位码与前端 JOB_LIST、sql/schema/rbac_init.sql 的 job → 角色映射一致） */
    @Select("select (case when job = 1 then '经理' " +
            "             when job = 2 then '副经理' " +
            "             when job = 3 then '实习生' " +
            "             when job = 4 then '线上运营' " +
            "             when job = 5 then '财务出纳' " +
            "             else '其它' end) pos, " +
            "       count(*) total " +
            "from emp group by job order by total")
    List<Map<String, Object>> countStaffByJob();

    @Select("select if(gender = 1, '男', '女') name, count(*) value from emp group by gender")
    List<Map<String, Object>> countStaffByGender();
}
