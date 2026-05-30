package com.itheima.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Order {
    private Integer id;
    private String orderNo; //订单号
    private String customerName; //客户姓名
    private String phone; //联系电话
    private String items; //商品明细
    private Double amount; //订单金额
    private Integer status; //订单状态 0待支付 1待接单 2制作中 3配送中 4已完成 5已取消
    private String address; //配送地址
    private LocalDateTime createTime; //下单时间
    private LocalDateTime updateTime; //更新时间
}
