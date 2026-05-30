package com.itheima.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.itheima.mapper.OperateLogMapper;
import com.itheima.pojo.OperateLog;
import com.itheima.pojo.OperateLogQueryParam;
import com.itheima.pojo.PageResult;
import com.itheima.service.OperateLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OperateLogServiceImpl implements OperateLogService {

    @Autowired
    private OperateLogMapper operateLogMapper;

    @Override
    public PageResult<OperateLog> page(OperateLogQueryParam queryParam) {
        //1. 设置分页参数
        PageHelper.startPage(queryParam.getPage(), queryParam.getPageSize());
        //2. 执行查询
        List<OperateLog> logList = operateLogMapper.list(queryParam);
        Page<OperateLog> p = (Page<OperateLog>) logList;
        //3. 封装结果
        return new PageResult(p.getTotal(), p.getResult());
    }

    @Override
    public OperateLog getById(Integer id) {
        return operateLogMapper.getById(id);
    }
}
