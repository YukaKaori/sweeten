package com.itheima.pojo;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Customer {
    private Integer id;

    @NotBlank(message = "客户姓名不能为空")
    @Size(max = 30, message = "姓名长度不能超过30个字符")
    private String name;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    private Integer gender;
    private String address;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
