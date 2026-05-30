package com.itheima.pojo;

import lombok.Data;

@Data
public class DessertQueryParam {
    private Integer page = 1; //页码
    private Integer pageSize = 10; //每页展示记录数
    private String name; //甜品名称
    private String category; //分类
}
