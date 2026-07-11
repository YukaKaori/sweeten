package com.itheima.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 系统权限
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SysPermission {
    private Integer id;
    private String code;   //权限码, 格式: 模块:资源:操作, 如 dessert:dessert:update
    private String name;   //权限名称
    private String module; //所属模块
    private LocalDateTime createTime;
}
