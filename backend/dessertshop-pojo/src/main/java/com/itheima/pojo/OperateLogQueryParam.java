package com.itheima.pojo;

import lombok.Data;

import jakarta.validation.constraints.Max;

@Data
public class OperateLogQueryParam {
    private Integer page = 1; //页码
    @Max(value = 100, message = "每页最多100条")
    private Integer pageSize = 10; //每页展示记录数
    private String className; //操作类名
    private String methodName; //操作方法名
}
