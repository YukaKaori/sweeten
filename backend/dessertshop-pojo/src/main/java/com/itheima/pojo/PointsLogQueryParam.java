package com.itheima.pojo;

import lombok.Data;

@Data
public class PointsLogQueryParam {
    private Integer page = 1;
    private Integer pageSize = 10;
    private Integer memberId;
    private Integer type;
}
