package com.sweeten.identity.service;

import com.sweeten.identity.mapper.SysRoleMapper;
import com.sweeten.identity.security.PermissionAspect;
import com.sweeten.identity.security.UserRoleLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleAssignmentServiceImpl implements RoleAssignmentService {

    private final SysRoleMapper sysRoleMapper;
    private final UserRoleLoader userRoleLoader;

    /**
     * 灰度期默认绑定 super_admin，与存量员工行为一致。
     * 注意：manager 角色拥有 emp:emp:create，灰度退出前必须把这里改为按 job 映射（见 sql/schema/rbac_init.sql 文末），
     * 否则新增员工即可获得超级管理员权限。
     */
    @Override
    public void assignDefaultRole(Integer userId) {
        Integer defaultRoleId = sysRoleMapper.selectRoleIdByCode(PermissionAspect.SUPER_ADMIN);
        if (defaultRoleId != null) {
            sysRoleMapper.insertUserRole(userId, defaultRoleId);
        }
    }

    @Override
    public void revokeAll(List<Integer> userIds) {
        sysRoleMapper.deleteUserRolesByUserIds(userIds);
        // 已登录的被删员工立即失去访问权（TokenFilter 按库校验账号）
        userRoleLoader.evict(userIds);
    }
}
