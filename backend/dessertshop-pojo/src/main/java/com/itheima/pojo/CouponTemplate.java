package com.itheima.pojo;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CouponTemplate {
    private Integer id;

    @NotBlank(message = "券名称不能为空")
    @Size(max = 50)
    private String name;

    /** 类型 1=满减券 2=折扣券 3=现金券 */
    @NotNull
    @Min(1) @Max(3)
    private Integer type;

    /** 使用门槛金额 */
    private BigDecimal threshold;

    /** 折扣率 (0.85=85折) */
    @DecimalMin("0.01") @DecimalMax("0.99")
    private BigDecimal discount;

    /** 减免金额（满减券/现金券用） */
    @DecimalMin("0.01")
    private BigDecimal reduceAmount;

    /** 自领取起有效天数 */
    @Min(1)
    private Integer validDays;

    /** 总库存 -1=不限 */
    private Integer totalStock;

    private Integer issuedCount;

    /** 兑换所需积分 0=免费 */
    @Min(0)
    private Integer pointsCost;

    /** 状态 1=启用 0=停用 */
    private Integer status;

    private LocalDateTime createdAt;
}
