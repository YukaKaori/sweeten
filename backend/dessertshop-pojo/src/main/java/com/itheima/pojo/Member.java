package com.itheima.pojo;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class Member {
    private Integer id;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 50)
    private String name;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 等级 0=普通 1=银卡 2=金卡 3=钻石 */
    private Integer level;

    /** 当前可用积分 */
    private Integer points;

    /** 累计获得积分 */
    private Integer totalPoints;

    /** 成长值（决定等级） */
    private Integer growth;

    /** 累计消费金额 */
    private BigDecimal totalSpent;

    /** 累计订单数 */
    private Integer orderCount;

    /** 生日 */
    private LocalDate birthday;

    private LocalDateTime registeredAt;
    private LocalDateTime updatedAt;
}
