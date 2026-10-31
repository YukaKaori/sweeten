package com.itheima.service;

import com.itheima.pojo.OperateLog;
import com.itheima.pojo.OperateLogQueryParam;
import com.itheima.pojo.PageResult;

public interface OperateLogService {

    //分页查询操作日志
    PageResult<OperateLog> page(OperateLogQueryParam queryParam);

    //根据ID查询操作日志详情
    OperateLog getById(Integer id);
}
