package com.itheima.pojo;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Order {

    /** items 列长度上限（与 orders.items VARCHAR(2000) 一致，见 sql/order_stock_and_images.sql） */
    public static final int ITEMS_MAX_LENGTH = 2000;

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
    @Size(max = ITEMS_MAX_LENGTH, message = "商品明细长度不能超过2000个字符")
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
