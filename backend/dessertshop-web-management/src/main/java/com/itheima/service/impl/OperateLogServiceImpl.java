package com.itheima.service.impl;

import com.itheima.mapper.OperateLogMapper;
import com.itheima.pojo.OperateLog;
import com.itheima.pojo.OperateLogQueryParam;
import com.itheima.pojo.PageResult;
import com.itheima.service.OperateLogService;
import com.itheima.util.PageHelperUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OperateLogServiceImpl implements OperateLogService {

    private final OperateLogMapper operateLogMapper;

    @Override
    public PageResult<OperateLog> page(OperateLogQueryParam queryParam) {
        return PageHelperUtils.executePage(queryParam.getPage(), queryParam.getPageSize(),
                () -> operateLogMapper.list(queryParam));
    }

    @Override
    public OperateLog getById(Integer id) {
        return operateLogMapper.getById(id);
    }
}
