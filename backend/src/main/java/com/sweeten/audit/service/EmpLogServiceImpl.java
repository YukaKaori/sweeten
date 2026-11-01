package com.sweeten.audit.service;

import com.sweeten.audit.mapper.EmpLogMapper;
import com.sweeten.audit.model.EmpLog;
import com.sweeten.common.paging.PageHelperUtils;
import com.sweeten.common.paging.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmpLogServiceImpl implements EmpLogService {

    private final EmpLogMapper empLogMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void insertLog(EmpLog empLog) {
        empLogMapper.insert(empLog);
    }

    @Override
    public PageResult<EmpLog> page(Integer page, Integer pageSize) {
        return PageHelperUtils.executePage(page, pageSize,
                () -> empLogMapper.list());
    }
}
