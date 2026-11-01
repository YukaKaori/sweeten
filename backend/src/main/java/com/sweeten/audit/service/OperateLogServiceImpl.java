package com.sweeten.audit.service;

import com.sweeten.audit.mapper.OperateLogMapper;
import com.sweeten.audit.model.OperateLog;
import com.sweeten.audit.model.OperateLogQueryParam;
import com.sweeten.common.paging.PageHelperUtils;
import com.sweeten.common.paging.PageResult;
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
