package com.sweeten.staff.service;

import com.sweeten.audit.model.EmpLog;
import com.sweeten.audit.service.EmpLogService;
import com.sweeten.common.exception.BusinessException;
import com.sweeten.common.paging.PageHelperUtils;
import com.sweeten.common.paging.PageResult;
import com.sweeten.identity.security.BCryptUtils;
import com.sweeten.identity.service.RoleAssignmentService;
import com.sweeten.staff.mapper.EmpExprMapper;
import com.sweeten.staff.mapper.EmpMapper;
import com.sweeten.staff.model.Emp;
import com.sweeten.staff.model.EmpExpr;
import com.sweeten.staff.model.EmpQueryParam;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 员工档案（基本信息、部门、工作经历）。
 * 登录、改密码与角色绑定属于 identity 模块：本类只在新增 / 删除员工时调用 {@link RoleAssignmentService}。
 */
@Service
@RequiredArgsConstructor
public class EmpServiceImpl implements EmpService {

    private final EmpMapper empMapper;
    private final EmpExprMapper empExprMapper;
    private final EmpLogService empLogService;
    private final RoleAssignmentService roleAssignmentService;

    @Override
    public PageResult page(EmpQueryParam empQueryParam) {
        return PageHelperUtils.executePage(empQueryParam.getPage(), empQueryParam.getPageSize(),
                () -> empMapper.list(empQueryParam));
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public void save(Emp emp) {
        try {
            //1. 加密初始密码
            emp.setPassword(BCryptUtils.hashPassword(emp.getPassword()));
            //2. 保存员工基本信息
            emp.setCreateTime(LocalDateTime.now());
            emp.setUpdateTime(LocalDateTime.now());
            empMapper.insert(emp);
            //3. 绑定默认角色（策略见 RoleAssignmentServiceImpl）
            roleAssignmentService.assignDefaultRole(emp.getId());
            //4. 保存员工工作经历信息
            Integer empId = emp.getId();
            List<EmpExpr> exprList = emp.getExprList();
            if (!CollectionUtils.isEmpty(exprList)) {
                exprList.forEach(empExpr -> empExpr.setEmpId(empId));
                empExprMapper.insertBatch(exprList);
            }
        } finally {
            EmpLog empLog = new EmpLog(null, LocalDateTime.now(), emp.toString());
            empLogService.insertLog(empLog);
        }
    }

    @Transactional
    @Override
    public void deleteByIds(List<Integer> ids) {
        // 空列表会生成 "delete ... where id in " 这样的非法 SQL
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("请选择要删除的员工");
        }
        //1. 根据ID批量删除员工基本信息
        empMapper.deleteByIds(ids);
        //2. 根据员工的ID批量删除员工的工作经历信息
        empExprMapper.deleteByEmpIds(ids);
        //3. 解除角色绑定，已登录的被删员工立即失去访问权
        roleAssignmentService.revokeAll(ids);
    }

    @Override
    public Emp getinfo(Integer id) {
        return empMapper.getById(id);
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public void update(Emp emp) {
        //1. 根据ID更新员工基本信息
        emp.setUpdateTime(LocalDateTime.now());
        empMapper.updateById(emp);
        //2. 根据员工ID删除员工的工作经历信息 【删除老的】
        empExprMapper.deleteByEmpIds(Arrays.asList(emp.getId()));
        //3. 新增员工的工作经历数据 【新增新的】
        Integer empId = emp.getId();
        List<EmpExpr> exprList = emp.getExprList();
        if (!CollectionUtils.isEmpty(exprList)) {
            exprList.forEach(empExpr -> empExpr.setEmpId(empId));
            empExprMapper.insertBatch(exprList);
        }
    }

    @Override
    public Emp getProfile(Integer id) {
        return empMapper.selectById(id);
    }
}
