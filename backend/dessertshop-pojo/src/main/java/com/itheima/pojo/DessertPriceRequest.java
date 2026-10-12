package com.itheima.pojo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 调价请求 — 仅携带价格字段，避免价格页整对象覆写甜品其它信息
 */
@Data
public class DessertPriceRequest {

    @NotNull(message = "甜品ID不能为空")
    private Integer id;

    @NotNull(message = "现价不能为空")
    @DecimalMin(value = "0.01", message = "现价必须大于0")
    private Double price;

    @DecimalMin(value = "0.01", message = "原价必须大于0")
    private Double originalPrice;
}
