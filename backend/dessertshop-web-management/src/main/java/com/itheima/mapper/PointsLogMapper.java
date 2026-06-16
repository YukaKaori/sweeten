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

    @Select("SELECT COALESCE(SUM(points), 0) FROM points_log WHERE member_id = #{memberId} AND type = 1")
    Integer sumEarnedPoints(@Param("memberId") Integer memberId);
}
