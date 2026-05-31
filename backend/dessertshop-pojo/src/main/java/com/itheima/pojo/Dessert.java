package com.itheima.pojo;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Dessert {
    private Integer id;

    @NotBlank(message = "甜品名称不能为空")
    @Size(max = 100, message = "甜品名称长度不能超过100个字符")
    private String name;

    @NotBlank(message = "分类不能为空")
    @Pattern(regexp = "^(cake|bread|drink|dessert|icecream)$", message = "分类值无效")
    private String category;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于0")
    private Double price;

    @DecimalMin(value = "0.01", message = "原价必须大于0")
    private Double originalPrice;

    private String image;

    @Min(value = 0, message = "销量不能为负数")
    private Integer sales;

    @Min(value = 0, message = "库存不能为负数")
    private Integer stock;

    @Min(value = 0, message = "状态值无效")
    @Max(value = 1, message = "状态值无效")
    private Integer status;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
