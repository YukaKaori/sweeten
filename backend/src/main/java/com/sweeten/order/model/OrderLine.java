package com.sweeten.order.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 订单明细行。
 * <p>
 * 两种用途、同一结构：顾客结算时提交的购物车行（{@link CheckoutRequest#getItems()}，价格仅用于核对），
 * 以及下单成功后以 JSON 数组存入 orders.items 的明细快照（商品名、单价取自下单时的商品库）。
 * 字段名 productId / productName / price / qty 是已存数据与前端解析的约定，不可改名。
 */
@Data
public class OrderLine {

    @NotNull
    private Integer productId;

    @NotBlank
    private String productName;

    @NotNull
    @DecimalMin(value = "0.01")
    private Double price;

    @Min(1)
    private Integer qty;
}
