package com.itheima.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Dessert {
    private Integer id;
    private String name; //甜品名称
    private String category; //分类 cake/bread/drink/dessert/icecream
    private Double price; //现价
    private Double originalPrice; //原价
    private String image; //图片URL
    private Integer sales; //月销量
    private Integer status; //状态 1上架 0下架
    private LocalDateTime createTime; //创建时间
    private LocalDateTime updateTime; //更新时间
}
