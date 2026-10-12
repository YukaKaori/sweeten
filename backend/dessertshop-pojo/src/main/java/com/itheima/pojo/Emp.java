package com.itheima.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class Emp {
    private Integer id;

    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 20, message = "用户名长度必须在4-20个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    private String username;

    // 只写不读：新增员工时可提交，任何响应（员工列表/详情/个人资料）都不回传密码哈希；也不进 toString（员工日志）
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @ToString.Exclude
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20个字符之间")
    private String password;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 20, message = "姓名长度不能超过20个字符")
    private String name;

    @NotNull(message = "性别不能为空")
    @Min(value = 1, message = "性别值无效")
    @Max(value = 2, message = "性别值无效")
    private Integer gender;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    private Integer job;
    private Integer salary;
    private String image;
    private LocalDate entryDate;
    private Integer deptId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // 封装部门名称
    private String deptName;
    // 封装工作经历信息
    private List<EmpExpr> exprList;
}
