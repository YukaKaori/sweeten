package com.itheima.mapper;

import com.itheima.pojo.Member;
import com.itheima.pojo.MemberQueryParam;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface MemberMapper {

    // ── 分页查询 ──
    List<Member> list(MemberQueryParam param);

    // ── 按ID查 ──
    @Select("SELECT * FROM member WHERE id = #{id}")
    Member getById(Integer id);

    // ── 按手机号查（会员唯一标识） ──
    @Select("SELECT * FROM member WHERE phone = #{phone}")
    Member getByPhone(String phone);

    // ── 注册新会员 ──
    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert("INSERT INTO member(name, phone, level, points, total_points, growth, total_spent, order_count, birthday, registered_at, updated_at) " +
            "VALUES(#{name}, #{phone}, #{level}, #{points}, #{totalPoints}, #{growth}, #{totalSpent}, #{orderCount}, #{birthday}, #{registeredAt}, #{updatedAt})")
    void insert(Member member);

    // ── 更新会员（积分/等级/消费） ──
    void updateById(Member member);

    // ── 增加积分 ──
    @Update("UPDATE member SET points = points + #{points}, total_points = total_points + #{points}, " +
            "growth = growth + #{growth}, total_spent = total_spent + #{amount}, " +
            "order_count = order_count + 1, updated_at = NOW() WHERE id = #{id}")
    int addPointsAndGrowth(@Param("id") Integer id, @Param("points") Integer points,
                           @Param("growth") Integer growth, @Param("amount") java.math.BigDecimal amount);

    // ── 扣除积分 ──
    @Update("UPDATE member SET points = points - #{points}, updated_at = NOW() WHERE id = #{id} AND points >= #{points}")
    int deductPoints(@Param("id") Integer id, @Param("points") Integer points);

    // ── 根据成长值刷新等级 ──
    @Update("UPDATE member SET level = CASE " +
            "WHEN growth >= 10000 THEN 3 " +
            "WHEN growth >= 3000  THEN 2 " +
            "WHEN growth >= 1000  THEN 1 " +
            "ELSE 0 END, updated_at = NOW() WHERE id = #{id}")
    void refreshLevel(@Param("id") Integer id);

    // ── 统计 ──
    @Select("SELECT COUNT(*) FROM member")
    Long countAll();

    @Select("SELECT COUNT(*) FROM member WHERE level >= #{level}")
    Long countByLevel(@Param("level") Integer level);
}
