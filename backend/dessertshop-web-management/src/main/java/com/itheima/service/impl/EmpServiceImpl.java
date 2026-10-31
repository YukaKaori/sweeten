package com.itheima.service.impl;

import com.itheima.aop.PermissionAspect;
import com.itheima.context.UserRoleLoader;
import com.itheima.exception.BusinessException;
import com.itheima.mapper.EmpExprMapper;
import com.itheima.mapper.EmpMapper;
import com.itheima.mapper.SysRoleMapper;
import com.itheima.pojo.*;
import com.itheima.service.EmpLogService;
import com.itheima.service.EmpService;
import com.itheima.util.PageHelperUtils;
import com.itheima.utils.BCryptUtils;
import com.itheima.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class EmpServiceImpl implements EmpService {

    private final EmpMapper empMapper;
    private final EmpExprMapper empExprMapper;
    private final EmpLogService empLogService;
    private final JwtUtils jwtUtils;
    private final SysRoleMapper sysRoleMapper;
    private final UserRoleLoader userRoleLoader;

    @Override
    public PageResult page(EmpQueryParam empQueryParam) {
        return PageHelperUtils.executePage(empQueryParam.getPage(), empQueryParam.getPageSize(),
                () -> empMapper.list(empQueryParam));
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
            //3. 绑定默认角色（灰度期默认 super_admin, 与存量员工行为一致; 灰度退出后应改为按 job 映射）
            Integer defaultRoleId = sysRoleMapper.selectRoleIdByCode(PermissionAspect.SUPER_ADMIN);
            if (defaultRoleId != null) {
                sysRoleMapper.insertUserRole(emp.getId(), defaultRoleId);
            }
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
        //3. 清理员工的角色绑定
        sysRoleMapper.deleteUserRolesByUserIds(ids);
        //4. 已登录的被删员工立即失去访问权（TokenFilter 按库校验账号）
        userRoleLoader.evict(ids);
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
    public LoginInfo login(Emp emp) {
        // 根据用户名查询员工（不再同时验证密码）
        Emp empLogin = empMapper.getByUsername(emp.getUsername());
        if (empLogin != null) {
            // 使用BCrypt验证密码
            if (BCryptUtils.verifyPassword(emp.getPassword(), empLogin.getPassword())) {
                //1. 查询用户角色与权限（角色进 token; 权限码仅随登录响应下发, 避免 token 膨胀）
                List<String> roles = sysRoleMapper.selectRoleCodesByUserId(empLogin.getId());
                List<String> permissions = roles.stream()
                        .flatMap(role -> sysRoleMapper.selectPermissionCodesByRoleCode(role).stream())
                        .distinct()
                        .toList();

                //2. 生成JWT令牌
                Map<String, Object> dataMap = new HashMap<>();
                dataMap.put("id", empLogin.getId());
                dataMap.put("username", empLogin.getUsername());
                dataMap.put("roles", roles);

                String jwt = jwtUtils.generateJwt(dataMap);
                return new LoginInfo(empLogin.getId(), empLogin.getUsername(), empLogin.getName(), jwt,
                        empLogin.getImage(), roles, permissions);
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
