package com.itheima.service;

import com.itheima.pojo.EmpLog;
import com.itheima.pojo.PageResult;

public interface EmpLogService {
    public void insertLog(EmpLog empLog);

    //分页查询员工日志
    PageResult<EmpLog> page(Integer page, Integer pageSize);
}
