package com.itheima.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MemberCoupon {
    private Integer id;
    private Integer memberId;
    private Integer templateId;

    /** 状态 1=未使用 2=已使用 3=已过期 */
    private Integer status;

    private LocalDateTime acquiredAt;
    private LocalDateTime usedAt;
    private LocalDateTime expireAt;
    private Integer orderId;

    // ── 关联展示字段（非表字段） ──
    /** 券名称（来自模板） */
    private String couponName;
    /** 券类型 */
    private Integer couponType;
    /** 门槛 */
    private java.math.BigDecimal threshold;
    /** 折扣率 */
    private java.math.BigDecimal discount;
    /** 减免金额 */
    private java.math.BigDecimal reduceAmount;
}
