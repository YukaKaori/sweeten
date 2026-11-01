package com.sweeten.staff.service;

import com.sweeten.common.paging.PageResult;
import com.sweeten.staff.model.Emp;
import com.sweeten.staff.model.EmpQueryParam;

import java.util.List;

/**
 * 员工档案。登录与修改密码见 identity 模块的 AuthService。
 */
public interface EmpService {

    PageResult page(EmpQueryParam empQueryParam);

    void save(Emp emp);

    void deleteByIds(List<Integer> ids);

    Emp getinfo(Integer id);

    void update(Emp emp);

    Emp getProfile(Integer id);
}
