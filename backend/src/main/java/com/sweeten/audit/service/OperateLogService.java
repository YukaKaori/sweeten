package com.sweeten.audit.service;

import com.sweeten.audit.model.OperateLog;
import com.sweeten.audit.model.OperateLogQueryParam;
import com.sweeten.common.paging.PageResult;

public interface OperateLogService {

    //分页查询操作日志
    PageResult<OperateLog> page(OperateLogQueryParam queryParam);

    //根据ID查询操作日志详情
    OperateLog getById(Integer id);
}
