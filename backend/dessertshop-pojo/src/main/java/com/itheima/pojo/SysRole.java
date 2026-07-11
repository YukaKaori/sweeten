package com.itheima.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 系统角色
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SysRole {
    private Integer id;
    private String code;        //角色码: super_admin/manager/cashier/warehouse/finance
    private String name;        //角色名称
    private String description; //角色说明
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
