package com.itheima.mapper;

import com.itheima.pojo.PointsLog;
import com.itheima.pojo.PointsLogQueryParam;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface PointsLogMapper {

    List<PointsLog> list(PointsLogQueryParam param);

    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert("INSERT INTO points_log(member_id, type, points, balance, reason, related_id, created_at) " +
            "VALUES(#{memberId}, #{type}, #{points}, #{balance}, #{reason}, #{relatedId}, #{createdAt})")
    void insert(PointsLog log);

    @Select("SELECT * FROM points_log WHERE member_id = #{memberId} ORDER BY created_at DESC")
    List<PointsLog> listByMemberId(@Param("memberId") Integer memberId);

    // 订单相关积分流水（related_id = 订单ID）
    @Select("SELECT * FROM points_log WHERE related_id = #{orderId} AND type IN (1, 2) ORDER BY id LIMIT 1")
    PointsLog findOrderAward(@Param("orderId") Integer orderId);

    @Select("SELECT COUNT(*) FROM points_log WHERE related_id = #{orderId} AND type = #{type}")
    int countByOrderAndType(@Param("orderId") Integer orderId, @Param("type") Integer type);

    @Select("SELECT COALESCE(SUM(points), 0) FROM points_log WHERE member_id = #{memberId} AND type = 1")
    Integer sumEarnedPoints(@Param("memberId") Integer memberId);
}
