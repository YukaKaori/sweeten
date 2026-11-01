package com.sweeten.audit.service;

import com.sweeten.audit.model.EmpLog;
import com.sweeten.common.paging.PageResult;

public interface EmpLogService {
    public void insertLog(EmpLog empLog);

    //分页查询员工日志
    PageResult<EmpLog> page(Integer page, Integer pageSize);
}
