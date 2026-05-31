package com.itheima.pojo;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Order {
    private Integer id;

    @NotBlank(message = "订单号不能为空")
    @Size(max = 32, message = "订单号长度不能超过32个字符")
    private String orderNo;

    @NotBlank(message = "客户姓名不能为空")
    @Size(max = 50, message = "客户姓名长度不能超过50个字符")
    private String customerName;

    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "商品明细不能为空")
    @Size(max = 500, message = "商品明细长度不能超过500个字符")
    private String items;

    @NotNull(message = "订单金额不能为空")
    @DecimalMin(value = "0.01", message = "订单金额必须大于0")
    private Double amount;

    @Min(value = 0, message = "订单状态值无效")
    @Max(value = 5, message = "订单状态值无效")
    private Integer status;

    @Size(max = 200, message = "配送地址长度不能超过200个字符")
    private String address;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
