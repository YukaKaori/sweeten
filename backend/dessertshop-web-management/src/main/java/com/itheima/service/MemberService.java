package com.itheima.service;

import com.itheima.pojo.*;

import java.util.Map;

public interface MemberService {

    /** 分页查询会员 */
    PageResult<Member> page(MemberQueryParam param);

    /** 按ID查会员 */
    Member getById(Integer id);

    /** 按手机号查会员 */
    Member getByPhone(String phone);

    /**
     * 下单后处理会员积分与成长值
     * - 自动注册/查找会员
     * - 消费积分（1元=1积分）
     * - 成长值（1元=1成长值）
     * - 生日月双倍积分
     * - 刷新会员等级
     * @return 本次获得积分
     */
    int afterOrder(String customerName, String phone, java.math.BigDecimal amount, Integer orderId);

    /** 积分兑换优惠券 */
    Map<String, Object> redeemCoupon(Integer memberId, Integer templateId);

    /** 获取会员可用优惠券 */
    java.util.List<MemberCoupon> getUsableCoupons(Integer memberId);

    /** 获取会员积分日志 */
    java.util.List<PointsLog> getPointsLog(Integer memberId);

    /** 查询会员完整信息（含积分、等级、优惠券）供移动端展示 */
    Map<String, Object> getMemberProfile(String phone);
}
