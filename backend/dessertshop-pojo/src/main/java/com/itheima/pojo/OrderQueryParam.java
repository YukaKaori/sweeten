package com.itheima.pojo;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.Max;
import java.time.LocalDate;

@Data
public class OrderQueryParam {
    private Integer page = 1; //页码
    @Max(value = 100, message = "每页最多100条")
    private Integer pageSize = 10; //每页展示记录数
    private String orderNo; //订单号
    private String customerName; //客户姓名
    private Integer status; //订单状态
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate begin; //开始日期
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate end; //结束日期
}
