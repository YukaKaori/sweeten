package com.itheima.pojo;

import lombok.Data;

@Data
public class MemberQueryParam {
    private Integer page = 1;
    private Integer pageSize = 10;
    private String name;
    private String phone;
    private Integer level;
}
