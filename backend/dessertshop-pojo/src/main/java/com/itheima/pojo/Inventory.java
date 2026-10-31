package com.itheima.pojo;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class Inventory {
    private Integer id;

    @NotBlank(message = "物料名称不能为空")
    @Size(max = 30, message = "物料名称长度不能超过30个字符")
    private String name;

    @NotBlank(message = "分类不能为空")
    private String category;

    @NotNull(message = "库存数量不能为空")
    @Min(value = 0, message = "库存不能为负数")
    private Integer stock;

    @NotBlank(message = "单位不能为空")
    private String unit;

    @NotNull(message = "安全阈值不能为空")
    @Min(value = 0, message = "安全阈值不能为负数")
    private Integer safetyThreshold;

    private String supplier;
    private LocalDate expiryDate;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
