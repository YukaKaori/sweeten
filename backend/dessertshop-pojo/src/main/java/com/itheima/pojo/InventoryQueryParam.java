package com.itheima.pojo;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class InventoryQueryParam {
    @Min(value = 1, message = "页码必须大于0")
    private Integer page = 1;

    @Min(value = 1, message = "每页条数必须大于0")
    private Integer pageSize = 10;

    private String name;
}
