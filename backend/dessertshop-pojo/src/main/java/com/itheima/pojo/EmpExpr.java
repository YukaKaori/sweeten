package com.itheima.pojo;

import lombok.Data;

import java.time.LocalDate;

@Data
public class EmpExpr {
    private Integer id;
    private Integer empId;
    private LocalDate begin;
    private LocalDate end;
    private String company;
    private String job;
}
