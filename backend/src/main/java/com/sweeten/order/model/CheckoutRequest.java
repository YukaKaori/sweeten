package com.sweeten.order.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

/**
 * 顾客商城结算请求（POST /mobile/orders）。
 * <p>
 * 金额与单价只用于核对：服务端按商品库当前价格重算，不一致即拒单，见 CheckoutServiceImpl。
 */
@Data
public class CheckoutRequest {

    @NotBlank(message = "客户姓名不能为空")
    @Size(max = 50, message = "客户姓名长度不能超过50个字符")
    private String customerName;

    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "配送地址不能为空")
    @Size(max = 200, message = "配送地址长度不能超过200个字符")
    private String address;

    @NotNull(message = "商品明细不能为空")
    @Size(min = 1, message = "至少选择一件商品")
    @Valid
    private List<OrderLine> items;

    @NotNull(message = "订单金额不能为空")
    @DecimalMin(value = "0.01", message = "订单金额必须大于0")
    private Double totalAmount;
}
