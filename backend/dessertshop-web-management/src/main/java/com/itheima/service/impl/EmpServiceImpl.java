package com.itheima.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.itheima.mapper.EmpExprMapper;
import com.itheima.mapper.EmpMapper;
import com.itheima.pojo.*;
import com.itheima.service.EmpLogService;
import com.itheima.service.EmpService;
import com.itheima.utils.BCryptUtils;
import com.itheima.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class EmpServiceImpl implements EmpService {

    @Autowired
    private EmpMapper empMapper;
    @Autowired
    private EmpExprMapper empExprMapper;
    @Autowired
    private EmpLogService empLogService;
    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public PageResult page(EmpQueryParam empQueryParam) {
        //1. 设置分页参数
        PageHelper.startPage(empQueryParam.getPage(), empQueryParam.getPageSize());
        //2. 执行查询
        List<Emp> empList = empMapper.list(empQueryParam);
        Page<Emp> p = (Page<Emp>) empList;
        //3. 封装结果
        return new PageResult(p.getTotal(), p.getResult());
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public void save(Emp emp) {
        try {
            //1. 加密密码
            emp.setPassword(BCryptUtils.hashPassword(emp.getPassword()));
            //2. 保存员工基本信息
            emp.setCreateTime(LocalDateTime.now());
            emp.setUpdateTime(LocalDateTime.now());
            empMapper.insert(emp);
            //3. 保存员工工作经历信息
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
        //1. 根据ID批量删除员工基本信息
        empMapper.deleteByIds(ids);
        //2. 根据员工的ID批量删除员工的工作经历信息
        empExprMapper.deleteByEmpIds(ids);
    }

    @Override
    public Emp getinfo(Integer id) {
        return empMapper.getById(id);
    }

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
    public LoginInfo login(Emp emp) {
        // 根据用户名查询员工（不再同时验证密码）
        Emp empLogin = empMapper.getByUsername(emp.getUsername());
        if (empLogin != null) {
            // 使用BCrypt验证密码
            if (BCryptUtils.verifyPassword(emp.getPassword(), empLogin.getPassword())) {
                //1. 生成JWT令牌
                Map<String, Object> dataMap = new HashMap<>();
                dataMap.put("id", empLogin.getId());
                dataMap.put("username", empLogin.getUsername());

                String jwt = jwtUtils.generateJwt(dataMap);
                LoginInfo loginInfo = new LoginInfo(empLogin.getId(), empLogin.getUsername(), empLogin.getName(), jwt);
                return loginInfo;
            }
        }
        return null;
    }

    @Override
    public Emp getProfile(Integer id) {
        return empMapper.selectById(id);
    }

    @Override
    public boolean updatePassword(Integer id, String oldPassword, String newPassword) {
        // 根据ID查询员工信息
        Emp emp = empMapper.getById(id);
        if (emp == null) {
            return false;
        }
        // 验证旧密码
        if (!BCryptUtils.verifyPassword(oldPassword, emp.getPassword())) {
            return false;
        }
        // 加密新密码并更新
        String hashedNewPassword = BCryptUtils.hashPassword(newPassword);
        int rows = empMapper.updatePassword(id, hashedNewPassword);
        return rows > 0;
    }
}
