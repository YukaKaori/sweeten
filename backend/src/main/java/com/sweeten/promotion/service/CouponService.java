package com.sweeten.promotion.service;

import com.sweeten.promotion.model.CouponTemplate;

import java.util.List;

public interface CouponService {

    /** 查询所有启用的券模板（供管理后台/积分兑换列表） */
    List<CouponTemplate> listActive();

    /** 按ID查模板 */
    CouponTemplate getTemplate(Integer id);

    /** 创建券模板 */
    void createTemplate(CouponTemplate template);

    /** 发放优惠券给会员 */
    void issueCoupon(Integer memberId, Integer templateId);
}
