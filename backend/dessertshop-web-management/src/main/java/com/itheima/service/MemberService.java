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
     * 下单时查找或注册会员（新会员发放新人券）。不发积分——积分在订单完成时发放。
     * 失败不影响下单（实现以保存点执行）。
     */
    Member ensureMember(String customerName, String phone);

    /**
     * 订单完成时发放积分与成长值、累计消费与订单数（1元=1积分=1成长值，生日月积分翻倍），并刷新等级。
     * 幂等：该订单已发放过（含旧规则下单即发放的历史订单）则不再发放。
     * @return 本次发放的积分；订单无手机号或已发放过时返回 0
     */
    int awardOrderPoints(Order order);

    /**
     * 订单取消时扣回该订单曾发放的积分并回退成长值/累计消费/订单数。
     * 只有发放过积分的订单才扣回，且只扣回一次；积分已被消费时最多扣到 0。
     * @return 实际扣回的积分
     */
    int revokeOrderPoints(Order order);

    /** 积分兑换优惠券（需会员 ID 与手机号匹配） */
    Map<String, Object> redeemCoupon(Integer memberId, String phone, Integer templateId);

    /** 校验会员 ID 与手机号是否属于同一会员（移动端无登录体系时的最低限度归属校验） */
    boolean memberMatchesPhone(Integer memberId, String phone);

    /** 获取会员可用优惠券 */
    java.util.List<MemberCoupon> getUsableCoupons(Integer memberId);

    /** 获取会员积分日志 */
    java.util.List<PointsLog> getPointsLog(Integer memberId);

    /** 查询会员完整信息（含积分、等级、优惠券）供移动端展示 */
    Map<String, Object> getMemberProfile(String phone);
}
