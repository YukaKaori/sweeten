package com.itheima.mapper;

import com.itheima.pojo.CouponTemplate;
import com.itheima.pojo.MemberCoupon;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CouponMapper {

    // ═══ 模板 ═══

    @Select("SELECT * FROM coupon_template WHERE status = 1 ORDER BY type, threshold")
    List<CouponTemplate> listActive();

    @Select("SELECT * FROM coupon_template WHERE id = #{id}")
    CouponTemplate getTemplateById(Integer id);

    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert("INSERT INTO coupon_template(name, type, threshold, discount, reduce_amount, valid_days, total_stock, issued_count, points_cost, status, created_at) " +
            "VALUES(#{name}, #{type}, #{threshold}, #{discount}, #{reduceAmount}, #{validDays}, #{totalStock}, #{issuedCount}, #{pointsCost}, #{status}, #{createdAt})")
    void insertTemplate(CouponTemplate template);

    @Update("UPDATE coupon_template SET issued_count = issued_count + 1 WHERE id = #{id}")
    int incrIssuedCount(Integer id);

    // ═══ 会员持有券 ═══

    /** 查会员的券（含模板信息） */
    List<MemberCoupon> listByMemberId(@Param("memberId") Integer memberId);

    /** 查会员未使用的有效券（下单时可用） */
    List<MemberCoupon> listUsable(@Param("memberId") Integer memberId);

    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert("INSERT INTO member_coupon(member_id, template_id, status, acquired_at, expire_at) " +
            "VALUES(#{memberId}, #{templateId}, #{status}, #{acquiredAt}, #{expireAt})")
    void insertMemberCoupon(MemberCoupon mc);

    /** 使用优惠券 */
    @Update("UPDATE member_coupon SET status = 2, used_at = NOW(), order_id = #{orderId} WHERE id = #{id} AND status = 1")
    int useCoupon(@Param("id") Integer id, @Param("orderId") Integer orderId);

    /** 检查是否已领取过某模板（防止重复领） */
    @Select("SELECT COUNT(*) FROM member_coupon WHERE member_id = #{memberId} AND template_id = #{templateId} AND status = 1")
    int countByMemberAndTemplate(@Param("memberId") Integer memberId, @Param("templateId") Integer templateId);
}
